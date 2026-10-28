package gt.edu.umg.desarrolloweb.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lee el bloque "chatbot:" de application.yml en un objeto tipado.
 * Ej.: chatbot.rag.top-k → rag().topK()
 */
@ConfigurationProperties(prefix = "chatbot")
public record ChatbotProperties(Rag rag, Ingesta ingesta, RateLimit rateLimit) {

    public record Rag(int topK, double similarityThreshold, double margenRelativo) {
    }

    public record Ingesta(boolean alIniciar) {
    }

    public record RateLimit(int preguntasPorMinuto) {
    }
}
