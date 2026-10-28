package gt.edu.umg.desarrolloweb.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DetectorConsultaCuentaTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "¿Cuánto debe el contador MTR-00012?",
            "cuanto debo",
            "¿Cuál es mi saldo?",
            "Quiero ver mi estado de cuenta",
            "deuda del contador 4521",
            "¿Cuánto pagué el mes pasado?",
            "Muéstrame mis pagos",
            "lecturas de CT1234",
            "cuanto devo",
            "cuanto le debo",
            "ver mi zaldo",
            "cuanto page"
    })
    void detectaPreguntasSobreUnaCuenta(String pregunta) {
        assertThat(DetectorConsultaCuenta.esConsultaDeCuenta(pregunta)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "¿Qué hago si tengo una fuga en mi casa?",
            "¿Cuánto cuesta la media paja?",
            "¿Dónde puedo pagar?",
            "Mi consumo salió muy alto, ¿qué puede ser?",
            "¿Cuál es el horario de atención?",
            "¿Cómo se calcula el monto a pagar?",
            "¿Cuándo debo pagar?"
    })
    void noBloqueaPreguntasGenerales(String pregunta) {
        assertThat(DetectorConsultaCuenta.esConsultaDeCuenta(pregunta)).isFalse();
    }
}
