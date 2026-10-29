package gt.edu.umg.desarrolloweb.chatbot.conocimiento;

import gt.edu.umg.desarrolloweb.chatbot.conocimiento.RedaccionMonolito.TarifaFila;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import gt.edu.umg.desarrolloweb.chatbot.config.ChatbotBasesDeDatos;
import org.springframework.stereotype.Component;

/**
 * Tarifas VIGENTES del monolito.
 *
 * Aplica la MISMA regla que la vista `tarifas_vigencia` del monolito
 * (migración 2026_09_04_000017), pero directo sobre la tabla `tarifas`:
 *   - vigente = la de vigente_desde más reciente que ya empezó (<= hoy)
 *   - próxima = la de vigente_desde más cercana en el futuro (> hoy)
 *
 * ¿Por qué no leer la vista? En la réplica real, el usuario de solo lectura
 * puede leer las tablas pero NO la vista (error 1045): una vista se ejecuta con
 * los permisos de su "definer", y en la réplica ese usuario no los tiene. Leer
 * las tablas no depende de eso, y el monolito sigue sin tocarse.
 *
 * Genera un fragmento por paja activa + un fragmento de resumen.
 */
@Component
@ConditionalOnProperty(name = "chatbot.monolito.enabled", havingValue = "true")
public class TarifasMonolitoReader implements FuenteMonolito {

    static final String SQL = """
            SELECT p.nombre,
                   p.equivalencia_m3,
                   t.monto_base,
                   t.precio_m3_excedente,
                   t.vigente_desde,
                   f.monto_base          AS prox_monto_base,
                   f.precio_m3_excedente AS prox_precio_m3_excedente,
                   f.vigente_desde       AS prox_vigente_desde
              FROM pajas p
              JOIN tarifas t
                ON t.paja_id = p.id
               AND t.vigente_desde = (SELECT MAX(a.vigente_desde) FROM tarifas a
                                       WHERE a.paja_id = p.id AND a.vigente_desde <= CURRENT_DATE)
              LEFT JOIN tarifas f
                ON f.paja_id = p.id
               AND f.vigente_desde = (SELECT MIN(b.vigente_desde) FROM tarifas b
                                       WHERE b.paja_id = p.id AND b.vigente_desde > CURRENT_DATE)
             WHERE p.activo = 1
             ORDER BY p.equivalencia_m3 DESC
            """;

    private final JdbcTemplate monolito;
    private final CatalogoTarifas catalogo;

    public TarifasMonolitoReader(ChatbotBasesDeDatos bd,
                                 CatalogoTarifas catalogo) {
        this.monolito = bd.monolito();
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
                    "tabla", "tarifas")));
        }
        if (!filas.isEmpty()) {
            docs.add(new Document(RedaccionMonolito.resumenTarifas(filas), Map.of(
                    "origen", ORIGEN,
                    "titulo", "Tarifas vigentes — resumen",
                    "tabla", "tarifas")));
        }
        return docs;
    }

    private static LocalDate fecha(Date d) {
        return d == null ? null : d.toLocalDate();
    }
}
