package dev.kuyo.autopsia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kuyo.dominio.Falecido;
import dev.kuyo.dominio.Licao;
import dev.kuyo.epitafio.Lingua;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Chama o <a href="https://github.com/mauricioandrade/shisho">Shishō</a> para examinar o corpo.
 *
 * <p>O último conteúdo do arquivo vai para uma pasta temporária, ao lado do arquivo de build que
 * estava vivo quando ele morreu, e o Shishō analisa essa pasta como um projeto. Só as lições que
 * caem no próprio morto contam: o que o Shishō diz do pom.xml é sobre o projeto, não sobre ele.
 */
@ApplicationScoped
public class Shisho {

    private static final long PACIENCIA_SEGUNDOS = 60;

    @Inject
    ObjectMapper json;

    @ConfigProperty(name = "kuyo.shisho")
    String comando;

    /** O Shishō só lê Java; os outros mortos ficam sem autópsia. */
    public boolean examina(Falecido f) {
        return f.caminho().endsWith(".java") && !f.binario() && f.ultimoConteudo() != null;
    }

    /** Vazio quando o Shishō responde; senão, o que impede a autópsia. */
    public Optional<String> problema() {
        try {
            Process p = iniciar(List.of("--version"), Map.of());
            if (p.waitFor(PACIENCIA_SEGUNDOS, TimeUnit.SECONDS) && p.exitValue() == 0) {
                return Optional.empty();
            }
            p.destroyForcibly();
        } catch (IOException e) {
            // cai na mensagem abaixo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.of("O Shishō não respondeu (" + comando + "). Instale com o install.sh de "
                + "https://github.com/mauricioandrade/shisho ou aponte KUYO_SHISHO para o shisho.jar.");
    }

    /** Vazio quando o Shishō não conseguiu ler o corpo (um arquivo cortado ao meio, por exemplo). */
    public Optional<List<Licao>> examinar(Falecido f, Optional<ArquivoDeBuild> build, Lingua lingua)
            throws IOException {
        Path mesa = Files.createTempDirectory("kuyo-autopsia");
        try {
            String relativo = build.map(b -> relativo(f.caminho(), b.pasta())).orElse(f.caminho());
            Path corpo = mesa.resolve(relativo);
            Files.createDirectories(corpo.getParent());
            Files.writeString(corpo, f.ultimoConteudo());
            if (build.isPresent()) {
                Files.writeString(mesa.resolve(build.get().nome()), build.get().conteudo());
            }

            Path sarif = mesa.resolve("shisho.sarif");
            Process p = iniciar(List.of("analyze", "--format", "sarif", "--output", sarif.toString(), mesa.toString()),
                    Map.of("SHISHO_IDIOMA", lingua.name()));
            if (!p.waitFor(PACIENCIA_SEGUNDOS, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return Optional.empty();
            }
            // 0 sem lição grave, 1 com lição grave; o resto é o Shishō dizendo que não conseguiu ler
            if (p.exitValue() > 1 || !Files.exists(sarif)) {
                return Optional.empty();
            }
            return Optional.of(licoes(json.readTree(sarif.toFile()), relativo));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            try (Stream<Path> arquivos = Files.walk(mesa)) {
                arquivos.sorted(Comparator.reverseOrder()).forEach(a -> a.toFile().delete());
            }
        }
    }

    static List<Licao> licoes(JsonNode sarif, String caminho) {
        JsonNode run = sarif.path("runs").path(0);
        Map<String, String> titulos = new HashMap<>();
        for (JsonNode regra : run.path("tool").path("driver").path("rules")) {
            titulos.put(regra.path("id").asText(), regra.path("shortDescription").path("text").asText());
        }
        List<Licao> licoes = new ArrayList<>();
        for (JsonNode r : run.path("results")) {
            JsonNode local = r.path("locations").path(0).path("physicalLocation");
            if (!caminho.equals(local.path("artifactLocation").path("uri").asText())) {
                continue;
            }
            String kata = r.path("ruleId").asText();
            licoes.add(new Licao(kata, r.path("properties").path("severity").asText("INFO"),
                    titulos.getOrDefault(kata, kata), local.path("region").path("startLine").asInt(0)));
        }
        // a mesma kata na mesma linha (um System.out e um printStackTrace, por exemplo) é uma lição só
        return licoes.stream().distinct().toList();
    }

    private static String relativo(String caminho, String pasta) {
        return pasta.isEmpty() ? caminho : caminho.substring(pasta.length() + 1);
    }

    /** Aceita um comando no PATH ou o caminho de um shisho.jar, que roda na mesma JVM do Kuyō. */
    private Process iniciar(List<String> argumentos, Map<String, String> ambiente) throws IOException {
        List<String> linha = new ArrayList<>();
        if (comando.endsWith(".jar")) {
            linha.add(ProcessHandle.current().info().command().orElse("java"));
            linha.add("-jar");
            linha.add(comando);
        } else {
            linha.addAll(List.of(comando.strip().split("\\s+")));
        }
        linha.addAll(argumentos);
        ProcessBuilder pb = new ProcessBuilder(linha).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.environment().putAll(ambiente);
        return pb.start();
    }
}
