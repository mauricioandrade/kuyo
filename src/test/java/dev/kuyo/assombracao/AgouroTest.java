package dev.kuyo.assombracao;

import dev.kuyo.assombracao.Agouro.Tipo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgouroTest {

    @Test
    void rmGuardaOsAlvosSemAsOpcoes() {
        assertEquals(List.of(new Agouro(Tipo.APAGAR, List.of("src/Email Service.java", "-estranho.txt"))),
                Agouro.ler("sudo rm -rf 'src/Email Service.java' -- -estranho.txt"));
        assertEquals(List.of(new Agouro(Tipo.APAGAR, List.of("a.java"))), Agouro.ler("git -C . rm --cached a.java"));
    }

    @Test
    void cadaComandoDaLinhaContaSeparado() {
        assertEquals(List.of(new Agouro(Tipo.APAGAR, List.of("x")), new Agouro(Tipo.COMMIT, List.of())),
                Agouro.ler("git rm x && git commit -m 'adeus; x'"));
    }

    @Test
    void soOQueDestroiAssombra() {
        assertTrue(Agouro.ler("git reset --soft HEAD~1").isEmpty());
        assertTrue(Agouro.ler("git clean -n").isEmpty());
        assertTrue(Agouro.ler("git push origin main").isEmpty());
        assertTrue(Agouro.ler("git restore --staged a.java").isEmpty());
        assertTrue(Agouro.ler("git status").isEmpty());
        assertTrue(Agouro.ler("echo 'rm -rf /'").isEmpty());
        assertTrue(Agouro.ler("rm").isEmpty());

        assertEquals(Tipo.RESET, Agouro.ler("git reset --hard origin/main").getFirst().tipo());
        assertEquals(Tipo.CLEAN, Agouro.ler("git clean -fdx").getFirst().tipo());
        assertEquals(Tipo.PUSH_FORCADO, Agouro.ler("git push --force-with-lease").getFirst().tipo());
        assertEquals(Tipo.BRANCH, Agouro.ler("git branch -D velha").getFirst().tipo());
        assertEquals(Tipo.STASH, Agouro.ler("git stash drop").getFirst().tipo());
        assertEquals(Tipo.RESTAURAR, Agouro.ler("git checkout -- .").getFirst().tipo());
    }

    @Test
    void condenadoAceitaArquivoPastaEGlob() {
        Path raiz = Path.of("/repo");
        Predicate<String> c = Assombracao.condenado(raiz, raiz.resolve("src"), List.of("Email.java", "util", "*.md"));

        assertTrue(c.test("src/Email.java"));
        assertTrue(c.test("src/util/Datas.java"));
        assertTrue(c.test("src/LEIAME.md"));
        assertFalse(c.test("Email.java"));
        assertFalse(c.test("src/utilitarios/X.java"));
        assertFalse(c.test("README.md"));
    }
}
