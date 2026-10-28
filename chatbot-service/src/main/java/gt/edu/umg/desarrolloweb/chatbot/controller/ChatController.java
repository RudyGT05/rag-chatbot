package gt.edu.umg.desarrolloweb.chatbot.controller;

import gt.edu.umg.desarrolloweb.chatbot.api.dto.ChatRequestDto;
import gt.edu.umg.desarrolloweb.chatbot.api.dto.ChatResponseDto;
import gt.edu.umg.desarrolloweb.chatbot.api.dto.SourceDto;
import gt.edu.umg.desarrolloweb.chatbot.api.spec.ChatApi;
import gt.edu.umg.desarrolloweb.chatbot.service.ChatService;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implementa la interfaz ChatApi que GENERA el plugin a partir del YAML.
 * Por eso aquí no hay @PostMapping, @RequestBody ni @Valid: ya vienen en la
 * interfaz. Si el contrato cambia, esta clase deja de compilar hasta
 * actualizarla. Esa es la ventaja de contract-first.
 */
@RestController
public class ChatController implements ChatApi {

    private static final int LARGO_SNIPPET = 200;

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public ResponseEntity<ChatResponseDto> enviarPregunta(ChatRequestDto chatRequestDto) {
        ChatService.Respuesta respuesta = chatService.responder(chatRequestDto.getQuestion().trim());

        List<SourceDto> fuentes = respuesta.fuentes().stream()
                .map(ChatController::aSource)
                .toList();

        return ResponseEntity.ok(new ChatResponseDto()
                .answer(respuesta.answer())
                .sources(fuentes));
    }

    private static SourceDto aSource(Document doc) {
        String texto = doc.getText() == null ? "" : doc.getText();
        String snippet = texto.length() > LARGO_SNIPPET
                ? texto.substring(0, LARGO_SNIPPET) + "…"
                : texto;
        return new SourceDto()
                .title(String.valueOf(doc.getMetadata().getOrDefault("titulo", "Sin título")))
                .origin(String.valueOf(doc.getMetadata().getOrDefault("origen", "desconocido")))
                .snippet(snippet)
                .score(doc.getScore());
    }
}
