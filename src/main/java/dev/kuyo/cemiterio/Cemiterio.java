package dev.kuyo.cemiterio;

import dev.kuyo.dominio.Alma;
import dev.kuyo.dominio.CausaMortis;
import io.quarkus.qute.TemplateData;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tudo que o template recebe: o cemitério dividido em terraços, um por mês de morte. */
@TemplateData
public record Cemiterio(
        String repo,
        int total,
        List<Censo> censo,
        List<Terraco> terracos,
        Textos textos,
        Map<String, String> fontes) {

    @TemplateData
    public record Censo(String causa, String rotulo, String kanji, int quantidade) {
    }

    @TemplateData
    public record Terraco(String rotulo, List<Tumulo> tumulos) {
    }

    private static final String[] FONTES = {
            "shippori-400-latin", "shippori-400-latin-ext",
            "shippori-700-latin", "shippori-700-latin-ext", "shippori-700-kanji"};

    /** Acima disso um terraço mensal vira paredão; o mês é dividido por dia de morte. */
    static final int TERRACO_MAXIMO = 24;

    public static Cemiterio montar(String repo, List<Alma> almas, Textos textos) {
        Map<YearMonth, Map<LocalDate, List<Tumulo>>> porMes = new LinkedHashMap<>();
        Map<CausaMortis, Integer> contagem = new EnumMap<>(CausaMortis.class);
        int indice = 0;
        for (Alma alma : almas) {
            LocalDate dia = alma.falecido().commitDaMorte().quando().toLocalDate();
            porMes.computeIfAbsent(YearMonth.from(dia), m -> new LinkedHashMap<>())
                    .computeIfAbsent(dia, d -> new ArrayList<>())
                    .add(Tumulo.de(alma, indice++, textos));
            contagem.merge(alma.causa(), 1, Integer::sum);
        }

        List<Terraco> terracos = new ArrayList<>();
        porMes.forEach((mes, porDia) -> {
            List<Tumulo> doMes = porDia.values().stream().flatMap(List::stream).toList();
            if (doMes.size() <= TERRACO_MAXIMO) {
                terracos.add(new Terraco(textos.mes(mes), doMes));
            } else {
                porDia.forEach((dia, tumulos) -> terracos.add(new Terraco(textos.data(dia), tumulos)));
            }
        });
        List<Censo> censo = contagem.entrySet().stream()
                .map(e -> {
                    Tumulo exemplo = terracos.stream().flatMap(t -> t.tumulos().stream())
                            .filter(t -> t.causa().equals(e.getKey().name().toLowerCase(java.util.Locale.ROOT)))
                            .findFirst().orElseThrow();
                    return new Censo(exemplo.causa(), exemplo.causaRotulo(), exemplo.causaKanji(), e.getValue());
                })
                .toList();
        return new Cemiterio(repo, almas.size(), censo, terracos, textos, carregarFontes());
    }

    /** As fontes vão embutidas em base64: o cemitério é um arquivo só, sem nada vindo da rede. */
    private static Map<String, String> carregarFontes() {
        Map<String, String> fontes = new LinkedHashMap<>();
        for (String nome : FONTES) {
            try (InputStream in = Cemiterio.class.getResourceAsStream("/fonts/" + nome + ".woff2")) {
                fontes.put(nome.replace("shippori-", "f").replace("-", ""),
                        Base64.getEncoder().encodeToString(in.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return fontes;
    }
}
