package gt.edu.umg.desarrolloweb.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class ChatServiceTest {

    @Test
    void elPromptNumeraLosFragmentosYTerminaConLaPregunta() {
        List<Document> fragmentos = List.of(
                new Document("Se paga en ventanilla.", Map.of("origen", "faq")),
                new Document("Horario de 8 a 16.", Map.of("origen", "faq")));

        String prompt = ChatService.armarPrompt("¿Dónde pago?", fragmentos);

        assertThat(prompt)
                .startsWith("CONTEXTO:")
                .contains("[1] Se paga en ventanilla.")
                .contains("[2] Horario de 8 a 16.")
                .contains("PREGUNTA DEL USUARIO:\n¿Dónde pago?")
                .endsWith("(dónde, cómo y por qué).");
    }
}
