package GUI;
import java.awt.EventQueue;

import javax.swing.JFrame;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import dev.langchain4j.service.Result;
import name.ncg777.computing.structures.JaggedList;

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

import com.fasterxml.jackson.core.JsonParseException;
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
		    personas = JaggedList.parseJSONFile("./personas.json", (s) -> s);
		    MyLilRAG window = new MyLilRAG();
		    window.frmMylilrag.setVisible(true);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	});
    }
    private static JaggedList<String> personas;
    /**
     * Create the application.
     * @throws IOException 
     * @throws JsonParseException 
     */
    public MyLilRAG() throws JsonParseException, IOException {
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
            
            var s = asAReplyTo;
            if(s.startsWith("MIME-Version: 1.0")) s = s.replaceFirst("MIME-Version: 1.0", "").trim();
            while((s.startsWith("\s") || s.startsWith("\r") || s.startsWith("\n")) && !s.isBlank()) {
        	s = s.substring(1).trim();
            }
            mimeEmail.append(s + "\r\n");
        }
        mimeEmail.append("--" + boundary + "--");
        return mimeEmail.toString().trim();
    }
    private void endisable(boolean v) {
	btnAIFollowUp.setEnabled(v);
	btnGen.setEnabled(v);
	textAreaInput.setEnabled(v);
	comboEndpoints.setEnabled(v);
	comboModel.setEnabled(v);
	btnAttachFiles.setEnabled(v);
	btnClearFiles.setEnabled(v);
	btnClear.setEnabled(v && this.lastEmail != null);
	btnLoadEml.setEnabled(v);
	textSubject.setEnabled(v);
	comboUserPersona.setEnabled(v);
	comboAgentPersona.setEnabled(v);
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
    private String getSubjectFromMail(String mail) {
	var subject_index = mail.indexOf("Subject: ");
	var reply_subject = "RE " + textSubject.getText();
	if(subject_index > 0) {
	    reply_subject = mail.substring(subject_index+9, mail.indexOf("\n", subject_index+9));
	}
	return reply_subject;
    }
    private void setLastMail(String mail) {
	textSubject.setText("RE: "+ getSubject(mail));
	textSubject.setEnabled(false);
	btnClear.setEnabled(true);
	this.lastEmail = mail;
	this.textAreaOutput.setText(mail);
    }
    private static String nonFileCharsRegex = "[<>:;?!]";
    private JaggedList<String> getUserPersona() {return getPersonaFromName(comboUserPersona.getSelectedItem().toString());}
    private JaggedList<String> getAgentPersona() {return getPersonaFromName(comboAgentPersona.getSelectedItem().toString());}
    private void interact(String str) {
	new Thread(() -> {
	    endisable(false);
	    var now = new Date();
	    var fn = "./archive/" + getTimeStamp(now) + " FROM " + getUserPersona().get(1).getValue() + " TO " + getAgentPersona().get(1).getValue()+" SUBJECT " + textSubject.getText().replaceAll(nonFileCharsRegex, "") + ".eml";
	    
	    var mail = generateMIMEEmail(now, textSubject.getText(), getUserPersona().get(0).getValue(), getUserPersona().get(1).getValue(), getAgentPersona().get(0).getValue(), getAgentPersona().get(1).getValue(), str, lastEmail);
	    
	    //printToOutput("=== " +textUserName.getText() +  " ===\n" + mail + "\n");
	    try {
		saveEmail(fn, mail);
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    }
	    Result<String> answer = MyLilRAGService.getAssistant(comboModel.getSelectedItem().toString(),
		    getUserPersona().get(0).getValue(),
		    getUserPersona().get(1).getValue(),
		    getUserPersona().get(2).getValue(),
		    getAgentPersona().get(0).getValue(),
		    getAgentPersona().get(1).getValue(),
		    getAgentPersona().get(2).getValue())
		    .chat(mail);
	    
	    // Use Calendar to add a second
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(now);
	    calendar.add(Calendar.SECOND, 1); // Add 1 second
	    var ans = answer.content();
	    // Get the updated date
	    now = calendar.getTime();
	    ans = ans.trim();
	    if(ans.startsWith("```")) {
		ans = ans.substring(ans.indexOf("\n")+1).trim();
		
	    }
	    while(ans.endsWith("```")) {
		ans = ans.substring(0,ans.length()-3).trim();
	    }
	    
	    var boundary_index = ans.indexOf("Content-Type: multipart/mixed; boundary=\"");
	    if(boundary_index > 0) {
		var provided_boundary = ans.substring(boundary_index+41, ans.indexOf('"', boundary_index+42));
		ans = ans.replaceAll(provided_boundary, getBoundary(getAgentPersona().get(1).getValue(),getUserPersona().get(1).getValue(),now));
	    }
	    var provided_date = ans.split("\n")[1];
	    if(provided_date.startsWith("Date: ")) {
		ans  = ans.replace(provided_date, "Date: " + getTimeStampMail(now));
	    }
	    fn = "./archive/" + getTimeStamp(now) + 
		    " FROM " + getAgentPersona().get(1).getValue() + 
		    " TO " + getUserPersona().get(1).getValue() +
		    " SUBJECT " + getSubjectFromMail(ans).replaceAll(nonFileCharsRegex, "") + ".eml";
	    try {
		saveEmail(fn, ans);
	    } catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    
	    this.setLastMail(ans);
	    
	    //printToOutput("=== " + textAgentName.getText()  + " ===\n" + ans + "\n");
	    textAreaOutput.setText(ans);
	    textAreaOutput.setCaretPosition(0);
	    attachments.clear();
	    textAreaFiles.setText("");
	    textAreaInput.setText("");
	    endisable(true);
	}).start();
    }
    private JButton btnGen;
    JButton btnAIFollowUp;
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
    private JButton btnClearFiles;
    private JButton btnLoadEml;
    private JaggedList<String> getPersonaFromName(String name) {
	for(int j=0; j < personas.size(); j++) {
	    if(personas.get(j).get(0).getValue().equals(name)) return personas.get(j);
	}
	return null;
    }
    
    private String[] getPersonaNames() {
	var o = new ArrayList<String>();
	for(int j=0; j < personas.size(); j++) {
	    o.add(personas.get(j).get(0).getValue());
	}
	return o.toArray(new String[0]);
    }
    
    private JComboBox<String> comboUserPersona = new JComboBox<>(new DefaultComboBoxModel<>(getPersonaNames()));
    private JComboBox<String> comboAgentPersona = new JComboBox<>(new DefaultComboBoxModel<>(getPersonaNames()));
    private JTextArea textAreaFiles;
    private void initialize() {
	frmMylilrag = new JFrame();
	frmMylilrag.getContentPane().setPreferredSize(new Dimension(550, 550));
	frmMylilrag.setTitle("MyLilRAG");
	frmMylilrag.setBounds(100, 100, 682, 574);
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
	
	JLabel lblNewLabel_1 = new JLabel("User Persona:");
	
	JLabel lblNewLabel_2 = new JLabel("Agent Persona:");
	
	lblNewLabel_5 = new JLabel("Exchange:");
	
	lblNewLabel_6 = new JLabel("Subject:");
	
	textSubject = new JTextField();
	
	JLabel lblNewLabel_7 = new JLabel("Model:");
	comboModel = new JComboBox<String>(getComboModel());
	
	lblNewLabel_8 = new JLabel("Endpoint:");
	
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
			textAreaFiles.setText(Joiner.on("\n").join(attachments));
		    }
		}
	});
	
	btnClearFiles = new JButton("Clear files");
	btnClearFiles.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    attachments.clear();
		    textAreaFiles.setText("");
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
	
	comboUserPersona.setSelectedIndex(0);
	
	comboAgentPersona.setSelectedIndex(1);;
	
	JScrollPane scrollPane_2 = new JScrollPane();
	
	GroupLayout groupLayout = new GroupLayout(frmMylilrag.getContentPane());
	groupLayout.setHorizontalGroup(
		groupLayout.createParallelGroup(Alignment.LEADING)
			.addGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
							.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, 750, Short.MAX_VALUE)
							.addGroup(groupLayout.createSequentialGroup()
								.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
									.addGroup(groupLayout.createSequentialGroup()
										.addComponent(lblNewLabel_6, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(textSubject, GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 92, GroupLayout.PREFERRED_SIZE))
									.addComponent(scrollPane, 0, 0, Short.MAX_VALUE)
									.addGroup(groupLayout.createSequentialGroup()
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
											.addComponent(lblNewLabel_2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
											.addComponent(lblNewLabel_1, GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE)
											.addComponent(lblNewLabel_5, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
										.addPreferredGap(ComponentPlacement.RELATED)
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
											.addGroup(groupLayout.createSequentialGroup()
												.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
													.addComponent(comboAgentPersona, GroupLayout.PREFERRED_SIZE, 225, GroupLayout.PREFERRED_SIZE)
													.addComponent(comboUserPersona, GroupLayout.PREFERRED_SIZE, 225, GroupLayout.PREFERRED_SIZE))
												.addPreferredGap(ComponentPlacement.RELATED)
												.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
													.addComponent(lblNewLabel_8, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
													.addComponent(lblNewLabel_7, GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE))
												.addPreferredGap(ComponentPlacement.RELATED)
												.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
													.addComponent(comboModel, GroupLayout.PREFERRED_SIZE, 230, GroupLayout.PREFERRED_SIZE)
													.addComponent(comboEndpoints, GroupLayout.PREFERRED_SIZE, 230, GroupLayout.PREFERRED_SIZE)))
											.addComponent(btnLoadEml, GroupLayout.PREFERRED_SIZE, 156, GroupLayout.PREFERRED_SIZE))))
								.addGap(130))
							.addGroup(groupLayout.createSequentialGroup()
								.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
									.addGroup(groupLayout.createSequentialGroup()
										.addComponent(btnAttachFiles)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(btnClearFiles)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(scrollPane_2, GroupLayout.DEFAULT_SIZE, 444, Short.MAX_VALUE))
									.addComponent(btnGen, GroupLayout.DEFAULT_SIZE, 622, Short.MAX_VALUE)
									.addComponent(scrollPane_1, GroupLayout.DEFAULT_SIZE, 622, Short.MAX_VALUE))
								.addGap(128)))
						.addGap(0))
					.addGroup(groupLayout.createSequentialGroup()
						.addComponent(btnAIFollowUp, GroupLayout.PREFERRED_SIZE, 166, GroupLayout.PREFERRED_SIZE)
						.addContainerGap(490, Short.MAX_VALUE))))
	);
	groupLayout.setVerticalGroup(
		groupLayout.createParallelGroup(Alignment.LEADING)
			.addGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
					.addComponent(lblNewLabel_1)
					.addComponent(comboUserPersona, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
					.addComponent(lblNewLabel_8)
					.addComponent(comboEndpoints, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(lblNewLabel_2, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE)
							.addComponent(comboAgentPersona, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
							.addComponent(lblNewLabel_7, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(lblNewLabel_5)
							.addComponent(btnLoadEml, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE)))
					.addComponent(comboModel, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 198, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
					.addComponent(lblNewLabel_6)
					.addComponent(textSubject, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
					.addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(lblNewLabel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(scrollPane_1, GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(btnGen)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(btnAttachFiles)
							.addComponent(btnClearFiles))
						.addGap(9)
						.addComponent(btnAIFollowUp, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE))
					.addComponent(scrollPane_2, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE))
				.addGap(39))
	);
	
	textAreaFiles = new JTextArea();
	textAreaFiles.setEditable(false);
	scrollPane_2.setViewportView(textAreaFiles);
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
