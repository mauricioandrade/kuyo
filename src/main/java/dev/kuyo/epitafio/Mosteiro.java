package dev.kuyo.epitafio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Bate na porta do Ollama antes do funeral começar. Sem isso, um Ollama desligado só aparece
 * depois de minutos de timeout e retry dentro do LangChain4j.
 */
@ApplicationScoped
public class Mosteiro {

    private static final Duration PACIENCIA = Duration.ofSeconds(3);

    @Inject
    ObjectMapper json;

    @ConfigProperty(name = "kuyo.ollama.url")
    String url;

    @ConfigProperty(name = "kuyo.modelo")
    String modelo;

    /** Vazio quando o monge está pronto; senão, o que impede o funeral. */
    public Optional<String> problema() {
        JsonNode tags;
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(PACIENCIA).build()) {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create(url + "/api/tags")).timeout(PACIENCIA).build();
            tags = json.readTree(http.send(pedido, HttpResponse.BodyHandlers.ofString()).body());
        } catch (IOException e) {
            return Optional.of("O Ollama não respondeu em " + url + ". Ele está rodando? (ollama serve)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.of("Interrompido enquanto chamava o Ollama.");
        }

        for (JsonNode m : tags.path("models")) {
            if (modelo.equals(m.path("name").asText()) || modelo.equals(m.path("model").asText())) {
                return Optional.empty();
            }
        }
        return Optional.of("O modelo " + modelo + " não está no Ollama. Baixe com: ollama pull " + modelo);
    }
}
