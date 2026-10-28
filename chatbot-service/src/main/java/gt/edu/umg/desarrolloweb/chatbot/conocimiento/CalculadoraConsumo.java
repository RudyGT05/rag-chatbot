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

    /**
     * Punto de entrada para el ChatService:
     *  - paja + consumo  → cálculo exacto para esa paja
     *  - solo consumo    → cálculo para CADA paja vigente ("depende de tu paja")
     *  - sin consumo     → vacío: la pregunta sigue por el RAG normal
     */
    public static Optional<Respuesta> responder(String pregunta, List<TarifaFila> tarifas) {
        if (tarifas.isEmpty()) {
            return Optional.empty();
        }
        String texto = normalizar(pregunta);
        Optional<BigDecimal> consumo = buscarConsumo(texto);
        if (consumo.isEmpty()) {
            return Optional.empty();
        }
        Optional<TarifaFila> tarifa = buscarPaja(texto, tarifas);
        if (tarifa.isPresent()) {
            return Optional.of(new Respuesta(redactar(calcular(tarifa.get(), consumo.get())),
                    List.of(tarifa.get())));
        }
        String m3 = RedaccionMonolito.m3(consumo.get());
        String porPaja = tarifas.stream()
                .map(t -> "con " + t.paja() + " pagas " + RedaccionMonolito.q(calcular(t, consumo.get()).total()))
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
        // Mencionó una paja que no está vigente (p. ej. una descontinuada): decírselo,
        // no hacer como si no la hubiera nombrado.
        String inicio = texto.contains("paja")
                ? "No encontré una tarifa vigente para ese tipo de paja. Las pajas vigentes"
                  + " son " + tarifas.stream().map(TarifaFila::paja).reduce((a, b) -> a + ", " + b).orElse("")
                  + ". Con un consumo de " + m3 + " m³: "
                : "Depende de la paja que tenga contratada tu contador. Con un consumo de " + m3 + " m³: ";
        return Optional.of(new Respuesta(
                inicio + porPaja + ". Si me dices tu tipo de paja te explico el cálculo.",
                tarifas));
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
            } else if (nombre.startsWith("1/4")) {
                alias.add(new Alias("cuarto de paja", t));
                alias.add(new Alias("1/4 de paja", t));
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
