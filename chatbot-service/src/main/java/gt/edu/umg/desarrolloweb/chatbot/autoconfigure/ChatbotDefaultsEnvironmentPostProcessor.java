package gt.edu.umg.desarrolloweb.chatbot.autoconfigure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Agrega chatbot-defaults.properties AL FINAL de las fuentes de configuración,
 * es decir, con la prioridad más baja.
 *
 * Así el proyecto que usa la librería no tiene que copiar 30 propiedades:
 * solo define las que quiere cambiar (URL de su BD, etc.) y el resto sale de aquí.
 *
 * Se registra en META-INF/spring.factories.
 */
public class ChatbotDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String NOMBRE = "chatbotDefaults";
    private static final String ARCHIVO = "chatbot-defaults.properties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(NOMBRE)) {
            return;
        }
        try {
            Properties defaults = PropertiesLoaderUtils.loadProperties(new ClassPathResource(ARCHIVO));
            environment.getPropertySources().addLast(new PropertiesPropertySource(NOMBRE, defaults));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ARCHIVO, e);
        }
    }
}
