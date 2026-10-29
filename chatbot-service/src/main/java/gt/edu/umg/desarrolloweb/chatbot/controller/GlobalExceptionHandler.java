package gt.edu.umg.desarrolloweb.chatbot.controller;

import gt.edu.umg.desarrolloweb.chatbot.api.dto.ErrorDto;
import gt.edu.umg.desarrolloweb.chatbot.exception.LlmNoDisponibleException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones a las respuestas de error que promete el contrato,
 * siempre con el schema Error { code, message, details[] }.
 *
 *   @Valid falló (question vacía o > 500)  → 400 VALIDATION_ERROR
 *   JSON mal formado                        → 400 VALIDATION_ERROR
 *   LLM / embeddings caídos                 → 503 LLM_UNAVAILABLE
 *   Cualquier otra cosa                     → 500 INTERNAL_ERROR (detalle solo en el log)
 *
 * assignableTypes: SOLO atiende errores de los controllers del chatbot. Como
 * librería vive dentro del proyecto de otro equipo; sin esta restricción
 * atraparía también los errores de SUS controllers y les cambiaría el formato.
 */
@RestControllerAdvice(assignableTypes = {ChatController.class, IngestaController.class})
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> validacion(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud no es válida", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> jsonInvalido(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "El cuerpo de la solicitud no es un JSON válido", List.of());
    }

    @ExceptionHandler(LlmNoDisponibleException.class)
    public ResponseEntity<ErrorDto> llmCaido(LlmNoDisponibleException e) {
        log.warn("Proveedor de IA no disponible: {}", e.getMessage(), e);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "LLM_UNAVAILABLE",
                "El asistente no está disponible en este momento.", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> inesperado(Exception e) {
        // Errores HTTP "normales" de Spring (404 ruta inexistente, 405 método
        // equivocado...) conservan su código; no son errores del servidor.
        if (e instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            return error(status, status.name(), status.getReasonPhrase(), List.of());
        }
        log.error("Error inesperado", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error inesperado.", List.of());
    }

    static ResponseEntity<ErrorDto> error(HttpStatus status, String code, String message,
                                          List<String> details) {
        return ResponseEntity.status(status).body(new ErrorDto()
                .code(code)
                .message(message)
                .details(details));
    }
}
