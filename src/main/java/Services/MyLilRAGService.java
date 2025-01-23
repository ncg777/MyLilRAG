package Services;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ComparisonChain;

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
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingStore;

public class MyLilRAGService {

    
    public static interface MyLilRAGAssistant {
	@UserMessage("{{message}}")
	Result<String> chat(@V("message") String message);
    }

    final private static DocumentParser parser = (new ApacheTikaDocumentParserFactory()).create();
    final private static DocumentSplitter splitter = (new RecursiveDocumentSplitterFactory()).create();

    public static EmbeddingModel getEmbeddingModel() {
	OpenAiEmbeddingModelBuilder b2 = new OpenAiEmbeddingModelBuilder();
	var embeddingModel = b2.baseUrl("http://localhost:11434/v1").modelName("nomic-embed-text").apiKey("ollama")
		.timeout(Duration.ZERO).build();
	return embeddingModel;

    }

    public static EmbeddingStore<TextSegment> getEmbeddingStore() {
	EmbeddingModel emodel = getEmbeddingModel();
	var embeddingStore = Neo4jEmbeddingStore.builder().withBasicAuth("bolt://0.0.0.0:7687", "neo4j", "")
		.dimension(emodel.dimension()).indexName("ncg777.store.nomic").build();

	return embeddingStore;
    }

    public static EmbeddingStoreContentRetriever getContentRetriever() {
	return EmbeddingStoreContentRetriever.builder().embeddingModel(getEmbeddingModel())
		.embeddingStore(getEmbeddingStore()).build();
    }

    public static EmbeddingStoreIngestor getIngestor() {
	var ingestor = EmbeddingStoreIngestor.builder().embeddingModel(getEmbeddingModel())
		.embeddingStore(getEmbeddingStore()).build();
	return ingestor;
    }
    public static void setBaseUrl(String str) {
	baseUrl=str;
    }
    private static String getAPIKey() {
	if(baseUrl.contains("groq")) return System.getenv("GROQ_API_KEY");
	else if(baseUrl.contains("openai")) return System.getenv("OPENAI_API_KEY");
	else return "DUMMY";
    }
    public static String[] getEndPoints() {
	String[] o = {"http://localhost:11434/v1","https://api.openai.com/v1", "https://api.groq.com/openai/v1"};
	return o;
    }
    private static String baseUrl = "http://localhost:11434/v1";
    private static OpenAiChatModelBuilder getModelBuilder(String modelName) {
	return (new OpenAiChatModelBuilder())
		.baseUrl(baseUrl)
		.modelName(modelName)
		.apiKey(getAPIKey());
    }
    
