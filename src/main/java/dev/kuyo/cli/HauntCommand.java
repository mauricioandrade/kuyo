package dev.kuyo.cli;

import dev.kuyo.assombracao.Assombracao;
import dev.kuyo.assombracao.Assombracao.Sussurro;
import dev.kuyo.epitafio.Lingua;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "haunt",
        description = {"Assombra o shell: os arquivos que você apaga sussurram de volta.",
                "Instale com: eval \"$(kuyo haunt)\" no ~/.bashrc ou ~/.zshrc."})
public class HauntCommand implements Callable<Integer> {

    enum Shell { bash, zsh }

    @Parameters(index = "0", arity = "0..1",
            description = "Uma linha de comando que acabou de rodar. Sem ela, imprime o hook do shell.")
    String linha;

    @Option(names = "--shell", description = "Shell do hook: ${COMPLETION-CANDIDATES}. Padrão: o do $SHELL.")
    Shell shell;

    @Option(names = "--lang", defaultValue = "en", description = "Língua dos sussurros: ${COMPLETION-CANDIDATES}.")
    Lingua lingua;

    @Option(names = "--sem-monge", description = "Só frases fixas, sem chamar o modelo.")
    boolean semMonge;

    @Option(names = "--comando", description = "Como o hook chama o Kuyō. Padrão: este mesmo java e jar.")
    String comando;

    @Inject
    Assombracao assombracao;

    @Override
    public Integer call() {
        if (linha == null) {
            System.out.print(hook(shell != null ? shell : shellDoUsuario(), invocacao()));
            return 0;
        }
        // um fantasma nunca mostra stack trace: fora de um repositório, ou com o git confuso, só fica quieto
        List<Sussurro> sussurros;
        try {
            sussurros = assombracao.ouvir(Path.of("").toAbsolutePath(), linha, lingua, !semMonge);
        } catch (Exception e) {
            if (System.getenv("KUYO_DEBUG") != null) {
                e.printStackTrace();
            }
            return 0;
        }
        if (!sussurros.isEmpty()) {
            System.out.print(desenhar(sussurros, System.getenv("NO_COLOR") == null));
            System.out.flush();
        }
        return 0;
    }

    static String desenhar(List<Sussurro> sussurros, boolean cor) {
        String voz = cor ? "\033[3;38;5;141m" : "";
        String nota = cor ? "\033[2m" : "";
        String fim = cor ? "\033[0m" : "";
        StringBuilder sb = new StringBuilder("\n");
        for (Sussurro s : sussurros) {
            sb.append("  ").append(voz).append("霊 ").append(s.fala()).append(fim).append('\n');
            if (s.nota() != null) {
                sb.append("     ").append(nota).append(s.nota()).append(fim).append('\n');
            }
        }
        return sb.toString();
    }

    private String invocacao() {
        if (comando != null) {
            return comando;
        }
        String classpath = System.getProperty("java.class.path", "");
        if (!classpath.endsWith(".jar") || classpath.contains(java.io.File.pathSeparator)) {
            return "kuyo";
        }
        String java = ProcessHandle.current().info().command().orElse("java");
        // o log do Quarkus no terminal estragaria o sussurro
        return String.join(" ", aspas(java), "-Dquarkus.log.console.enable=false", "-jar",
                aspas(Path.of(classpath).toAbsolutePath().toString()));
    }

    private static Shell shellDoUsuario() {
        String s = System.getenv().getOrDefault("SHELL", "");
        return s.endsWith("zsh") ? Shell.zsh : Shell.bash;
    }

    static String aspas(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    String hook(Shell shell, String invocacao) {
        List<String> opcoes = new ArrayList<>(List.of("haunt", "--lang", lingua.name()));
        if (semMonge) {
            opcoes.add("--sem-monge");
        }
        // o filtro é largo e barato; quem decide se alguém morreu é o Kuyō, em segundo plano,
        // para o prompt voltar na hora e o sussurro chegar alguns segundos depois
        String comum = """
                # 供養 kuyo haunt: the files you delete whisper back.
                __kuyo_sussurro() {
                  case "$1" in
                    *rm\\ *|*git*commit*|*git*reset*|*git*clean*|*git*push*|*git*branch*|*git*stash*|*git*checkout*|*git*restore*) ;;
                    *) return ;;
                  esac
                  ( %s %s -- "$1" & ) 2>/dev/null
                }
                """.formatted(invocacao, String.join(" ", opcoes));
        return comum + switch (shell) {
            case bash -> """
                    __kuyo_ultimo=$(HISTTIMEFORMAT= history 1)
                    __kuyo_precmd() {
                      local s=$? h
                      h=$(HISTTIMEFORMAT= history 1)
                      if [ "$h" != "$__kuyo_ultimo" ]; then
                        __kuyo_ultimo=$h
                        __kuyo_sussurro "${h#"${h%%[![:space:][:digit:]*]*}"}"
                      fi
                      return $s
                    }
                    case ";${PROMPT_COMMAND};" in
                      *";__kuyo_precmd;"*) ;;
                      *) PROMPT_COMMAND="__kuyo_precmd${PROMPT_COMMAND:+;$PROMPT_COMMAND}" ;;
                    esac
                    """;
            case zsh -> """
                    __kuyo_preexec() { __kuyo_linha=$1 }
                    __kuyo_precmd() {
                      local s=$?
                      [[ -n $__kuyo_linha ]] && __kuyo_sussurro "$__kuyo_linha"
                      __kuyo_linha=
                      return $s
                    }
                    autoload -Uz add-zsh-hook
                    add-zsh-hook preexec __kuyo_preexec
                    add-zsh-hook precmd __kuyo_precmd
                    """;
        };
    }
}
