package gt.edu.umg.desarrolloweb.chatbotapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Modo APLICACIÓN INDEPENDIENTE (Docker, demo, desarrollo).
 *
 * Está en un paquete aparte (chatbotapp) a propósito: cuando otro proyecto usa
 * chatbot-rag como LIBRERÍA, esta clase no se carga. El chatbot llega por la
 * auto-configuración (ChatbotAutoConfiguration), igual en los dos modos.
 *
 *  - Activa el perfil "standalone" → lee application-standalone.yml
 *    (puerto, variables de entorno de Docker).
 *  - Excluye DataSourceAutoConfiguration: el chatbot maneja sus propias
 *    conexiones y aquí no hay una BD "principal" que configurar.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ChatbotApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ChatbotApplication.class);
        app.setAdditionalProfiles("standalone");
        app.run(args);
    }
}
