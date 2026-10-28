package dev.kuyo.epitaph;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kuyo.domain.CauseOfDeath;
import dev.kuyo.domain.DeathCommit;
import dev.kuyo.domain.Deceased;
import dev.kuyo.domain.Soul;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Asks the Monk for the epitaphs and keeps each one in a kuyo.json, so they are not rewritten on every run.
 * The key includes the death commit, the language and the model: changing any of them writes a new epitaph.
 */
@ApplicationScoped
public class EpitaphWriter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    @Inject
    Monk monk;

    @Inject
    ObjectMapper json;

    @ConfigProperty(name = "kuyo.model")
    String model;

    private Path cacheFile;
    private Map<String, String> cache = new TreeMap<>();

    public void openCache(Path file) throws IOException {
        cacheFile = file;
        cache = Files.exists(file)
                ? json.readValue(file.toFile(), new TypeReference<TreeMap<String, String>>() {})
                : new TreeMap<>();
    }

    public boolean cached(Soul soul, Language language) {
        return cache.containsKey(key(soul, language));
    }

    /** Uses the cache when it can; saves after every new epitaph so nothing is lost if the process dies. */
    public String epitaph(Soul soul, Language language) throws IOException {
        String key = key(soul, language);
        String kept = cache.get(key);
        if (kept != null) {
            return kept;
        }
        String fresh = clean(write(soul, language));
        cache.put(key, fresh);
        if (cacheFile != null) {
            json.writerWithDefaultPrettyPrinter().writeValue(cacheFile.toFile(), cache);
        }
        return fresh;
    }

    private String key(Soul soul, Language language) {
        Deceased d = soul.deceased();
        return d.path() + "@" + d.deathCommit().hash() + "#" + language + "#" + model;
    }

    private String write(Soul soul, Language language) {
        Deceased d = soul.deceased();
        DeathCommit death = d.deathCommit();
        return monk.epitaph(
                language.nameForTheModel,
                d.path(),
                life(Duration.between(d.born(), d.died())),
                DATE.format(d.born()),
                DATE.format(d.died()),
                String.join(", ", d.authors()),
                d.commitsInLife(),
                cause(soul.cause()),
                death.message().isBlank() ? "(none)" : death.message(),
                HOUR.format(death.when()),
                death.filesChanged() - 1);
    }

    /** In hours when it did not reach a day: "3 hours" says more than "0 days". */
    public static String life(Duration life) {
        if (life.toDays() >= 1) {
            return life.toDays() + (life.toDays() == 1 ? " day" : " days");
        }
        if (life.toHours() >= 1) {
            return life.toHours() + (life.toHours() == 1 ? " hour" : " hours");
        }
        long minutes = Math.max(1, life.toMinutes());
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }

    private static String cause(CauseOfDeath cause) {
        return switch (cause) {
            case VIOLENT -> "violent";
            case PREMATURE -> "premature";
            case FORGOTTEN -> "forgotten";
            case NATURAL -> "natural causes";
        };
    }

    /** Models sometimes wrap the answer in quotes even when asked not to. */
    public static String clean(String text) {
        String t = text.strip();
        if (t.length() > 1 && (t.startsWith("\"") && t.endsWith("\"") || t.startsWith("“") && t.endsWith("”"))) {
            t = t.substring(1, t.length() - 1).strip();
        }
        return t;
    }
}
