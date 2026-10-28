package gt.edu.umg.desarrolloweb.chatbot.controller;

import gt.edu.umg.desarrolloweb.chatbot.api.dto.IngestResultDto;
import gt.edu.umg.desarrolloweb.chatbot.api.spec.IngestaApi;
import gt.edu.umg.desarrolloweb.chatbot.service.IngestaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IngestaController implements IngestaApi {

    private final IngestaService ingestaService;

    public IngestaController(IngestaService ingestaService) {
        this.ingestaService = ingestaService;
    }

    @Override
    public ResponseEntity<IngestResultDto> reingestarConocimiento() {
        IngestaService.Resultado r = ingestaService.reingestar();
        return ResponseEntity.ok(new IngestResultDto()
                .faqChunks(r.faqChunks())
                .monolitoChunks(r.monolitoChunks())
                .durationMs(r.durationMs()));
    }
}
