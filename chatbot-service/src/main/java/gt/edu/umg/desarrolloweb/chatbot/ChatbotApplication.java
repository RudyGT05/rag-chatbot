package gt.edu.umg.desarrolloweb.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada. Va en el paquete raíz para que el escaneo de componentes
 * encuentre controller/, service/, config/, etc.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotApplication.class, args);
    }
}
