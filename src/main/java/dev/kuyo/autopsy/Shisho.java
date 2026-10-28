package dev.kuyo.autopsy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kuyo.domain.Deceased;
import dev.kuyo.domain.Lesson;
import dev.kuyo.epitaph.Language;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Calls the <a href="https://github.com/mauricioandrade/shisho">Shishō</a> to examine the body.
 *
 * <p>The file's last contents go into a temporary folder, next to the build file that was alive when
 * it died, and the Shishō analyzes that folder as a project. Only the lessons that land on the
 * deceased itself count: what the Shishō says about the pom.xml is about the project, not about it.
 */
@ApplicationScoped
public class Shisho {

    private static final long PATIENCE_SECONDS = 60;

    @Inject
    ObjectMapper json;

    @ConfigProperty(name = "kuyo.shisho")
    String command;

    /** The Shishō only reads Java; the other dead go without an autopsy. */
    public boolean examines(Deceased d) {
        return d.path().endsWith(".java") && !d.binary() && d.lastContents() != null;
    }

    /** Empty when the Shishō answers; otherwise, what stands in the way of the autopsy. */
    public Optional<String> problem() {
        try {
            Process p = start(List.of("--version"), Map.of());
            if (p.waitFor(PATIENCE_SECONDS, TimeUnit.SECONDS) && p.exitValue() == 0) {
                return Optional.empty();
            }
            p.destroyForcibly();
        } catch (IOException e) {
            // falls through to the message below
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.of("The Shishō did not answer (" + command + "). Install it with the install.sh from "
                + "https://github.com/mauricioandrade/shisho or point KUYO_SHISHO at shisho.jar.");
    }

    /** Empty when the Shishō could not read the body (a file cut in half, for example). */
    public Optional<List<Lesson>> examine(Deceased d, Optional<BuildFile> build, Language language)
            throws IOException {
        Path table = Files.createTempDirectory("kuyo-autopsy");
        try {
            String relative = build.map(b -> relative(d.path(), b.folder())).orElse(d.path());
            Path body = table.resolve(relative);
            Files.createDirectories(body.getParent());
            Files.writeString(body, d.lastContents());
            if (build.isPresent()) {
                Files.writeString(table.resolve(build.get().name()), build.get().contents());
            }

            Path sarif = table.resolve("shisho.sarif");
            // SHISHO_IDIOMA is the Shishō's own setting for the language it writes in
            Process p = start(List.of("analyze", "--format", "sarif", "--output", sarif.toString(), table.toString()),
                    Map.of("SHISHO_IDIOMA", language.name()));
            if (!p.waitFor(PATIENCE_SECONDS, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return Optional.empty();
            }
            // 0 without a severe lesson, 1 with one; anything else is the Shishō saying it could not read
            if (p.exitValue() > 1 || !Files.exists(sarif)) {
                return Optional.empty();
            }
            return Optional.of(lessons(json.readTree(sarif.toFile()), relative));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            try (Stream<Path> files = Files.walk(table)) {
                files.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
            }
        }
    }

    static List<Lesson> lessons(JsonNode sarif, String path) {
        JsonNode run = sarif.path("runs").path(0);
        Map<String, String> titles = new HashMap<>();
        for (JsonNode rule : run.path("tool").path("driver").path("rules")) {
            titles.put(rule.path("id").asText(), rule.path("shortDescription").path("text").asText());
        }
        List<Lesson> lessons = new ArrayList<>();
        for (JsonNode r : run.path("results")) {
            JsonNode location = r.path("locations").path(0).path("physicalLocation");
            if (!path.equals(location.path("artifactLocation").path("uri").asText())) {
                continue;
            }
            String kata = r.path("ruleId").asText();
            lessons.add(new Lesson(kata, r.path("properties").path("severity").asText("INFO"),
                    titles.getOrDefault(kata, kata), location.path("region").path("startLine").asInt(0)));
        }
        // the same kata on the same line (a System.out and a printStackTrace, say) is a single lesson
        return lessons.stream().distinct().toList();
    }

    private static String relative(String path, String folder) {
        return folder.isEmpty() ? path : path.substring(folder.length() + 1);
    }

    /** Accepts a command on the PATH or the path of a shisho.jar, which runs on Kuyō's own JVM. */
    private Process start(List<String> arguments, Map<String, String> environment) throws IOException {
        List<String> line = new ArrayList<>();
        if (command.endsWith(".jar")) {
            line.add(ProcessHandle.current().info().command().orElse("java"));
            line.add("-jar");
            line.add(command);
        } else {
            line.addAll(List.of(command.strip().split("\\s+")));
        }
        line.addAll(arguments);
        ProcessBuilder pb = new ProcessBuilder(line).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.environment().putAll(environment);
        return pb.start();
    }
}
