package name.ncg777.aiMailExchangeSimulator.GUI;
import java.awt.EventQueue;

import javax.swing.JFrame;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import dev.langchain4j.service.Result;
import name.ncg777.aiMailExchangeSimulator.Services.MainService;
import name.ncg777.maths.Matrix;

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
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JTextField;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.base.Joiner;
import java.awt.Dimension;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.commons.text.StringEscapeUtils;

public class AIMailExchangeSimulator {

    private JFrame frmAIMailExchangeSimulator;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
	EventQueue.invokeLater(new Runnable() {
	    public void run() {
		try {
		    personas = Matrix.parseJSONFile("./personas.json", (s) -> s);
		    AIMailExchangeSimulator window = new AIMailExchangeSimulator();
		    window.frmAIMailExchangeSimulator.setVisible(true);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	});
    }
    private static Matrix<String> personas;
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
    private static String getTimeStampTable(Date date) {
	var sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	return sdf.format(date);
  }
    public static void saveEmail(
	    Date date,
	    String from,
	    String to,
	    String subject,
            String mail) throws FileNotFoundException {
	String[] newRow = {getTimeStampTable(date),from,to,subject};
	getMailsTableModel().addRow(newRow);
	var r = new ArrayList<String>();
	r.add(getTimeStampTable(date));
	r.add(from);
	r.add(to);
	r.add(subject);
	r.add(mail);
	mails.insertRow(0, r);
        writeMails();
        tableMails.getSelectionModel().setLeadSelectionIndex(0);
        MainService.ingestString(mail);
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
	textSubject.setEnabled(v);
	
	comboUserPersona.setEnabled(v);
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
    
    private void setSelectedMail(String mail) {
	if(mail == null || mail.isBlank()) {
	    textSubject.setText("");
	    this.textAreaOutput.setText("");
	} else {
	    textSubject.setText("RE: "+ getSubjectFromMail(mail));
	    btnClear.setEnabled(true);
	    this.textAreaOutput.setText(mail);
	    textAreaOutput.setCaretPosition(0);
	    
	}
    }
    //private static String nonFileCharsRegex = "[<>:;?!/]";
    private List<String> getUserPersona() {return getPersonaFromName(comboUserPersona.getSelectedItem().toString());}
    private List<String> getAgentPersona() {return getPersonaFromName(comboAgentPersona.getSelectedItem().toString());}
    private void interact(String str, String subject, String lastEmail, List<String> userPersona, List<String> agentPersona) {
	interact(str, subject, lastEmail, userPersona, agentPersona, false);
    }
    
    private void interact(String str, String subject, String lastEmail, List<String> userPersona, List<String> agentPersona, boolean thenSwapped) {
	new Thread(() -> {
	    endisable(false);
	    var now = new Date();
	    
	    var mail = (
		    (str == null) ? lastEmail : 
			generateMIMEEmail(now, 
				subject, 
				userPersona.get(0), userPersona.get(1), 
				agentPersona.get(0), agentPersona.get(1), 
				str, lastEmail));
	    
	    if(str != null) {
		try {
		    saveEmail(
			    now,
			    userPersona.get(1),
		            agentPersona.get(1),
		            subject, 
		            mail);
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		}
	    }
	    Result<String> answer;
	    try {
		answer = MainService.getAssistant(comboModel.getSelectedItem().toString(),
			userPersona.get(0),
			userPersona.get(1),
			userPersona.get(2),
			agentPersona.get(0),
			agentPersona.get(1),
			agentPersona.get(2))
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
		ans = ans.replaceAll(provided_boundary, getBoundary(agentPersona.get(1),userPersona.get(1),now));
	    }
	    var provided_date = ans.split("\n")[1];
	    if(provided_date.startsWith("Date: ")) {
		ans  = ans.replace(provided_date, "Date: " + getTimeStampMail(now));
	    }
	    
	    this.setSelectedMail(ans);
	    
	    try {
		saveEmail(
			now,
			agentPersona.get(1),
			userPersona.get(1),
			getSubjectFromMail(ans),
			ans);
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    }
	    	    
	    
	    attachments.clear();
	    textAreaFiles.setText("");
	    textAreaInput.setText("");
	    if(thenSwapped) {interact(null, null, textAreaOutput.getText(), agentPersona,userPersona, false);}
	    else { endisable(true);}
	}).start();
    }
    private JButton btnGen;
    JButton btnAIFollowUp;
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
    private List<String> getPersonaFromName(String name) {
	for(int i=0; i < personas.rowCount(); i++) {
	    if(personas.get(i,0).equals(name)) return personas.getRow(i);
	}
	return null;
    }
    
    private String[] getPersonaNames() {
	var o = new ArrayList<String>();
	for(int i=0; i < personas.rowCount(); i++) {
	    o.add(personas.get(i,0));
	}
	return o.toArray(new String[0]);
    }
    private void clearSubject() {
	this.textAreaOutput.setText("");
	tableMails.getSelectionModel().clearSelection();
	textSubject.setText(null);
	endisable(true);
    }
    private JComboBox<String> comboUserPersona = new JComboBox<>(new DefaultComboBoxModel<>(getPersonaNames()));
    private JComboBox<String> comboAgentPersona = new JComboBox<>(new DefaultComboBoxModel<>(getPersonaNames()));
    private JTextArea textAreaFiles;
    final static String PREF_ENDPOINT = "endpoint";
    final static String PREF_MODEL = "model";
    private JSplitPane splitPane;
    private static JTable tableMails;
	
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
    private static String mailsFilePath = "./mails.json";
    private static Matrix<String> mails = readMails();
    private static Matrix<String> readMails(){
	if(!(new File(mailsFilePath)).exists()) {
	    PrintWriter pw;
	    try {
		pw = new PrintWriter(new File(mailsFilePath));
		pw.println("");
		pw.flush();
		pw.close();
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    }
	}
	try {
	    Matrix<String> m = Matrix.parseJSONFile(mailsFilePath, s -> StringEscapeUtils.unescapeJava(s));
	    if(m.rowCount() == 0) m = new Matrix<String>(0,5);
	    return m;
	} catch (IOException e) {
	    return null;
	}
    }
    
    private static void writeMails() throws FileNotFoundException {
	mails.printToJSON((s) -> s, mailsFilePath);
    }
    private static String[] mailsColumns = new String[] {
		"date", "from", "to", "subject"
	};
    private static DefaultTableModel mailsTableModel = getMailsTableModel();
    private static DefaultTableModel getMailsTableModel() {
	if(mailsTableModel == null) mailsTableModel = new DefaultTableModel(
		mails.rowCount() == 0 ? new String[0][4] : (String[][])mails.toJaggedList(s -> s).toArray(),
		mailsColumns);
	return mailsTableModel;
    }
    
    private void initialize() {
	frmAIMailExchangeSimulator = new JFrame();
	frmAIMailExchangeSimulator.getContentPane().setPreferredSize(new Dimension(550, 550));
	frmAIMailExchangeSimulator.setTitle("AIMailExchangeSimulator");
	frmAIMailExchangeSimulator.setBounds(100, 100, 984, 625);
	frmAIMailExchangeSimulator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	JScrollPane scrollPane_1 = new JScrollPane();

	textAreaInput = new JTextArea();
	textAreaInput.setWrapStyleWord(true);
	textAreaInput.setTabSize(2);
	textAreaInput.setLineWrap(true);
	scrollPane_1.setViewportView(textAreaInput);

	btnGen = new JButton("Send & Generate Reply");
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
	
	loadUserPrefs();
	
	comboUserPersona.setSelectedIndex(0);
	
	comboAgentPersona.setSelectedIndex(1);;
	
	JScrollPane scrollPane_2 = new JScrollPane();
	
	splitPane = new JSplitPane();
	
	GroupLayout groupLayout = new GroupLayout(frmAIMailExchangeSimulator.getContentPane());
	groupLayout.setHorizontalGroup(
		groupLayout.createParallelGroup(Alignment.LEADING)
			.addGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
					.addGroup(groupLayout.createSequentialGroup()
						.addComponent(btnAIFollowUp, GroupLayout.PREFERRED_SIZE, 166, GroupLayout.PREFERRED_SIZE)
						.addContainerGap(792, Short.MAX_VALUE))
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, 958, Short.MAX_VALUE)
								.addGroup(groupLayout.createSequentialGroup()
									.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
										.addGroup(groupLayout.createSequentialGroup()
											.addComponent(lblNewLabel_6, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
											.addPreferredGap(ComponentPlacement.RELATED)
											.addComponent(textSubject, GroupLayout.DEFAULT_SIZE, 764, Short.MAX_VALUE)
											.addPreferredGap(ComponentPlacement.UNRELATED)
											.addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 92, GroupLayout.PREFERRED_SIZE))
										.addGroup(groupLayout.createSequentialGroup()
											.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
												.addComponent(lblNewLabel_2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												.addComponent(lblNewLabel_1, GroupLayout.DEFAULT_SIZE, 89, Short.MAX_VALUE))
											.addPreferredGap(ComponentPlacement.RELATED)
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
										.addComponent(splitPane, GroupLayout.DEFAULT_SIZE, 940, Short.MAX_VALUE))
									.addGap(18))
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(btnAttachFiles)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(btnClearFiles)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(scrollPane_2, GroupLayout.DEFAULT_SIZE, 760, Short.MAX_VALUE)))
							.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
								.addComponent(btnGen, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 938, Short.MAX_VALUE)
								.addComponent(scrollPane_1)))
						.addGap(20))))
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
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblNewLabel_2, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE)
						.addComponent(comboAgentPersona, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblNewLabel_7, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE))
					.addComponent(comboModel, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(splitPane, GroupLayout.PREFERRED_SIZE, 195, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
					.addComponent(textSubject, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
					.addComponent(lblNewLabel_6)
					.addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(lblNewLabel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(scrollPane_1, GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
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
	
	JScrollPane scrollPane_3 = new JScrollPane();
	splitPane.setLeftComponent(scrollPane_3);
	
	tableMails = new JTable();
	
	tableMails.setModel(getMailsTableModel());
	var sorter = new TableRowSorter<TableModel>(tableMails.getModel());
	sorter.setSortable(0, true);
	sorter.toggleSortOrder(0);
	sorter.toggleSortOrder(0);
	sorter.setSortsOnUpdates(true);
	sorter.setSortable(1, true);
	sorter.setSortable(2, true);
	sorter.setSortable(3, true);
	
	tableMails.setRowSorter(sorter);
	tableMails.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	tableMails.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
	    
	    @Override
	    public void valueChanged(ListSelectionEvent e) {
		if(e.getValueIsAdjusting()) return;
		if(tableMails.getSelectionModel().getSelectedIndices().length == 0) {
		    setSelectedMail(null);
		    return;
		}
		setSelectedMail(mails.getRow(tableMails.getSelectionModel().getSelectedIndices()[0]).get(4));
		/*
		var v = mails.getRow(tableMails.getSelectionModel().getSelectedIndices()[0]);
		for(int i=0;i<mails.rowCount();i++) {
		    if(
			    mails.get(i, 0).equals(v.get(0)) &&
			    mails.get(i, 1).equals(v.get(1)) &&
			    mails.get(i, 2).equals(v.get(2)) &&
			    mails.get(i, 3).equals(v.get(3))) {
			setSelectedMail(mails.get(i, 4));
			break;
		    }
		}
		*/
	    }
	});
	
	tableMails.getColumnModel().getColumn(0).setPreferredWidth(93);
	tableMails.getColumnModel().getColumn(1).setPreferredWidth(115);
	tableMails.getColumnModel().getColumn(2).setPreferredWidth(110);
	tableMails.getColumnModel().getColumn(3).setPreferredWidth(259);
	scrollPane_3.setViewportView(tableMails);
	
		JScrollPane scrollPane = new JScrollPane();
		splitPane.setRightComponent(scrollPane);
		
		textAreaOutput = new JTextArea();
		scrollPane.setViewportView(textAreaOutput);
		textAreaOutput.setEditable(false);
		textAreaOutput.setWrapStyleWord(true);
		textAreaOutput.setTabSize(2);
		textAreaOutput.setLineWrap(true);
		textAreaOutput.setText("");
		splitPane.setDividerLocation(450);
	
	textAreaFiles = new JTextArea();
	textAreaFiles.setEditable(false);
	scrollPane_2.setViewportView(textAreaFiles);
	frmAIMailExchangeSimulator.getContentPane().setLayout(groupLayout);
	MainService.setPrintToOutput((s) -> printToOutput(s));
	
	new Thread(() -> {
	    endisable(false);
	    MainService.ingest();
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
