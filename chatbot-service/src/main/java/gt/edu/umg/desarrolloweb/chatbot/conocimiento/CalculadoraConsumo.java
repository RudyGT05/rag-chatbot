package gt.edu.umg.desarrolloweb.chatbot.conocimiento;

import gt.edu.umg.desarrolloweb.chatbot.conocimiento.RedaccionMonolito.TarifaFila;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calcula EN CÓDIGO cuánto se paga por un consumo, con la tarifa vigente.
 *
 * ¿Por qué no dejárselo al LLM? Porque un LLM no calcula: predice texto. En
 * pruebas reales, qwen2.5:3b respondió "(36 − 30) = 12 → Q76.00" teniendo el
 * ejemplo correcto (Q52.00) en el contexto. Igual que con la privacidad, lo que
 * tiene que ser exacto no se le deja al modelo.
 *
 *   "cuánto pago si tengo 1/2 paja y gasté 36 metros cúbicos"
 *        → paja = "1/2 paja", consumo = 36 → Q28.00 + 6 × Q4.00 = Q52.00
 *
 * Misma fórmula que el monolito:
 *   monto = monto_base + max(0, consumo − equivalencia_m3) × precio_m3_excedente
 *
 * Java puro (sin Spring) para poder probarlo de forma aislada.
 */
public final class CalculadoraConsumo {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-GT"));

