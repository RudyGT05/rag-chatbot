package gt.edu.umg.desarrolloweb.chatbot.conocimiento;

import java.util.List;
import org.springframework.ai.document.Document;

/**
 * Fuente 2 del RAG: datos PÚBLICOS leídos de la BD del monolito (solo lectura)
 * y convertidos a oraciones en español.
 *
 * Cada implementación lee una tabla (o una consulta) y devuelve fragmentos con
 * metadato origen = "monolito".
 *
 * Regla de privacidad: solo datos que cualquier persona podría ver en un
 * cartel de la oficina (tarifas, equivalencias). NUNCA clientes, lecturas,
 * pagos ni usuarios: el chatbot es público y sin login.
 */
public interface FuenteMonolito {

    String ORIGEN = "monolito";

    List<Document> leer();
}
