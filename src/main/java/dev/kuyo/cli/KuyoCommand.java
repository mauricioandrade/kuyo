package dev.kuyo.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

@TopCommand
@Command(name = "kuyo",
        mixinStandardHelpOptions = true,
        version = "kuyo 0.1.0",
        description = "供養 · um funeral para o código que se foi.",
        subcommands = {ScanCommand.class})
public class KuyoCommand {
}
