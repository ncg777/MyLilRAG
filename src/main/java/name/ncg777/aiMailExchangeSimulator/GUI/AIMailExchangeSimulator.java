package name.ncg777.aiMailExchangeSimulator.GUI;
import java.awt.EventQueue;

import javax.swing.JFrame;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import dev.langchain4j.service.Result;
import name.ncg777.aiMailExchangeSimulator.Services.MainService;
import name.ncg777.computing.structures.JaggedList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

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
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.filechooser.FileFilter;
import javax.swing.JTextField;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.base.Joiner;
import java.awt.Dimension;

public class AIMailExchangeSimulator {

    private JFrame frmAIMailExchangeSimulator;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
	EventQueue.invokeLater(new Runnable() {
	    public void run() {
		try {
		    personas = JaggedList.parseJSONFile("./personas.json", (s) -> s);
		    AIMailExchangeSimulator window = new AIMailExchangeSimulator();
		    window.frmAIMailExchangeSimulator.setVisible(true);
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
    public AIMailExchangeSimulator() throws JsonParseException, IOException {
	initialize();
    }

    

    private JTextArea textAreaInput;
    private JTextArea textAreaOutput;
    
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
	
	MainService.ingestSingleFile(fn);
    }
    private static List<File> attachments = new ArrayList<File>();
    private static String getBoundary(String from, String to, Date date) {
	String o = "boundary_"+ getTimeStampMail(date).toLowerCase();
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
    private String lastEmail() {
	var o = this.textAreaOutput.getText();
	if(o == null || o.isBlank()) return null;
	return o;
    }
    private void endisable(boolean v) {
	btnAIFollowUp.setEnabled(v);
	btnGen.setEnabled(v);
	textAreaInput.setEnabled(v);
	comboEndpoints.setEnabled(v);
	comboModel.setEnabled(v);
	btnAttachFiles.setEnabled(v);
	btnClearFiles.setEnabled(v);
	btnClear.setEnabled(v && this.lastEmail() != null);
	btnLoadEml.setEnabled(v);
	textSubject.setEnabled(v);
	comboUserPersona.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    clearSubject();
		}
	});
	comboUserPersona.setEnabled(v);
	comboAgentPersona.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    clearSubject();
		}
	});
	comboAgentPersona.setEnabled(v);
    }
    private static String getSubjectFromMail(String mail) {
	var pat = Pattern.compile("Subject: (?<subject>.+)", 0);
	var matcher = pat.matcher(mail);

	for(var match :matcher.results().toList()) {
	    return match.group("subject");

	}
	return "";
    }
    
    private void setLastMail(String mail) {
	textSubject.setText("RE: "+ getSubjectFromMail(mail));
	btnClear.setEnabled(true);
	this.textAreaOutput.setText(mail);
	textAreaOutput.setCaretPosition(0);
    }
    private static String nonFileCharsRegex = "[<>:;?!/]";
    private JaggedList<String> getUserPersona() {return getPersonaFromName(comboUserPersona.getSelectedItem().toString());}
    private JaggedList<String> getAgentPersona() {return getPersonaFromName(comboAgentPersona.getSelectedItem().toString());}
    private void interact(String str, String subject, String lastEmail, JaggedList<String> userPersona, JaggedList<String> agentPersona) {
	interact(str, subject, lastEmail, userPersona, agentPersona, false);
    }
    
    private void interact(String str, String subject, String lastEmail, JaggedList<String> userPersona, JaggedList<String> agentPersona, boolean thenSwapped) {
	new Thread(() -> {
	    endisable(false);
	    var now = new Date();
	    
	    var mail = (
		    (str == null) ? lastEmail : 
			generateMIMEEmail(now, 
				subject, 
				userPersona.get(0).getValue(), userPersona.get(1).getValue(), 
				agentPersona.get(0).getValue(), agentPersona.get(1).getValue(), 
				str, lastEmail));
	    
	    if(str != null) {
		try {
		    saveEmail(
			    "./archive/" + 
		            MainService.getTimeStamp(now) + 
		            " FROM " + userPersona.get(1).getValue() + 
		            " TO " + agentPersona.get(1).getValue() +
		            " SUBJECT " + subject.replaceAll(nonFileCharsRegex, "") + ".eml", 
		            mail);
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		}
	    }
	    Result<String> answer;
	    try {
		answer = MainService.getAssistant(comboModel.getSelectedItem().toString(),
			userPersona.get(0).getValue(),
			userPersona.get(1).getValue(),
			userPersona.get(2).getValue(),
			agentPersona.get(0).getValue(),
			agentPersona.get(1).getValue(),
			agentPersona.get(2).getValue())
			.chat(mail);
	    } catch(Exception e) {
		JOptionPane.showMessageDialog(
			frmAIMailExchangeSimulator, 
			"There was an error fetching completions.",
			"Error", JOptionPane.ERROR_MESSAGE, null);
		return;
	    }
	    // Use Calendar to add a second
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(now);
	    calendar.add(Calendar.SECOND, 1); // Add 1 second
	    var ans = answer.content();
	    if(!ans.contains(MainService.placeholder)) {
 		throw new RuntimeException("The AI ain't cooperating.");
	    }
	    ans = ans.replace(MainService.placeholder, mail);
	    // Get the updated date
	    now = calendar.getTime();
	    ans = ans.trim();
	    if(ans.startsWith("```")) {
		ans = ans.substring(ans.indexOf("\n")+1).trim();
	    }
	    while(ans.endsWith("```")) {
		ans = ans.substring(0,ans.lastIndexOf("`")-3).trim();
	    }
	    
	    var boundary_index = ans.indexOf("Content-Type: multipart/mixed; boundary=\"");
	    if(boundary_index > 0) {
		var provided_boundary = ans.substring(boundary_index+41, ans.indexOf('"', boundary_index+42));
		ans = ans.replaceAll(provided_boundary, getBoundary(agentPersona.get(1).getValue(),userPersona.get(1).getValue(),now));
	    }
	    var provided_date = ans.split("\n")[1];
	    if(provided_date.startsWith("Date: ")) {
		ans  = ans.replace(provided_date, "Date: " + getTimeStampMail(now));
	    }
	    
	    this.setLastMail(ans);
	    
	    try {
		saveEmail("./archive/" + MainService.getTimeStamp(now) + 
			    " FROM " + agentPersona.get(1).getValue() + 
			    " TO " + userPersona.get(1).getValue() +
			    " SUBJECT " + getSubjectFromMail(ans).replaceAll(nonFileCharsRegex, "").trim() + ".eml", 
			    ans);
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    }
	    	    
	    //printToOutput("=== " + textAgentName.getText()  + " ===\n" + ans + "\n");
	    //textAreaOutput.setText(ans);
	    
	    attachments.clear();
	    textAreaFiles.setText("");
	    textAreaInput.setText("");
	    if(thenSwapped) {interact(null, null, textAreaOutput.getText(), agentPersona,userPersona, false);}
	    else { endisable(true);}
	}).start();
    }
    private JButton btnGen;
    JButton btnAIFollowUp;
    private JLabel lblNewLabel_5;
    private JLabel lblNewLabel_6;
    private JTextField textSubject;
    private JLabel lblNewLabel_8;
    private JComboBox<String> comboModel;
    private JComboBox<String> comboEndpoints;
    private JButton btnClear;
    private static DefaultComboBoxModel<String> getComboModel() {
	try {
	    return new DefaultComboBoxModel<>(MainService.getModels());
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
    private void clearSubject() {
	this.textAreaOutput.setText("");;
	textSubject.setText(null);
	endisable(true);
	textAreaOutput.setText("");
    }
    private JComboBox<String> comboUserPersona = new JComboBox<>(new DefaultComboBoxModel<>(getPersonaNames()));
    private JComboBox<String> comboAgentPersona = new JComboBox<>(new DefaultComboBoxModel<>(getPersonaNames()));
    private JTextArea textAreaFiles;
    final static String PREF_ENDPOINT = "endpoint";
    final static String PREF_MODEL = "model";
	
    private void loadUserPrefs() {
	Preferences prefs = Preferences.userNodeForPackage(AIMailExchangeSimulator.class);
	comboEndpoints.setSelectedItem(prefs.get(PREF_ENDPOINT, MainService.getEndPoints()[0]));
	try {
	    comboModel.setSelectedItem(prefs.get(PREF_MODEL, MainService.getModels()[0]));
	} catch (IOException | InterruptedException | URISyntaxException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }
    private void saveEndpointPref() {
	Preferences prefs = Preferences.userNodeForPackage(AIMailExchangeSimulator.class);
	prefs.put(PREF_ENDPOINT, comboEndpoints.getSelectedItem().toString());  
    }
    private void saveModelPref() {
	Preferences prefs = Preferences.userNodeForPackage(AIMailExchangeSimulator.class);
	prefs.put(PREF_MODEL, comboModel.getSelectedItem().toString());
    }
    
    private void initialize() {
	frmAIMailExchangeSimulator = new JFrame();
	frmAIMailExchangeSimulator.getContentPane().setPreferredSize(new Dimension(550, 550));
	frmAIMailExchangeSimulator.setTitle("AIMailExchangeSimulator");
	frmAIMailExchangeSimulator.setBounds(100, 100, 682, 574);
	frmAIMailExchangeSimulator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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
		if(!textAreaInput.getText().trim().isEmpty()) {
		    interact(
			    textAreaInput.getText(), 
			    textSubject.getText(),
			    lastEmail(),
			    getUserPersona(),
			    getAgentPersona());
		}
	    }
	});

	JLabel lblNewLabel = new JLabel("User message:");
	
	btnAIFollowUp = new JButton("Generate AI Follow-up");
	btnAIFollowUp.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    interact(
			    null, 
			    null,
			    lastEmail(),
			    getAgentPersona(),
			    getUserPersona(), true);
		}
	});
	
	JLabel lblNewLabel_1 = new JLabel("User Persona:");
	
	JLabel lblNewLabel_2 = new JLabel("Agent Persona:");
	
	lblNewLabel_5 = new JLabel("Exchange:");
	
	lblNewLabel_6 = new JLabel("Subject:");
	
	textSubject = new JTextField();
	
	JLabel lblNewLabel_7 = new JLabel("Model:");
	
	
	lblNewLabel_8 = new JLabel("Endpoint:");
	
	comboEndpoints = new JComboBox<String>(MainService.getEndPoints());
	comboEndpoints.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    MainService.setBaseUrl(comboEndpoints.getSelectedItem().toString());
		    comboModel.setModel(getComboModel());
		    saveEndpointPref();
		}
	});
	
	comboModel = new JComboBox<String>(getComboModel());
	comboModel.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    saveModelPref();
		}
	});
	
	btnClear = new JButton("Clear");
	btnClear.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    clearSubject();
		}
	});
	
	btnAttachFiles = new JButton("Attach files");
	btnAttachFiles.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    JFileChooser fileChooser = new JFileChooser();
		    fileChooser.setMultiSelectionEnabled(true);

		    int returnValue = fileChooser.showOpenDialog(frmAIMailExchangeSimulator);
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

		    int returnValue = fileChooser.showOpenDialog(frmAIMailExchangeSimulator);
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
	
	loadUserPrefs();
	
	comboUserPersona.setSelectedIndex(0);
	
	comboAgentPersona.setSelectedIndex(1);;
	
	JScrollPane scrollPane_2 = new JScrollPane();
	
	GroupLayout groupLayout = new GroupLayout(frmAIMailExchangeSimulator.getContentPane());
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
	frmAIMailExchangeSimulator.getContentPane().setLayout(groupLayout);
	MainService.setPrintToOutput((s) -> printToOutput(s));
	
	new Thread(() -> {
	    endisable(false);
	    MainService.ingest();
	    textAreaOutput.setText("");
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
