package gt.edu.umg.desarrolloweb.chatbot.conocimiento;

import gt.edu.umg.desarrolloweb.chatbot.conocimiento.RedaccionMonolito.SectorFila;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Sectores ACTIVOS (zonas con servicio) de la tabla `sectores` del monolito. */
@Component
@ConditionalOnProperty(name = "monolito.enabled", havingValue = "true")
public class SectoresMonolitoReader implements FuenteMonolito {

    static final String SQL = """
            SELECT nombre, descripcion
              FROM sectores
             WHERE activo = 1
             ORDER BY orden, nombre
            """;

    private final JdbcTemplate monolito;

    public SectoresMonolitoReader(@Qualifier("monolitoJdbcTemplate") JdbcTemplate monolito) {
        this.monolito = monolito;
    }

    @Override
    public List<Document> leer() {
        List<SectorFila> sectores = monolito.query(SQL, (rs, i) ->
                new SectorFila(rs.getString("nombre"), rs.getString("descripcion")));
        if (sectores.isEmpty()) {
            return List.of();
        }
        return List.of(new Document(RedaccionMonolito.sectores(sectores), Map.of(
                "origen", ORIGEN,
                "titulo", "Sectores con servicio",
                "tabla", "sectores")));
    }
}
