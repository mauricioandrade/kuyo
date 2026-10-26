package dev.kuyo.cli;

import dev.kuyo.dominio.Alma;
import dev.kuyo.dominio.CausaMortis;
import dev.kuyo.dominio.Falecido;
import dev.kuyo.dominio.Laudo;
import dev.kuyo.dominio.Legista;
import dev.kuyo.git.Exumador;
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

@Command(name = "scan", description = "Lista os arquivos mortos e a causa mortis, sem LLM.")
public class ScanCommand implements Callable<Integer> {

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATA_COM_FUSO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm xxx");

    enum Ordem { CRONOLOGICA, INQUIETACAO }

    @Parameters(index = "0", description = "Caminho do repositório git.", defaultValue = ".")
    Path repo;

    @Option(names = "--ordem", defaultValue = "CRONOLOGICA",
            description = "Ordem da listagem: ${COMPLETION-CANDIDATES}.")
    Ordem ordem;

    @Option(names = "--laudo", description = "Mostra cada fator da inquietação.")
    boolean laudo;

    @Inject
    Exumador exumador;

    @Inject
    Legista legista;

    @Override
    public Integer call() throws Exception {
        List<Alma> almas = exumador.exumar(repo).stream().map(legista::examinar).toList();
        if (almas.isEmpty()) {
            System.out.println("Nenhum arquivo morto. Ninguém para velar.");
            return 0;
        }
        if (ordem == Ordem.INQUIETACAO) {
            almas = almas.stream().sorted(Comparator.comparingDouble(Alma::inquietacao).reversed()).toList();
        }

        almas.forEach(this::imprimir);
        imprimirResumo(almas);
        return 0;
    }

    private void imprimir(Alma alma) {
        Falecido f = alma.falecido();
        var morte = f.commitDaMorte();
        System.out.printf("✝ %s  [%s · %.2f]%n", f.caminho(), alma.causa(), alma.inquietacao());
        System.out.printf("    %s → %s  (%d dias, %d commits, %s)%n",
                DATA.format(f.nascimento()), DATA_COM_FUSO.format(morte.quando()), f.diasDeVida(),
                f.commitsEmVida(), f.binario() ? "binário" : f.linhasAoMorrer() + " linhas");
        System.out.printf("    autores: %s%n", String.join(", ", f.autores()));
        System.out.printf("    morto em %s por %s: \"%s\"%n",
                morte.hash(), morte.autor(), primeiraLinha(morte.mensagem()));
        System.out.printf("    commit: %d arquivos, -%d linhas%s%n",
                morte.arquivosAlterados(), morte.linhasRemovidas(),
                morte.mexeuEmTestes() ? ", com testes" : ", sem testes");
        if (laudo) {
            Laudo l = alma.laudo();
            System.out.printf("    laudo: tamanho %.2f · hora %.2f · mensagem %.2f · sem testes %.2f · vida curta %.2f%n",
                    l.tamanhoDoCommit(), l.horaDaMorte(), l.mensagemSuspeita(), l.semTestes(), l.vidaCurta());
        }
        System.out.println();
    }

    private static void imprimirResumo(List<Alma> almas) {
        Map<CausaMortis, Integer> porCausa = new EnumMap<>(CausaMortis.class);
        int[] faixas = new int[3];
        for (Alma a : almas) {
            porCausa.merge(a.causa(), 1, Integer::sum);
            faixas[a.inquietacao() < 0.3 ? 0 : a.inquietacao() < 0.6 ? 1 : 2]++;
        }
        System.out.printf("%d almas encontradas.%n", almas.size());
        porCausa.forEach((causa, n) -> System.out.printf("  %-10s %d%n", causa, n));
        System.out.printf("  inquietação: %d calmas (<0,3) · %d agitadas · %d atormentadas (≥0,6)%n",
                faixas[0], faixas[1], faixas[2]);
    }

    private static String primeiraLinha(String mensagem) {
        int quebra = mensagem.indexOf('\n');
        return quebra < 0 ? mensagem : mensagem.substring(0, quebra);
    }
}
