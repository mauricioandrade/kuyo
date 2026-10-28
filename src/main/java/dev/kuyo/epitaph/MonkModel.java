package dev.kuyo.epitaph;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Builds the model from configuration read at runtime. The Ollama extension fixes the model id at
 * build time, and switching models without recompiling is part of Kuyō's point.
 */
public class MonkModel implements Supplier<ChatModel> {

    @Override
    public ChatModel get() {
        return create(160);
    }

    /** The same model with another token limit: a séance needs more breath than an epitaph. */
    public static ChatModel create(int maxTokens) {
        Config config = ConfigProvider.getConfig();
        return OllamaChatModel.builder()
                .baseUrl(config.getValue("kuyo.ollama.url", String.class))
                .modelName(config.getValue("kuyo.model", String.class))
                .temperature(config.getValue("kuyo.temperature", Double.class))
                .numPredict(maxTokens)
                // no thinking, or Gemma 4 spends every token reasoning and returns empty text
                .think(false)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(1)
                .logRequests(config.getOptionalValue("kuyo.log-requests", Boolean.class).orElse(false))
                .build();
    }
}