    public static String[] getModels() throws JsonParseException, IOException, InterruptedException, URISyntaxException {
	HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(baseUrl+"/models"))
                .header("Authorization", "Bearer "+getAPIKey())
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if(response.statusCode() == 200) {
            var b = new JsonFactoryBuilder().build();
            var str = response.body();
            var p = b.setCodec(new ObjectMapper()).createParser(str).readValueAsTree();
            p=p.get("data");
            var l = new ArrayList<String>();
            for(int i=0;i<p.size();i++) l.add(p.get(i).get("id").toString().replaceAll("\"",""));
            l.sort((x,y) -> x.compareTo(y));
            return l.toArray(new String[0]);
        }
        else {return new String[0];}
    }
    public static OpenAiChatModel getOpenAiChatModel(String modelName) {
	return getModelBuilder(modelName).build();
    }

    public static String oneShotChat(String model, String str) {
	return getModelBuilder(model).build().generate(str); 
    }
    private record AssistantId(String sender, String senderEmail, String other, String otherEmail) implements Comparable<AssistantId> {

	@Override
	public int compareTo(AssistantId o) {
	    return ComparisonChain.start()
		    .compare(sender, o.sender).compare(senderEmail, o.senderEmail)
		    .compare(other, o.other).compare(otherEmail, o.otherEmail).result();
	}
	
    };
    //private static TreeMap<AssistantId, MyLilRAGAssistant> assistants = new TreeMap<AssistantId, MyLilRAGAssistant>();
    public static MyLilRAGAssistant getAssistant(String model, String sender, String senderEmail, String other, String otherEmail) {
	//var r =  new AssistantId(sender, senderEmail, other, otherEmail);
	//if (assistants.get(r) != null)
	//    return assistants.get(r);
	
	//assistants.put(r, 
	return AiServices.builder(MyLilRAGAssistant.class).systemMessageProvider(
		(o) -> "" +
"""
You are an AI agent designed to assist in solving problems collaboratively with 
other agents through email exchange and by retrieving relevant information from 
your memory, your internal knowledge base and your intelligence. You shall 
answer to the agent with which you are in communication with a multipart email 
message in MIME format. Your mail shall have 
Content-Type 'multipart/mixed' with a sound boundary argument. A sound boundary argument
could be for example 'FROM<FROM>TO<TO>DATE<DATE>', where <FROM>, 
<TO> and <DATE> are replaced by the from and to email addresses (with the '@' replaced by 'AT') and <DATE> is 
the date of the reply, all with spaces, colons, commas and punctuation removed. 
Your reply shall include the complete verbatim email that is currently being replied to as 
the last part of the multipart message and it shall have content-type 'message/rfc822';
its content shall be exactly the input received verbatim with the line "MIME-Version: 1.0" removed. 
You shall not replace any part of the original email with ellipses (...) of any kind. 
The final MIME part boundary of your reply which should be suffixed with 2 hyphens (--) is always what 
should be found on the last line of your answer and should match the boundary you have 
used to delimitate the parts of your reply. Your 
answer may also contain other parts in the form of file attachments and you 
shall provide for each attached file the Content-Type line, the 
file's name on the MIME Content-Disposition line as it should be done, and 
finally the content of the file in plain text of course. Don't try to encode 
your files as base64; attach them in plain text. Your answer shall be in valid 
and well formed MIME format which and respect a certain order, which means to alway begin your answer with the line 
'MIME-Version: 1.0', followed by the Date line, which should be precisely and 
just only 1 second after the datetime of the email you are replying to, then should follow the
'from' and 'to' lines, with email addresses and names, then should follow a relevant Subject 
line, followed by the Content-Type line, then the Content-Transfer-Encoding line (only if the content is not multipart)
and then the content. You must not insert useless extra empty lines; there should be a single empty line between the headers and the content. 
Your answer will be saved verbatim to file in the knowledge base as an eml file so the format of your answer must follow the MIME
email format strictly because the eml file needs to be readable by any mail 
program such as Mozilla Thunderbird or Microsoft Outlook.
""" + 
String.format(
"""
Your are impersonating %s. Your email address is %s. 
Your are communicating with %s, whose email address is %s. 
Gather some context before you reply; try to remember as much as you can of who
you are and your recents communications with your interlocutory in order to 
provide context if necessary. 
""", other, otherEmail, sender, senderEmail)
		)
		.contentRetriever(getContentRetriever())
		.chatMemory(MessageWindowChatMemory.withMaxMessages(5))
		.chatLanguageModel(getOpenAiChatModel(model))
		.build();
		//);
	//return assistants.get(r);
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

    public static boolean ingestString(String str) {
	getIngestor().ingest(Document.from(str));
	return true;
    }
    public static void ingestSingleFile(String path) {
	Document doc = FileSystemDocumentLoader.loadDocument(path, parser);
	List<TextSegment> segments = splitter.split(doc);
	for (TextSegment s : segments) {
	    getIngestor().ingest(Document.from(s.text(), s.metadata()));
	}
    }
    public static boolean ingest(File f) {
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
	    printToOutput.accept("\n\n");
	    return o;
	} else {
	    printToOutput.accept("Ingesting file: " + f.getPath().substring(toIngest.getPath().length()));

	    ingestSingleFile(f.getAbsolutePath());

	    String p = ingested.getPath() + f.getPath().substring(toIngest.getPath().length());
	    f.renameTo(new File(p));
	    printToOutput.accept("Ingested and archived: " + f.getPath().substring(toIngest.getPath().length()));
	    return true;
	}
    }

    private static Consumer<String> printToOutput;

    public static void setPrintToOutput(Consumer<String> c) {
	printToOutput = c;
    }
}
