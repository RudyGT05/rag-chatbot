package gt.edu.umg.desarrolloweb.chatbot.service;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Guardia DETERMINISTA: detecta preguntas sobre una cuenta específica
 * (saldo, deuda, pagos de un contador) ANTES de llamar al LLM.
 *
 * ¿Por qué en código y no solo en el system prompt? Porque un LLM (sobre todo
 * uno pequeño) no siempre obedece las reglas. Una regla de seguridad tiene que
 * cumplirse el 100% de las veces, y eso solo lo garantiza el código.
 * El prompt queda como segunda capa, no como la única.
 */
public final class DetectorConsultaCuenta {

    static final String RESPUESTA = """
            Por seguridad, este asistente no tiene acceso a datos de ninguna cuenta. \
            Para ver el saldo, los pagos o el consumo de tu contador, usa la sección \
            "Consulta pública" de esta página e ingresa el código de tu contador; \
            lo encuentras en tu recibo o en la placa del contador.""";

    /** Un código de contador: letras + guion opcional + 3 o más dígitos (MTR-00012, CT1234...). */
    private static final Pattern CODIGO_CONTADOR =
            Pattern.compile("\\b[a-z]{2,5}-?\\d{3,}\\b");

    /**
     * Frases que piden datos de una cuenta concreta. Se comparan sin tildes y en
     * minúsculas, y toleran errores comunes de ortografía:
     *   [bv]  → debo / devo,  pague / page
     *   [sz]  → saldo / zaldo
     *   (cuanto|qto|cto) → abreviaturas comunes de "cuánto"
     * OJO: "cuando" NO se incluye: "¿cuándo debo pagar?" es una pregunta general.
     */
    private static final Pattern PIDE_DATOS_DE_CUENTA = Pattern.compile(
            "\\b(cuanto|qto|cto) (le )?(de[bv]o|de[bv]e|de[bv]emos|adeudo|tengo pendiente)\\b"
            + "|\\bmi (s|z)aldo\\b|\\bmi (deuda|cuenta|estado de cuenta)\\b"
            + "|\\b((s|z)aldo|deuda|estado de cuenta) (de|del)\\b"
            + "|\\bmis (pagos|recibos|lecturas|boletas)\\b"
            + "|\\bcuanto (pa?gu?e|consumi)\\b");

    private DetectorConsultaCuenta() {
    }

    public static boolean esConsultaDeCuenta(String pregunta) {
        String texto = normalizar(pregunta);
        return CODIGO_CONTADOR.matcher(texto).find()
                || PIDE_DATOS_DE_CUENTA.matcher(texto).find();
    }

    /** "¿Cuánto DEBE?" → "¿cuanto debe?" (minúsculas y sin tildes). */
    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase();
    }
}
