package gt.edu.umg.desarrolloweb.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DetectorCharlaTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // Caso real: "Hola!" respondía el procedimiento de fugas en la calle
            "Hola!                 | ¡Hola!",
            "buenas tardes         | ¡Hola!",
            "Buenos días           | ¡Hola!",
            "hola que tal          | ¡Hola!",
            "muchas gracias!       | ¡Con gusto!",
            "ok gracias            | ¡Con gusto!",
            "gracias, hasta luego  | ¡Con gusto!",
            "adiós                 | ¡Hasta pronto!"
    })
    void respondeCortesiaSinBuscarEnElMapa(String mensaje, String inicio) {
        assertThat(DetectorCharla.responder(mensaje)).hasValueSatisfying(r -> assertThat(r).startsWith(inicio));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "hola, ¿cuánto cuesta la media paja?",
            "buenas, tengo una fuga",
            "gracias, ¿y dónde pago?",
            "¿Dónde puedo pagar?",
            "cuanto debo"
    })
    void siTraeUnaPreguntaSigueElFlujoNormal(String mensaje) {
        assertThat(DetectorCharla.responder(mensaje)).isEmpty();
    }
}
