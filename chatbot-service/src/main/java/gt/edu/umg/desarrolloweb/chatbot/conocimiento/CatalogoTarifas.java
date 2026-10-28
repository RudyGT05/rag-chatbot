package gt.edu.umg.desarrolloweb.chatbot.conocimiento;

import gt.edu.umg.desarrolloweb.chatbot.conocimiento.RedaccionMonolito.TarifaFila;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Copia en memoria de las tarifas vigentes, para que la CalculadoraConsumo no
 * tenga que consultar MySQL en cada pregunta.
 *
 * La llena TarifasMonolitoReader en cada ingesta (al arrancar y con
 * POST /api/v1/ingest), así queda sincronizada con los fragmentos del RAG.
 * Si el monolito está desactivado queda vacía y la calculadora no se usa.
 */
@Component
public class CatalogoTarifas {

    private volatile List<TarifaFila> vigentes = List.of();

    public List<TarifaFila> vigentes() {
        return vigentes;
    }

    void actualizar(List<TarifaFila> nuevas) {
        this.vigentes = List.copyOf(nuevas);
    }
}
