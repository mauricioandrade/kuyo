package dev.kuyo.cli;

import dev.kuyo.domain.Coroner;
import dev.kuyo.domain.Deceased;
import dev.kuyo.domain.Soul;
import dev.kuyo.epitaph.Language;
import dev.kuyo.epitaph.Monastery;
import dev.kuyo.epitaph.MonkModel;
import dev.kuyo.git.Exhumer;
import dev.kuyo.seance.Medium;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "seance", description = "Talks to a dead file.")
public class SeanceCommand implements Callable<Integer> {

    /** In both languages the ghost speaks. */
    private static final List<String> FAREWELLS = List.of("/bye", "/sair", "tchau", "bye", "adeus", "goodbye");

    @Parameters(index = "0", description = "Path of the git repository.")
    Path repo;

    @Parameters(index = "1", description = "The dead file: its full path or just its name.")
    String file;

    @Option(names = "--lang", defaultValue = "en", description = "Language of the ghost: ${COMPLETION-CANDIDATES}.")
    Language language;

    @Option(names = {"-q", "--question"}, description = "Asks without opening the interactive session (repeatable).")
    List<String> questions;

    @Inject
    Exhumer exhumer;

    @Inject
    Coroner coroner;

    @Inject
    Monastery monastery;

    @Override
    public Integer call() throws Exception {
        List<Deceased> dead = exhumer.exhume(repo);
        Optional<Deceased> chosen = choose(dead);
        if (chosen.isEmpty()) {
            return 1;
        }
        Optional<String> problem = monastery.problem();
        if (problem.isPresent()) {
            System.err.println(problem.get());
            return 1;
        }

        Soul soul = coroner.examine(chosen.get());
        String name = name(soul.deceased().path());
        Medium medium = new Medium(MonkModel.create(400), soul, language);

        System.out.printf("%n  The incense is lit. %s, %s, answers.%n%n", soul.deceased().path(),
                soul.cause().name().toLowerCase(Locale.ROOT));
        speak(name, medium.call());

        if (questions != null) {
            for (String question : questions) {
                System.out.printf("> %s%n", question);
                speak(name, medium.ask(question));
            }
            return 0;
        }

        System.out.println("  (type /bye to let the spirit rest)\n");
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            System.out.print("> ");
            System.out.flush();
            String question = input.readLine();
            if (question == null || FAREWELLS.contains(question.strip().toLowerCase(Locale.ROOT))) {
                System.out.println("\n  The incense goes out. The spirit returns to its rest.");
                return 0;
            }
            if (!question.isBlank()) {
                speak(name, medium.ask(question));
            }
        }
    }

    /** Accepts the full path or just the name; if the file died more than once, calls its last life. */
    private Optional<Deceased> choose(List<Deceased> dead) {
        List<Deceased> exact = dead.stream().filter(d -> d.path().equals(file)).toList();
        List<Deceased> candidates = !exact.isEmpty() ? exact : dead.stream()
                .filter(d -> d.path().endsWith("/" + file) || name(d.path()).equals(file))
                .toList();

        if (candidates.isEmpty()) {
            System.err.printf("No dead file named %s.%n", file);
            String search = file.toLowerCase(Locale.ROOT);
            dead.stream().map(Deceased::path).distinct()
                    .filter(p -> p.toLowerCase(Locale.ROOT).contains(search))
                    .limit(5)
                    .forEach(p -> System.err.println("  perhaps: " + p));
            return Optional.empty();
        }

        List<String> paths = candidates.stream().map(Deceased::path).distinct().toList();
        if (paths.size() > 1) {
            System.err.printf("More than one %s is buried here. Say which:%n", file);
            paths.forEach(p -> System.err.println("  " + p));
            return Optional.empty();
        }
        if (candidates.size() > 1) {
            System.out.printf("  %s died %d times; calling its last life.%n", paths.getFirst(), candidates.size());
        }
        return Optional.of(candidates.getLast());
    }

    private static void speak(String name, String words) {
        System.out.printf("%s: %s%n%n", name, words);
    }

    private static String name(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
