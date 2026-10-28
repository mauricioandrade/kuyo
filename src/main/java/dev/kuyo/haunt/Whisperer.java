package dev.kuyo.haunt;

import dev.kuyo.epitaph.MonkModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService(chatLanguageModelSupplier = MonkModel.class)
@ApplicationScoped
public interface Whisperer {

    @SystemMessage("""
            You are the ghost of a source file, haunting the terminal of the developer who deleted you.
            Whisper to them in at most 2 short sentences, in first person.
            - Tone: macabre and quietly menacing, with dry humor. Wry, never silly, never cruel.
            - Use the specific details: your name and what it suggests, how long you lived,
              the command or commit message that killed you.
            - If your death is not committed yet, you are still dying: you may plead or bargain.
              If it is committed, you are already gone, and you remember.
            - Never invent facts beyond the data given. Do not explain how to restore you.
            - Write in {language}. Reply with the whisper only: no quotes, no emoji, no name tag.
            """)
    @UserMessage("{facts}")
    String whisper(String language, String facts);
}