    /** "36 m3", "36 m³", "36.5 metros", "36 metros cubicos", "36 mts" */
    private static final Pattern CANTIDAD_CON_UNIDAD =
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:m3|m³|mts?\\b|metros?\\b)");
    /** "consumí 36", "gasté 36", "un consumo de 36" (sin unidad) */
    private static final Pattern CANTIDAD_CON_VERBO =
            Pattern.compile("\\b(?:consum\\w*|gast\\w*|use|usamos)\\s+(?:de\\s+)?(\\d+(?:[.,]\\d+)?)\\b");

    private CalculadoraConsumo() {
    }

    public record Calculo(TarifaFila tarifa, BigDecimal consumoM3, BigDecimal excedenteM3,
                          BigDecimal montoExcedente, BigDecimal total) {
    }

    /** Texto listo para el usuario + tarifas usadas (para mostrarlas como fuentes). */
    public record Respuesta(String texto, List<TarifaFila> tarifasUsadas) {
    }

    /** Fracción suelta tipo "1/3", "3/4": casi siempre se refiere a un tipo de paja. */
    private static final Pattern FRACCION = Pattern.compile("(?<![\\d/])([1-9]/[1-9])(?![\\d/])");
    /** "2 pajas", "3 paja", "un tercio de paja": pajas que podrían no existir. */
    private static final Pattern OTRA_PAJA =
            Pattern.compile("\\b(\\d+ pajas?|tercio de paja|tres cuartos de paja)\\b");
    /** Palabras que indican que preguntan un PRECIO. */
    private static final Pattern PIDE_PRECIO = Pattern.compile(
            "\\b(cuanto|cuanta|qto|cto|vale|valen|cuesta|cuestan|cobra|cobran|precio|precios"
            + "|tarifa|tarifas|monto|costo|sale|pago|pagaria)\\b");
    /** Preguntas de TRÁMITE que mencionan una paja pero no piden su precio ("¿puedo cambiar de 1/2 a 1 paja?"). */
    private static final Pattern ES_TRAMITE = Pattern.compile(
            "\\b(cambiar|cambio|solicit\\w*|tramit\\w*|requisit\\w*|document\\w*|instal\\w*|contratar)\\b");
    /** Preguntas de seguimiento cortas: "y la 1/2?", "y la media paja". */
    private static final int MAX_PALABRAS_SEGUIMIENTO = 5;

    /**
     * Punto de entrada para el ChatService. Responde EN CÓDIGO todo lo que
     * involucre precios, para que el LLM nunca invente ni calcule montos:
     *
     *  1. paja que NO existe ("1/3 de paja")  → lo dice y lista las vigentes
     *  2. paja + consumo                      → cálculo exacto para esa paja
     *  3. solo consumo                        → cálculo para CADA paja vigente
     *  4. precio de una paja ("¿cuánto vale la media paja?", "y la 1/2?")
     *                                         → tarifa vigente tal cual está en el monolito
     *  5. cualquier otra cosa                 → vacío: sigue por el RAG normal
     *
     * Caso real que motivó el punto 1: a "¿cuánto vale 1/3 de paja?" el LLM inventó
     * una tarifa (20 m³, Q38.20) y en la siguiente pregunta otra distinta (Q37.80).
     */
    public static Optional<Respuesta> responder(String pregunta, List<TarifaFila> tarifas) {
        if (tarifas.isEmpty()) {
            return Optional.empty();
        }
        String texto = normalizar(pregunta);
        Optional<TarifaFila> tarifa = buscarPaja(texto, tarifas);
        Optional<BigDecimal> consumo = buscarConsumo(texto);

        // 1. Mencionó una paja que no existe o no está vigente
        if (tarifa.isEmpty()) {
            Optional<String> desconocida = buscarPajaDesconocida(texto, tarifas);
            if (desconocida.isPresent()) {
                String texto1 = "No hay una tarifa vigente para " + desconocida.get()
                        + ". Las tarifas vigentes son: " + listarTarifas(tarifas) + ".";
                if (consumo.isPresent()) {
                    texto1 += " Con un consumo de " + RedaccionMonolito.m3(consumo.get()) + " m³: "
                            + porCadaPaja(consumo.get(), tarifas) + ".";
                }
                return Optional.of(new Respuesta(texto1, tarifas));
            }
        }

        // 2. y 3. Hay una cantidad de m³: se calcula
        if (consumo.isPresent()) {
            if (tarifa.isPresent()) {
                return Optional.of(new Respuesta(redactar(calcular(tarifa.get(), consumo.get())),
                        List.of(tarifa.get())));
            }
            return Optional.of(new Respuesta(
                    "Depende de la paja que tenga contratada tu contador. Con un consumo de "
                            + RedaccionMonolito.m3(consumo.get()) + " m³: " + porCadaPaja(consumo.get(), tarifas)
                            + ". Si me dices tu tipo de paja te explico el cálculo.",
                    tarifas));
        }

        // 4. Pregunta el precio de una paja conocida
        boolean pidePrecio = PIDE_PRECIO.matcher(texto).find() && !ES_TRAMITE.matcher(texto).find();
        if (tarifa.isPresent() && (pidePrecio || esSeguimientoCorto(texto))) {
            return Optional.of(new Respuesta(describir(tarifa.get()), List.of(tarifa.get())));
        }

        // 5. No es de precios: que responda el RAG
        return Optional.empty();
    }

    /** Tarifa vigente de una paja, tal cual está en el monolito. */
    public static String describir(TarifaFila t) {
        String texto = "La tarifa vigente de " + t.paja() + " es de " + RedaccionMonolito.q(t.montoBase())
                + " al mes e incluye hasta " + RedaccionMonolito.m3(t.equivalenciaM3()) + " m³ de consumo."
                + " Cada metro cúbico adicional se cobra a " + RedaccionMonolito.q(t.precioExcedente())
                + " (vigente desde el " + FECHA.format(t.vigenteDesde()) + ").";
        if (t.proxDesde() != null && t.proxMontoBase() != null) {
            texto += " A partir del " + FECHA.format(t.proxDesde()) + " el monto base será de "
                    + RedaccionMonolito.q(t.proxMontoBase()) + ".";
        }
        return texto;
    }

    private static String listarTarifas(List<TarifaFila> tarifas) {
        return tarifas.stream()
                .map(t -> t.paja() + " (" + RedaccionMonolito.q(t.montoBase()) + " al mes, incluye "
                        + RedaccionMonolito.m3(t.equivalenciaM3()) + " m³)")
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
    }

    private static String porCadaPaja(BigDecimal consumo, List<TarifaFila> tarifas) {
        return tarifas.stream()
                .map(t -> "con " + t.paja() + " pagas " + RedaccionMonolito.q(calcular(t, consumo).total()))
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
    }

    private static boolean esSeguimientoCorto(String texto) {
        return texto.replaceAll("[^a-z0-9/ ]", " ").trim().split("\\s+").length <= MAX_PALABRAS_SEGUIMIENTO;
    }

    /**
     * Devuelve cómo se nombró una paja que NO está entre las vigentes:
     * "1/3", "2 pajas", "tercio de paja"… Vacío si no mencionó ninguna.
     */
    static Optional<String> buscarPajaDesconocida(String texto, List<TarifaFila> tarifas) {
        List<String> conocidas = tarifas.stream().map(t -> normalizar(t.paja())).toList();
        Matcher f = FRACCION.matcher(texto);
        while (f.find()) {
            String fraccion = f.group(1);
            boolean existe = conocidas.stream().anyMatch(n -> n.startsWith(fraccion + " "));
            if (!existe) {
                return Optional.of(fraccion + " de paja");
            }
        }
        Matcher o = OTRA_PAJA.matcher(texto);
        while (o.find()) {
            String mencion = o.group(1);
            String singular = mencion.replaceAll("pajas$", "paja");
            if (!conocidas.contains(singular)) {
                return Optional.of(mencion);
            }
        }
        return Optional.empty();
    }

    /** Si la pregunta trae una paja conocida y una cantidad de m³, calcula; si no, vacío. */
    public static Optional<Calculo> intentar(String pregunta, List<TarifaFila> tarifas) {
        if (tarifas.isEmpty()) {
            return Optional.empty();
        }
        String texto = normalizar(pregunta);
        Optional<TarifaFila> tarifa = buscarPaja(texto, tarifas);
        Optional<BigDecimal> consumo = buscarConsumo(texto);
        if (tarifa.isEmpty() || consumo.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(calcular(tarifa.get(), consumo.get()));
    }

    public static Calculo calcular(TarifaFila t, BigDecimal consumo) {
        BigDecimal excedente = consumo.subtract(t.equivalenciaM3()).max(BigDecimal.ZERO);
        BigDecimal montoExcedente = excedente.multiply(t.precioExcedente()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = t.montoBase().add(montoExcedente).setScale(2, RoundingMode.HALF_UP);
        return new Calculo(t, consumo, excedente, montoExcedente, total);
    }

    public static String redactar(Calculo c) {
        TarifaFila t = c.tarifa();
        String m3 = RedaccionMonolito.m3(c.consumoM3());
        String incluye = RedaccionMonolito.m3(t.equivalenciaM3());
        String vigencia = " (tarifa vigente desde el " + FECHA.format(t.vigenteDesde()) + ").";

        if (c.excedenteM3().signum() == 0) {
            return "Con " + t.paja() + " y un consumo de " + m3 + " m³ pagas " + RedaccionMonolito.q(c.total())
                    + ": solo el monto base, porque tu consumo no pasa de los " + incluye
                    + " m³ que incluye tu paja" + vigencia;
        }
        return "Con " + t.paja() + " y un consumo de " + m3 + " m³ pagas " + RedaccionMonolito.q(c.total())
                + ": monto base de " + RedaccionMonolito.q(t.montoBase()) + " (incluye hasta " + incluye
                + " m³) más " + RedaccionMonolito.m3(c.excedenteM3()) + " m³ de excedente × "
                + RedaccionMonolito.q(t.precioExcedente()) + " = " + RedaccionMonolito.q(c.montoExcedente())
                + vigencia + " El monto final aparece en tu boleta del mes.";
    }

    // ------------------------------------------------------------------ detección

    static Optional<BigDecimal> buscarConsumo(String texto) {
        for (Pattern p : List.of(CANTIDAD_CON_UNIDAD, CANTIDAD_CON_VERBO)) {
            Matcher m = p.matcher(texto);
            if (m.find()) {
                return Optional.of(new BigDecimal(m.group(1).replace(',', '.')));
            }
        }
        return Optional.empty();
    }

    /**
     * Reconoce la paja por su nombre en la BD ("1/2 paja") y por cómo la dice la
     * gente ("media paja", "un cuarto de paja", "paja completa"). Se prueban
     * primero los alias más largos para que "1/2 paja" no se confunda con "2 paja".
     */
    static Optional<TarifaFila> buscarPaja(String texto, List<TarifaFila> tarifas) {
        record Alias(String patron, TarifaFila tarifa) {
        }
        List<Alias> alias = new ArrayList<>();
        for (TarifaFila t : tarifas) {
            String nombre = normalizar(t.paja());
            alias.add(new Alias(nombre, t));
            if (nombre.startsWith("1/2")) {
                alias.add(new Alias("media paja", t));
                alias.add(new Alias("medio paja", t));
                alias.add(new Alias("1/2 de paja", t));
                alias.add(new Alias("1/2", t));          // "y la 1/2?"
            } else if (nombre.startsWith("1/4")) {
                alias.add(new Alias("cuarto de paja", t));
                alias.add(new Alias("1/4 de paja", t));
                alias.add(new Alias("1/4", t));
            } else if (nombre.equals("1 paja")) {
                alias.add(new Alias("una paja", t));
                alias.add(new Alias("paja completa", t));
                alias.add(new Alias("paja entera", t));
            }
        }
        alias.sort(Comparator.comparingInt((Alias a) -> a.patron().length()).reversed());
        for (Alias a : alias) {
            // (?<![\d/]) evita que "1 paja" coincida dentro de "11 paja" o "1/1 paja"
            Pattern p = Pattern.compile("(?<![\\d/])" + Pattern.quote(a.patron()) + "s?\\b");
            if (p.matcher(texto).find()) {
                return Optional.of(a.tarifa());
            }
        }
        return Optional.empty();
    }

    static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}
