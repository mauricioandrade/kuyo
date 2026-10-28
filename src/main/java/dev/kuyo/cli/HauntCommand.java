package dev.kuyo.cli;

import dev.kuyo.epitaph.Language;
import dev.kuyo.haunt.Haunting;
import dev.kuyo.haunt.Haunting.Whisper;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "haunt",
        description = {"Haunts your shell: the files you delete whisper back.",
                "Install with: kuyo haunt >> ~/.bashrc (or ~/.zshrc)."})
public class HauntCommand implements Callable<Integer> {

    enum Shell { bash, zsh }

    @Parameters(index = "0", arity = "0..1",
            description = "A command line that just ran. Without it, prints the shell hook.")
    String line;

    @Option(names = "--shell", description = "Shell of the hook: ${COMPLETION-CANDIDATES}. Default: the one in $SHELL.")
    Shell shell;

    @Option(names = "--lang", defaultValue = "en", description = "Language of the whispers: ${COMPLETION-CANDIDATES}.")
    Language language;

    @Option(names = "--no-monk", description = "Fixed lines only, without calling the model.")
    boolean noMonk;

    @Option(names = "--command", description = "How the hook calls Kuyō. Default: this same java and jar.")
    String command;

    @Inject
    Haunting haunting;

    @Override
    public Integer call() {
        if (line == null) {
            System.out.print(hook(shell != null ? shell : userShell(), invocation()));
            return 0;
        }
        // a ghost never shows a stack trace: outside a repository, or with git confused, it just stays quiet
        List<Whisper> whispers;
        try {
            whispers = haunting.listen(Path.of("").toAbsolutePath(), line, language, !noMonk);
        } catch (Exception e) {
            if (System.getenv("KUYO_DEBUG") != null) {
                e.printStackTrace();
            }
            return 0;
        }
        if (!whispers.isEmpty()) {
            System.out.print(draw(whispers, System.getenv("NO_COLOR") == null));
            System.out.flush();
        }
        return 0;
    }

    static String draw(List<Whisper> whispers, boolean color) {
        String voice = color ? "\033[3;38;5;141m" : "";
        String dim = color ? "\033[2m" : "";
        String reset = color ? "\033[0m" : "";
        StringBuilder sb = new StringBuilder("\n");
        for (Whisper w : whispers) {
            sb.append("  ").append(voice).append("霊 ").append(w.words()).append(reset).append('\n');
            if (w.note() != null) {
                sb.append("     ").append(dim).append(w.note()).append(reset).append('\n');
            }
        }
        return sb.toString();
    }

    private String invocation() {
        if (command != null) {
            return command;
        }
        String classpath = System.getProperty("java.class.path", "");
        if (!classpath.endsWith(".jar") || classpath.contains(java.io.File.pathSeparator)) {
            return "kuyo";
        }
        String java = ProcessHandle.current().info().command().orElse("java");
        // Quarkus logging on the terminal would spoil the whisper
        return String.join(" ", quote(java), "-Dquarkus.log.console.enable=false", "-jar",
                quote(Path.of(classpath).toAbsolutePath().toString()));
    }

    private static Shell userShell() {
        String s = System.getenv().getOrDefault("SHELL", "");
        return s.endsWith("zsh") ? Shell.zsh : Shell.bash;
    }

    static String quote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }

    String hook(Shell shell, String invocation) {
        List<String> options = new ArrayList<>(List.of("haunt", "--lang", language.name()));
        if (noMonk) {
            options.add("--no-monk");
        }
        // the filter is wide and cheap; Kuyō decides whether anyone died, in the background,
        // so the prompt comes back at once and the whisper arrives a few seconds later
        String common = """
                # 供養 kuyo haunt: the files you delete whisper back.
                __kuyo_whisper() {
                  case "$1" in
                    *rm\\ *|*git*commit*|*git*reset*|*git*clean*|*git*push*|*git*branch*|*git*stash*|*git*checkout*|*git*restore*) ;;
                    *) return ;;
                  esac
                  ( %s %s -- "$1" & ) 2>/dev/null
                }
                """.formatted(invocation, String.join(" ", options));
        return common + switch (shell) {
            case bash -> """
                    __kuyo_last=$(HISTTIMEFORMAT= history 1)
                    __kuyo_precmd() {
                      local s=$? h
                      h=$(HISTTIMEFORMAT= history 1)
                      if [ "$h" != "$__kuyo_last" ]; then
                        __kuyo_last=$h
                        __kuyo_whisper "${h#"${h%%[![:space:][:digit:]*]*}"}"
                      fi
                      return $s
                    }
                    case ";${PROMPT_COMMAND};" in
                      *";__kuyo_precmd;"*) ;;
                      *) PROMPT_COMMAND="__kuyo_precmd${PROMPT_COMMAND:+;$PROMPT_COMMAND}" ;;
                    esac
                    """;
            case zsh -> """
                    __kuyo_preexec() { __kuyo_line=$1 }
                    __kuyo_precmd() {
                      local s=$?
                      [[ -n $__kuyo_line ]] && __kuyo_whisper "$__kuyo_line"
                      __kuyo_line=
                      return $s
                    }
                    autoload -Uz add-zsh-hook
                    add-zsh-hook preexec __kuyo_preexec
                    add-zsh-hook precmd __kuyo_precmd
                    """;
        };
    }
}
