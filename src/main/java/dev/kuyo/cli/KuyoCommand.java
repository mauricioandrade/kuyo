package dev.kuyo.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

@TopCommand
@Command(name = "kuyo",
        mixinStandardHelpOptions = true,
        version = "kuyo 0.1.0",
        description = "供養 · a funeral for the code that is gone.",
        subcommands = {ScanCommand.class, BuryCommand.class, SeanceCommand.class, HauntCommand.class})
public class KuyoCommand {
}
