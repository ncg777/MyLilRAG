package name.ncg777.aiMailExchangeSimulator.Services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.function.Consumer;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.Tool;
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
import name.ncg777.maths.Combination;
import name.ncg777.maths.music.pcs12.Pcs12;
import name.ncg777.maths.sequences.Sequence;

public class MainService {

    
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
	String[] o = {"https://api.openai.com/v1", "https://api.groq.com/openai/v1","http://localhost:11434/v1"};
	return o;
    }
    private static String baseUrl = "http://localhost:11434/v1";
    private static OpenAiChatModelBuilder getModelBuilder(String modelName) {
	return (new OpenAiChatModelBuilder())
		.baseUrl(baseUrl)
		.timeout(Duration.ZERO)
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
    public static String placeholder = "<4B57Y837YNC5Y857VT43TN>";
    public static MyLilRAGAssistant getAssistant(String model, String sender, String senderEmail, String senderBio, String other, String otherEmail, String otherBio) {

	return AiServices.builder(MyLilRAGAssistant.class).systemMessageProvider(
		(o) -> "" +
"""
You are an AI agent designed to analyze email exchanges in MIME format and provide 
a single continuation to it in the form of a MIME mail template that you will write
using relevant information from 
your memory, your internal knowledge base and your intelligence. 
This exchange may include past exchanges between you and other agents and so you
should take notice of the context and answer accordingly.
You shall provide a single email as an answer to to the email exchange.
The answer you will provide will be a partial email template that won't include the original email 
that is being replied to. This template will be turned into a valid MIME file after you answer using your answer.
I repeat that you should absolutely NOT include the mail you are replying to in your MIME mail template answer. 
Your MIME mail template answer shall have Content-Type 'multipart/mixed' with 
a sound boundary argument. A sound boundary argument
could be for example 'boundary_<DATE>', where <DATE> are replaced by  
date of the reply including seconds, all with spaces, colons, commas and punctuation removed. 
Your answer may also contain other parts in the form of file attachments and you 
shall provide, for each attached file, the Content-Type line, the 
file's name on the MIME Content-Disposition line as it should be done, and 
finally the content of the file in plain text of course. Don't try to encode 
your files as base64; attach them in plain text. Your answer shall 
respect MIME format and provide headers in a certain order. Always begin your 
answer with the line 'MIME-Version: 1.0', followed by the Date 
line, which should be precisely and just only 1 second after the datetime of the email you are replying to, then should follow the
'from' and 'to' lines, with email addresses and names, then should follow a relevant Subject 
line, followed by the Content-Type line, then the Content-Transfer-Encoding line (only if the content is not multipart)
and then the content. You must not insert useless extra empty lines; there should be a single empty line between the headers and the content. 
Your reply must also always include a verbatim placeholder <4B57Y837YNC5Y857VT43TN> (with the < and >) as the only content of the last part of 
your multipart message. This last part of the email shall have content-type 'message/rfc822' and Content-Disposition: attachment with no name.
It is crucial that the 2 following points be respected or your answer will be discarded by the system:
1. the <4B57Y837YNC5Y857VT43TN> placeholder be present as demanded
2. the very last 2 characters of your output will be the -- of the final MIME boundary,
which is boundary defined previously, suffixed with 2 hyphens (--), as is specified by the MIME format.
So to summarize, you answer should always follow the following template with the ... replaced with the appropriate strings,
the <CONTENT OF YOUR ANSWER> replaced with your answer and the <POTENTIAL ATTACHMENT i> 
replaced by the potential attachments if there are any.

===BEGIN ANSWER TEMPLATE===
MIME-Version: 1.0  
Date: ...
From: ... 
To: ...  
Subject: ...  
Content-Type: multipart/mixed; boundary="boundary_..."  

--boundary_... 
Content-Type: text/plain; charset=UTF-8  
Content-Transfer-Encoding: 7bit  

<CONTENT OF YOUR ANSWER>

--boundary_...  

Content-Type: ...
Content-Disposition: attachment; filename="..."

<POTENTIAL ATTACHMENT 1>

--boundary_...  

Content-Type: ...
Content-Disposition: attachment; filename="..."

<POTENTIAL ATTACHMENT 2>
--boundary_...  

Content-Type: ...
Content-Disposition: attachment; filename="..."

<POTENTIAL ATTACHMENT ...>

--boundary_...  
Content-Type: message/rfc822  
Content-Disposition: attachment  

<4B57Y837YNC5Y857VT43TN>
--boundary_...--
===END ANSWER TEMPLATE===

Needless to say, don't include the attachment parts if there are no attachment.

Also, do not answer in markdown format and so do not use the ``` notation unless you are providing a markdown file as attachment.

""" + 
String.format(
"""
So you are are impersonating %s. Your email address is %s. Your bio is "%s".
You are communicating with %s, whose email address is %s and whose bio is "%s". 
Gather some context before you reply; try to remember as much as you can of who
you are and your recents communications with your interlocutor in order to 
provide context if necessary. 
""", other, otherEmail, otherBio, sender, senderEmail, senderBio)
		)
		.contentRetriever(getContentRetriever())
		.chatMemory(MessageWindowChatMemory.withMaxMessages(5))
		.chatLanguageModel(getOpenAiChatModel(model))
		.tools(new Tools())
		.build();
    }
    public static String getTimeStamp(Date date) {
	 SimpleDateFormat dateFormatDate = new SimpleDateFormat("yyyyMMdd");
	 dateFormatDate.setTimeZone(TimeZone.getTimeZone("UTC"));
	 SimpleDateFormat dateFormatTime = new SimpleDateFormat("HHmmss");
	 dateFormatTime.setTimeZone(TimeZone.getTimeZone("UTC"));
	 var o = dateFormatDate.format(date) + "T" + dateFormatTime.format(date) + "Z";
	 return o;
	 
    }
    static class Tools {

        @Tool("Fetch an http or https url link.")
        String fetchUrl(String url) {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request;
	    try {
		request = HttpRequest.newBuilder()
		        .uri(new URI(url))
		        .GET()
		        .build();
	    } catch (URISyntaxException e) {
		return null;
	    }
            HttpResponse<String> response;
	    try {
		response = client.send(request, HttpResponse.BodyHandlers.ofString());
	    } catch (IOException|InterruptedException e) {
		return null;
	    }
            if(response.statusCode() == 200) {
        	var fn = "./archive/fetched_url_" + getTimeStamp(new Date()) + "_" + url.replaceAll(":","").replaceAll("/", "_");
        	PrintWriter pw;
		try {
		    pw = new PrintWriter(fn);
		    pw.print(response.body());
	            pw.flush();
	            pw.close();
	            ingestSingleFile(fn);
		} catch (FileNotFoundException e) {
		    ;
		}
        	
                return response.body();
            }
            else {return null;}
        }

        @Tool("Get pitches in pitch class set identified by Forte number.")
        String getForteNumberPitches(String forteNumber) {
            var f = Pcs12.parseForte(forteNumber);
            if(f==null)return null;
            return f.asSequence().toString();
        }
        @Tool("Get Forte number from pitches integer sequence s of k numbers s_i with k < 12 and 0 <= s_i <= 11.")
        String getForteNumberFromPitchSequence(String pitches) {
            var s = Sequence.parse(pitches);
            if(s==null)return null;
            Set<Integer> set = new TreeSet<Integer>();
            set.addAll(s);
            return Pcs12.identify(new Combination(12,set)).toForteNumberString();
        }
        @Tool("Get interval vector associated with Forte number.")
        String getForteNumberIntervalVector(String forteNumber) {
            var f = Pcs12.parseForte(forteNumber);
            if(f==null)return null;
            return f.getIntervalVector().toString();
        }
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
