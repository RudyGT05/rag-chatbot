package gt.edu.umg.desarrolloweb.chatbot.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Guardia DETERMINISTA para saludos, agradecimientos y despedidas.
 *
 * ¿Por qué hace falta? Un mensaje como "Hola!" no trae ningún tema, pero su
 * embedding igual queda en ALGÚN lugar del mapa, y con textos tan cortos la
 * similitud con cualquier fragmento puede pasar el umbral (0.35). Caso real:
 * "Hola!" respondió el procedimiento de fugas en la calle.
 *
 * Solo intercepta mensajes formados ÚNICAMENTE por palabras de cortesía:
 * "hola", "buenas tardes", "muchas gracias!". Si además trae una pregunta
 * ("hola, ¿cuánto cuesta la media paja?") NO la toca y sigue el flujo normal.
 */
public final class DetectorCharla {

    static final String SALUDO = """
            ¡Hola! Soy el asistente de la Oficina del Agua. Puedo ayudarte con \
            tarifas, cómo se calcula el consumo, formas de pago, trámites y qué \
            hacer ante una fuga. ¿Qué te gustaría saber?""";

    static final String GRACIAS = """
            ¡Con gusto! Si tienes otra pregunta sobre el servicio de agua, aquí estoy.""";

    static final String DESPEDIDA = """
            ¡Hasta pronto! Si necesitas algo más, aquí estaré.""";

    private static final Set<String> SALUDOS = Set.of(
            "hola", "holi", "ola", "buenas", "buenos", "buen", "dias", "dia", "tardes", "tarde",
            "noches", "noche", "hey", "saludos", "que", "tal", "onda", "hi", "hello");

    private static final Set<String> AGRADECIMIENTOS = Set.of(
            "gracias", "muchas", "mil", "ok", "okay", "vale", "perfecto", "excelente",
            "genial", "listo", "entendido", "muy", "amable", "bien", "chilero");

    private static final Set<String> DESPEDIDAS = Set.of(
            "adios", "bye", "chao", "chau", "hasta", "luego", "pronto", "nos", "vemos", "feliz");

    private DetectorCharla() {
    }

    /** La respuesta de cortesía, o vacío si el mensaje trae algo más que cortesía. */
    public static Optional<String> responder(String pregunta) {
        String[] palabras = Arrays.stream(
                        DetectorConsultaCuenta.normalizar(pregunta).split("[^a-z]+"))
                .filter(p -> !p.isBlank())
                .toArray(String[]::new);
        if (palabras.length == 0 || palabras.length > 5) {
            return Optional.empty();
        }
        if (todasEn(palabras, DESPEDIDAS)) {
            return Optional.of(DESPEDIDA);
        }
        if (todasEn(palabras, AGRADECIMIENTOS) || todasEn(palabras, union(AGRADECIMIENTOS, DESPEDIDAS))) {
            return Optional.of(GRACIAS);
        }
        if (todasEn(palabras, SALUDOS)) {
            return Optional.of(SALUDO);
        }
        return Optional.empty();
    }

    private static boolean todasEn(String[] palabras, Set<String> vocabulario) {
        return Arrays.stream(palabras).allMatch(vocabulario::contains);
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> u = new HashSet<>(a);
        u.addAll(b);
        return u;
    }
}
