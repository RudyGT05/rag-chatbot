package gt.edu.umg.desarrolloweb.chatbot.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import gt.edu.umg.desarrolloweb.chatbot.api.dto.ErrorDto;
import gt.edu.umg.desarrolloweb.chatbot.config.ChatbotProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limit por IP para POST /api/v1/chat → 429 + header Retry-After.
 *
 * ¿Por qué? Cada pregunta cuesta: llama al modelo de embeddings y al LLM.
 * Sin límite, un script podría hacer miles de preguntas por minuto y agotar
 * la API (o el CPU si es Ollama local).
 *
 * Algoritmo "ventana fija": por cada IP se cuenta cuántas preguntas hizo en
 * el minuto actual. Al pasar al siguiente minuto el contador vuelve a 0.
 * Es simple y suficiente para un solo servidor (en memoria).
 *
 * IP real: las peticiones llegan desde el servidor de Next.js (BFF), no desde
 * el navegador. Por eso se usa el header X-Forwarded-For que agrega el BFF.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long VENTANA_MS = 60_000;
    private static final String RUTA_PROTEGIDA = "/api/v1/chat";

    private final int maximoPorVentana;
    private final ObjectMapper objectMapper;
    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();

    public RateLimitFilter(ChatbotProperties props, ObjectMapper objectMapper) {
        this.maximoPorVentana = props.rateLimit().preguntasPorMinuto();
        this.objectMapper = objectMapper;
    }

    /** Contador de una IP dentro de un minuto concreto. */
    private record Ventana(long inicio, int conteo) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && RUTA_PROTEGIDA.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long ahora = System.currentTimeMillis();
        long inicioVentana = ahora - (ahora % VENTANA_MS);

        Ventana v = ventanas.compute(ipCliente(request), (ip, actual) ->
                actual == null || actual.inicio() != inicioVentana
                        ? new Ventana(inicioVentana, 1)
                        : new Ventana(inicioVentana, actual.conteo() + 1));

        if (v.conteo() > maximoPorVentana) {
            long segundosRestantes = Math.max(1, (inicioVentana + VENTANA_MS - ahora + 999) / 1000);
            responder429(response, segundosRestantes);
            return;
        }

        limpiarVentanasViejas(inicioVentana);
        chain.doFilter(request, response);
    }

    public static String ipCliente(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // "cliente, proxy1, proxy2" → el primero es el usuario real
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void responder429(HttpServletResponse response, long retryAfter) throws IOException {
        ErrorDto error = new ErrorDto()
                .code("RATE_LIMITED")
                .message("Demasiadas preguntas. Intenta de nuevo en unos segundos.")
                .details(List.of());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), error);
    }

    /** Evita que el mapa crezca sin fin con IPs de minutos pasados. */
    private void limpiarVentanasViejas(long inicioVentanaActual) {
        if (ventanas.size() > 10_000) {
            ventanas.values().removeIf(v -> v.inicio() < inicioVentanaActual);
        }
    }
}
