package gt.edu.umg.desarrolloweb.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FiltroRelevanciaTest {

    record Frag(String titulo, Double score) {
    }

    private static final Function<Frag, Double> SCORE = Frag::score;

    @Test
    void casoRealDeLaFuga_soloQuedaElMejor() {
        List<Frag> encontrados = List.of(
                new Frag("fuga en casa", 0.7055),
                new Frag("fuga en la calle", 0.6484),
                new Frag("detectar fuga oculta", 0.6327),
                new Frag("consumo alto", 0.6207));

        assertThat(FiltroRelevancia.dentroDelMargen(encontrados, SCORE, 0.05))
                .extracting(Frag::titulo)
                .containsExactly("fuga en casa");
    }

    @Test
    void conservaLosQueEstanCercaDelMejor() {
        List<Frag> encontrados = List.of(
                new Frag("a", 0.60), new Frag("b", 0.58), new Frag("c", 0.40));

        assertThat(FiltroRelevancia.dentroDelMargen(encontrados, SCORE, 0.05))
                .extracting(Frag::titulo)
                .containsExactly("a", "b");
    }

    @Test
    void listaVaciaYSinScoresNoFallan() {
        assertThat(FiltroRelevancia.dentroDelMargen(List.<Frag>of(), SCORE, 0.05)).isEmpty();
        List<Frag> sinScore = List.of(new Frag("x", null));
        assertThat(FiltroRelevancia.dentroDelMargen(sinScore, SCORE, 0.05)).hasSize(1);
    }
}
