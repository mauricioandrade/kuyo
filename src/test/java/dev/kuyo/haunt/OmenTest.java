package dev.kuyo.haunt;

import dev.kuyo.haunt.Omen.Type;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OmenTest {

    @Test
    void rmKeepsTheTargetsWithoutTheOptions() {
        assertEquals(List.of(new Omen(Type.DELETE, List.of("src/Email Service.java", "-odd.txt"))),
                Omen.read("sudo rm -rf 'src/Email Service.java' -- -odd.txt"));
        assertEquals(List.of(new Omen(Type.DELETE, List.of("a.java"))), Omen.read("git -C . rm --cached a.java"));
    }

    @Test
    void eachCommandOnTheLineCountsSeparately() {
        assertEquals(List.of(new Omen(Type.DELETE, List.of("x")), new Omen(Type.COMMIT, List.of())),
                Omen.read("git rm x && git commit -m 'farewell; x'"));
    }

    @Test
    void onlyWhatDestroysHaunts() {
        assertTrue(Omen.read("git reset --soft HEAD~1").isEmpty());
        assertTrue(Omen.read("git clean -n").isEmpty());
        assertTrue(Omen.read("git push origin main").isEmpty());
        assertTrue(Omen.read("git restore --staged a.java").isEmpty());
        assertTrue(Omen.read("git status").isEmpty());
        assertTrue(Omen.read("echo 'rm -rf /'").isEmpty());
        assertTrue(Omen.read("rm").isEmpty());

        assertEquals(Type.RESET, Omen.read("git reset --hard origin/main").getFirst().type());
        assertEquals(Type.CLEAN, Omen.read("git clean -fdx").getFirst().type());
        assertEquals(Type.FORCE_PUSH, Omen.read("git push --force-with-lease").getFirst().type());
        assertEquals(Type.BRANCH, Omen.read("git branch -D old").getFirst().type());
        assertEquals(Type.STASH, Omen.read("git stash drop").getFirst().type());
        assertEquals(Type.RESTORE, Omen.read("git checkout -- .").getFirst().type());
    }

    @Test
    void condemnedAcceptsFileFolderAndGlob() {
        Path root = Path.of("/repo");
        Predicate<String> c = Haunting.condemned(root, root.resolve("src"), List.of("Email.java", "util", "*.md"));

        assertTrue(c.test("src/Email.java"));
        assertTrue(c.test("src/util/Dates.java"));
        assertTrue(c.test("src/NOTES.md"));
        assertFalse(c.test("Email.java"));
        assertFalse(c.test("src/utilities/X.java"));
        assertFalse(c.test("README.md"));
    }
}
