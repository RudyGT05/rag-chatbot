package gt.edu.umg.desarrolloweb.chatbot.conocimiento;

import gt.edu.umg.desarrolloweb.chatbot.conocimiento.RedaccionMonolito.MetodoPagoFila;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Formas de pago ACTIVAS del catálogo `metodos_pago` del monolito. */
@Component
@ConditionalOnProperty(name = "monolito.enabled", havingValue = "true")
public class MetodosPagoMonolitoReader implements FuenteMonolito {

    static final String SQL = """
            SELECT nombre, requiere_referencia
              FROM metodos_pago
             WHERE activo = 1
             ORDER BY id
            """;

    private final JdbcTemplate monolito;

    public MetodosPagoMonolitoReader(@Qualifier("monolitoJdbcTemplate") JdbcTemplate monolito) {
        this.monolito = monolito;
    }

    @Override
    public List<Document> leer() {
        List<MetodoPagoFila> metodos = monolito.query(SQL, (rs, i) ->
                new MetodoPagoFila(rs.getString("nombre"), rs.getBoolean("requiere_referencia")));
        if (metodos.isEmpty()) {
            return List.of();
        }
        return List.of(new Document(RedaccionMonolito.metodosPago(metodos), Map.of(
                "origen", ORIGEN,
                "titulo", "Formas de pago aceptadas",
                "tabla", "metodos_pago")));
    }
}
