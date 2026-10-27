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
        Config config = ConfigProvider.getConfig();
        return OllamaChatModel.builder()
                .baseUrl(config.getValue("kuyo.ollama.url", String.class))
                .modelName(config.getValue("kuyo.modelo", String.class))
                // epitáfio é curto e tem que respeitar os fatos; sem raciocínio, senão o Gemma 4
                // gasta todos os tokens pensando e devolve o epitáfio vazio
                .temperature(config.getValue("kuyo.temperatura", Double.class))
                .numPredict(160)
                .think(false)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(1)
                .logRequests(config.getOptionalValue("kuyo.log-requests", Boolean.class).orElse(false))
                .build();
    }
}
