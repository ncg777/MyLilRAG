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
import java.nio.file.Files;
import java.awt.event.ActionEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

import Services.MyLilRAGService;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.filechooser.FileFilter;
import javax.swing.JTextField;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import com.google.common.base.Joiner;
import java.awt.Dimension;

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
	
	MyLilRAGService.ingestSingleFile(fn);
    }
    private static List<File> attachments = new ArrayList<File>();
    private static String getBoundary(String from, String to, Date date) {
	String o = "FROM" + from.toLowerCase() + 
		"TO" + to.toLowerCase()+ 
		"DATE" + getTimeStampMail(date).toLowerCase();
	o = o.replaceAll("(,|:|\\.|\s)", "").replaceAll("@", "AT");
	
	return o;
    }
    public static String generateMIMEEmail(
	    Date now,
	    String subject,
            String senderName, 
            String senderEmail, 
            String destinationName, 
            String destinationEmail, 
            String content) {return generateMIMEEmail(
        	    now,
        	    subject,
                    senderName, 
                    senderEmail, 
                    destinationName, 
                    destinationEmail, 
                    content,null);
            }
    public static String generateMIMEEmail(
	    Date now,
	    String subject,
            String senderName, 
            String senderEmail, 
            String destinationName, 
            String destinationEmail, 
            String content,
            String asAReplyTo) {
        StringBuilder mimeEmail = new StringBuilder();
        var timestamp = getTimeStampMail(now);
        mimeEmail.append("MIME-Version: 1.0\r\n");
        mimeEmail.append("Date: ").append(timestamp).append("\r\n");
        mimeEmail.append("From: ").append(senderName).append(" <").append(senderEmail).append(">\r\n");
        mimeEmail.append("To: ").append(destinationName).append(" <").append(destinationEmail).append(">\r\n");
        mimeEmail.append("Subject: ").append(subject + "\r\n");
    
        var boundary = getBoundary(senderEmail, destinationEmail, now);
        mimeEmail.append("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n\r\n");
        
        mimeEmail.append("--" + boundary + "\r\n");
        mimeEmail.append("Content-Type: text/plain; charset=UTF-8\r\n");
	mimeEmail.append("Content-Transfer-Encoding: 7bit\r\n\r\n");
        mimeEmail.append(content + "\r\n\r\n");
        
        for(var f : attachments) {
    	    mimeEmail.append("--" + boundary + "\r\n");
    	
    	    mimeEmail.append("Content-Type: text/plain; charset=UTF-8\r\n");
    	    mimeEmail.append("Content-Disposition: attachment; filename=\"" + f.getName() + "\"\r\n");
    	    mimeEmail.append("Content-Transfer-Encoding: 7bit\r\n\r\n");
            try {
        	mimeEmail.append(Joiner.on("\n").join(Files.readAllLines(f.toPath()))+"\r\n");
	     } catch (IOException e) {
		 e.printStackTrace();
	     }
        	
        }
        if(asAReplyTo!= null) {
            mimeEmail.append("--" + boundary + "\r\n");
            mimeEmail.append("Content-Type: message/rfc822" + "\r\n\r\n");
            var s = asAReplyTo.replace("MIME-Version: 1.0", "").trim();
            mimeEmail.append(s + "\r\n");
            
        }
        mimeEmail.append("--" + boundary + "--");
        return mimeEmail.toString();
    }
    private void endisable(boolean v) {
	btnAIFollowUp.setEnabled(v);
	btnGen.setEnabled(v);
	textAreaInput.setEnabled(v);
	comboEndpoints.setEnabled(v);
	comboModel.setEnabled(v);
	textUserName.setEnabled(v);
	textUserEmail.setEnabled(v);
	textOtherEmail.setEnabled(v);
	textOtherName.setEnabled(v);
	btnAttachFiles.setEnabled(v);
	btnClearFiles.setEnabled(v);
	btnClear.setEnabled(v && this.lastEmail != null);
	btnLoadEml.setEnabled(v);
	textSubject.setEnabled(v && this.lastEmail==null);
    }
    private String lastEmail = null;
    private static String getSubject(String mail) {
	var pat = Pattern.compile("Subject: (?<subject>.+)", 0);
	var matcher = pat.matcher(mail);

	for(var match :matcher.results().toList()) {
	    return match.group("subject");

	}
	return "";
    }
    private void setLastMail(String mail) {
	textSubject.setText("RE: "+ getSubject(mail));
	textSubject.setEnabled(false);
	btnClear.setEnabled(true);
	this.lastEmail = mail;
	this.textAreaOutput.setText(mail);
    }
    
    private void interact(String str) {
	new Thread(() -> {
	    endisable(false);
	    var now = new Date();
	    var fn = "./archive/" + getTimeStamp(now) + " FROM " + textUserEmail.getText() + " TO " + textOtherEmail.getText()+".eml";
	    
	    var mail = generateMIMEEmail(now, textSubject.getText(), textUserName.getText(), textUserEmail.getText(), textOtherName.getText(), textOtherEmail.getText(),str, lastEmail);
	    
	    //printToOutput("=== " +textUserName.getText() +  " ===\n" + mail + "\n");
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
	   
	    if(ans.startsWith("```")) {
		ans = ans.substring(ans.indexOf("\n")+1);
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
	    
	    this.setLastMail(ans);
	    
	    //printToOutput("=== " + textOtherName.getText()  + " ===\n" + ans + "\n");
	    textAreaOutput.setText(ans);
	    textAreaOutput.setCaretPosition(0);
	    attachments.clear();
	    textAreaInput.setText("");
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
    
    private JButton btnAttachFiles;
    private JLabel lblFiles;
    private JButton btnClearFiles;
    private JButton btnLoadEml;
    private void initialize() {
	frmMylilrag = new JFrame();
	frmMylilrag.getContentPane().setPreferredSize(new Dimension(550, 550));
	frmMylilrag.setTitle("MyLilRAG");
	frmMylilrag.setBounds(100, 100, 740, 592);
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
		    lastEmail = null;
		    textSubject.setText(lastEmail);
		    endisable(true);
		    textAreaOutput.setText("");
		}
	});
	
	btnAttachFiles = new JButton("Attach files");
	btnAttachFiles.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    JFileChooser fileChooser = new JFileChooser();
		    fileChooser.setMultiSelectionEnabled(true);

		    int returnValue = fileChooser.showOpenDialog(frmMylilrag);
		    if (returnValue == JFileChooser.APPROVE_OPTION) {
			File[] selectedFiles = fileChooser.getSelectedFiles();
			List<File> fileList = new ArrayList<>();
			for (File file : selectedFiles) {
			    fileList.add(file);
			}
			attachments.removeAll(fileList);
			attachments.addAll(fileList);
			lblFiles.setText(Joiner.on(", ").join(fileList));
		    }
		}
	});
	
	lblFiles = new JLabel("");
	
	btnClearFiles = new JButton("Clear files");
	btnClearFiles.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    attachments.clear();
		    lblFiles.setText("");
		}
	});
	
	btnLoadEml = new JButton("Load EML");
	btnLoadEml.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    JFileChooser fileChooser = new JFileChooser();
		    fileChooser.setFileFilter(new FileFilter() {
		        
		        @Override
		        public String getDescription() {
		    	return "Email (*.eml)";
		        }
		        
		        @Override
		        public boolean accept(File f) {
		    	
		    		return f.isDirectory() || f.getName().endsWith(".eml");
		        }
		    });

		    int returnValue = fileChooser.showOpenDialog(frmMylilrag);
		    if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			try {
			    String mail = Joiner.on("\n").join(Files.readAllLines(selectedFile.toPath()));
			    setLastMail(mail);
			} catch (IOException e1) {
			    // TODO Auto-generated catch block
			    e1.printStackTrace();
			}
			
		    }
		}
	});
	
	GroupLayout groupLayout = new GroupLayout(frmMylilrag.getContentPane());
	groupLayout.setHorizontalGroup(
		groupLayout.createParallelGroup(Alignment.LEADING)
			.addGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
					.addGroup(groupLayout.createSequentialGroup()
						.addComponent(lblNewLabel_5)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(btnLoadEml, GroupLayout.PREFERRED_SIZE, 156, GroupLayout.PREFERRED_SIZE)
						.addGap(816))
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
							.addGroup(groupLayout.createSequentialGroup()
								.addComponent(btnAttachFiles)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(btnClearFiles)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(lblFiles, GroupLayout.DEFAULT_SIZE, 474, Short.MAX_VALUE))
							.addComponent(btnGen, GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE)
							.addComponent(btnAIFollowUp, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 646, GroupLayout.PREFERRED_SIZE)
							.addComponent(scrollPane_1, GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE))
						.addGap(347))
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
							.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE)
							.addGroup(groupLayout.createSequentialGroup()
								.addComponent(lblNewLabel_6, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(textSubject, GroupLayout.DEFAULT_SIZE, 488, Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 82, GroupLayout.PREFERRED_SIZE))
							.addComponent(scrollPane, 0, 0, Short.MAX_VALUE)
							.addGroup(groupLayout.createSequentialGroup()
								.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
									.addComponent(lblNewLabel_8, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(lblNewLabel_2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(lblNewLabel_1, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE))
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
											.addComponent(textOtherEmail, GroupLayout.PREFERRED_SIZE, 230, GroupLayout.PREFERRED_SIZE))))))
						.addGap(347))))
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
					.addComponent(btnLoadEml, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
					.addComponent(lblNewLabel_6)
					.addComponent(textSubject, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
					.addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(lblNewLabel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(scrollPane_1, GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(btnGen)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnAttachFiles)
						.addComponent(btnClearFiles))
					.addComponent(lblFiles, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(btnAIFollowUp, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
				.addGap(28))
	);
	frmMylilrag.getContentPane().setLayout(groupLayout);
	MyLilRAGService.setPrintToOutput((s) -> printToOutput(s));
	
	new Thread(() -> {
	    endisable(false);
	    MyLilRAGService.ingest();
	    endisable(true);

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
