package gt.edu.umg.desarrolloweb.chatbot.exception;

/**
 * El proveedor de IA (LLM o embeddings) no respondió: caído, timeout,
 * API key inválida, modelo no descargado... El contrato lo traduce a 503.
 */
public class LlmNoDisponibleException extends RuntimeException {

    public LlmNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
