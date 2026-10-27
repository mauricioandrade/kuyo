package dev.kuyo.seance;

import dev.kuyo.dominio.Alma;
import dev.kuyo.dominio.Falecido;
import dev.kuyo.epitafio.Epitafista;
import dev.kuyo.epitafio.Lingua;
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
 * Conduz a conversa com um arquivo morto. O fantasma só sabe o que viveu: o último conteúdo
 * e os fatos do Legista. A conversa inteira vai a cada pergunta, então ela é curta por natureza.
 */
public class Medium {

    /** Um modelo pequeno se perde com arquivos enormes; o começo do arquivo basta para lembrar quem era. */
    static final int LEMBRANCA_MAXIMA = 12_000;

    private static final DateTimeFormatter DATA = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault());

    private final ChatModel modelo;
    private final List<ChatMessage> conversa = new ArrayList<>();

    public Medium(ChatModel modelo, Alma alma, Lingua lingua) {
        this.modelo = modelo;
        conversa.add(SystemMessage.from(invocacao(alma, lingua)));
    }

    /** O fantasma fala primeiro, ao ser chamado. */
    public String chamar() {
        return perguntar("(The medium lights the incense and calls your name. Greet the living in one or two sentences.)");
    }

    public String perguntar(String pergunta) {
        conversa.add(UserMessage.from(pergunta));
        String resposta = modelo.chat(conversa).aiMessage().text().strip();
        conversa.add(AiMessage.from(resposta));
        return resposta;
    }

    static String invocacao(Alma alma, Lingua lingua) {
        Falecido f = alma.falecido();
        var morte = f.commitDaMorte();
        String lembranca = f.binario() || f.ultimoConteudo() == null
                ? "(You were a binary file. You remember no words, only shapes.)"
                : recortar(f.ultimoConteudo());

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
                f.caminho(), lingua == Lingua.pt ? "Brazilian Portuguese" : "English",
                DATA.format(f.nascimento()), DATA.format(f.morte()),
                Epitafista.vida(Duration.between(f.nascimento(), f.morte())),
                String.join(", ", f.autores()), f.commitsEmVida(),
                alma.causa().name().toLowerCase(), alma.inquietacao(),
                morte.hash(), morte.autor(), morte.quando().toLocalTime(), morte.arquivosAlterados() - 1,
                morte.mensagem(),
                lembranca);
    }

    static String recortar(String conteudo) {
        if (conteudo.length() <= LEMBRANCA_MAXIMA) {
            return conteudo;
        }
        return conteudo.substring(0, LEMBRANCA_MAXIMA) + "\n… (the rest has faded)";
    }
}
