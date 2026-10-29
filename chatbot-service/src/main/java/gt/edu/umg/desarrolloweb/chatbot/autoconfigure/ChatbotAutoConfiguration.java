package gt.edu.umg.desarrolloweb.chatbot.autoconfigure;

import gt.edu.umg.desarrolloweb.chatbot.config.ChatbotProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Punto de entrada de la LIBRERÍA.
 *
 * Spring Boot la descubre sola porque está listada en
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
 * El proyecto que agrega chatbot-rag a su pom.xml NO tiene que hacer
 * @ComponentScan ni @Import: con la dependencia basta.
 *
 * Para apagar el chatbot sin quitar la dependencia: chatbot.enabled=false
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChatbotProperties.class)
@ComponentScan(
        basePackages = "gt.edu.umg.desarrolloweb.chatbot",
        // Esta misma clase no se escanea como componente normal
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = AutoConfiguration.class))
public class ChatbotAutoConfiguration {
}
