package gt.edu.umg.desarrolloweb.chatbot.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Las dos conexiones del chatbot, ENCAPSULADAS.
 *
 * ¿Por qué no declararlas como beans DataSource/JdbcTemplate normales?
 * Porque esta librería vive dentro del proyecto de otro equipo. Si
 * declaráramos un DataSource, Spring Boot dejaría de auto-configurar el
 * DataSource de ESE proyecto (su BD dejaría de funcionar). Encapsuladas en
 * esta clase, nadie más las ve ni las confunde con las suyas.
 *
 *  - vector():   Postgres + pgvector, la BD propia del chatbot.
 *  - monolito(): MySQL del monolito en SOLO LECTURA (si está habilitado).
 *
 * Los pools se crean "perezosos": no se conectan hasta la primera consulta,
 * así un monolito caído no impide que arranque la aplicación.
 */
public class ChatbotBasesDeDatos implements AutoCloseable {

    private final HikariDataSource vectorDs;
    private final JdbcTemplate vector;
    private final HikariDataSource monolitoDs;   // null si chatbot.monolito.enabled=false
    private final JdbcTemplate monolito;

    public ChatbotBasesDeDatos(ChatbotProperties props) {
        ChatbotProperties.VectorDb v = props.vectorDb();
        this.vectorDs = pool("chatbot-pgvector", v.url(), v.username(), v.password(), false, 5);
        this.vector = new JdbcTemplate(vectorDs);

        ChatbotProperties.Monolito m = props.monolito();
        if (m.enabled()) {
            // Doble candado: además del usuario con solo SELECT en MySQL,
            // la conexión misma se abre en modo read-only.
            this.monolitoDs = pool("chatbot-monolito-solo-lectura", m.url(), m.username(), m.password(), true, 2);
            this.monolito = new JdbcTemplate(monolitoDs);
        } else {
            this.monolitoDs = null;
            this.monolito = null;
        }
    }

    public JdbcTemplate vector() {
        return vector;
    }

    public JdbcTemplate monolito() {
        if (monolito == null) {
            throw new IllegalStateException("La lectura del monolito está desactivada (chatbot.monolito.enabled=false)");
        }
        return monolito;
    }

    private static HikariDataSource pool(String nombre, String url, String usuario, String clave,
                                         boolean soloLectura, int maximo) {
        HikariDataSource ds = new HikariDataSource();   // constructor vacío = no conecta todavía
        ds.setPoolName(nombre);
        ds.setJdbcUrl(url);
        ds.setUsername(usuario);
        ds.setPassword(clave);
        ds.setReadOnly(soloLectura);
        ds.setMaximumPoolSize(maximo);
        return ds;
    }

    /** Spring lo llama al apagar la aplicación (cierra los pools). */
    @Override
    public void close() {
        vectorDs.close();
        if (monolitoDs != null) {
            monolitoDs.close();
        }
    }
}
