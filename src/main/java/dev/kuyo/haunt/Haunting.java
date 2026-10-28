package dev.kuyo.haunt;

import dev.kuyo.cemetery.Texts;
import dev.kuyo.domain.Coroner;
import dev.kuyo.domain.Deceased;
import dev.kuyo.domain.Soul;
import dev.kuyo.epitaph.EpitaphWriter;
import dev.kuyo.epitaph.Language;
import dev.kuyo.epitaph.Monastery;
import dev.kuyo.git.Exhumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Decides what the terminal whispers after a command. When a file dies (or is dying: deleted but not
 * committed yet), its ghost speaks through Gemma; everything else gets fixed lines.
 * Silence is the answer to anything that kills nothing.
 */
@ApplicationScoped
public class Haunting {

    /** How long after the commit it still counts as what the user just did. */
    private static final Duration COMMIT_WINDOW = Duration.ofSeconds(90);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    @Inject
    Exhumer exhumer;

    @Inject
    Coroner coroner;

    @Inject
    Monastery monastery;

    @Inject
    Whisperer whisperer;

    /** What the ghost says and, when there is still time, how to bring it back. */
    public record Whisper(String words, String note) {
    }

    public List<Whisper> listen(Path cwd, String line, Language language, boolean withMonk) throws IOException {
        List<Whisper> whispers = new ArrayList<>();
        for (Omen omen : Omen.read(line)) {
            switch (omen.type()) {
                case COMMIT -> justDead(cwd).ifPresent(souls ->
                        whispers.add(ghost(souls, line, false, null, language, withMonk)));
                case DELETE -> {
                    List<Soul> souls = dying(cwd, omen.targets(), line);
                    if (!souls.isEmpty()) {
                        whispers.add(ghost(souls, line, true, omen.targets(), language, withMonk));
                    }
                }
                default -> whispers.add(new Whisper(Lines.of(omen.type(), language), null));
            }
        }
        return whispers;
    }

    private Optional<List<Soul>> justDead(Path cwd) throws IOException {
        Optional<String> hash = exhumer.recentCommit(cwd, COMMIT_WINDOW);
        if (hash.isEmpty()) {
            return Optional.empty();
        }
        List<Soul> souls = exhumer.exhume(cwd).stream()
                .filter(d -> d.deathCommit().hash().equals(hash.get()))
                .map(coroner::examine)
                .toList();
        return souls.isEmpty() ? Optional.empty() : Optional.of(souls);
    }

    private List<Soul> dying(Path cwd, List<String> targets, String line) throws IOException {
        Optional<Path> root = exhumer.root(cwd);
        if (root.isEmpty()) {
            return List.of();
        }
        return exhumer.dying(cwd, condemned(root.get(), cwd, targets), line).stream()
                .map(coroner::examine)
                .toList();
    }

    /** A repository path is condemned when it is one of the targets, lies inside one, or matches a glob. */
    static Predicate<String> condemned(Path root, Path cwd, List<String> targets) {
        Path absoluteRoot = root.toAbsolutePath().normalize();
        List<Predicate<Path>> tests = new ArrayList<>();
        for (String target : targets) {
            Path absolute = cwd.toAbsolutePath()
                    .resolve(target.replaceFirst("^~(?=/|$)", System.getProperty("user.home")))
                    .normalize();
            if (target.matches(".*[*?\\[].*")) {
                PathMatcher glob = FileSystems.getDefault().getPathMatcher("glob:" + absolute);
                tests.add(p -> glob.matches(p) || p.getParent() != null && glob.matches(p.getParent()));
            } else {
                tests.add(p -> p.startsWith(absolute));
            }
        }
        return path -> {
            Path p = absoluteRoot.resolve(path);
            return tests.stream().anyMatch(t -> t.test(p));
        };
    }

    private Whisper ghost(List<Soul> souls, String line, boolean dying, List<String> targets,
                          Language language, boolean withMonk) {
        Soul soul = souls.stream().max(Comparator.comparingDouble(Soul::restlessness)).orElseThrow();
        String words = null;
        if (withMonk && monastery.problem().isEmpty()) {
            try {
                words = EpitaphWriter.clean(whisperer.whisper(language.nameForTheModel,
                        facts(soul, souls.size() - 1, line, dying)));
            } catch (RuntimeException e) {
                words = null;
            }
        }
        if (words == null || words.isBlank()) {
            words = Lines.ghost(soul, dying, language);
        }

        List<String> note = new ArrayList<>();
        if (souls.size() > 1) {
            note.add(Lines.companions(souls.size() - 1, language));
        }
        if (dying) {
            String pathspec = souls.size() == 1
                    ? "':/" + soul.deceased().path().replace("'", "'\\''") + "'"
                    : String.join(" ", targets);
            note.add(Lines.stillTime(language) + " git restore -SW -- " + pathspec);
        }
        return new Whisper(words, note.isEmpty() ? null : String.join(" · ", note));
    }

    static String facts(Soul soul, int companions, String line, boolean dying) {
        Deceased d = soul.deceased();
        Texts en = Texts.of(Language.en);
        String death = dying
                ? "Killed by: the shell command `" + line + "`. Not committed yet: you are still dying."
                : "Killed by: the commit \"" + firstLine(d.deathCommit().message()) + "\", just made.";
        return """
                File: %s
                Lived: %s
                Commits in life: %d
                Authors: %s
                Cause of death: %s (restlessness %.2f of 1)
                %s
                Time of death: %s
                Going with you: %d other files
                """.formatted(d.path(), en.life(Duration.between(d.born(), d.died())), d.commitsInLife(),
                String.join(", ", d.authors()), en.cause(soul.cause()).toLowerCase(), soul.restlessness(), death,
                HOUR.format(d.deathCommit().when()), companions);
    }

    private static String firstLine(String message) {
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
