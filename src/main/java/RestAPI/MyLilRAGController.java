package RestAPI;
import org.springframework.web.bind.annotation.*;

import AiService.MyLilRAGService;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;

@RestController
@RequestMapping("/chat")
public class MyLilRAGController {

    private final MyLilRAGService.MyLilRAGAssistant myLilRAGAssistant;

    public MyLilRAGController() {
        this.myLilRAGAssistant = MyLilRAGService.getAssistant();
        MyLilRAGService.setPrintToOutput((s) -> System.out.println(s));
    }

    @PostMapping("/completions")
    public Result<String> completions(@UserMessage String messages) {
        return myLilRAGAssistant.chat(messages);
    }
}