package gt.edu.umg.desarrolloweb.chatbot.conocimiento;

import static org.assertj.core.api.Assertions.assertThat;

import gt.edu.umg.desarrolloweb.chatbot.conocimiento.RedaccionMonolito.TarifaFila;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class CalculadoraConsumoTest {

    private static final List<TarifaFila> TARIFAS = List.of(
            new TarifaFila("1 paja", new BigDecimal("60.00"), new BigDecimal("45.00"), new BigDecimal("4.0000"),
                    LocalDate.of(2026, 7, 1), null, null, null),
            new TarifaFila("1/2 paja", new BigDecimal("30.00"), new BigDecimal("28.00"), new BigDecimal("4.0000"),
                    LocalDate.of(2026, 7, 1), null, null, null),
            new TarifaFila("1/4 paja", new BigDecimal("15.00"), new BigDecimal("17.00"), new BigDecimal("4.0000"),
                    LocalDate.of(2026, 7, 1), null, null, null));

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // El caso real en el que qwen2.5:3b respondió Q76.00
            "cuanto pago si tengo 1/2 paja y gasté 36 metros cúbicos | Q52.00",
            "con media paja y 36 m3 cuanto es                        | Q52.00",
            "tengo un cuarto de paja y consumí 20                    | Q37.00",
            "1 paja, 50 m³                                           | Q45.00",
            "paja completa con 66.5 metros                           | Q71.00",
            "si consumo 30,5 m3 con 1/2 paja?                        | Q30.00"
    })
    void calculaExactoConPajaYConsumo(String pregunta, String total) {
        assertThat(CalculadoraConsumo.responder(pregunta, TARIFAS))
                .hasValueSatisfying(r -> assertThat(r.texto()).contains("pagas " + total));
    }

    @Test
    void sinPajaCalculaParaCadaUna() {
        assertThat(CalculadoraConsumo.responder("gasté 40 metros cúbicos, cuánto pago?", TARIFAS))
                .hasValueSatisfying(r -> {
                    assertThat(r.texto())
                            .contains("con 1 paja pagas Q45.00")
                            .contains("con 1/2 paja pagas Q68.00")
                            .contains("con 1/4 paja pagas Q117.00");
                    assertThat(r.tarifasUsadas()).hasSize(3);
                });
    }

    @Test
    void pajaNoVigenteLoDiceEnVezDeIgnorarla() {
        assertThat(CalculadoraConsumo.responder("tengo 2 pajas y gaste 100 m3", TARIFAS))
                .hasValueSatisfying(r -> assertThat(r.texto())
                        .startsWith("No encontré una tarifa vigente para ese tipo de paja")
                        .contains("1 paja, 1/2 paja, 1/4 paja"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "¿Cuánto cuesta la media paja?",
            "¿Qué hago si tengo una fuga?",
            "¿Dónde puedo pagar?"
    })
    void sinCantidadNoInterviene(String pregunta) {
        assertThat(CalculadoraConsumo.responder(pregunta, TARIFAS)).isEmpty();
    }

    @Test
    void sinTarifasCargadasNoInterviene() {
        assertThat(CalculadoraConsumo.responder("1/2 paja y 36 m3", List.of())).isEmpty();
    }
}
