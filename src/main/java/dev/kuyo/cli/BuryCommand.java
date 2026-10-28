package dev.kuyo.cli;

import dev.kuyo.autopsy.Shisho;
import dev.kuyo.cemetery.Cemetery;
import dev.kuyo.cemetery.Texts;
import dev.kuyo.domain.Coroner;
import dev.kuyo.domain.Soul;
import dev.kuyo.epitaph.EpitaphWriter;
import dev.kuyo.epitaph.Language;
import dev.kuyo.epitaph.Monastery;
import dev.kuyo.git.Exhumer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "bury", description = "Writes the epitaphs and renders the cemetery as HTML.")
public class BuryCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path of the git repository.", defaultValue = ".")
    Path repo;

    @Option(names = {"-o", "--output"}, defaultValue = "cemetery.html", description = "Output HTML file.")
    Path output;

    @Option(names = "--lang", defaultValue = "en", description = "Language of the epitaphs: ${COMPLETION-CANDIDATES}.")
    Language language;

    @Option(names = "--no-epitaphs", description = "Renders the cemetery without calling the model.")
    boolean noEpitaphs;

    @Option(names = "--autopsy", description = "Asks the Shishō to examine the last contents of every dead Java file.")
    boolean autopsy;

    @Inject
    Exhumer exhumer;

    @Inject
    Shisho shisho;

    @Inject
    Coroner coroner;

    @Inject
    EpitaphWriter epitaphWriter;

    @Inject
    Monastery monastery;

    @Inject
    @Location("cemetery.html")
    Template cemetery;

    @ConfigProperty(name = "kuyo.ollama.url")
    String ollamaUrl;

    @Override
    public Integer call() throws Exception {
        List<Soul> souls = exhumer.exhume(repo).stream().map(coroner::examine).toList();
        // the empty cemetery is published too: in an Action, the page exists before the first death
        if (souls.isEmpty()) {
            System.out.println("No dead files. Nobody to mourn.");
        }

        if (autopsy && !souls.isEmpty()) {
            Optional<String> problem = shisho.problem();
            if (problem.isPresent()) {
                System.err.println(problem.get());
                return 1;
            }
            souls = performAutopsies(souls);
        }

        Path folder = output.toAbsolutePath().getParent();
        Files.createDirectories(folder);
        if (!noEpitaphs && !souls.isEmpty()) {
            epitaphWriter.openCache(folder.resolve("kuyo.json"));
            // with every epitaph already cached, the monk does not need to be awake
            boolean allCached = souls.stream().allMatch(s -> epitaphWriter.cached(s, language));
            Optional<String> problem = allCached ? Optional.empty() : monastery.problem();
            if (problem.isPresent()) {
                System.err.println(problem.get());
                System.err.println("To render the cemetery without epitaphs, use --no-epitaphs.");
                return 1;
            }
            try {
                souls = writeEpitaphs(souls);
            } catch (RuntimeException e) {
                System.err.printf("%nThe monk did not answer. Is Ollama running at %s with the model pulled?%n  %s%n",
                        ollamaUrl, e.getMessage());
                return 1;
            }
        }

        String repoName = repo.toAbsolutePath().normalize().getFileName().toString().replaceFirst("\\.git$", "");
        String html = cemetery.data("c", Cemetery.build(repoName, souls, Texts.of(language))).render();
        Files.writeString(output, html);
        System.out.printf("%n%d souls laid to rest in %s%n", souls.size(), output.toAbsolutePath());
        return 0;
    }

    private List<Soul> performAutopsies(List<Soul> souls) throws Exception {
        List<Soul> examined = new ArrayList<>(souls.size());
        for (Soul soul : souls) {
            if (shisho.examines(soul.deceased())) {
                System.out.printf("師 autopsy of %s%n", soul.deceased().path());
                soul = shisho.examine(soul.deceased(), exhumer.buildFile(repo, soul.deceased()), language)
                        .map(soul::withAutopsy)
                        .orElse(soul);
            }
            examined.add(soul);
        }
        return examined;
    }

    private List<Soul> writeEpitaphs(List<Soul> souls) throws Exception {
        List<Soul> withEpitaph = new ArrayList<>(souls.size());
        for (int i = 0; i < souls.size(); i++) {
            Soul soul = souls.get(i);
            boolean cached = epitaphWriter.cached(soul, language);
            System.out.printf("[%d/%d] ✝ %s%s%n", i + 1, souls.size(), soul.deceased().path(),
                    cached ? "  (cache)" : "");
            withEpitaph.add(soul.withEpitaph(epitaphWriter.epitaph(soul, language)));
        }
        return withEpitaph;
    }
}
