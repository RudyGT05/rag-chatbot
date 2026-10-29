package gt.edu.umg.desarrolloweb.chatbot.historial;

import java.util.List;

/**
 * Guarda cada conversación (pregunta + respuesta) para poder revisarlas después:
 * qué pregunta la gente, qué no supo responder el bot, cuánto tarda.
 *
 * Es una interfaz para que el ChatController no dependa de DynamoDB: si el
 * historial está apagado (chatbot.historial.enabled=false) simplemente no hay
 * implementación y no se guarda nada.
 */
public interface HistorialConversaciones {

    /** No debe lanzar excepciones ni bloquear: el chat nunca falla por el historial. */
    void registrar(Conversacion conversacion);

    record Conversacion(String pregunta, String respuesta, String tipo, List<String> fuentes,
                        long latenciaMs, String ipCliente) {
    }
}
