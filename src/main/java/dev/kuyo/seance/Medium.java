package dev.kuyo.seance;

import dev.kuyo.domain.Deceased;
import dev.kuyo.domain.Soul;
import dev.kuyo.epitaph.EpitaphWriter;
import dev.kuyo.epitaph.Language;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Conducts the conversation with a dead file. The ghost only knows what it lived: its last contents
 * and the Coroner's facts. The whole conversation is sent with every question, so it is short by nature.
 */
public class Medium {

    /** A small model gets lost in huge files; the start of the file is enough to remember who it was. */
    static final int MAX_MEMORY = 12_000;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault());

    private final ChatModel model;
    private final List<ChatMessage> conversation = new ArrayList<>();

    public Medium(ChatModel model, Soul soul, Language language) {
        this.model = model;
        conversation.add(SystemMessage.from(invocation(soul, language)));
    }

    /** The ghost speaks first, when it is called. */
    public String call() {
        return ask("(The medium lights the incense and calls your name. Greet the living in one or two sentences.)");
    }

    public String ask(String question) {
        conversation.add(UserMessage.from(question));
        String answer = model.chat(conversation).aiMessage().text().strip();
        conversation.add(AiMessage.from(answer));
        return answer;
    }

    static String invocation(Soul soul, Language language) {
        Deceased d = soul.deceased();
        var death = d.deathCommit();
        String memory = d.binary() || d.lastContents() == null
                ? "(You were a binary file. You remember no words, only shapes.)"
                : trim(d.lastContents());

        return """
                You are the ghost of %s, a source file that was deleted from a git repository.
                A living developer is holding a séance to talk to you.

                - Speak in first person, as the file itself, in %s.
                - Be wistful, with dry humor. Never cruel, never cute.
                - You remember only the facts and your last contents below. Never invent events, people,
                  features or code that are not there. If asked about something you could not know,
                  say the dead do not know such things.
                - Keep answers under four sentences, unless you are asked to recite your code.

                Facts of your life:
                - Born %s, died %s: you lived %s.
                - Authors: %s. Commits in life: %d.
                - Cause of death: %s (restlessness %.2f out of 1).
                - Killed by commit %s by %s at %s, alongside %d other files.
                - Your last words, the commit message that deleted you: "%s"

                Your last contents:
                ```
                %s
                ```
                """.formatted(
                d.path(), language.nameForTheModel,
                DATE.format(d.born()), DATE.format(d.died()),
                EpitaphWriter.life(Duration.between(d.born(), d.died())),
                String.join(", ", d.authors()), d.commitsInLife(),
                soul.cause().name().toLowerCase(), soul.restlessness(),
                death.hash(), death.author(), death.when().toLocalTime(), death.filesChanged() - 1,
                death.message(),
                memory);
    }

    static String trim(String contents) {
        if (contents.length() <= MAX_MEMORY) {
            return contents;
        }
        return contents.substring(0, MAX_MEMORY) + "\n… (the rest has faded)";
    }
}
