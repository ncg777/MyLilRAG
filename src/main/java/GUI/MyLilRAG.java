package GUI;
import java.awt.EventQueue;

import javax.swing.JFrame;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import dev.langchain4j.service.Result;

import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

import Services.MyLilRAGService;
import javax.swing.LayoutStyle.ComponentPlacement;

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

    

    private JTextArea textAreaInput;
    private JTextArea textAreaOutput;
    private void interact(String str) {
	new Thread(() -> {
	    btnAIFollowUp.setEnabled(false);
	    btnGen.setEnabled(false);
	    textAreaInput.setEnabled(false);
	    textAreaInput.setText("");

	    textAreaOutput.setText(textAreaOutput.getText() + "===USER MESSAGE ===\n" + str + "\n\n"
		    + "=== AI ANSWER ===");
	    Result<String> answer = MyLilRAGService.getAssistant().chat(str);

	    printToOutput(answer.content() + "\n\n");
	    textAreaInput.setEnabled(true);
	    btnGen.setEnabled(true);
	    btnAIFollowUp.setEnabled(true);
	}).start();
    }
    private JButton btnGen;
    JButton btnAIFollowUp;
    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
	frmMylilrag = new JFrame();
	frmMylilrag.setTitle("MyLilRAG");
	frmMylilrag.setBounds(100, 100, 928, 533);
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

	btnGen = new JButton("Send message");
	btnGen.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		if(!textAreaInput.getText().trim().isEmpty()) interact(textAreaInput.getText());
	    }
	});

	JLabel lblNewLabel = new JLabel("User message:");
	
	btnAIFollowUp = new JButton("Generate AI Follow-up");
	btnAIFollowUp.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    var prompt = "Generate a follow-up to this conversation between a USER and an AI. Come up with some next interaction from the user and just answer with whatever you come up with without adding the ===USER MESSAGE=== part. Here is the conversation: \n\n" +
			    textAreaOutput.getText();
		    
		    interact(MyLilRAGService.oneShotChat(prompt));
		}
	});
	GroupLayout groupLayout = new GroupLayout(frmMylilrag.getContentPane());
	groupLayout.setHorizontalGroup(
		groupLayout.createParallelGroup(Alignment.LEADING)
			.addGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, 891, Short.MAX_VALUE)
				.addGap(11))
			.addGroup(groupLayout.createSequentialGroup()
				.addGap(10)
				.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 892, Short.MAX_VALUE)
				.addContainerGap())
			.addGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addComponent(scrollPane_1, GroupLayout.DEFAULT_SIZE, 892, Short.MAX_VALUE)
				.addContainerGap())
			.addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
				.addContainerGap()
				.addComponent(btnGen, GroupLayout.DEFAULT_SIZE, 892, Short.MAX_VALUE)
				.addContainerGap())
			.addGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addComponent(btnAIFollowUp, GroupLayout.PREFERRED_SIZE, 890, GroupLayout.PREFERRED_SIZE)
				.addContainerGap(12, Short.MAX_VALUE))
	);
	groupLayout.setVerticalGroup(
		groupLayout.createParallelGroup(Alignment.TRAILING)
			.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
				.addContainerGap()
				.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 277, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(lblNewLabel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(scrollPane_1, GroupLayout.PREFERRED_SIZE, 113, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(btnGen)
				.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(btnAIFollowUp, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
				.addContainerGap())
	);
	frmMylilrag.getContentPane().setLayout(groupLayout);
	MyLilRAGService.setPrintToOutput((s) -> printToOutput(s));
	new Thread(() -> {
	    btnGen.setEnabled(false);
	    textAreaInput.setEnabled(false);
	    MyLilRAGService.ingest();
	    btnGen.setEnabled(true);
	    textAreaInput.setEnabled(true);

	}).start();

    }

    /*
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
    */

    private void printToOutput(String s) {
	textAreaOutput.setText(textAreaOutput.getText() + "\n" + s);
	textAreaOutput.setCaretPosition(textAreaOutput.getText().length());
    }
}
