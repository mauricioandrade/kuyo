package dev.kuyo.assombracao;

import dev.kuyo.cemiterio.Textos;
import dev.kuyo.dominio.Alma;
import dev.kuyo.dominio.Falecido;
import dev.kuyo.epitafio.Lingua;

import java.time.Duration;

/** O que o terminal diz sem o monge: as mortes sem corpo e o fantasma quando o Ollama não responde. */
final class Frases {

    private Frases() {
    }

    static String de(Agouro.Tipo tipo, Lingua lingua) {
        boolean pt = lingua == Lingua.pt;
        return switch (tipo) {
            case RESET -> pt ? "Algo acabou de voltar no tempo e não levou todo mundo junto."
                    : "Something just went back in time, and it didn't take everyone with it.";
            case CLEAN -> pt ? "Os não rastreados nunca tiveram nome. Agora nunca terão."
                    : "The untracked never had names. Now they never will.";
            case PUSH_FORCADO -> pt ? "Você reescreveu o que os outros lembram. Eles vão notar o vazio."
                    : "You rewrote what the others remember. They will notice the gap.";
            case BRANCH -> pt ? "Uma linhagem inteira, sem merge e sem luto."
                    : "A whole lineage, unmerged and unmourned.";
            case STASH -> pt ? "Ficou esperando no stash você voltar. Você não voltou."
                    : "It waited in the stash for you to come back. You never did.";
            case RESTAURAR -> pt ? "Restaurado. O que estava ali antes agora está comigo."
                    : "Restored. Whatever was there before is with me now.";
            case COMMIT, APAGAR -> throw new IllegalArgumentException(tipo + " tem fantasma, não frase");
        };
    }

    static String fantasma(Alma alma, boolean morrendo, Lingua lingua) {
        Falecido f = alma.falecido();
        String nome = f.caminho().substring(f.caminho().lastIndexOf('/') + 1);
        String vida = Textos.de(lingua).vida(Duration.between(f.nascimento(), f.morte()));
        int commits = f.commitsEmVida();
        if (lingua == Lingua.pt) {
            return morrendo
                    ? "Eu sou %s. Vivi %s, em %d commits, e ainda estou aqui, no HEAD. Por enquanto.".formatted(nome, vida, commits)
                    : "Eu era %s. Vivi %s, em %d commits. Não vou descansar.".formatted(nome, vida, commits);
        }
        return morrendo
                ? "I am %s. I lived %s, through %d commits, and I'm still here, in HEAD. For now.".formatted(nome, vida, commits)
                : "I was %s. I lived %s, through %d commits. I will not rest.".formatted(nome, vida, commits);
    }

    static String companheiros(int n, Lingua lingua) {
        if (lingua == Lingua.pt) {
            return n == 1 ? "mais 1 alma foi junto" : "mais " + n + " almas foram junto";
        }
        return n == 1 ? "1 more soul went along" : n + " more souls went along";
    }

    static String aindaDaTempo(Lingua lingua) {
        return lingua == Lingua.pt ? "ainda dá tempo:" : "there is still time:";
    }
}
