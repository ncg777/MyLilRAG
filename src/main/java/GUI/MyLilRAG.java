package GUI;
import java.awt.EventQueue;

import javax.swing.JFrame;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import dev.langchain4j.service.Result;

import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.awt.event.ActionEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

import Services.MyLilRAGService;
import javax.swing.LayoutStyle.ComponentPlacement;

import javax.swing.JTextField;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import javax.swing.JComboBox;

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
    private static String getTimeStamp(Date date) {
	 SimpleDateFormat dateFormatDate = new SimpleDateFormat("yyyyMMdd");
	 dateFormatDate.setTimeZone(TimeZone.getTimeZone("UTC"));
	 SimpleDateFormat dateFormatTime = new SimpleDateFormat("HHmmss");
	 dateFormatTime.setTimeZone(TimeZone.getTimeZone("UTC"));
	 var o = dateFormatDate.format(date) + "T" + dateFormatTime.format(date) + "Z";
	 return o;
	 
    }
    private static String getTimeStampMail(Date date) {
	 SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
	 var o = dateFormat.format(date);
	 return o;
   }
    public static void saveEmail(
	    String fn,
            String mail) throws FileNotFoundException {
	
	PrintWriter pw = new PrintWriter(fn);
	pw.print(mail.toString());
	pw.flush();
	pw.close();
	MyLilRAGService.ingestSingleFile(new File(fn));
    }
    public static String generateMIMEEmail(
	    Date now,
	    String subject,
            String senderName, 
            String senderEmail, 
            String destinationName, 
            String destinationEmail, 
            String content) {
        StringBuilder mimeEmail = new StringBuilder();
        var timestamp = getTimeStampMail(now);
        mimeEmail.append("MIME-Version: 1.0\r\n");
        mimeEmail.append("Date: ").append(timestamp).append("\r\n");
        mimeEmail.append("From: ").append(senderName).append(" <").append(senderEmail).append(">\r\n");
        mimeEmail.append("To: ").append(destinationName).append(" <").append(destinationEmail).append(">\r\n");
        mimeEmail.append("Subject: ").append(subject + "\r\n");
        mimeEmail.append("Content-Type: text/plain; charset=UTF-8\r\n");
        mimeEmail.append("Content-Transfer-Encoding: 7bit\r\n");
        mimeEmail.append("\r\n");
        mimeEmail.append(content);
        
        return mimeEmail.toString();
    }
    private void endisable(boolean v) {
	btnClear.setEnabled(v);
	btnAIFollowUp.setEnabled(v);
	btnGen.setEnabled(v);
	textAreaInput.setEnabled(v);
	comboEndpoints.setEnabled(v);
	comboModel.setEnabled(v);
	textSubject.setEnabled(v);
	textUserName.setEnabled(v);
	textUserEmail.setEnabled(v);
	textOtherEmail.setEnabled(v);
	textOtherName.setEnabled(v);
    }
    private void interact(String str) {
	new Thread(() -> {
	    endisable(false);
	    var now = new Date();
	    var fn = "./archive/" + getTimeStamp(now) + " FROM " + textUserEmail.getText() + " TO " + textOtherEmail.getText()+".eml";
	    var mail = generateMIMEEmail(now, textSubject.getText(), textUserName.getText(), textUserEmail.getText(), textOtherName.getText(), textOtherEmail.getText(),str);
	    
	    textSubject.setText("");
	    textAreaInput.setText("");

	    printToOutput("=== " +textUserName.getText() +  " ===\n" + mail + "\n");
	    try {
		saveEmail(fn, mail);
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    }
	    Result<String> answer = MyLilRAGService.getAssistant(comboModel.getSelectedItem().toString(),
		    textUserName.getText(),textUserEmail.getText(),textOtherName.getText(),textOtherEmail.getText())
		    .chat(mail);
	    
	    // Use Calendar to add a second
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(now);
	    calendar.add(Calendar.SECOND, 1); // Add 1 second
	    var ans = answer.content();
	    // Get the updated date
	    now = calendar.getTime();
	    if(ans.startsWith("```mime")) {
		ans = ans.substring(8);
		ans = ans.substring(0, ans.length()-3);
	    }
	    var provided_date = ans.split("\n")[1];
	    if(provided_date.startsWith("Date: ")) {
		ans  = ans.replace(provided_date, "Date: " + getTimeStampMail(now));
	    }
	    
	    fn = "./archive/" + getTimeStamp(now) + " FROM " + textOtherEmail.getText() + " TO " + textUserEmail.getText()+".eml";
	    try {
		saveEmail(fn, ans);
	    } catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    printToOutput("=== " + textOtherName.getText()  + " ===\n" + ans + "\n");
	    
	    endisable(true);
	}).start();
    }
    private JButton btnGen;
    JButton btnAIFollowUp;
    private JTextField textUserName;
    private JTextField textUserEmail;
    private JTextField textOtherEmail;
    private JLabel lblNewLabel_3;
    private JLabel lblNewLabel_4;
    private JTextField textOtherName;
    private JLabel lblNewLabel_5;
    private JLabel lblNewLabel_6;
    private JTextField textSubject;
    private JComboBox<String> comboModel;
    private JLabel lblNewLabel_8;
    private JComboBox<String> comboEndpoints;
    private JButton btnClear;
    private static DefaultComboBoxModel<String> getComboModel() {
	try {
	    return new DefaultComboBoxModel<>(MyLilRAGService.getModels());
	} catch (IOException | InterruptedException | URISyntaxException e) {
	    return null;
	}
    }
    private void initialize() {
	frmMylilrag = new JFrame();
	frmMylilrag.setTitle("MyLilRAG");
	frmMylilrag.setBounds(100, 100, 928, 554);
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
	btnAIFollowUp.setVisible(false);
	btnAIFollowUp.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    var prompt = "Generate a follow-up to this email exchange between two users. Here is the conversation: \n\n" +
			    textAreaOutput.getText();
		    
		    interact(MyLilRAGService.oneShotChat(comboModel.getSelectedItem().toString(), prompt));
		}
	});
	
	JLabel lblNewLabel_1 = new JLabel("User Name:");
	
	textUserName = new JTextField();
	textUserName.setText("The User");
	textUserName.setColumns(10);
	
	JLabel lblNewLabel_2 = new JLabel("User email:");
	
	textUserEmail = new JTextField();
	textUserEmail.setText("the.user@mail.com");
	textUserEmail.setColumns(10);
	
	textOtherEmail = new JTextField();
	textOtherEmail.setText("the.other@mail.com");
	textOtherEmail.setColumns(10);
	
	lblNewLabel_3 = new JLabel("Other email:");
	
	lblNewLabel_4 = new JLabel("Other Name:");
	
	textOtherName = new JTextField();
	textOtherName.setText("The Other");
	textOtherName.setColumns(10);
	
	lblNewLabel_5 = new JLabel("Log:");
	
	lblNewLabel_6 = new JLabel("Subject:");
	
	textSubject = new JTextField();
	textSubject.setColumns(10);
	
	JLabel lblNewLabel_7 = new JLabel("Model:");
	comboModel = new JComboBox<String>(getComboModel());
	
	lblNewLabel_8 = new JLabel("Endpoint URL:");
	
	comboEndpoints = new JComboBox<String>(MyLilRAGService.getEndPoints());
	comboEndpoints.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    MyLilRAGService.setBaseUrl(comboEndpoints.getSelectedItem().toString());
		    comboModel.setModel(getComboModel());
		}
	});
	
	btnClear = new JButton("Clear");
	btnClear.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    textAreaOutput.setText("");
		}
	});
	
	GroupLayout groupLayout = new GroupLayout(frmMylilrag.getContentPane());
	groupLayout.setHorizontalGroup(
		groupLayout.createParallelGroup(Alignment.LEADING)
			.addGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
							.addComponent(lblNewLabel_8, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(lblNewLabel_2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(lblNewLabel_1, GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
							.addComponent(textUserName, GroupLayout.PREFERRED_SIZE, 230, GroupLayout.PREFERRED_SIZE)
							.addComponent(textUserEmail, GroupLayout.PREFERRED_SIZE, 230, GroupLayout.PREFERRED_SIZE)
							.addComponent(comboEndpoints, GroupLayout.PREFERRED_SIZE, 230, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
							.addGroup(groupLayout.createSequentialGroup()
								.addComponent(lblNewLabel_4, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
								.addGap(4)
								.addComponent(textOtherName, GroupLayout.PREFERRED_SIZE, 230, GroupLayout.PREFERRED_SIZE))
							.addGroup(groupLayout.createSequentialGroup()
								.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
									.addComponent(lblNewLabel_7, GroupLayout.PREFERRED_SIZE, 86, GroupLayout.PREFERRED_SIZE)
									.addComponent(lblNewLabel_3, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE))
								.addGap(4)
								.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
									.addComponent(comboModel, GroupLayout.PREFERRED_SIZE, 230, GroupLayout.PREFERRED_SIZE)
									.addComponent(textOtherEmail, GroupLayout.PREFERRED_SIZE, 230, GroupLayout.PREFERRED_SIZE))))
						.addGap(250))
					.addGroup(groupLayout.createSequentialGroup()
						.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, 891, Short.MAX_VALUE)
						.addGap(11))
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
							.addComponent(scrollPane_1, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 879, Short.MAX_VALUE)
							.addComponent(btnAIFollowUp, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 879, Short.MAX_VALUE)
							.addComponent(btnGen, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 879, Short.MAX_VALUE))
						.addGap(23))
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
							.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 881, Short.MAX_VALUE)
							.addGroup(groupLayout.createSequentialGroup()
								.addComponent(lblNewLabel_6, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(textSubject, GroupLayout.DEFAULT_SIZE, 807, Short.MAX_VALUE)))
						.addGap(21))
					.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
						.addComponent(lblNewLabel_5)
						.addGap(18)
						.addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 82, GroupLayout.PREFERRED_SIZE)
						.addContainerGap())))
	);
	groupLayout.setVerticalGroup(
		groupLayout.createParallelGroup(Alignment.LEADING)
			.addGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(lblNewLabel_1)
							.addComponent(textUserName, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(lblNewLabel_2, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE)
							.addComponent(textUserEmail, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)))
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
							.addGroup(groupLayout.createSequentialGroup()
								.addGap(2)
								.addComponent(lblNewLabel_4))
							.addComponent(textOtherName, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE))
						.addGap(6)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
							.addComponent(lblNewLabel_3, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE)
							.addComponent(textOtherEmail, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE))))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
					.addComponent(comboModel, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
					.addComponent(lblNewLabel_7, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
					.addComponent(lblNewLabel_8)
					.addComponent(comboEndpoints, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
				.addGap(8)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
					.addComponent(lblNewLabel_5)
					.addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 162, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
					.addComponent(lblNewLabel_6)
					.addComponent(textSubject, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(lblNewLabel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(scrollPane_1, GroupLayout.PREFERRED_SIZE, 113, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(btnGen)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(btnAIFollowUp, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
				.addContainerGap(28, Short.MAX_VALUE))
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
	textAreaOutput.setText(textAreaOutput.getText() + s + "\n");
	textAreaOutput.setCaretPosition(textAreaOutput.getText().length());
    }
}
