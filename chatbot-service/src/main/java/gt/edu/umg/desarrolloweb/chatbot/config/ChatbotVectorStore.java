package gt.edu.umg.desarrolloweb.chatbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;

/**
 * Envoltorio del vector store (pgvector) del chatbot.
 *
 * El PgVectorStore crea su tabla y la extensión "vector" al inicializarse. Si
 * fuera un bean normal, Spring lo inicializaría al arrancar y, con Postgres
 * caído, tumbaría TODA la aplicación anfitriona. Aquí la inicialización es
 * explícita (asegurarEsquema) y la hace la ingesta, que ya maneja errores.
 */
public class ChatbotVectorStore {

    private static final Logger log = LoggerFactory.getLogger(ChatbotVectorStore.class);

    private final PgVectorStore store;
    private volatile boolean esquemaListo = false;

    public ChatbotVectorStore(PgVectorStore store) {
        this.store = store;
    }

    public VectorStore store() {
        return store;
    }

    /** Crea extensión + tabla + índice si no existen. Idempotente. */
    public synchronized void asegurarEsquema() {
        if (esquemaListo) {
            return;
        }
        try {
            store.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar pgvector: " + e.getMessage(), e);
        }
        esquemaListo = true;
        log.info("Esquema de pgvector listo");
    }
}
