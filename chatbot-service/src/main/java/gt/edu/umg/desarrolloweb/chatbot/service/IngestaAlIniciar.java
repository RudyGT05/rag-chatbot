package gt.edu.umg.desarrolloweb.chatbot.service;

import gt.edu.umg.desarrolloweb.chatbot.config.ChatbotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Se ejecuta una vez cuando el servicio termina de arrancar: llena pgvector.
 *
 * Si falla (Ollama aún no listo, monolito apagado...), NO tumba el servicio:
 * se registra el error y se puede reintentar con POST /api/v1/ingest.
 */
@Component
public class IngestaAlIniciar implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestaAlIniciar.class);

    private final IngestaService ingestaService;
    private final ChatbotProperties props;

    public IngestaAlIniciar(IngestaService ingestaService, ChatbotProperties props) {
        this.ingestaService = ingestaService;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.ingesta().alIniciar()) {
            log.info("Ingesta al iniciar desactivada (chatbot.ingesta.al-iniciar=false)");
            return;
        }
        try {
            ingestaService.reingestar();
        } catch (RuntimeException e) {
            log.error("La ingesta al iniciar falló. Reintenta con POST /api/v1/ingest. Causa: {}",
                    e.getMessage(), e);
        }
    }
}
