package dev.kuyo.assombracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * O que uma linha de comando já executada anuncia. Só lê o texto: nada é executado de novo.
 * Os {@code alvos} são os caminhos que um {@code rm} ou {@code git rm} recebeu, como foram digitados.
 */
public record Agouro(Tipo tipo, List<String> alvos) {

    public enum Tipo { COMMIT, APAGAR, RESET, CLEAN, PUSH_FORCADO, BRANCH, STASH, RESTAURAR }

    private static final Set<String> OPERADORES = Set.of(";", "&&", "||", "|", "&");
    private static final Set<String> PREFIXOS = Set.of("sudo", "command", "env", "nohup", "time", "builtin");

    public static List<Agouro> ler(String linha) {
        List<Agouro> agouros = new ArrayList<>();
        for (List<String> segmento : segmentos(linha)) {
            Agouro a = ler(segmento);
            if (a != null && !agouros.contains(a)) {
                agouros.add(a);
            }
        }
        return agouros;
    }

    private static Agouro ler(List<String> t) {
        int i = 0;
        while (i < t.size() && (PREFIXOS.contains(t.get(i)) || t.get(i).matches("[A-Za-z_][A-Za-z0-9_]*=.*"))) {
            i++;
        }
        if (i >= t.size()) {
            return null;
        }
        if (t.get(i).equals("rm") || t.get(i).endsWith("/rm")) {
            return apagar(t.subList(i + 1, t.size()));
        }
        if (!t.get(i).equals("git")) {
            return null;
        }
        i++;
        // opções globais do git: -C e -c levam um valor
        while (i < t.size() && t.get(i).startsWith("-")) {
            i += t.get(i).equals("-C") || t.get(i).equals("-c") ? 2 : 1;
        }
        if (i >= t.size()) {
            return null;
        }
        String sub = t.get(i);
        List<String> args = t.subList(i + 1, t.size());
        return switch (sub) {
            case "commit" -> new Agouro(Tipo.COMMIT, List.of());
            case "rm" -> apagar(args);
            case "reset" -> args.contains("--hard") ? new Agouro(Tipo.RESET, List.of()) : null;
            case "clean" -> args.stream().anyMatch(a -> a.equals("--force") || a.matches("-[a-zA-Z]*f[a-zA-Z]*"))
                    ? new Agouro(Tipo.CLEAN, List.of()) : null;
            case "push" -> args.stream().anyMatch(a -> a.equals("-f") || a.startsWith("--force") || a.startsWith("+"))
                    ? new Agouro(Tipo.PUSH_FORCADO, List.of()) : null;
            case "branch" -> args.contains("-D") || args.contains("--delete") && args.contains("--force")
                    ? new Agouro(Tipo.BRANCH, List.of()) : null;
            case "stash" -> !args.isEmpty() && (args.getFirst().equals("drop") || args.getFirst().equals("clear"))
                    ? new Agouro(Tipo.STASH, List.of()) : null;
            case "checkout" -> args.contains("--") || args.contains(".") ? new Agouro(Tipo.RESTAURAR, List.of()) : null;
            // tirar do índice não apaga nada: só restaurar a árvore de trabalho descarta mudanças
            case "restore" -> (args.contains("--staged") || args.contains("-S"))
                    && !(args.contains("--worktree") || args.contains("-W") || args.contains("-SW"))
                    ? null : new Agouro(Tipo.RESTAURAR, List.of());
            default -> null;
        };
    }

    private static Agouro apagar(List<String> args) {
        List<String> alvos = new ArrayList<>();
        boolean fimDasOpcoes = false;
        for (String a : args) {
            if (!fimDasOpcoes && a.equals("--")) {
                fimDasOpcoes = true;
            } else if (fimDasOpcoes || !a.startsWith("-")) {
                alvos.add(a);
            }
        }
        return alvos.isEmpty() ? null : new Agouro(Tipo.APAGAR, List.copyOf(alvos));
    }

    /** Separa a linha em comandos simples, respeitando aspas e barras como um shell faria. */
    static List<List<String>> segmentos(String linha) {
        List<List<String>> segmentos = new ArrayList<>();
        List<String> atual = new ArrayList<>();
        StringBuilder palavra = new StringBuilder();
        boolean temPalavra = false;
        char aspas = 0;

        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (aspas != 0) {
                if (c == aspas) {
                    aspas = 0;
                } else if (c == '\\' && aspas == '"' && i + 1 < linha.length()) {
                    palavra.append(linha.charAt(++i));
                } else {
                    palavra.append(c);
                }
            } else if (c == '\'' || c == '"') {
                aspas = c;
                temPalavra = true;
            } else if (c == '\\' && i + 1 < linha.length()) {
                palavra.append(linha.charAt(++i));
                temPalavra = true;
            } else if (Character.isWhitespace(c) || c == ';' || c == '&' || c == '|') {
                if (temPalavra) {
                    atual.add(palavra.toString());
                    palavra.setLength(0);
                    temPalavra = false;
                }
                if (!Character.isWhitespace(c)) {
                    String op = i + 1 < linha.length() && linha.charAt(i + 1) == c && c != ';' ? "" + c + c : "" + c;
                    i += op.length() - 1;
                    if (OPERADORES.contains(op) && !atual.isEmpty()) {
                        segmentos.add(atual);
                        atual = new ArrayList<>();
                    }
                }
            } else {
                palavra.append(c);
                temPalavra = true;
            }
        }
        if (temPalavra) {
            atual.add(palavra.toString());
        }
        if (!atual.isEmpty()) {
            segmentos.add(atual);
        }
        return segmentos;
    }
}
