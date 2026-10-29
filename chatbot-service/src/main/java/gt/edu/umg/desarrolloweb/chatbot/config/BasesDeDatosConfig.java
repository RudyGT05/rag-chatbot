package gt.edu.umg.desarrolloweb.chatbot.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Crea las conexiones y el vector store del chatbot con nombres propios
 * (chatbot...), para no chocar con los beans del proyecto que nos usa.
 */
@Configuration(proxyBeanMethods = false)
public class BasesDeDatosConfig {

    @Bean
    public ChatbotBasesDeDatos chatbotBasesDeDatos(ChatbotProperties props) {
        return new ChatbotBasesDeDatos(props);
    }

    /**
     * El EmbeddingModel lo auto-configura Spring AI con Ollama
     * (spring.ai.ollama.*): convierte cada texto en un vector de 1024 números.
     */
    @Bean
    public ChatbotVectorStore chatbotVectorStore(ChatbotBasesDeDatos bd, EmbeddingModel embeddingModel,
                                                 ChatbotProperties props) {
        PgVectorStore store = PgVectorStore.builder(bd.vector(), embeddingModel)
                .schemaName("public")
                .vectorTableName(props.vectorDb().tabla())
                .dimensions(props.vectorDb().dimensiones())
                .distanceType(PgDistanceType.COSINE_DISTANCE)
                .indexType(PgIndexType.HNSW)
                .initializeSchema(true)
                .build();
        return new ChatbotVectorStore(store);
    }
}
