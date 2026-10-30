package chatbotrag.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Modo APLICACIÓN INDEPENDIENTE (Docker, demo, desarrollo).
 *
 * Está FUERA de gt.edu.umg.desarrolloweb a propósito. Si estuviera ahí y el
 * proyecto que usa la librería tuviera su main en gt.edu.umg.desarrolloweb.algo,
 * el escaneo de componentes de ese proyecto encontraría esta clase y la cargaría
 * como configuración. Su exclude de DataSourceAutoConfiguration se aplicaría a
 * TODO ese proyecto y le apagaría su propia base de datos.
 *
 * El chatbot llega por la auto-configuración (ChatbotAutoConfiguration), igual
 * en los dos modos; esta clase solo existe para arrancar el jar -exec.
 *
 *  - Activa el perfil "chatbot-standalone" → lee application-chatbot-standalone.yml
 *    (puerto, variables de entorno de Docker). El nombre largo evita chocar con
 *    un perfil "standalone" que el otro proyecto pudiera tener.
 *  - Excluye DataSourceAutoConfiguration: el chatbot maneja sus propias
 *    conexiones y aquí no hay una BD "principal" que configurar.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ChatbotApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ChatbotApplication.class);
        app.setAdditionalProfiles("chatbot-standalone");
        app.run(args);
    }
}
