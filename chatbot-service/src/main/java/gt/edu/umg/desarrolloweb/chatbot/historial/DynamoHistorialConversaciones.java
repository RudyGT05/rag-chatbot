package gt.edu.umg.desarrolloweb.chatbot.historial;

import gt.edu.umg.desarrolloweb.chatbot.config.ChatbotProperties;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Historial de conversaciones en DynamoDB (NoSQL).
 *
 * ¿Por qué NoSQL para esto? Cada conversación es un documento independiente,
 * se escribe mucho y casi no se consulta con JOINs: es el caso ideal para una
 * tabla clave-valor. Además no toca ninguna BD relacional del sistema.
 *
 * Cada ítem de la tabla:
 *   id (clave) · fecha · pregunta · respuesta · tipo (RAG, CALCULO, PRIVACIDAD,
 *   SIN_INFORMACION) · fuentes · latenciaMs · ipHash · expiraEn (TTL)
 *
 * Decisiones:
 *  - ASÍNCRONO: se guarda en otro hilo; el usuario no espera a DynamoDB.
 *  - NUNCA rompe el chat: si DynamoDB está caído, solo queda un aviso en el log.
 *  - PRIVACIDAD: la IP no se guarda tal cual, solo un hash; y "expiraEn"
 *    permite que AWS borre las conversaciones viejas solo (TTL).
 *  - La tabla se crea sola la primera vez (cómodo con DynamoDB Local).
 */
@Component
@ConditionalOnProperty(name = "chatbot.historial.enabled", havingValue = "true")
public class DynamoHistorialConversaciones implements HistorialConversaciones, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DynamoHistorialConversaciones.class);

    private final DynamoDbClient dynamo;
    private final String tabla;
    private final int diasRetencion;
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chatbot-historial");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean tablaLista = false;

    public DynamoHistorialConversaciones(ChatbotProperties props) {
        ChatbotProperties.Historial h = props.historial();
        this.tabla = h.tabla();
        this.diasRetencion = h.diasRetencion();

        DynamoDbClientBuilder builder = DynamoDbClient.builder().region(Region.of(h.region()));
        if (h.endpoint() != null && !h.endpoint().isBlank()) {
            // DynamoDB Local: acepta cualquier credencial (solo letras y números)
            builder.endpointOverride(URI.create(h.endpoint()))
                   .credentialsProvider(StaticCredentialsProvider.create(
                           AwsBasicCredentials.create("local", "local")));
        } else {
            // AWS real: toma las credenciales del entorno (variables, perfil, rol IAM)
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        this.dynamo = builder.build();
        log.info("Historial de conversaciones en DynamoDB: tabla '{}'", tabla);
    }

    @Override
    public void registrar(Conversacion c) {
        try {
            ejecutor.submit(() -> guardar(c));
        } catch (RuntimeException e) {
            log.warn("No se pudo encolar la conversación para el historial: {}", e.getMessage());
        }
    }

    private void guardar(Conversacion c) {
        try {
            asegurarTabla();
            Instant ahora = Instant.now();
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", s(UUID.randomUUID().toString()));
            item.put("fecha", s(ahora.toString()));
            item.put("pregunta", s(c.pregunta()));
            item.put("respuesta", s(c.respuesta()));
            item.put("tipo", s(c.tipo()));
            item.put("fuentes", AttributeValue.builder()
                    .l(c.fuentes().stream().map(DynamoHistorialConversaciones::s).toList())
                    .build());
            item.put("latenciaMs", n(c.latenciaMs()));
            item.put("ipHash", s(hash(c.ipCliente())));
            item.put("expiraEn", n(ahora.plus(diasRetencion, ChronoUnit.DAYS).getEpochSecond()));

            dynamo.putItem(PutItemRequest.builder().tableName(tabla).item(item).build());
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar la conversación en DynamoDB: {}", e.getMessage());
        }
    }

    /** Crea la tabla si no existe. Clave: "id" (texto). Pago por uso, sin capacidad fija. */
    private void asegurarTabla() {
        if (tablaLista) {
            return;
        }
        try {
            dynamo.describeTable(DescribeTableRequest.builder().tableName(tabla).build());
        } catch (ResourceNotFoundException noExiste) {
            try {
                dynamo.createTable(CreateTableRequest.builder()
                        .tableName(tabla)
                        .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
                        .attributeDefinitions(AttributeDefinition.builder()
                                .attributeName("id").attributeType(ScalarAttributeType.S).build())
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build());
                dynamo.waiter().waitUntilTableExists(r -> r.tableName(tabla));
                log.info("Tabla DynamoDB '{}' creada", tabla);
            } catch (ResourceInUseException otraInstanciaLaCreo) {
                // Otra instancia la creó al mismo tiempo: no pasa nada
            }
        }
        tablaLista = true;
    }

    private static AttributeValue s(String valor) {
        return AttributeValue.builder().s(valor == null ? "" : valor).build();
    }

    private static AttributeValue n(long valor) {
        return AttributeValue.builder().n(Long.toString(valor)).build();
    }

    /** SHA-256 recortado: permite contar usuarios distintos sin guardar su IP. */
    static String hash(String ip) {
        if (ip == null || ip.isBlank()) {
            return "desconocida";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(ip.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "desconocida";
        }
    }

    @Override
    public void destroy() {
        ejecutor.shutdown();
        dynamo.close();
    }
}
