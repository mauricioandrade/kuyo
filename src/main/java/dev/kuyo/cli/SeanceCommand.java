package dev.kuyo.cli;

import dev.kuyo.dominio.Alma;
import dev.kuyo.dominio.Falecido;
import dev.kuyo.dominio.Legista;
import dev.kuyo.epitafio.Lingua;
import dev.kuyo.epitafio.ModeloDoMonge;
import dev.kuyo.epitafio.Mosteiro;
import dev.kuyo.git.Exumador;
import dev.kuyo.seance.Medium;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "seance", description = "Conversa com um arquivo morto.")
public class SeanceCommand implements Callable<Integer> {

    private static final List<String> DESPEDIDAS = List.of("/bye", "/sair", "tchau", "bye", "adeus", "goodbye");

    @Parameters(index = "0", description = "Caminho do repositório git.")
    Path repo;

    @Parameters(index = "1", description = "O arquivo morto: caminho completo ou só o nome.")
    String arquivo;

    @Option(names = "--lang", defaultValue = "en", description = "Língua do fantasma: ${COMPLETION-CANDIDATES}.")
    Lingua lingua;

    @Option(names = {"-p", "--pergunta"}, description = "Pergunta sem abrir a conversa interativa (pode repetir).")
    List<String> perguntas;

    @Inject
    Exumador exumador;

    @Inject
    Legista legista;

    @Inject
    Mosteiro mosteiro;

    @Override
    public Integer call() throws Exception {
        List<Falecido> mortos = exumador.exumar(repo);
        Optional<Falecido> escolhido = escolher(mortos);
        if (escolhido.isEmpty()) {
            return 1;
        }
        Optional<String> problema = mosteiro.problema();
        if (problema.isPresent()) {
            System.err.println(problema.get());
            return 1;
        }

        Alma alma = legista.examinar(escolhido.get());
        String nome = nome(alma.falecido().caminho());
        Medium medium = new Medium(ModeloDoMonge.criar(400), alma, lingua);

        System.out.printf("%n  O incenso é aceso. %s, %s, responde.%n%n", alma.falecido().caminho(),
                alma.causa().name().toLowerCase(Locale.ROOT));
        falar(nome, medium.chamar());

        if (perguntas != null) {
            for (String pergunta : perguntas) {
                System.out.printf("> %s%n", pergunta);
                falar(nome, medium.perguntar(pergunta));
            }
            return 0;
        }

        System.out.println("  (escreva /bye para deixar o espírito descansar)\n");
        BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            System.out.print("> ");
            System.out.flush();
            String pergunta = entrada.readLine();
            if (pergunta == null || DESPEDIDAS.contains(pergunta.strip().toLowerCase(Locale.ROOT))) {
                System.out.println("\n  O incenso apaga. O espírito volta a descansar.");
                return 0;
            }
            if (!pergunta.isBlank()) {
                falar(nome, medium.perguntar(pergunta));
            }
        }
    }

    /** Aceita o caminho completo ou só o nome; se o arquivo morreu mais de uma vez, chama a última vida. */
    private Optional<Falecido> escolher(List<Falecido> mortos) {
        List<Falecido> exatos = mortos.stream().filter(f -> f.caminho().equals(arquivo)).toList();
        List<Falecido> candidatos = !exatos.isEmpty() ? exatos : mortos.stream()
                .filter(f -> f.caminho().endsWith("/" + arquivo) || nome(f.caminho()).equals(arquivo))
                .toList();

        if (candidatos.isEmpty()) {
            System.err.printf("Nenhum arquivo morto chamado %s.%n", arquivo);
            String busca = arquivo.toLowerCase(Locale.ROOT);
            mortos.stream().map(Falecido::caminho).distinct()
                    .filter(c -> c.toLowerCase(Locale.ROOT).contains(busca))
                    .limit(5)
                    .forEach(c -> System.err.println("  talvez: " + c));
            return Optional.empty();
        }

        List<String> caminhos = candidatos.stream().map(Falecido::caminho).distinct().toList();
        if (caminhos.size() > 1) {
            System.err.printf("Mais de um %s está enterrado aqui. Diga qual:%n", arquivo);
            caminhos.forEach(c -> System.err.println("  " + c));
            return Optional.empty();
        }
        if (candidatos.size() > 1) {
            System.out.printf("  %s morreu %d vezes; chamando a última vida.%n", caminhos.getFirst(), candidatos.size());
        }
        return Optional.of(candidatos.getLast());
    }

    private static void falar(String nome, String fala) {
        System.out.printf("%s: %s%n%n", nome, fala);
    }

    private static String nome(String caminho) {
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }
}
