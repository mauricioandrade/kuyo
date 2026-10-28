package dev.kuyo.git;

import dev.kuyo.autopsia.ArquivoDeBuild;
import dev.kuyo.dominio.CommitDaMorte;
import dev.kuyo.dominio.Falecido;
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
        try (Repository repo = abrir(caminhoRepo)) {
            List<Falecido> mortos = new ArrayList<>();
            percorrer(repo, mortos);
            return mortos;
        }
    }

    /**
     * Os arquivos vivos no HEAD que o {@code condenado} aponta e que já sumiram do disco ou do índice,
     * mas cuja morte ainda não virou commit. São examinados como se morressem agora, com o comando
     * que os apagou como últimas palavras: dá para saber do que vão morrer antes do commit.
     */
    public List<Falecido> moribundos(Path caminhoRepo, Predicate<String> condenado, String ultimasPalavras)
            throws IOException {
        try (Repository repo = abrir(caminhoRepo); ObjectReader reader = repo.newObjectReader()) {
            Map<String, Vida> vivos = percorrer(repo, new ArrayList<>());
            ObjectId head = repo.resolve("HEAD^{tree}");
            if (head == null || repo.isBare()) {
                return List.of();
            }
            DirCache indice = repo.readDirCache();
            Path raiz = repo.getWorkTree().toPath();

            List<String> caminhos = new ArrayList<>();
            for (String caminho : vivos.keySet()) {
                if (condenado.test(caminho)
                        && (!Files.exists(raiz.resolve(caminho)) || indice.findEntry(caminho) < 0)) {
                    caminhos.add(caminho);
                }
            }
            caminhos.sort(null);

            List<byte[]> corpos = new ArrayList<>();
            int linhasRemovidas = 0;
            boolean mexeuEmTestes = false;
            for (String caminho : caminhos) {
                byte[] bytes = new byte[0];
                try (TreeWalk tw = TreeWalk.forPath(repo, caminho, head)) {
                    if (tw != null) {
                        bytes = reader.open(tw.getObjectId(0)).getCachedBytes(Integer.MAX_VALUE);
                    }
                }
                corpos.add(bytes);
                linhasRemovidas += RawText.isBinary(bytes) ? 0 : new RawText(bytes).size();
                mexeuEmTestes |= TESTE.matcher(caminho).find();
            }

            String autor = Optional.ofNullable(repo.getConfig().getString("user", null, "name")).orElse("?");
            CommitDaMorte agora = new CommitDaMorte("", ultimasPalavras, autor, ZonedDateTime.now(),
                    caminhos.size(), linhasRemovidas, mexeuEmTestes);
            List<Falecido> moribundos = new ArrayList<>();
            for (int i = 0; i < caminhos.size(); i++) {
                moribundos.add(sepultar(caminhos.get(i), corpos.get(i), vivos.get(caminhos.get(i)), agora));
            }
            return moribundos;
        }
    }

    /** A raiz da árvore de trabalho; vazia num clone --bare. */
    public Optional<Path> raiz(Path caminhoRepo) throws IOException {
        try (Repository repo = abrir(caminhoRepo)) {
            return repo.isBare() ? Optional.empty() : Optional.of(repo.getWorkTree().toPath());
        }
    }

    /** O hash curto do HEAD, se ele foi commitado há menos de {@code janela}. */
    public Optional<String> commitRecente(Path caminhoRepo, Duration janela) throws IOException {
        try (Repository repo = abrir(caminhoRepo); RevWalk walk = new RevWalk(repo)) {
            ObjectId head = repo.resolve("HEAD");
            if (head == null) {
                return Optional.empty();
            }
            RevCommit commit = walk.parseCommit(head);
            Instant quando = commit.getCommitterIdent().getWhenAsInstant();
            return Duration.between(quando, Instant.now()).compareTo(janela) <= 0
                    ? Optional.of(commit.getId().abbreviate(8).name())
                    : Optional.empty();
        }
    }

    /** Percorre o histórico guardando os mortos em {@code mortos}; devolve quem continua vivo no HEAD. */
    private static Map<String, Vida> percorrer(Repository repo, List<Falecido> mortos) throws IOException {
        try (RevWalk walk = new RevWalk(repo);
             DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
             ObjectReader reader = repo.newObjectReader()) {

            Map<String, Vida> vivos = new HashMap<>();
            ObjectId head = repo.resolve("HEAD");
            if (head == null) {
                return vivos;
            }

            df.setRepository(repo);
            df.setDetectRenames(true);
            // mesmo limiar do git (50%); o padrão do JGit (60%) transforma renames com refactor em morte
            df.getRenameDetector().setRenameScore(50);

            walk.setFirstParent(true);
            walk.sort(RevSort.TOPO);
            walk.sort(RevSort.REVERSE, true);
            walk.markStart(walk.parseCommit(head));

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
                            byte[] bytes = reader.open(d.getOldId().toObjectId()).getCachedBytes(Integer.MAX_VALUE);
                            mortos.add(sepultar(d.getOldPath(), bytes, vida, commitDaMorte));
                        }
                    }
                }
            }
            return vivos;
        }
    }

    /**
     * O pom.xml ou build.gradle mais próximo do morto, subindo de pasta em pasta, como estava no
     * commit anterior à morte. Diz ao Shishō qual Java e qual framework o arquivo conheceu em vida.
     */
    public Optional<ArquivoDeBuild> arquivoDeBuild(Path caminhoRepo, Falecido f) throws IOException {
        try (Repository repo = abrir(caminhoRepo); RevWalk walk = new RevWalk(repo)) {
            ObjectId antes = repo.resolve(f.commitDaMorte().hash() + "^");
            if (antes == null) {
                return Optional.empty();
            }
            RevCommit commit = walk.parseCommit(antes);
            String pasta = f.caminho().contains("/") ? f.caminho().substring(0, f.caminho().lastIndexOf('/')) : "";
            while (true) {
                for (String nome : BUILDS) {
                    String caminho = pasta.isEmpty() ? nome : pasta + "/" + nome;
                    try (TreeWalk tw = TreeWalk.forPath(repo, caminho, commit.getTree())) {
                        if (tw != null) {
                            byte[] bytes = repo.open(tw.getObjectId(0)).getCachedBytes(Integer.MAX_VALUE);
                            return Optional.of(new ArquivoDeBuild(pasta, nome, new String(bytes, StandardCharsets.UTF_8)));
                        }
                    }
                }
                if (pasta.isEmpty()) {
                    return Optional.empty();
                }
                pasta = pasta.contains("/") ? pasta.substring(0, pasta.lastIndexOf('/')) : "";
            }
        }
    }

    private static final List<String> BUILDS = List.of("pom.xml", "build.gradle", "build.gradle.kts");

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

    private static Falecido sepultar(String caminho, byte[] bytes, Vida vida, CommitDaMorte morte) {
        boolean binario = RawText.isBinary(bytes);
        int linhas = binario ? 0 : new RawText(bytes).size();
        String conteudo = binario ? null
                : new String(bytes, 0, Math.min(bytes.length, LIMITE_ULTIMO_CONTEUDO), StandardCharsets.UTF_8);

        return new Falecido(
                caminho,
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
