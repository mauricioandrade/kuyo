package dev.kuyo.haunt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What a command line that already ran foretells. It only reads the text: nothing is run again.
 * The {@code targets} are the paths an {@code rm} or {@code git rm} received, as they were typed.
 */
public record Omen(Type type, List<String> targets) {

    public enum Type { COMMIT, DELETE, RESET, CLEAN, FORCE_PUSH, BRANCH, STASH, RESTORE }

    private static final Set<String> OPERATORS = Set.of(";", "&&", "||", "|", "&");
    private static final Set<String> PREFIXES = Set.of("sudo", "command", "env", "nohup", "time", "builtin");

    public static List<Omen> read(String line) {
        List<Omen> omens = new ArrayList<>();
        for (List<String> segment : segments(line)) {
            Omen o = read(segment);
            if (o != null && !omens.contains(o)) {
                omens.add(o);
            }
        }
        return omens;
    }

    private static Omen read(List<String> t) {
        int i = 0;
        while (i < t.size() && (PREFIXES.contains(t.get(i)) || t.get(i).matches("[A-Za-z_][A-Za-z0-9_]*=.*"))) {
            i++;
        }
        if (i >= t.size()) {
            return null;
        }
        if (t.get(i).equals("rm") || t.get(i).endsWith("/rm")) {
            return delete(t.subList(i + 1, t.size()));
        }
        if (!t.get(i).equals("git")) {
            return null;
        }
        i++;
        // git's global options: -C and -c take a value
        while (i < t.size() && t.get(i).startsWith("-")) {
            i += t.get(i).equals("-C") || t.get(i).equals("-c") ? 2 : 1;
        }
        if (i >= t.size()) {
            return null;
        }
        String subcommand = t.get(i);
        List<String> args = t.subList(i + 1, t.size());
        return switch (subcommand) {
            case "commit" -> new Omen(Type.COMMIT, List.of());
            case "rm" -> delete(args);
            case "reset" -> args.contains("--hard") ? new Omen(Type.RESET, List.of()) : null;
            case "clean" -> args.stream().anyMatch(a -> a.equals("--force") || a.matches("-[a-zA-Z]*f[a-zA-Z]*"))
                    ? new Omen(Type.CLEAN, List.of()) : null;
            case "push" -> args.stream().anyMatch(a -> a.equals("-f") || a.startsWith("--force") || a.startsWith("+"))
                    ? new Omen(Type.FORCE_PUSH, List.of()) : null;
            case "branch" -> args.contains("-D") || args.contains("--delete") && args.contains("--force")
                    ? new Omen(Type.BRANCH, List.of()) : null;
            case "stash" -> !args.isEmpty() && (args.getFirst().equals("drop") || args.getFirst().equals("clear"))
                    ? new Omen(Type.STASH, List.of()) : null;
            case "checkout" -> args.contains("--") || args.contains(".") ? new Omen(Type.RESTORE, List.of()) : null;
            // unstaging deletes nothing: only restoring the working tree throws changes away
            case "restore" -> (args.contains("--staged") || args.contains("-S"))
                    && !(args.contains("--worktree") || args.contains("-W") || args.contains("-SW"))
                    ? null : new Omen(Type.RESTORE, List.of());
            default -> null;
        };
    }

    private static Omen delete(List<String> args) {
        List<String> targets = new ArrayList<>();
        boolean endOfOptions = false;
        for (String a : args) {
            if (!endOfOptions && a.equals("--")) {
                endOfOptions = true;
            } else if (endOfOptions || !a.startsWith("-")) {
                targets.add(a);
            }
        }
        return targets.isEmpty() ? null : new Omen(Type.DELETE, List.copyOf(targets));
    }

    /** Splits the line into simple commands, honoring quotes and backslashes as a shell would. */
    static List<List<String>> segments(String line) {
        List<List<String>> segments = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        boolean hasWord = false;
        char quote = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else if (c == '\\' && quote == '"' && i + 1 < line.length()) {
                    word.append(line.charAt(++i));
                } else {
                    word.append(c);
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
                hasWord = true;
            } else if (c == '\\' && i + 1 < line.length()) {
                word.append(line.charAt(++i));
                hasWord = true;
            } else if (Character.isWhitespace(c) || c == ';' || c == '&' || c == '|') {
                if (hasWord) {
                    current.add(word.toString());
                    word.setLength(0);
                    hasWord = false;
                }
                if (!Character.isWhitespace(c)) {
                    String op = i + 1 < line.length() && line.charAt(i + 1) == c && c != ';' ? "" + c + c : "" + c;
                    i += op.length() - 1;
                    if (OPERATORS.contains(op) && !current.isEmpty()) {
                        segments.add(current);
                        current = new ArrayList<>();
                    }
                }
            } else {
                word.append(c);
                hasWord = true;
            }
        }
        if (hasWord) {
            current.add(word.toString());
        }
        if (!current.isEmpty()) {
            segments.add(current);
        }
        return segments;
    }
}
