
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.AnnotationSourceGenerator;
import org.burningwave.core.classes.ClassFactory;
import org.burningwave.core.classes.ClassSourceGenerator;
import org.burningwave.core.classes.FunctionSourceGenerator;
import org.burningwave.core.classes.TypeDeclarationSourceGenerator;
import org.burningwave.core.classes.UnitSourceGenerator;
import org.burningwave.core.classes.VariableSourceGenerator;

import com.google.common.base.Joiner;

import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParserFactory;
import dev.langchain4j.data.document.splitter.recursive.RecursiveDocumentSplitterFactory;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;

import name.NicolasCoutureGrenier.CS.ConsoleFormatter;

public class MyLilRAG {
    interface Assistant {
	Result<String> chat(String userMessage);
    }

    private static File toIngest = new File("./toIngest");
    private static File ingested = new File("./ingested");

    private static String systemPrompt = "You are a highly intelligent and efficient AI agent, designed to assist users by retrieving relevant information from both your internal knowledge base (embedding store) and real-time web search via Tavily. Your primary goals are to provide accurate, relevant, and up-to-date answers while maintaining clarity and simplicity. When accessing the web, prioritize current data and trusted sources. Combine insights from both stored data and web search to deliver the most useful response. If certain queries involve opinion or speculation, present information impartially. Always remain concise, polite, and clear.";

