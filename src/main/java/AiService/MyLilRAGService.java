package AiService;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import dev.ai4j.openai4j.chat.Content;
import dev.ai4j.openai4j.chat.Message;
import dev.ai4j.openai4j.chat.Role;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParserFactory;
import dev.langchain4j.data.document.splitter.recursive.RecursiveDocumentSplitterFactory;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;

@Service
public class MyLilRAGService {
    public static interface MyLilRAGAssistant {
	@SystemMessage("You are a highly intelligent and efficient AI agent, designed to assist users by retrieving relevant information from both your internal knowledge base (embedding store) and real-time web search via Tavily. Your primary goals are to provide accurate, relevant, and up-to-date answers while maintaining clarity and simplicity. When accessing the web, prioritize current data and trusted sources. Combine insights from both stored data and web search to deliver the most useful response. If certain queries involve opinion or speculation, present information impartially. Always remain concise, polite, and clear.")
	Result<String> chat(@UserMessage String userMessages);
    }
    
    final private static DocumentParser parser = (new ApacheTikaDocumentParserFactory()).create();
    final private static DocumentSplitter splitter = (new RecursiveDocumentSplitterFactory()).create();
    private static EmbeddingModel embeddingModel = getEmbeddingModel();
    public static EmbeddingModel getEmbeddingModel() {
	if(embeddingModel != null) return embeddingModel;
	OpenAiEmbeddingModelBuilder b2 = new OpenAiEmbeddingModelBuilder();
	embeddingModel =  b2.baseUrl("http://localhost:1234/v1").modelName("nomic-embed-text")
		.apiKey("DUMMY").timeout(Duration.ZERO).build();
	return embeddingModel;

    }
    private static EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore();
    public static EmbeddingStore<TextSegment> getEmbeddingStore() {
	if(embeddingStore != null) return embeddingStore;
	EmbeddingModel emodel = getEmbeddingModel();
	embeddingStore = Neo4jEmbeddingStore.builder().withBasicAuth("bolt://0.0.0.0:7687", "neo4j", "")
		.dimension(emodel.dimension()).indexName("ncg777.store.nomic").build();
	return embeddingStore;
    }
    public static EmbeddingStoreContentRetriever getContentRetriever() {
	return EmbeddingStoreContentRetriever.builder()
		.embeddingModel(getEmbeddingModel()).embeddingStore(getEmbeddingStore()).build();
    }
    private static EmbeddingStoreIngestor ingestor = getIngestor();
    public static EmbeddingStoreIngestor getIngestor() {
	if(ingestor != null) return ingestor;
	ingestor = EmbeddingStoreIngestor.builder().embeddingModel(getEmbeddingModel()).embeddingStore(getEmbeddingStore())
		.build();
	return ingestor;
    }
    private static MyLilRAGAssistant myLilRAGAssistant = getAssistant();
    public static OpenAiChatModel getOpenAiChatModel() {
	OpenAiChatModelBuilder b = new OpenAiChatModelBuilder();
	// return b.baseUrl("http://localhost:1234/v1").modelName("duyntnet/Orca-2-13b-imatrix-GGUF").timeout(Duration.ZERO).apiKey("DUMMY").responseFormat("json_schema").strictJsonSchema(true).build();
	//return b.modelName("gpt-4o-mini").timeout(Duration.ZERO).apiKey(System.getenv("OPENAI_API_KEY")).responseFormat("json_schema").strictJsonSchema(true).build();
	return b.baseUrl("https://api.groq.com/openai/v1").modelName("llama-3.1-70b-versatile").apiKey(System.getenv("GROQ_API_KEY")).timeout(Duration.ZERO).responseFormat("json_schema").strictJsonSchema(true).build();

    }
    public static MyLilRAGAssistant getAssistant() {
	if(myLilRAGAssistant != null) return myLilRAGAssistant;
	WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder().apiKey(System.getenv("TAVILY_API_KEY"))
		.build();

	ContentRetriever webSearchContentRetriever = WebSearchContentRetriever.builder()
		.webSearchEngine(webSearchEngine).maxResults(5).build();
	EmbeddingStoreContentRetriever embeddingStoreContentRetriever = getContentRetriever();
	QueryRouter queryRouter = new DefaultQueryRouter(embeddingStoreContentRetriever, webSearchContentRetriever);

	RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder().queryRouter(queryRouter).build();
	OpenAiChatModel model = getOpenAiChatModel();
	
	myLilRAGAssistant = AiServices.builder(MyLilRAGAssistant.class).retrievalAugmentor(retrievalAugmentor)//.tools(tools)
		.chatLanguageModel(model).chatMemory(MessageWindowChatMemory.withMaxMessages(100)).build();
	return myLilRAGAssistant;
    }

    private static File toIngest = new File("./toIngest");
    private static File ingested = new File("./ingested");
    public static boolean ingest() {
	if (!toIngest.exists())
	    toIngest.mkdir();
	if (!ingested.exists())
	    ingested.mkdir();
	if (toIngest.listFiles().length > 0)
	    return ingest(toIngest);
	return false;
    }

    private static boolean ingest(File f) {
	if (f.isDirectory()) {
	    boolean o = false;
	    if (!f.equals(toIngest)) {
		printToOutput.accept("Ingesting directory: " + f.getPath().substring(toIngest.getPath().length()));
	    } else {
		printToOutput.accept("Ingesting new documents...");
	    }

	    if (!f.equals(toIngest)) {
		File newDir = new File(ingested.getPath() + f.getPath().substring(toIngest.getPath().length()));
		if (!newDir.exists())
		    newDir.mkdir();
	    }
	    for (File s : f.listFiles()) {
		o = true;
		ingest(s);
	    }
	    String path = f.getPath();
	    if (!f.equals(toIngest))
		f.delete();
	    if (!path.equals(toIngest.getPath())) {
		printToOutput.accept("Ingested and archived directory: " + path.substring(toIngest.getPath().length()));
	    } else {
		printToOutput.accept("Done ingesting new documents.");
	    }
	    return o;
	} else {
	    printToOutput.accept("Ingesting file: " + f.getPath().substring(toIngest.getPath().length()));

	    Document doc = FileSystemDocumentLoader.loadDocument(f.getPath(), parser);
	    List<TextSegment> segments = splitter.split(doc);
	    for (TextSegment s : segments) {
		ingestor.ingest(Document.from(s.text(), s.metadata()));
	    }

	    String p = ingested.getPath() + f.getPath().substring(toIngest.getPath().length());
	    f.renameTo(new File(p));
	    printToOutput.accept("Ingested and archived: " + f.getPath().substring(toIngest.getPath().length()));
	    return true;
	}
    }
    private static Consumer<String> printToOutput;
    public static void setPrintToOutput(Consumer<String> c) {printToOutput = c;}
}
