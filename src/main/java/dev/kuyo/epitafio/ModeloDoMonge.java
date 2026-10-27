package dev.kuyo.epitafio;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Monta o modelo com configuração lida em tempo de execução. O model-id da extensão do Ollama é
 * fixado no build, e trocar de modelo sem recompilar é parte do ponto do Kuyō.
 */
public class ModeloDoMonge implements Supplier<ChatModel> {

    @Override
    public ChatModel get() {
        return criar(160);
    }

    /** O mesmo modelo com outro limite de tokens: a séance precisa de mais fôlego que um epitáfio. */
    public static ChatModel criar(int maxTokens) {
        Config config = ConfigProvider.getConfig();
        return OllamaChatModel.builder()
                .baseUrl(config.getValue("kuyo.ollama.url", String.class))
                .modelName(config.getValue("kuyo.modelo", String.class))
                // sem raciocínio, senão o Gemma 4 gasta todos os tokens pensando e devolve texto vazio
                .temperature(config.getValue("kuyo.temperatura", Double.class))
                .numPredict(maxTokens)
                .think(false)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(1)
                .logRequests(config.getOptionalValue("kuyo.log-requests", Boolean.class).orElse(false))
                .build();
    }
}
