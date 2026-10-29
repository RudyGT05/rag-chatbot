package gt.edu.umg.desarrolloweb.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Toda la configuración del chatbot vive bajo el prefijo "chatbot.".
 *
 * Al usarse como LIBRERÍA dentro de otro proyecto, un prefijo propio evita
 * chocar con la configuración de ese proyecto (su spring.datasource, etc.).
 * Los valores por defecto están en chatbot-defaults.properties; el proyecto
 * que nos usa solo sobreescribe lo que necesite.
 *
 * Ej.: chatbot.rag.top-k → rag().topK()
 */
@ConfigurationProperties(prefix = "chatbot")
public record ChatbotProperties(boolean enabled, Rag rag, Ingesta ingesta, RateLimit rateLimit,
                                VectorDb vectorDb, Monolito monolito, Historial historial) {

    public record Rag(int topK, double similarityThreshold, double margenRelativo) {
    }

    public record Ingesta(boolean alIniciar) {
    }

    public record RateLimit(int preguntasPorMinuto) {
    }

    /** Postgres + pgvector: la BD PROPIA del chatbot. */
    public record VectorDb(String url, String username, String password, String tabla, int dimensiones) {
    }

    /** MySQL del monolito: solo lectura, opcional. */
    public record Monolito(boolean enabled, String url, String username, String password) {
    }

    /** DynamoDB: historial de conversaciones, opcional. endpoint vacío = AWS real. */
    public record Historial(boolean enabled, String endpoint, String region, String tabla, int diasRetencion) {
    }
}
