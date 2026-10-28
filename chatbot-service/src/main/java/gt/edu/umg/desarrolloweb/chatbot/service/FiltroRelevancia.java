package gt.edu.umg.desarrolloweb.chatbot.service;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Filtro RELATIVO de fragmentos.
 *
 * El umbral fijo (similarity-threshold) descarta lo irrelevante, pero deja
 * pasar fragmentos "parecidos" del mismo tema. Ejemplo real:
 *
 *   0.71  ¿Qué hago si hay una fuga dentro de mi casa?   ← la respuesta
 *   0.65  ¿Qué hago si veo una fuga en la calle?
 *   0.63  ¿Cómo detecto una fuga que no se ve?
 *
 * Con los tres en el prompt, un LLM pequeño MEZCLA procedimientos ("anota la
 * lectura del contador" no es lo que se hace ante una fuga). Este filtro solo
 * deja los fragmentos cuya similitud está a menos de "margen" del mejor.
 *
 * Es genérico (no depende de Spring AI) para poder probarlo sin dependencias.
 */
public final class FiltroRelevancia {

    private FiltroRelevancia() {
    }

    public static <T> List<T> dentroDelMargen(List<T> items, Function<T, Double> score, double margen) {
        double mejor = items.stream()
                .map(score)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(Double.NaN);
        if (Double.isNaN(mejor)) {
            return items;   // sin scores: no se puede filtrar
        }
        return items.stream()
                .filter(i -> {
                    Double s = score.apply(i);
                    return s == null || s >= mejor - margen;
                })
                .toList();
    }
}
