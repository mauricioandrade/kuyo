package dev.kuyo.git;

import dev.kuyo.autopsy.BuildFile;
import dev.kuyo.domain.DeathCommit;
import dev.kuyo.domain.Deceased;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Walks HEAD's history from oldest to newest and returns the files that died.
 *
 * <p>Only the first parent is followed: a merge counts as a single commit, so the same deletion never
 * shows up twice, and whatever died on a branch that was never merged is left out. Renames and moves
 * are not deaths; life goes on at the new path.
 */
@ApplicationScoped
public class Exhumer {

    private static final int LAST_CONTENTS_LIMIT = 64 * 1024;

    private static final Pattern TEST = Pattern.compile(
            "(^|/)(src/test|tests?|__tests__|spec)/"
                    + "|(Test|Tests|IT|Spec)\\.[A-Za-z]+$"
                    + "|(^|/)test_[^/]+\\.py$|_test\\.(go|py)$"
                    + "|\\.(test|spec)\\.[jt]sx?$");

    /** Files the operating system leaves behind. They are not code and get no funeral. */
    private static final Pattern LITTER = Pattern.compile(
            ":Zone\\.Identifier$|(^|/)(\\.DS_Store|Thumbs\\.db|desktop\\.ini)$");

    public List<Deceased> exhume(Path repoPath) throws IOException {
        try (Repository repo = open(repoPath)) {
            List<Deceased> dead = new ArrayList<>();
            walk(repo, dead);
            return dead;
        }
    }

    /**
     * The files alive in HEAD that {@code condemned} points at and that are already gone from disk or
     * from the index, but whose death is not committed yet. They are examined as if they died right now,
     * with the command that deleted them as their last words: you can tell how they will die before the commit.
     */
    public List<Deceased> dying(Path repoPath, Predicate<String> condemned, String lastWords) throws IOException {
        try (Repository repo = open(repoPath); ObjectReader reader = repo.newObjectReader()) {
            Map<String, Life> alive = walk(repo, new ArrayList<>());
            ObjectId head = repo.resolve("HEAD^{tree}");
            if (head == null || repo.isBare()) {
                return List.of();
            }
            DirCache index = repo.readDirCache();
            Path root = repo.getWorkTree().toPath();

            List<String> paths = new ArrayList<>();
            for (String path : alive.keySet()) {
                if (condemned.test(path) && (!Files.exists(root.resolve(path)) || index.findEntry(path) < 0)) {
                    paths.add(path);
                }
            }
            paths.sort(null);

            List<byte[]> bodies = new ArrayList<>();
            int linesRemoved = 0;
            boolean touchedTests = false;
            for (String path : paths) {
                byte[] bytes = new byte[0];
                try (TreeWalk tw = TreeWalk.forPath(repo, path, head)) {
                    if (tw != null) {
                        bytes = reader.open(tw.getObjectId(0)).getCachedBytes(Integer.MAX_VALUE);
                    }
                }
                bodies.add(bytes);
                linesRemoved += RawText.isBinary(bytes) ? 0 : new RawText(bytes).size();
                touchedTests |= TEST.matcher(path).find();
            }

            String author = Optional.ofNullable(repo.getConfig().getString("user", null, "name")).orElse("?");
            DeathCommit now = new DeathCommit("", lastWords, author, ZonedDateTime.now(),
                    paths.size(), linesRemoved, touchedTests);
            List<Deceased> dying = new ArrayList<>();
            for (int i = 0; i < paths.size(); i++) {
                dying.add(bury(paths.get(i), bodies.get(i), alive.get(paths.get(i)), now));
            }
            return dying;
        }
    }

    /** The root of the working tree; empty for a --bare clone. */
    public Optional<Path> root(Path repoPath) throws IOException {
        try (Repository repo = open(repoPath)) {
            return repo.isBare() ? Optional.empty() : Optional.of(repo.getWorkTree().toPath());
        }
    }

    /** HEAD's short hash, if it was committed less than {@code window} ago. */
    public Optional<String> recentCommit(Path repoPath, Duration window) throws IOException {
        try (Repository repo = open(repoPath); RevWalk walk = new RevWalk(repo)) {
            ObjectId head = repo.resolve("HEAD");
            if (head == null) {
                return Optional.empty();
            }
            RevCommit commit = walk.parseCommit(head);
            Instant when = commit.getCommitterIdent().getWhenAsInstant();
            return Duration.between(when, Instant.now()).compareTo(window) <= 0
                    ? Optional.of(commit.getId().abbreviate(8).name())
                    : Optional.empty();
        }
    }

