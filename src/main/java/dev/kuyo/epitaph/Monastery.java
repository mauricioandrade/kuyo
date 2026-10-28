package dev.kuyo.epitaph;

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
 * Knocks on Ollama's door before the funeral starts. Without it, a stopped Ollama only shows up
 * after minutes of timeouts and retries inside LangChain4j.
 */
@ApplicationScoped
public class Monastery {

    private static final Duration PATIENCE = Duration.ofSeconds(3);

    @Inject
    ObjectMapper json;

    @ConfigProperty(name = "kuyo.ollama.url")
    String url;

    @ConfigProperty(name = "kuyo.model")
    String model;

    /** Empty when the monk is ready; otherwise, what stands in the way of the funeral. */
    public Optional<String> problem() {
        JsonNode tags;
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(PATIENCE).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url + "/api/tags")).timeout(PATIENCE).build();
            tags = json.readTree(http.send(request, HttpResponse.BodyHandlers.ofString()).body());
        } catch (IOException e) {
            return Optional.of("Ollama did not answer at " + url + ". Is it running? (ollama serve)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.of("Interrupted while calling Ollama.");
        }

        for (JsonNode m : tags.path("models")) {
            if (model.equals(m.path("name").asText()) || model.equals(m.path("model").asText())) {
                return Optional.empty();
            }
        }
        return Optional.of("The model " + model + " is not in Ollama. Pull it with: ollama pull " + model);
    }
}
