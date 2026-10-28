package gt.edu.umg.desarrolloweb.chatbot.conocimiento;

import gt.edu.umg.desarrolloweb.chatbot.conocimiento.RedaccionMonolito.TarifaFila;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Tarifas VIGENTES del monolito, usando su propia vista `tarifas_vigencia`
 * (migración 2026_09_04_000017). Reusar la vista del monolito garantiza que el
 * chatbot entiende la vigencia exactamente igual que el sistema que cobra.
 *
 * Genera un fragmento por paja activa + un fragmento de resumen.
 */
@Component
@ConditionalOnProperty(name = "monolito.enabled", havingValue = "true")
public class TarifasMonolitoReader implements FuenteMonolito {

    static final String SQL = """
            SELECT p.nombre,
                   p.equivalencia_m3,
                   tv.monto_base,
                   tv.precio_m3_excedente,
                   tv.vigente_desde,
                   f.monto_base          AS prox_monto_base,
                   f.precio_m3_excedente AS prox_precio_m3_excedente,
                   f.vigente_desde       AS prox_vigente_desde
              FROM tarifas_vigencia tv
              JOIN pajas p ON p.id = tv.paja_id
              LEFT JOIN tarifas f ON f.paja_id = tv.paja_id AND f.vigente_desde = tv.proxima_vigencia
             WHERE p.activo = 1
               AND tv.vigente_desde <= CURRENT_DATE
               AND (tv.proxima_vigencia IS NULL OR tv.proxima_vigencia > CURRENT_DATE)
             ORDER BY p.equivalencia_m3 DESC
            """;

    private final JdbcTemplate monolito;
    private final CatalogoTarifas catalogo;

    public TarifasMonolitoReader(@Qualifier("monolitoJdbcTemplate") JdbcTemplate monolito,
                                 CatalogoTarifas catalogo) {
        this.monolito = monolito;
        this.catalogo = catalogo;
    }

    @Override
    public List<Document> leer() {
        List<TarifaFila> filas = monolito.query(SQL, (rs, i) -> new TarifaFila(
                rs.getString("nombre"),
                rs.getBigDecimal("equivalencia_m3"),
                rs.getBigDecimal("monto_base"),
                rs.getBigDecimal("precio_m3_excedente"),
                fecha(rs.getDate("vigente_desde")),
                rs.getBigDecimal("prox_monto_base"),
                rs.getBigDecimal("prox_precio_m3_excedente"),
                fecha(rs.getDate("prox_vigente_desde"))));

        // La calculadora usa estas mismas tarifas: quedan sincronizadas con el RAG
        catalogo.actualizar(filas);

        List<Document> docs = new ArrayList<>();
        for (TarifaFila t : filas) {
            docs.add(new Document(RedaccionMonolito.tarifa(t), Map.of(
                    "origen", ORIGEN,
                    "titulo", "Tarifa vigente — " + t.paja(),
                    "tabla", "tarifas_vigencia")));
        }
        if (!filas.isEmpty()) {
            docs.add(new Document(RedaccionMonolito.resumenTarifas(filas), Map.of(
                    "origen", ORIGEN,
                    "titulo", "Tarifas vigentes — resumen",
                    "tabla", "tarifas_vigencia")));
        }
        return docs;
    }

    private static LocalDate fecha(Date d) {
        return d == null ? null : d.toLocalDate();
    }
}