    /** Walks the history, collecting the dead into {@code dead}; returns whoever is still alive in HEAD. */
    private static Map<String, Life> walk(Repository repo, List<Deceased> dead) throws IOException {
        try (RevWalk walk = new RevWalk(repo);
             DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
             ObjectReader reader = repo.newObjectReader()) {

            Map<String, Life> alive = new HashMap<>();
            ObjectId head = repo.resolve("HEAD");
            if (head == null) {
                return alive;
            }

            df.setRepository(repo);
            df.setDetectRenames(true);
            // the same threshold as git (50%); JGit's default (60%) turns renames with a refactor into deaths
            df.getRenameDetector().setRenameScore(50);

            walk.setFirstParent(true);
            walk.sort(RevSort.TOPO);
            walk.sort(RevSort.REVERSE, true);
            walk.markStart(walk.parseCommit(head));

            for (RevCommit commit : walk) {
                RevCommit parent = commit.getParentCount() > 0 ? walk.parseCommit(commit.getParent(0)) : null;
                List<DiffEntry> diffs = df.scan(tree(reader, parent), tree(reader, commit));
                Instant when = commit.getCommitterIdent().getWhenAsInstant();
                String author = commit.getAuthorIdent().getName();

                DeathCommit deathCommit = null;
                for (DiffEntry d : diffs) {
                    switch (d.getChangeType()) {
                        case ADD, COPY -> alive.put(d.getNewPath(), new Life(when, author));
                        case MODIFY -> alive.computeIfAbsent(d.getNewPath(), p -> new Life(when, author))
                                .record(when, author);
                        case RENAME -> {
                            Life life = alive.remove(d.getOldPath());
                            if (life == null) {
                                life = new Life(when, author);
                            } else {
                                life.record(when, author);
                            }
                            alive.put(d.getNewPath(), life);
                        }
                        case DELETE -> {
                            if (LITTER.matcher(d.getOldPath()).find()) {
                                alive.remove(d.getOldPath());
                                continue;
                            }
                            if (deathCommit == null) {
                                deathCommit = describeDeath(commit, diffs, df);
                            }
                            Life life = alive.remove(d.getOldPath());
                            if (life == null) {
                                life = new Life(when, author);
                            }
                            byte[] bytes = reader.open(d.getOldId().toObjectId()).getCachedBytes(Integer.MAX_VALUE);
                            dead.add(bury(d.getOldPath(), bytes, life, deathCommit));
                        }
                    }
                }
            }
            return alive;
        }
    }

    /**
     * The pom.xml or build.gradle closest to the deceased, climbing folder by folder, as it was in the
     * commit before the death. It tells the Shishō which Java and which framework the file knew in life.
     */
    public Optional<BuildFile> buildFile(Path repoPath, Deceased d) throws IOException {
        try (Repository repo = open(repoPath); RevWalk walk = new RevWalk(repo)) {
            ObjectId before = repo.resolve(d.deathCommit().hash() + "^");
            if (before == null) {
                return Optional.empty();
            }
            RevCommit commit = walk.parseCommit(before);
            String folder = d.path().contains("/") ? d.path().substring(0, d.path().lastIndexOf('/')) : "";
            while (true) {
                for (String name : BUILDS) {
                    String path = folder.isEmpty() ? name : folder + "/" + name;
                    try (TreeWalk tw = TreeWalk.forPath(repo, path, commit.getTree())) {
                        if (tw != null) {
                            byte[] bytes = repo.open(tw.getObjectId(0)).getCachedBytes(Integer.MAX_VALUE);
                            return Optional.of(new BuildFile(folder, name, new String(bytes, StandardCharsets.UTF_8)));
                        }
                    }
                }
                if (folder.isEmpty()) {
                    return Optional.empty();
                }
                folder = folder.contains("/") ? folder.substring(0, folder.lastIndexOf('/')) : "";
            }
        }
    }

    private static final List<String> BUILDS = List.of("pom.xml", "build.gradle", "build.gradle.kts");

    private static Repository open(Path repoPath) throws IOException {
        File dir = repoPath.toFile();
        FileRepositoryBuilder builder = new FileRepositoryBuilder().setMustExist(true);
        // also accepts --bare clones, where the directory itself is the .git
        if (RepositoryCache.FileKey.isGitRepository(dir, FS.DETECTED)) {
            return builder.setGitDir(dir).build();
        }
        return builder.findGitDir(dir).build();
    }

    private static AbstractTreeIterator tree(ObjectReader reader, RevCommit commit) throws IOException {
        if (commit == null) {
            return new EmptyTreeIterator();
        }
        return new CanonicalTreeParser(null, reader, commit.getTree());
    }

    private static DeathCommit describeDeath(RevCommit commit, List<DiffEntry> diffs, DiffFormatter df)
            throws IOException {
        int linesRemoved = 0;
        boolean touchedTests = false;
        for (DiffEntry d : diffs) {
            for (Edit e : df.toFileHeader(d).toEditList()) {
                linesRemoved += e.getLengthA();
            }
            String path = d.getChangeType() == DiffEntry.ChangeType.DELETE ? d.getOldPath() : d.getNewPath();
            touchedTests |= TEST.matcher(path).find();
        }
        PersonIdent author = commit.getAuthorIdent();
        return new DeathCommit(
                commit.getId().abbreviate(8).name(),
                commit.getFullMessage().strip(),
                author.getName(),
                author.getWhenAsInstant().atZone(author.getZoneId()),
                diffs.size(),
                linesRemoved,
                touchedTests);
    }

    private static Deceased bury(String path, byte[] bytes, Life life, DeathCommit death) {
        boolean binary = RawText.isBinary(bytes);
        int lines = binary ? 0 : new RawText(bytes).size();
        String contents = binary ? null
                : new String(bytes, 0, Math.min(bytes.length, LAST_CONTENTS_LIMIT), StandardCharsets.UTF_8);

        return new Deceased(
                path,
                life.born,
                death.when().toInstant(),
                life.lastCommit,
                Set.copyOf(life.authors),
                life.commits,
                lines,
                binary,
                death,
                contents);
    }

    private static final class Life {
        final Instant born;
        Instant lastCommit;
        final Set<String> authors = new LinkedHashSet<>();
        int commits = 1;

        Life(Instant born, String author) {
            this.born = born;
            this.lastCommit = born;
            authors.add(author);
        }

        void record(Instant when, String author) {
            lastCommit = when;
            authors.add(author);
            commits++;
        }
    }
}
