package RestAPI;
import org.springframework.web.bind.annotation.*;

import AiService.AiService;

@RestController
@RequestMapping("/api")
public class MyLilRAGController {

    private final AiService.Assistant assistant;

    public MyLilRAGController() {
        this.assistant = AiService.getAssistant();
        AiService.setPrintToOutput((s) -> System.out.println(s));
    }

    @PostMapping("/ask")
    public String ask(@RequestBody String question) {
        String response = assistant.chat(question).content();
        return response;
    }
}