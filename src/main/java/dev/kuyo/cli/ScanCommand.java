package dev.kuyo.cli;

import dev.kuyo.domain.CauseOfDeath;
import dev.kuyo.domain.Coroner;
import dev.kuyo.domain.Deceased;
import dev.kuyo.domain.Findings;
import dev.kuyo.domain.Soul;
import dev.kuyo.git.Exhumer;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "scan", description = "Lists the dead files and their cause of death, without an LLM.")
public class ScanCommand implements Callable<Integer> {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_WITH_ZONE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm xxx");

    enum Order { chronological, restlessness }

    @Parameters(index = "0", description = "Path of the git repository.", defaultValue = ".")
    Path repo;

    @Option(names = "--order", defaultValue = "chronological",
            description = "Listing order: ${COMPLETION-CANDIDATES}.")
    Order order;

    @Option(names = "--findings", description = "Shows each restlessness factor.")
    boolean findings;

    @Inject
    Exhumer exhumer;

    @Inject
    Coroner coroner;

    @Override
    public Integer call() throws Exception {
        List<Soul> souls = exhumer.exhume(repo).stream().map(coroner::examine).toList();
        if (souls.isEmpty()) {
            System.out.println("No dead files. Nobody to mourn.");
            return 0;
        }
        if (order == Order.restlessness) {
            souls = souls.stream().sorted(Comparator.comparingDouble(Soul::restlessness).reversed()).toList();
        }

        souls.forEach(this::print);
        printSummary(souls);
        return 0;
    }

    private void print(Soul soul) {
        Deceased d = soul.deceased();
        var death = d.deathCommit();
        System.out.printf("✝ %s  [%s · %.2f]%n", d.path(), soul.cause(), soul.restlessness());
        System.out.printf("    %s → %s  (%d days, %d commits, %s)%n",
                DATE.format(d.born()), DATE_WITH_ZONE.format(death.when()), d.daysLived(),
                d.commitsInLife(), d.binary() ? "binary" : d.linesAtDeath() + " lines");
        System.out.printf("    authors: %s%n", String.join(", ", d.authors()));
        System.out.printf("    killed in %s by %s: \"%s\"%n",
                death.hash(), death.author(), firstLine(death.message()));
        System.out.printf("    commit: %d files, -%d lines%s%n",
                death.filesChanged(), death.linesRemoved(),
                death.touchedTests() ? ", with tests" : ", without tests");
        if (findings) {
            Findings f = soul.findings();
            System.out.printf("    findings: size %.2f · hour %.2f · message %.2f · no tests %.2f · short life %.2f%n",
                    f.commitSize(), f.hourOfDeath(), f.suspiciousMessage(), f.noTests(), f.shortLife());
        }
        System.out.println();
    }

    private static void printSummary(List<Soul> souls) {
        Map<CauseOfDeath, Integer> byCause = new EnumMap<>(CauseOfDeath.class);
        int[] bands = new int[3];
        for (Soul s : souls) {
            byCause.merge(s.cause(), 1, Integer::sum);
            bands[s.restlessness() < 0.3 ? 0 : s.restlessness() < 0.6 ? 1 : 2]++;
        }
        System.out.printf("%d souls found.%n", souls.size());
        byCause.forEach((cause, n) -> System.out.printf("  %-10s %d%n", cause, n));
        System.out.printf("  restlessness: %d calm (<0.3) · %d uneasy · %d tormented (≥0.6)%n",
                bands[0], bands[1], bands[2]);
    }

    private static String firstLine(String message) {
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