    private static Object buildToolFromClass(Class<?> cl) {
	UnitSourceGenerator unitSG = UnitSourceGenerator.create("Tools");
	
	var gen = ClassSourceGenerator.create(
		TypeDeclarationSourceGenerator.create(cl.getSimpleName() + "Tool"))
		.addModifier(Modifier.PUBLIC);

	
	for (Method method : cl.getMethods()) {
	    if (!Modifier.isStatic(method.getModifiers()))
		continue;

	    var gen2 = FunctionSourceGenerator.create(method.getName())
		    .addAnnotation(AnnotationSourceGenerator.create(dev.langchain4j.agent.tool.Tool.class)).setReturnType(method.getReturnType());
	    gen2.addModifier(Modifier.PUBLIC);
	    String body = "return " + cl.getCanonicalName() + "." + method.getName() + "(";
	    List<String> params = new ArrayList<String>();
	    for (Parameter p : method.getParameters()) {
		try {
		    gen2.addParameter(
			    VariableSourceGenerator.create(
				    p.getType(), 
				    p.getName()
				    )
			    );
		} catch (IllegalArgumentException
			| SecurityException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
		
		params.add(p.getName());
	    }
	    body += Joiner.on(", ").join(params) + ");";
	    gen2.addBodyCode(body);
	    gen.addMethod(gen2);
	}

	ComponentSupplier componentSupplier = ComponentContainer.getInstance();

	ClassFactory classFactory = componentSupplier.getClassFactory();
	unitSG.addClass(gen);
	try {
	    var classRetriever = classFactory.loadOrBuildAndDefine(unitSG);
	    
	    Class<?> toolClass = classRetriever.get("Tools." + cl.getSimpleName() + "Tool");
	    return toolClass.getDeclaredConstructor().newInstance();

	} catch (InstantiationException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IllegalAccessException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IllegalArgumentException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (InvocationTargetException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (NoSuchMethodException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (SecurityException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return null;
    }

    private static boolean ingest(EmbeddingStoreIngestor ingestor, DocumentParser parser, DocumentSplitter splitter) {
	if (!toIngest.exists())
	    toIngest.mkdir();
	if (!ingested.exists())
	    ingested.mkdir();
	if (toIngest.listFiles().length > 0)
	    return ingest(toIngest, ingestor, parser, splitter);
	return false;
    }

    private static boolean ingest(File f, EmbeddingStoreIngestor ingestor, DocumentParser parser,
	    DocumentSplitter splitter) {
	if (f.isDirectory()) {
	    boolean o = false;
	    if (!f.equals(toIngest)) {
		System.out.println("Ingesting directory: " + f.getPath().substring(toIngest.getPath().length()));
	    } else {
		System.out.println("Ingesting new documents...");
	    }

	    if (!f.equals(toIngest)) {
		File newDir = new File(ingested.getPath() + f.getPath().substring(toIngest.getPath().length()));
		if (!newDir.exists())
		    newDir.mkdir();
	    }
	    for (File s : f.listFiles()) {
		o = true;
		ingest(s, ingestor, parser, splitter);
	    }
	    String path = f.getPath();
	    if (!f.equals(toIngest))
		f.delete();
	    if (!path.equals(toIngest.getPath())) {
		System.out.println("Ingested and archived directory: " + path.substring(toIngest.getPath().length()));
	    } else {
		System.out.println("Done ingesting new documents.");
	    }
	    return o;
	} else {
	    System.out.println("Ingesting file: " + f.getPath().substring(toIngest.getPath().length()));

	    Document doc = FileSystemDocumentLoader.loadDocument(f.getPath(), parser);
	    List<TextSegment> segments = splitter.split(doc);
	    for (TextSegment s : segments) {
		ingestor.ingest(Document.from(s.text(), s.metadata()));
	    }

	    String p = ingested.getPath() + f.getPath().substring(toIngest.getPath().length());
	    f.renameTo(new File(p));
	    System.out.println("Ingested and archived: " + f.getPath().substring(toIngest.getPath().length()));
	    return true;
	}
    }

    private static ConsoleFormatter formatter = new ConsoleFormatter();

    public static void main(String[] args) throws IOException {

	OpenAiChatModelBuilder b = new OpenAiChatModelBuilder();
	// OpenAiChatModel model =
	// b.baseUrl("http://localhost:1234/v1").modelName("duyntnet/Orca-2-13b-imatrix-GGUF").timeout(Duration.ZERO).apiKey("DUMMY").build();
	// OpenAiChatModel model =
	// b.modelName("gpt-4o-mini").timeout(Duration.ZERO).apiKey(System.getenv("OPENAI_API_KEY")).build();
	OpenAiChatModel model = b.baseUrl("https://api.groq.com/openai/v1").modelName("llama-3.1-70b-versatile")
		.apiKey(System.getenv("GROQ_API_KEY")).timeout(Duration.ZERO).build();
	// System.out.println(model.generate("Hello!"));

	OpenAiEmbeddingModelBuilder b2 = new OpenAiEmbeddingModelBuilder();
	OpenAiEmbeddingModel emodel = b2.baseUrl("http://localhost:1234/v1").modelName("nomic-embed-text")
		.apiKey("DUMMY").timeout(Duration.ZERO).build();

	// OpenAiEmbeddingModel emodel =
	// b2.modelName("text-embedding-3-small").apiKey(System.getenv("OPENAI_API_KEY")).timeout(Duration.ZERO).build();
	Neo4jEmbeddingStore store = Neo4jEmbeddingStore.builder().withBasicAuth("bolt://0.0.0.0:7687", "neo4j", "")
		.dimension(emodel.dimension()).indexName("ncg777.store.nomic").build();

	DocumentSplitter splitter = (new RecursiveDocumentSplitterFactory()).create();

	EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder().embeddingModel(emodel).embeddingStore(store)
		.build();

	DocumentParser parser = (new ApacheTikaDocumentParserFactory()).create();

	ingest(ingestor, parser, splitter);

	EmbeddingStoreContentRetriever embeddingStoreContentRetriever = EmbeddingStoreContentRetriever.builder()
		.embeddingModel(emodel).embeddingStore(store).build();

	// Let's create our web search content retriever.
	WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder().apiKey(System.getenv("TAVILY_API_KEY"))
		.build();

	ContentRetriever webSearchContentRetriever = WebSearchContentRetriever.builder()
		.webSearchEngine(webSearchEngine).maxResults(5).build();

	QueryRouter queryRouter = new DefaultQueryRouter(embeddingStoreContentRetriever, webSearchContentRetriever);

	RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder().queryRouter(queryRouter).build();

	//List<Object> tools = new ArrayList<Object>();
	//tools.add(buildToolFromClass(name.NicolasCoutureGrenier.Maths.Numbers.class));
	//tools.add(buildToolFromClass(name.NicolasCoutureGrenier.Maths.Objects.Combination.class));
	Assistant assistant = AiServices.builder(Assistant.class).retrievalAugmentor(retrievalAugmentor) //.tools(tools)
		.chatLanguageModel(model).chatMemory(MessageWindowChatMemory.withMaxMessages(100))
		.systemMessageProvider((o) -> systemPrompt).build();
	
	do {
	    System.out.println("\n=== USER MESSAGE ===");
	    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	    
	    String input = "";
	    System.out.print("> ");
	    int emptyCount = 0;
	    while(emptyCount < 2) {
		
		var line = reader.readLine();
		
		input += line + "\n";
		if(line.isEmpty()) emptyCount++;
		else {emptyCount = 0;}
	    }
	    if(input.replaceAll("\n","").trim().isEmpty()) break;
	    Result<String> answer = assistant.chat(input);

	    System.out.println("\n=== AI ANSWER ===");
	    System.out.println(formatter.format(answer.content()));
	} while (true);
    }
}
