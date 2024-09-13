import java.awt.EventQueue;

import javax.swing.JFrame;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

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

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParserFactory;
import dev.langchain4j.data.document.splitter.recursive.RecursiveDocumentSplitterFactory;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
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
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.neo4j.Neo4jEmbeddingStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;

import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

public class MyLilRAG {

    private JFrame frmMylilrag;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
	EventQueue.invokeLater(new Runnable() {
	    public void run() {
		try {
		    MyLilRAG window = new MyLilRAG();
		    window.frmMylilrag.setVisible(true);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	});
    }

    /**
     * Create the application.
     */
    public MyLilRAG() {
	initialize();
    }

    interface Assistant {
	@SystemMessage("You are a highly intelligent and efficient AI agent, designed to assist users by retrieving relevant information from both your internal knowledge base (embedding store) and real-time web search via Tavily. Your primary goals are to provide accurate, relevant, and up-to-date answers while maintaining clarity and simplicity. When accessing the web, prioritize current data and trusted sources. Combine insights from both stored data and web search to deliver the most useful response. If certain queries involve opinion or speculation, present information impartially. Always remain concise, polite, and clear.")
	Result<String> chat(String userMessage);
    }

    private JTextArea textAreaInput;
    private JTextArea textAreaOutput;

    private Assistant assistant;

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {

	OpenAiChatModelBuilder b = new OpenAiChatModelBuilder();
	// OpenAiChatModel model =
	// b.baseUrl("http://localhost:1234/v1").modelName("duyntnet/Orca-2-13b-imatrix-GGUF").timeout(Duration.ZERO).apiKey("DUMMY").build();
	// OpenAiChatModel model =
	// b.modelName("gpt-4o-mini").timeout(Duration.ZERO).apiKey(System.getenv("OPENAI_API_KEY")).build();
	OpenAiChatModel model = b.baseUrl("https://api.groq.com/openai/v1").modelName("llama-3.1-70b-versatile")
		.apiKey(System.getenv("GROQ_API_KEY")).timeout(Duration.ZERO).responseFormat("json_schema")
		.strictJsonSchema(true).build();
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

	EmbeddingStoreContentRetriever embeddingStoreContentRetriever = EmbeddingStoreContentRetriever.builder()
		.embeddingModel(emodel).embeddingStore(store).build();

	// Let's create our web search content retriever.
	WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder().apiKey(System.getenv("TAVILY_API_KEY"))
		.build();

	ContentRetriever webSearchContentRetriever = WebSearchContentRetriever.builder()
		.webSearchEngine(webSearchEngine).maxResults(5).build();

	QueryRouter queryRouter = new DefaultQueryRouter(embeddingStoreContentRetriever, webSearchContentRetriever);

	RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder().queryRouter(queryRouter).build();

	// List<Object> tools = new ArrayList<Object>();
	// tools.add(buildToolFromClass(name.NicolasCoutureGrenier.Maths.Numbers.class));
	// tools.add(buildToolFromClass(name.NicolasCoutureGrenier.Maths.Objects.Combination.class));
	assistant = AiServices.builder(Assistant.class).retrievalAugmentor(retrievalAugmentor) // .tools(tools)
		.chatLanguageModel(model).chatMemory(MessageWindowChatMemory.withMaxMessages(100)).build();

	frmMylilrag = new JFrame();
	frmMylilrag.setTitle("MyLilRAG");
	frmMylilrag.setBounds(100, 100, 533, 438);
	frmMylilrag.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	JScrollPane scrollPane = new JScrollPane();

	textAreaOutput = new JTextArea();
	textAreaOutput.setEditable(false);
	textAreaOutput.setWrapStyleWord(true);
	textAreaOutput.setTabSize(2);
	textAreaOutput.setLineWrap(true);
	scrollPane.setViewportView(textAreaOutput);

	JScrollPane scrollPane_1 = new JScrollPane();

	textAreaInput = new JTextArea();
	textAreaInput.setWrapStyleWord(true);
	textAreaInput.setTabSize(2);
	textAreaInput.setLineWrap(true);
	scrollPane_1.setViewportView(textAreaInput);

	JButton btnNewButton = new JButton("Send message");
	btnNewButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		new Thread(() -> {
		    btnNewButton.setEnabled(false);
		    textAreaInput.setEnabled(false);
		    String input = textAreaInput.getText();
		    textAreaInput.setText("");
		    Result<String> answer = assistant.chat(input);

		    textAreaOutput.setText(textAreaOutput.getText() + "===USER MESSAGE ===\n" + input + "\n\n"
			    + "=== AI ANSWER ===\n" + answer.content() + "\n\n");
		    textAreaOutput.setCaretPosition(textAreaOutput.getText().length());
		    textAreaInput.setEnabled(true);
		    btnNewButton.setEnabled(true);
		}).start();
	    }
	});

	JLabel lblNewLabel = new JLabel("User message:");
	GroupLayout groupLayout = new GroupLayout(frmMylilrag.getContentPane());
	groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout
		.createSequentialGroup().addGap(10)
		.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
			.addComponent(btnNewButton, GroupLayout.DEFAULT_SIZE, 497, Short.MAX_VALUE)
			.addComponent(scrollPane)
			.addGroup(groupLayout.createSequentialGroup()
				.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, 497, Short.MAX_VALUE).addGap(1))
			.addGroup(groupLayout.createSequentialGroup()
				.addComponent(scrollPane_1, GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE).addGap(4)))
		.addContainerGap()));
	groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
		.addGroup(groupLayout.createSequentialGroup().addContainerGap()
			.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE).addGap(11)
			.addComponent(lblNewLabel).addGap(3)
			.addComponent(scrollPane_1, GroupLayout.PREFERRED_SIZE, 107, GroupLayout.PREFERRED_SIZE)
			.addGap(11).addComponent(btnNewButton).addGap(11)));
	frmMylilrag.getContentPane().setLayout(groupLayout);
	new Thread(() -> {
	    btnNewButton.setEnabled(false);
	    textAreaInput.setEnabled(false);
	    ingest(ingestor, parser, splitter);
	    btnNewButton.setEnabled(true);
	    textAreaInput.setEnabled(true);

	}).start();

    }

    private static File toIngest = new File("./toIngest");
    private static File ingested = new File("./ingested");

    @SuppressWarnings("unused")
    private static Object buildToolFromClass(Class<?> cl) {
	UnitSourceGenerator unitSG = UnitSourceGenerator.create("Tools");

	var gen = ClassSourceGenerator.create(TypeDeclarationSourceGenerator.create(cl.getSimpleName() + "Tool"))
		.addModifier(Modifier.PUBLIC);

	for (Method method : cl.getMethods()) {
	    if (!Modifier.isStatic(method.getModifiers()))
		continue;

	    var gen2 = FunctionSourceGenerator.create(method.getName())
		    .addAnnotation(AnnotationSourceGenerator.create(dev.langchain4j.agent.tool.Tool.class))
		    .setReturnType(method.getReturnType());
	    gen2.addModifier(Modifier.PUBLIC);
	    String body = "return " + cl.getCanonicalName() + "." + method.getName() + "(";
	    List<String> params = new ArrayList<String>();
	    for (Parameter p : method.getParameters()) {
		try {
		    gen2.addParameter(VariableSourceGenerator.create(p.getType(), p.getName()));
		} catch (IllegalArgumentException | SecurityException e) {
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

    private boolean ingest(EmbeddingStoreIngestor ingestor, DocumentParser parser, DocumentSplitter splitter) {
	if (!toIngest.exists())
	    toIngest.mkdir();
	if (!ingested.exists())
	    ingested.mkdir();
	if (toIngest.listFiles().length > 0)
	    return ingest(toIngest, ingestor, parser, splitter);
	return false;
    }

    private void printToOutput(String s) {
	textAreaOutput.setText(textAreaOutput.getText() + "\n" + s);
    }

    private boolean ingest(File f, EmbeddingStoreIngestor ingestor, DocumentParser parser, DocumentSplitter splitter) {
	if (f.isDirectory()) {
	    boolean o = false;
	    if (!f.equals(toIngest)) {
		printToOutput("Ingesting directory: " + f.getPath().substring(toIngest.getPath().length()));
	    } else {
		printToOutput("Ingesting new documents...");
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
		printToOutput("Ingested and archived directory: " + path.substring(toIngest.getPath().length()));
	    } else {
		printToOutput("Done ingesting new documents.");
	    }
	    return o;
	} else {
	    printToOutput("Ingesting file: " + f.getPath().substring(toIngest.getPath().length()));

	    Document doc = FileSystemDocumentLoader.loadDocument(f.getPath(), parser);
	    List<TextSegment> segments = splitter.split(doc);
	    for (TextSegment s : segments) {
		ingestor.ingest(Document.from(s.text(), s.metadata()));
	    }

	    String p = ingested.getPath() + f.getPath().substring(toIngest.getPath().length());
	    f.renameTo(new File(p));
	    printToOutput("Ingested and archived: " + f.getPath().substring(toIngest.getPath().length()));
	    return true;
	}
    }
}
