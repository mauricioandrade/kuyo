package dev.kuyo.assombracao;

import dev.kuyo.cemiterio.Textos;
import dev.kuyo.dominio.Alma;
import dev.kuyo.dominio.Falecido;
import dev.kuyo.dominio.Legista;
import dev.kuyo.epitafio.Epitafista;
import dev.kuyo.epitafio.Lingua;
import dev.kuyo.epitafio.Mosteiro;
import dev.kuyo.git.Exumador;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Decide o que o terminal sussurra depois de um comando. Quando um arquivo morre (ou está morrendo,
 * apagado mas ainda não commitado), o fantasma dele fala pelo Gemma; o resto ganha frases fixas.
 * Silêncio é a resposta para tudo que não mata nada.
 */
@ApplicationScoped
public class Assombracao {

    /** Quanto tempo depois do commit ele ainda conta como o que o usuário acabou de fazer. */
    private static final Duration JANELA_DO_COMMIT = Duration.ofSeconds(90);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    @Inject
    Exumador exumador;

    @Inject
    Legista legista;

    @Inject
    Mosteiro mosteiro;

    @Inject
    Sussurrador sussurrador;

    /** Uma fala do fantasma e, quando ainda dá tempo, como trazê-lo de volta. */
    public record Sussurro(String fala, String nota) {
    }

    public List<Sussurro> ouvir(Path cwd, String linha, Lingua lingua, boolean comMonge) throws IOException {
        List<Sussurro> sussurros = new ArrayList<>();
        for (Agouro agouro : Agouro.ler(linha)) {
            switch (agouro.tipo()) {
                case COMMIT -> recemMortos(cwd).ifPresent(almas ->
                        sussurros.add(fantasma(almas, linha, false, null, lingua, comMonge)));
                case APAGAR -> {
                    List<Alma> almas = moribundas(cwd, agouro.alvos(), linha);
                    if (!almas.isEmpty()) {
                        sussurros.add(fantasma(almas, linha, true, agouro.alvos(), lingua, comMonge));
                    }
                }
                default -> sussurros.add(new Sussurro(Frases.de(agouro.tipo(), lingua), null));
            }
        }
        return sussurros;
    }

    private Optional<List<Alma>> recemMortos(Path cwd) throws IOException {
        Optional<String> hash = exumador.commitRecente(cwd, JANELA_DO_COMMIT);
        if (hash.isEmpty()) {
            return Optional.empty();
        }
        List<Alma> almas = exumador.exumar(cwd).stream()
                .filter(f -> f.commitDaMorte().hash().equals(hash.get()))
                .map(legista::examinar)
                .toList();
        return almas.isEmpty() ? Optional.empty() : Optional.of(almas);
    }

    private List<Alma> moribundas(Path cwd, List<String> alvos, String linha) throws IOException {
        Optional<Path> raiz = exumador.raiz(cwd);
        if (raiz.isEmpty()) {
            return List.of();
        }
        return exumador.moribundos(cwd, condenado(raiz.get(), cwd, alvos), linha).stream()
                .map(legista::examinar)
                .toList();
    }

    /** Um caminho do repositório é condenado quando é um dos alvos, está dentro de um, ou casa com um glob. */
    static Predicate<String> condenado(Path raiz, Path cwd, List<String> alvos) {
        Path raizAbs = raiz.toAbsolutePath().normalize();
        List<Predicate<Path>> testes = new ArrayList<>();
        for (String alvo : alvos) {
            Path abs = cwd.toAbsolutePath().resolve(alvo.replaceFirst("^~(?=/|$)", System.getProperty("user.home")))
                    .normalize();
            if (alvo.matches(".*[*?\\[].*")) {
                PathMatcher glob = FileSystems.getDefault().getPathMatcher("glob:" + abs);
                testes.add(p -> glob.matches(p) || p.getParent() != null && glob.matches(p.getParent()));
            } else {
                testes.add(p -> p.startsWith(abs));
            }
        }
        return caminho -> {
            Path p = raizAbs.resolve(caminho);
            return testes.stream().anyMatch(t -> t.test(p));
        };
    }

    private Sussurro fantasma(List<Alma> almas, String linha, boolean morrendo, List<String> alvos,
                              Lingua lingua, boolean comMonge) {
        Alma alma = almas.stream().max(Comparator.comparingDouble(Alma::inquietacao)).orElseThrow();
        String fala = null;
        if (comMonge && mosteiro.problema().isEmpty()) {
            try {
                fala = Epitafista.limpar(sussurrador.sussurro(lingua.nomeParaOModelo,
                        fatos(alma, almas.size() - 1, linha, morrendo)));
            } catch (RuntimeException e) {
                fala = null;
            }
        }
        if (fala == null || fala.isBlank()) {
            fala = Frases.fantasma(alma, morrendo, lingua);
        }

        List<String> nota = new ArrayList<>();
        if (almas.size() > 1) {
            nota.add(Frases.companheiros(almas.size() - 1, lingua));
        }
        if (morrendo) {
            String pathspec = almas.size() == 1
                    ? "':/" + alma.falecido().caminho().replace("'", "'\\''") + "'"
                    : String.join(" ", alvos);
            nota.add(Frases.aindaDaTempo(lingua) + " git restore -SW -- " + pathspec);
        }
        return new Sussurro(fala, nota.isEmpty() ? null : String.join(" · ", nota));
    }

    static String fatos(Alma alma, int companheiros, String linha, boolean morrendo) {
        Falecido f = alma.falecido();
        Textos en = Textos.de(Lingua.en);
        String morte = morrendo
                ? "Killed by: the shell command `" + linha + "`. Not committed yet: you are still dying."
                : "Killed by: the commit \"" + primeiraLinha(f.commitDaMorte().mensagem()) + "\", just made.";
        return """
                File: %s
                Lived: %s
                Commits in life: %d
                Authors: %s
                Cause of death: %s (restlessness %.2f of 1)
                %s
                Time of death: %s
                Going with you: %d other files
                """.formatted(f.caminho(), en.vida(Duration.between(f.nascimento(), f.morte())), f.commitsEmVida(),
                String.join(", ", f.autores()), en.causa(alma.causa()).toLowerCase(), alma.inquietacao(), morte,
                HORA.format(f.commitDaMorte().quando()), companheiros);
    }

    private static String primeiraLinha(String mensagem) {
        int quebra = mensagem.indexOf('\n');
        return quebra < 0 ? mensagem : mensagem.substring(0, quebra);
    }
}
