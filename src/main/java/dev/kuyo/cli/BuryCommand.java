package dev.kuyo.cli;

import dev.kuyo.autopsia.Shisho;
import dev.kuyo.cemiterio.Cemiterio;
import dev.kuyo.cemiterio.Textos;
import dev.kuyo.dominio.Alma;
import dev.kuyo.dominio.Legista;
import dev.kuyo.epitafio.Epitafista;
import dev.kuyo.epitafio.Lingua;
import dev.kuyo.epitafio.Mosteiro;
import dev.kuyo.git.Exumador;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "bury", description = "Escreve os epitáfios e gera o cemitério em HTML.")
public class BuryCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Caminho do repositório git.", defaultValue = ".")
    Path repo;

    @Option(names = {"-o", "--saida"}, defaultValue = "cemiterio.html", description = "Arquivo HTML de saída.")
    Path saida;

    @Option(names = "--lang", defaultValue = "en", description = "Língua dos epitáfios: ${COMPLETION-CANDIDATES}.")
    Lingua lingua;

    @Option(names = "--sem-epitafios", description = "Gera o cemitério sem chamar o modelo.")
    boolean semEpitafios;

    @Option(names = "--autopsia", description = "Pede ao Shishō que examine o último conteúdo de cada arquivo Java morto.")
    boolean autopsia;

    @Inject
    Exumador exumador;

    @Inject
    Shisho shisho;

    @Inject
    Legista legista;

    @Inject
    Epitafista epitafista;

    @Inject
    Mosteiro mosteiro;

    @Inject
    @Location("cemiterio.html")
    Template cemiterio;

    @ConfigProperty(name = "kuyo.ollama.url")
    String urlOllama;

    @Override
    public Integer call() throws Exception {
        List<Alma> almas = exumador.exumar(repo).stream().map(legista::examinar).toList();
        if (almas.isEmpty()) {
            System.out.println("Nenhum arquivo morto. Ninguém para velar.");
            return 0;
        }

        if (autopsia) {
            Optional<String> problema = shisho.problema();
            if (problema.isPresent()) {
                System.err.println(problema.get());
                return 1;
            }
            almas = autopsiar(almas);
        }

        Path pasta = saida.toAbsolutePath().getParent();
        Files.createDirectories(pasta);
        if (!semEpitafios) {
            Optional<String> problema = mosteiro.problema();
            if (problema.isPresent()) {
                System.err.println(problema.get());
                System.err.println("Para gerar o cemitério sem epitáfios, use --sem-epitafios.");
                return 1;
            }
            epitafista.abrirCache(pasta.resolve("kuyo.json"));
            try {
                almas = escreverEpitafios(almas);
            } catch (RuntimeException e) {
                System.err.printf("%nO monge não respondeu. O Ollama está rodando em %s com o modelo baixado?%n  %s%n",
                        urlOllama, e.getMessage());
                return 1;
            }
        }

        String nomeRepo = repo.toAbsolutePath().normalize().getFileName().toString().replaceFirst("\\.git$", "");
        String html = cemiterio.data("c", Cemiterio.montar(nomeRepo, almas, Textos.de(lingua))).render();
        Files.writeString(saida, html);
        System.out.printf("%n%d almas sepultadas em %s%n", almas.size(), saida.toAbsolutePath());
        return 0;
    }

    private List<Alma> autopsiar(List<Alma> almas) throws Exception {
        List<Alma> examinadas = new ArrayList<>(almas.size());
        for (Alma alma : almas) {
            if (shisho.examina(alma.falecido())) {
                System.out.printf("師 autópsia de %s%n", alma.falecido().caminho());
                alma = shisho.examinar(alma.falecido(), exumador.arquivoDeBuild(repo, alma.falecido()), lingua)
                        .map(alma::comAutopsia)
                        .orElse(alma);
            }
            examinadas.add(alma);
        }
        return examinadas;
    }

    private List<Alma> escreverEpitafios(List<Alma> almas) throws Exception {
        List<Alma> comEpitafio = new ArrayList<>(almas.size());
        for (int i = 0; i < almas.size(); i++) {
            Alma alma = almas.get(i);
            boolean emCache = epitafista.emCache(alma, lingua);
            System.out.printf("[%d/%d] ✝ %s%s%n", i + 1, almas.size(), alma.falecido().caminho(),
                    emCache ? "  (cache)" : "");
            comEpitafio.add(alma.comEpitafio(epitafista.epitafio(alma, lingua)));
        }
        return comEpitafio;
    }
}
