package gt.edu.umg.desarrolloweb.chatbot.conocimiento;

import static org.assertj.core.api.Assertions.assertThat;

import gt.edu.umg.desarrolloweb.chatbot.conocimiento.RedaccionMonolito.MetodoPagoFila;
import gt.edu.umg.desarrolloweb.chatbot.conocimiento.RedaccionMonolito.SectorFila;
import gt.edu.umg.desarrolloweb.chatbot.conocimiento.RedaccionMonolito.TarifaFila;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RedaccionMonolitoTest {

    private static final TarifaFila MEDIA_PAJA = new TarifaFila("1/2 paja",
            new BigDecimal("30.00"), new BigDecimal("28.00"), new BigDecimal("4.0000"),
            LocalDate.of(2026, 7, 1), null, null, null);

    private static final TarifaFila PAJA_CON_CAMBIO = new TarifaFila("1 paja",
            new BigDecimal("60.00"), new BigDecimal("45.00"), new BigDecimal("4.0000"),
            LocalDate.of(2026, 7, 1), new BigDecimal("50.00"), new BigDecimal("4.0000"),
            LocalDate.of(2027, 1, 1));

    @Test
    void tarifaIncluyeMontosVigenciaYEjemploCalculado() {
        assertThat(RedaccionMonolito.tarifa(MEDIA_PAJA))
                .contains("1/2 paja")
                .contains("1 de julio de 2026")
                .contains("hasta 30 metros cúbicos")
                .contains("monto base de Q28.00")
                .contains("excedente a Q4.00")
                .contains("36 m³ se pagan Q28.00 + 6 m³ × Q4.00 = Q52.00")
                .doesNotContain("Cambio programado");
    }

    @Test
    void tarifaConCambioProgramadoLoMenciona() {
        assertThat(RedaccionMonolito.tarifa(PAJA_CON_CAMBIO))
                .contains("Cambio programado: a partir del 1 de enero de 2027")
                .contains("será de Q50.00");
    }

    @Test
    void resumenListaTodasLasPajas() {
        assertThat(RedaccionMonolito.resumenTarifas(List.of(PAJA_CON_CAMBIO, MEDIA_PAJA)))
                .contains("1 paja incluye 60 m³ al mes por Q45.00")
                .contains("1/2 paja incluye 30 m³ al mes por Q28.00");
    }

    @Test
    void metodosDePagoIndicanCuandoPidenReferencia() {
        String texto = RedaccionMonolito.metodosPago(List.of(
                new MetodoPagoFila("Efectivo", false),
                new MetodoPagoFila("Depósito bancario", true)));
        assertThat(texto)
                .contains("Efectivo; Depósito bancario (debes indicar el número de referencia");
    }

    @Test
    void sectoresConYSinDescripcion() {
        String texto = RedaccionMonolito.sectores(List.of(
                new SectorFila("Sector Centro", "Calles del parque"),
                new SectorFila("Los Pinos", null)));
        assertThat(texto).contains("Sector Centro (Calles del parque); Los Pinos.");
    }
}
