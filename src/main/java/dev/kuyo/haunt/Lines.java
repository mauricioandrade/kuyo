package dev.kuyo.haunt;

import dev.kuyo.cemetery.Texts;
import dev.kuyo.domain.Deceased;
import dev.kuyo.domain.Soul;
import dev.kuyo.epitaph.Language;

import java.time.Duration;

/** What the terminal says without the monk: deaths with no body, and the ghost when Ollama does not answer. */
final class Lines {

    private Lines() {
    }

    static String of(Omen.Type type, Language language) {
        boolean pt = language == Language.pt;
        return switch (type) {
            case RESET -> pt ? "Algo acabou de voltar no tempo e não levou todo mundo junto."
                    : "Something just went back in time, and it didn't take everyone with it.";
            case CLEAN -> pt ? "Os não rastreados nunca tiveram nome. Agora nunca terão."
                    : "The untracked never had names. Now they never will.";
            case FORCE_PUSH -> pt ? "Você reescreveu o que os outros lembram. Eles vão notar o vazio."
                    : "You rewrote what the others remember. They will notice the gap.";
            case BRANCH -> pt ? "Uma linhagem inteira, sem merge e sem luto."
                    : "A whole lineage, unmerged and unmourned.";
            case STASH -> pt ? "Ficou esperando no stash você voltar. Você não voltou."
                    : "It waited in the stash for you to come back. You never did.";
            case RESTORE -> pt ? "Restaurado. O que estava ali antes agora está comigo."
                    : "Restored. Whatever was there before is with me now.";
            case COMMIT, DELETE -> throw new IllegalArgumentException(type + " has a ghost, not a line");
        };
    }

    static String ghost(Soul soul, boolean dying, Language language) {
        Deceased d = soul.deceased();
        String name = d.path().substring(d.path().lastIndexOf('/') + 1);
        String life = Texts.of(language).life(Duration.between(d.born(), d.died()));
        int commits = d.commitsInLife();
        if (language == Language.pt) {
            return dying
                    ? "Eu sou %s. Vivi %s, em %d commits, e ainda estou aqui, no HEAD. Por enquanto.".formatted(name, life, commits)
                    : "Eu era %s. Vivi %s, em %d commits. Não vou descansar.".formatted(name, life, commits);
        }
        return dying
                ? "I am %s. I lived %s, through %d commits, and I'm still here, in HEAD. For now.".formatted(name, life, commits)
                : "I was %s. I lived %s, through %d commits. I will not rest.".formatted(name, life, commits);
    }

    static String companions(int n, Language language) {
        if (language == Language.pt) {
            return n == 1 ? "mais 1 alma foi junto" : "mais " + n + " almas foram junto";
        }
        return n == 1 ? "1 more soul went along" : n + " more souls went along";
    }

    static String stillTime(Language language) {
        return language == Language.pt ? "ainda dá tempo:" : "there is still time:";
    }
}
