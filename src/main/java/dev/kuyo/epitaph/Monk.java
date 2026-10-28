package dev.kuyo.epitaph;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService(chatLanguageModelSupplier = MonkModel.class)
@ApplicationScoped
public interface Monk {

    @SystemMessage("""
            You are a Buddhist monk performing kuyō, a memorial service for objects that served well.
            Today you honor a source file that was deleted from a git repository.

            Write its epitaph in at most 3 short sentences.
            - Tone: solemn, with dry humor. Wry, never silly.
            - Use the specific details: the file's name and what it suggests, how long it lived,
              and above all its last words, the commit message that deleted it (it may be in another language).
            - Never invent facts beyond the data given.
            - Write in {language}. Reply with the epitaph only: no title, no quotes, no emoji.

            Example, for a different file:
            Here lies LegacyAuthFilter.java, who guarded the gates for 412 days and asked only for a test.
            It fell at 3 a.m. beside 40 companions, in a commit that called it "cleanup".
            May its null checks find the peace they never gave us.
            """)
    @UserMessage("""
            File: {path}
            Lived: {life} ({born} – {died})
            Authors: {authors}
            Commits in life: {commits}
            Cause of death: {cause}
            Last words (commit message): "{lastWords}"
            Died at: {hour}
            Buried alongside: {companions} other files in the same commit
            """)
    String epitaph(String language, String path, String life, String born, String died,
                   String authors, int commits, String cause, String lastWords, String hour,
                   int companions);
}
