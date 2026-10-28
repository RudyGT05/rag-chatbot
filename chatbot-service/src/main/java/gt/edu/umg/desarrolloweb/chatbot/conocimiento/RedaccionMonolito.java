package gt.edu.umg.desarrolloweb.chatbot.conocimiento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Convierte filas del monolito en párrafos en español para el RAG.
 *
 * ¿Por qué oraciones y no la fila cruda? El modelo de embeddings y el LLM
 * entienden lenguaje natural: "La 1/2 paja incluye 30 m³ por Q28.00" se
 * encuentra y se usa mucho mejor que "2|1/2 paja|30.00|28.00".
 *
 * Java puro (sin Spring ni BD) para poder probarlo de forma aislada.
 */
public final class RedaccionMonolito {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-GT"));

    private RedaccionMonolito() {
    }

    /** Una fila de tarifas_vigencia + pajas, con la próxima tarifa si ya está programada. */
    public record TarifaFila(String paja, BigDecimal equivalenciaM3, BigDecimal montoBase,
                             BigDecimal precioExcedente, LocalDate vigenteDesde,
                             BigDecimal proxMontoBase, BigDecimal proxPrecioExcedente, LocalDate proxDesde) {
    }

    public record MetodoPagoFila(String nombre, boolean requiereReferencia) {
    }

    public record SectorFila(String nombre, String descripcion) {
    }

    // ------------------------------------------------------------------ tarifas

    public static String tarifa(TarifaFila t) {
        BigDecimal ejemploConsumo = t.equivalenciaM3().add(BigDecimal.valueOf(6));
        BigDecimal ejemploTotal = t.montoBase().add(t.precioExcedente().multiply(BigDecimal.valueOf(6)));

        StringBuilder sb = new StringBuilder()
                .append("Tarifa vigente de agua para ").append(t.paja()).append(". ")
                .append("Vigente desde el ").append(FECHA.format(t.vigenteDesde())).append(". ")
                .append("Incluye hasta ").append(m3(t.equivalenciaM3()))
                .append(" metros cúbicos (m³) de consumo al mes por un monto base de ")
                .append(q(t.montoBase())).append(". ")
                .append("Cada metro cúbico que pase de ").append(m3(t.equivalenciaM3()))
                .append(" m³ se cobra como excedente a ").append(q(t.precioExcedente())).append(". ")
                .append("Ejemplo: con ").append(t.paja()).append(" y un consumo de ")
                .append(m3(ejemploConsumo)).append(" m³ se pagan ").append(q(t.montoBase()))
                .append(" + 6 m³ × ").append(q(t.precioExcedente())).append(" = ")
                .append(q(ejemploTotal)).append('.');

        if (t.proxDesde() != null && t.proxMontoBase() != null) {
            sb.append(" Cambio programado: a partir del ").append(FECHA.format(t.proxDesde()))
              .append(" el monto base de ").append(t.paja()).append(" será de ").append(q(t.proxMontoBase()))
              .append(" y el excedente de ").append(q(t.proxPrecioExcedente())).append(" por m³.");
        }
        return sb.toString();
    }

    /** Un fragmento con TODAS las tarifas: responde "¿qué tipos de paja hay?" o comparaciones. */
    public static String resumenTarifas(List<TarifaFila> tarifas) {
        return "Resumen de tarifas vigentes de agua por tipo de paja: "
                + tarifas.stream()
                        .map(t -> t.paja() + " incluye " + m3(t.equivalenciaM3()) + " m³ al mes por "
                                + q(t.montoBase()) + " y el excedente cuesta " + q(t.precioExcedente()) + " por m³")
                        .collect(Collectors.joining("; "))
                + ". La paja contratada depende de cada contador.";
    }

    // ------------------------------------------------------------ formas de pago

    public static String metodosPago(List<MetodoPagoFila> metodos) {
        String lista = metodos.stream()
                .map(m -> m.requiereReferencia()
                        ? m.nombre() + " (debes indicar el número de referencia o presentar el comprobante)"
                        : m.nombre())
                .collect(Collectors.joining("; "));
        return "Formas de pago que acepta la Oficina del Agua en ventanilla: " + lista + ".";
    }

    // ------------------------------------------------------------------ sectores

    public static String sectores(List<SectorFila> sectores) {
        String lista = sectores.stream()
                .map(s -> s.descripcion() == null || s.descripcion().isBlank()
                        ? s.nombre()
                        : s.nombre() + " (" + s.descripcion() + ")")
                .collect(Collectors.joining("; "));
        return "Sectores donde la Oficina del Agua presta el servicio de agua: " + lista
                + ". Si tu sector no aparece, consulta en la oficina si hay ampliaciones de la red.";
    }

    // ------------------------------------------------------------------ formato

    static String q(BigDecimal monto) {
        return "Q" + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static String m3(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }
}
