package dev.kuyo.git;

import dev.kuyo.dominio.CommitDaMorte;
import dev.kuyo.dominio.Falecido;
import jakarta.enterprise.context.ApplicationScoped;
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
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Percorre o histórico do HEAD do mais antigo ao mais novo e devolve os arquivos que morreram.
 *
 * <p>Só segue o primeiro pai: um merge conta como um commit só, então a mesma deleção não aparece
 * duas vezes e o que morreu numa branch nunca mergeada fica de fora. Renomeações e movimentações
 * não são mortes, a vida continua no caminho novo.
 */
@ApplicationScoped
public class Exumador {

    private static final int LIMITE_ULTIMO_CONTEUDO = 64 * 1024;

    private static final Pattern TESTE = Pattern.compile(
            "(^|/)(src/test|tests?|__tests__|spec)/"
                    + "|(Test|Tests|IT|Spec)\\.[A-Za-z]+$"
                    + "|(^|/)test_[^/]+\\.py$|_test\\.(go|py)$"
                    + "|\\.(test|spec)\\.[jt]sx?$");

    /** Arquivos que o sistema operacional deixa para trás. Não são código, não ganham funeral. */
    private static final Pattern LIXO = Pattern.compile(
            ":Zone\\.Identifier$|(^|/)(\\.DS_Store|Thumbs\\.db|desktop\\.ini)$");

    public List<Falecido> exumar(Path caminhoRepo) throws IOException {
        try (Repository repo = abrir(caminhoRepo);
             RevWalk walk = new RevWalk(repo);
             DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

            ObjectId head = repo.resolve("HEAD");
            if (head == null) {
                return List.of();
            }

            df.setRepository(repo);
            df.setDetectRenames(true);
            // mesmo limiar do git (50%); o padrão do JGit (60%) transforma renames com refactor em morte
            df.getRenameDetector().setRenameScore(50);

            walk.setFirstParent(true);
            walk.sort(RevSort.TOPO);
            walk.sort(RevSort.REVERSE, true);
            walk.markStart(walk.parseCommit(head));

            ObjectReader reader = repo.newObjectReader();
            Map<String, Vida> vivos = new HashMap<>();
            List<Falecido> mortos = new ArrayList<>();

            for (RevCommit commit : walk) {
                RevCommit pai = commit.getParentCount() > 0 ? walk.parseCommit(commit.getParent(0)) : null;
                List<DiffEntry> diffs = df.scan(arvore(reader, pai), arvore(reader, commit));
                Instant quando = commit.getCommitterIdent().getWhenAsInstant();
                String autor = commit.getAuthorIdent().getName();

                CommitDaMorte commitDaMorte = null;
                for (DiffEntry d : diffs) {
                    switch (d.getChangeType()) {
                        case ADD, COPY -> vivos.put(d.getNewPath(), new Vida(quando, autor));
                        case MODIFY -> vivos.computeIfAbsent(d.getNewPath(), p -> new Vida(quando, autor))
                                .registrar(quando, autor);
                        case RENAME -> {
                            Vida vida = vivos.remove(d.getOldPath());
                            if (vida == null) {
                                vida = new Vida(quando, autor);
                            } else {
                                vida.registrar(quando, autor);
                            }
                            vivos.put(d.getNewPath(), vida);
                        }
                        case DELETE -> {
                            if (LIXO.matcher(d.getOldPath()).find()) {
                                vivos.remove(d.getOldPath());
                                continue;
                            }
                            if (commitDaMorte == null) {
                                commitDaMorte = descreverMorte(commit, diffs, df);
                            }
                            Vida vida = vivos.remove(d.getOldPath());
                            if (vida == null) {
                                vida = new Vida(quando, autor);
                            }
                            mortos.add(sepultar(d, vida, commitDaMorte, reader));
                        }
                    }
                }
            }
            return mortos;
        }
    }

    private static Repository abrir(Path caminhoRepo) throws IOException {
        File dir = caminhoRepo.toFile();
        FileRepositoryBuilder builder = new FileRepositoryBuilder().setMustExist(true);
        // aceita também clones --bare, onde o próprio diretório é o .git
        if (RepositoryCache.FileKey.isGitRepository(dir, FS.DETECTED)) {
            return builder.setGitDir(dir).build();
        }
        return builder.findGitDir(dir).build();
    }

    private static AbstractTreeIterator arvore(ObjectReader reader, RevCommit commit) throws IOException {
        if (commit == null) {
            return new EmptyTreeIterator();
        }
        return new CanonicalTreeParser(null, reader, commit.getTree());
    }

    private static CommitDaMorte descreverMorte(RevCommit commit, List<DiffEntry> diffs, DiffFormatter df)
            throws IOException {
        int linhasRemovidas = 0;
        boolean mexeuEmTestes = false;
        for (DiffEntry d : diffs) {
            for (Edit e : df.toFileHeader(d).toEditList()) {
                linhasRemovidas += e.getLengthA();
            }
            String caminho = d.getChangeType() == DiffEntry.ChangeType.DELETE ? d.getOldPath() : d.getNewPath();
            mexeuEmTestes |= TESTE.matcher(caminho).find();
        }
        PersonIdent autor = commit.getAuthorIdent();
        return new CommitDaMorte(
                commit.getId().abbreviate(8).name(),
                commit.getFullMessage().strip(),
                autor.getName(),
                autor.getWhenAsInstant().atZone(autor.getZoneId()),
                diffs.size(),
                linhasRemovidas,
                mexeuEmTestes);
    }

    private static Falecido sepultar(DiffEntry d, Vida vida, CommitDaMorte morte, ObjectReader reader)
            throws IOException {
        byte[] bytes = reader.open(d.getOldId().toObjectId()).getCachedBytes(Integer.MAX_VALUE);
        boolean binario = RawText.isBinary(bytes);
        int linhas = binario ? 0 : new RawText(bytes).size();
        String conteudo = binario ? null
                : new String(bytes, 0, Math.min(bytes.length, LIMITE_ULTIMO_CONTEUDO), StandardCharsets.UTF_8);

        return new Falecido(
                d.getOldPath(),
                vida.nascimento,
                morte.quando().toInstant(),
                vida.ultimoCommit,
                Set.copyOf(vida.autores),
                vida.commits,
                linhas,
                binario,
                morte,
                conteudo);
    }

    private static final class Vida {
        final Instant nascimento;
        Instant ultimoCommit;
        final Set<String> autores = new LinkedHashSet<>();
        int commits = 1;

        Vida(Instant nascimento, String autor) {
            this.nascimento = nascimento;
            this.ultimoCommit = nascimento;
            autores.add(autor);
        }

        void registrar(Instant quando, String autor) {
            ultimoCommit = quando;
            autores.add(autor);
            commits++;
        }
    }
}
