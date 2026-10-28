package gt.edu.umg.desarrolloweb.chatbot.conocimiento;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Fuente 1 del RAG: los archivos Markdown de src/main/resources/conocimiento/.
 *
 * CHUNKING: cada archivo se parte por sus secciones "## ". Una sección = un
 * fragmento (chunk). Así cada fragmento habla de UNA sola cosa (ej. "¿Dónde
 * puedo pagar?"), y la búsqueda por similitud trae justo lo que se preguntó.
 *
 * Cada fragmento lleva metadatos:
 *   - origen = "faq"   → para poder borrar solo estos al reingestar
 *   - titulo           → se devuelve en "sources" de la respuesta
 */
@Component
public class FaqLoader {

    public static final String ORIGEN = "faq";
    private static final String PATRON = "classpath:conocimiento/*.md";

    public List<Document> cargar() throws IOException {
        Resource[] archivos = new PathMatchingResourcePatternResolver().getResources(PATRON);
        Arrays.sort(archivos, Comparator.comparing(Resource::getFilename));

        List<Document> fragmentos = new ArrayList<>();
        for (Resource archivo : archivos) {
            try (InputStream in = archivo.getInputStream()) {
                String markdown = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                fragmentos.addAll(partirEnSecciones(markdown, archivo.getFilename()));
            }
        }
        return fragmentos;
    }

    /** Público y estático para poder probarlo sin levantar Spring. */
    public static List<Document> partirEnSecciones(String markdown, String nombreArchivo) {
        String tituloDocumento = nombreArchivo;
        List<Document> resultado = new ArrayList<>();

        String seccionActual = null;
        StringBuilder cuerpo = new StringBuilder();

        for (String linea : markdown.split("\\R")) {
            if (linea.startsWith("# ")) {
                tituloDocumento = linea.substring(2).trim();
            } else if (linea.startsWith("## ")) {
                agregar(resultado, tituloDocumento, seccionActual, cuerpo, nombreArchivo);
                seccionActual = linea.substring(3).trim();
                cuerpo.setLength(0);
            } else if (seccionActual != null) {
                cuerpo.append(linea).append('\n');
            }
        }
        agregar(resultado, tituloDocumento, seccionActual, cuerpo, nombreArchivo);
        return resultado;
    }

    private static void agregar(List<Document> destino, String tituloDocumento, String seccion,
                                StringBuilder cuerpo, String archivo) {
        if (seccion == null || cuerpo.toString().isBlank()) {
            return;
        }
        String titulo = tituloDocumento + " — " + seccion;
        // El título va DENTRO del texto: así el embedding también "sabe" de qué
        // trata la sección, no solo el cuerpo.
        String texto = titulo + "\n\n" + cuerpo.toString().trim();
        destino.add(new Document(texto, Map.of(
                "origen", ORIGEN,
                "titulo", titulo,
                "archivo", archivo)));
    }
}
