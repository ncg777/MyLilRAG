
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.agent.tool.Tool;
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

/**
 * Hello world!
 *
 */
public class MyLilRAG 
{
    interface Assistant {
	Result<String> chat(String userMessage);
    }
    
    public static void main( String[] args ) throws IOException
    {
	OpenAiChatModelBuilder b = new OpenAiChatModelBuilder();
	//OpenAiChatModel model = b.baseUrl("http://localhost:1234/v1").modelName("duyntnet/Orca-2-13b-imatrix-GGUF").timeout(Duration.ZERO).apiKey("DUMMY").build();
	OpenAiChatModel model = b.modelName("gpt-4o-mini").timeout(Duration.ZERO).apiKey(System.getenv("OPENAI_API_KEY")).build();
	//OpenAiChatModel model = b.baseUrl("https://api.groq.com/openai/v1").modelName("llama-3.1-70b-versatile").apiKey(System.getenv("GROQ_API_KEY")).timeout(Duration.ZERO).build();
	//System.out.println(model.generate("Hello!"));
        
	OpenAiEmbeddingModelBuilder b2 = new OpenAiEmbeddingModelBuilder();
        OpenAiEmbeddingModel emodel = b2.baseUrl("http://localhost:1234/v1").modelName("nomic-embed-text").apiKey("DUMMY").timeout(Duration.ZERO).build();
        
        //OpenAiEmbeddingModel emodel = b2.modelName("text-embedding-3-small").apiKey(System.getenv("OPENAI_API_KEY")).timeout(Duration.ZERO).build();
        Neo4jEmbeddingStore store = Neo4jEmbeddingStore.builder()
        	.withBasicAuth("bolt://0.0.0.0:7687", "neo4j", "")
        	.dimension(emodel.dimension())
        	.indexName("ncg777.store.nomic")
        	.build();
        
        String toIngest = "./toIngest";
        String ingested = "./ingested";
        
        {
            File f = new File(toIngest);
            if(!f.exists()) {
        	f.mkdir();
            }
            f = new File(ingested);
            if(!f.exists()) {
        	f.mkdir();
            }
        }
        
        DocumentSplitter splitter = (new RecursiveDocumentSplitterFactory()).create();
        
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
        	.embeddingModel(emodel)
        	.embeddingStore(store)
        	.build();
        
        DocumentParser parser = (new ApacheTikaDocumentParserFactory()).create(); 
    
        List<Document> docs = FileSystemDocumentLoader.loadDocumentsRecursively(toIngest, parser);
        List<TextSegment> segments = splitter.splitAll(docs);
        for(TextSegment s : segments) {
            ingestor.ingest(Document.from(s.text(), s.metadata()));
        }
        
        {
            File d = new File(toIngest);
            for(File s : d.listFiles()) {
                String p = Paths.get(ingested, s.getName()).toString();
                s.renameTo(new File(p));
            }
        }
    
        EmbeddingStoreContentRetriever embeddingStoreContentRetriever = 
        	EmbeddingStoreContentRetriever.builder().embeddingModel(emodel).embeddingStore(store).build();
        
        // Let's create our web search content retriever.
        WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder()
                .apiKey(System.getenv("TAVILY_API_KEY"))
                .build();

        ContentRetriever webSearchContentRetriever = WebSearchContentRetriever.builder()
                .webSearchEngine(webSearchEngine)
                .maxResults(5)
                .build();

        QueryRouter queryRouter = new DefaultQueryRouter(embeddingStoreContentRetriever, webSearchContentRetriever);

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();

        
        Assistant ass = AiServices.builder(Assistant.class)
        	.retrievalAugmentor(retrievalAugmentor)
        	.chatLanguageModel(model)
        	.chatMemory(MessageWindowChatMemory.withMaxMessages(25))
        	.build();
        do {
            System.out.println("\nUSER MESSAGE:");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            String input = reader.readLine();
            
            if(input.isEmpty()) break;
            Result<String> answer = ass.chat(input);
            
            Pattern p = Pattern.compile("\\b.{1," + (80-1) + "}\\b\\W?");
            Matcher m = p.matcher(answer.content());
            StringBuilder sbans = new StringBuilder();
            
            while(m.find()) {
            	sbans.append(m.group()+"\n");
            }
            
            System.out.println("\nAI ANSWER:");
            System.out.println(sbans.toString());
        } while(true);
    }
}
