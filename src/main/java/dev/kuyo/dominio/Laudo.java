package dev.kuyo.dominio;

/**
 * Cada fator da inquietação, de 0 a 1, antes de aplicar os pesos.
 * Fica guardado para o cemitério poder explicar por que uma alma não descansa.
 */
public record Laudo(
        double tamanhoDoCommit,
        double horaDaMorte,
        double mensagemSuspeita,
        double semTestes,
        double vidaCurta) {

    public static final double PESO_TAMANHO = 0.30;
    public static final double PESO_HORA = 0.15;
    public static final double PESO_MENSAGEM = 0.20;
    public static final double PESO_SEM_TESTES = 0.15;
    public static final double PESO_VIDA_CURTA = 0.20;

    public double inquietacao() {
        return PESO_TAMANHO * tamanhoDoCommit
                + PESO_HORA * horaDaMorte
                + PESO_MENSAGEM * mensagemSuspeita
                + PESO_SEM_TESTES * semTestes
                + PESO_VIDA_CURTA * vidaCurta;
    }
}
