package gt.edu.umg.desarrolloweb.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import gt.edu.umg.desarrolloweb.chatbot.conocimiento.FaqLoader;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

/**
 * Pruebas unitarias SIN levantar Spring, Postgres ni Ollama: corren en
 * segundos con "mvn test". Prueban la lógica propia (chunking y redacción).
 */
class RagUnitTest {

    @Test
    void partePorSeccionesYConservaTitulos() {
        String md = """
                # Pagos

                ## ¿Dónde pago?
                En ventanilla.

                ## ¿Qué pasa si me atraso?
                Se cobra mora.
                """;

        List<Document> docs = FaqLoader.partirEnSecciones(md, "01-pagos.md");

        assertThat(docs).hasSize(2);
        assertThat(docs.get(0).getText()).startsWith("Pagos — ¿Dónde pago?").contains("En ventanilla.");
        assertThat(docs.get(0).getMetadata())
                .containsEntry("origen", "faq")
                .containsEntry("titulo", "Pagos — ¿Dónde pago?");
        assertThat(docs.get(1).getText()).contains("Se cobra mora.");
    }

    @Test
    void ignoraSeccionesVacias() {
        String md = """
                # Doc
                ## Vacía
                ## Con contenido
                Texto.
                """;
        assertThat(FaqLoader.partirEnSecciones(md, "x.md")).hasSize(1);
    }

    @Test
    void cargaLosArchivosRealesDelFaq() throws IOException {
        List<Document> docs = new FaqLoader().cargar();
        assertThat(docs).hasSizeGreaterThan(20);
        assertThat(docs).allSatisfy(d -> {
            assertThat(d.getText()).isNotBlank();
            // Un fragmento demasiado largo diluye la búsqueda por similitud
            assertThat(d.getText().length()).isLessThan(1500);
        });
    }
}
