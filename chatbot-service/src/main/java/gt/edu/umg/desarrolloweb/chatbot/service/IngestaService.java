package gt.edu.umg.desarrolloweb.chatbot.service;

import gt.edu.umg.desarrolloweb.chatbot.config.ChatbotVectorStore;
import gt.edu.umg.desarrolloweb.chatbot.conocimiento.FaqLoader;
import gt.edu.umg.desarrolloweb.chatbot.conocimiento.FuenteMonolito;
import gt.edu.umg.desarrolloweb.chatbot.exception.LlmNoDisponibleException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * INGESTA = llenar la base vectorial.
 *
 *   texto → embedding (vector de 1024 números) → fila en pgvector
 *
 * vectorStore.add(...) hace las dos cosas: llama al modelo de embeddings y
 * guarda texto + vector + metadatos en Postgres.
 *
 * Estrategia: "borrar y volver a cargar" por origen. Antes de cargar el FAQ
 * se borran los fragmentos con origen = 'faq'; igual con 'monolito'. Así una
 * tarifa que cambió en el monolito no deja su versión vieja en el RAG.
 */
@Service
public class IngestaService {

    private static final Logger log = LoggerFactory.getLogger(IngestaService.class);

    private final ChatbotVectorStore chatbotVectorStore;
    private final VectorStore vectorStore;
    private final FaqLoader faqLoader;
    private final List<FuenteMonolito> fuentesMonolito;   // vacía si monolito.enabled=false

    public IngestaService(ChatbotVectorStore chatbotVectorStore, FaqLoader faqLoader,
                          ObjectProvider<FuenteMonolito> fuentesMonolito) {
        this.chatbotVectorStore = chatbotVectorStore;
        this.vectorStore = chatbotVectorStore.store();
        this.faqLoader = faqLoader;
        // ObjectProvider: si no hay ninguna fuente (monolito desactivado) da una lista vacía
        this.fuentesMonolito = fuentesMonolito.orderedStream().toList();
    }

    public record Resultado(int faqChunks, int monolitoChunks, long durationMs) {
    }

    /** synchronized: evita dos ingestas a la vez (arranque + POST /ingest). */
    public synchronized Resultado reingestar() {
        long inicio = System.currentTimeMillis();
        chatbotVectorStore.asegurarEsquema();   // crea la tabla de pgvector la primera vez

        int faq = reemplazar(FaqLoader.ORIGEN, cargarFaq());

        int monolito = 0;
        if (!fuentesMonolito.isEmpty()) {
            List<Document> docs = new ArrayList<>();
            for (FuenteMonolito fuente : fuentesMonolito) {
                docs.addAll(fuente.leer());
            }
            monolito = reemplazar(FuenteMonolito.ORIGEN, docs);
        }

        long duracion = System.currentTimeMillis() - inicio;
        log.info("Ingesta completada: {} fragmentos FAQ, {} del monolito, {} ms", faq, monolito, duracion);
        return new Resultado(faq, monolito, duracion);
    }

    private int reemplazar(String origen, List<Document> documentos) {
        vectorStore.delete("origen == '" + origen + "'");
        if (!documentos.isEmpty()) {
            try {
                vectorStore.add(documentos);   // aquí se llama al modelo de embeddings
            } catch (RuntimeException e) {
                throw new LlmNoDisponibleException(
                        "No se pudieron generar los embeddings de '" + origen + "'", e);
            }
        }
        return documentos.size();
    }

    private List<Document> cargarFaq() {
        try {
            return faqLoader.cargar();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron leer los archivos de conocimiento", e);
        }
    }
}
