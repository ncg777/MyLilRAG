package name.ncg777.aiMailExchangeSimulator.GUI;
import java.awt.EventQueue;

import javax.swing.JFrame;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.Result;
import io.github.furstenheim.CopyDown;
import name.ncg777.aiMailExchangeSimulator.Services.MainService;
import name.ncg777.maths.Combination;
import name.ncg777.maths.Matrix;
import name.ncg777.maths.enumerations.MixedRadixEnumeration;
import name.ncg777.maths.music.pcs12.Pcs12;
import name.ncg777.maths.sequences.Sequence;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.awt.event.ActionEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JTextField;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
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
import org.jsoup.parser.Parser;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import javax.swing.JList;

public class AIMailExchangeSimulator {
    class Tools {
		/*
        @Tool("Fetches a markdown version of an HTML web page or any plain text web resource as is from a url.")
        public static String fetchUrl(@P("The url to fetch") String url) {
            WebDriver driver = new ChromeDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            driver.get(url);
            wait.until(
        	    (ExpectedCondition<Boolean>) wd ->  
        	    	((JavascriptExecutor)wd)
        	    		.executeScript("return document.readyState")
        	    			.equals("complete"));
            var src = driver.getPageSource();
            driver.close();
            boolean isMd = false;
            try {
		var doc = Parser.htmlParser().parseInput(src,(new URI(url)).getHost());
		for(var e : doc.select("a[href]")) {
		    e.attr("href", e.absUrl("href"));
		    e.text(e.text().trim());
		}
		var converter = new CopyDown();
		src = converter.convert(doc.toString());
		isMd = true;
	    } catch (URISyntaxException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
            var fn = "./archive/fetched_url_" + getTimeStamp(new Date()) + "_" + url.replaceAll(":","").replaceAll("[/%&?]", "_") + (isMd ? ".md" : "");
            PrintWriter pw;
            try {
        	pw = new PrintWriter(fn);
        	pw.print(src);
        	pw.flush();
        	pw.close();
        	MainService.ingestSingleFile(fn);
            } catch (FileNotFoundException e) {
        	;
            }
            return src;
        }
		*/
        @Tool("Get pitches in pitch class set identified by Forte number.")
        public static String getForteNumberPitches(@P("The forte number, with or without the transposition part, that is 'N-O' or 'N-O.T' where N is an integer for the number of notes, O is the order of the pitch class set and T is the optional zero-padded transposition integer where 00 <= T < 12, for example 7-35 or 7-35.11.") String forteNumber) {
            if(!forteNumber.contains(".")) forteNumber = forteNumber.trim() + ".00";
            var f = Pcs12.parseForte(forteNumber);
            if(f==null)return null;
            return f.asSequence().toString();
            
        }
        @Tool("Get Forte number from pitch set.")
        public static String getForteNumberFromPitchSequence(@P("A string representing a set of pitches as an integer sequence s of k numbers s_i with k <= 12 and 0 <= s_i <= 11.") String pitches) {
            var s = Sequence.parse(pitches);
            if(s==null)return null;
            Set<Integer> set = new TreeSet<Integer>();
            set.addAll(s);
            return Pcs12.identify(new Combination(12,set)).toForteNumberString();
        }
        @Tool("Get interval vector associated with Forte number.")
        public static String getForteNumberIntervalVector(@P("The forte number, with or without the transposition part, that is 'N-O' or 'N-O.T' where N is an integer for the number of notes, O is the order of the pitch class set and T is the optional zero-padded transposition integer where 00 <= T < 12, for example 7-35 or 7-35.11.") String forteNumber) {
            if(!forteNumber.contains(".")) forteNumber = forteNumber + ".00";
            var f = Pcs12.parseForte(forteNumber);
            if(f==null)return null;
            return f.getIntervalVector().toString();
        }
        
        @Tool("Enumerate a mixed base of dimension k, that is the n k-tuples of elements from each set base_i, with the integers considered as sets, and n being the product of the k base_i integers.")
        public static String enumerateMixedBase(@P("The integer base as a string of k space separated positive non-zero integers base_i with 0 <= i < k.") String base) {
            var b = Sequence.parse(base);
            var mre = new MixedRadixEnumeration(b);
            StringBuilder sb = new StringBuilder();
            while(mre.hasMoreElements()) sb.append((new Sequence(mre.nextElement())).toString()+"\n");
            return sb.toString();
        }
        
        @Tool("List all the contacts.")
        public static String getContactList() {
            var names = personas.getColumn(0);
            var emails = personas.getColumn(1);
            
            Matrix<String> o = new Matrix<>(names.size(),2);
            o.setColumn(0, names);
            o.setColumn(1, emails);
            return "NAME,EMAIL\n" + o.toString((s) -> s);
        }
    }
    private JFrame frmAIMailExchangeSimulator;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
	EventQueue.invokeLater(new Runnable() {
	    public void run() {
		if(tableMails != null) return;
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
    private static String getTimeStamp(Date date) {
	var sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	return sdf.format(date);
    }
    public static void saveEmail(
	    Date date,
            String mail) throws FileNotFoundException {
	String from = getFromFromMail(mail);
	String to = getToFromMail(mail);
	String subject = getSubjectFromMail(mail);
	String[] newRow = {getTimeStamp(date),from,to,subject};
	((DefaultTableModel)tableMails.getModel()).addRow(newRow);
	var r = new ArrayList<String>();
	r.add(getTimeStamp(date));
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
    private static String getBoundary(Date date) {
	String o = "boundary_"+ getTimeStampMail(date).toLowerCase();
	o = o.replaceAll("(,|:|\\.|\s)", "").replaceAll("@", "AT");
	
	return o;
    }
    public static String generateMIMEEmail(
	    Date now,
	    String subject,
            String senderName, 
            String senderEmail, 
            List<List<String>> destinations,
            String content) {return generateMIMEEmail(
        	    now,
        	    subject,
                    senderName, 
                    senderEmail, 
                    destinations, 
                    content,null);
            }
    public static String generateMIMEEmail(
	    Date now,
	    String subject,
            String senderName, 
            String senderEmail, 
            List<List<String>> destinations, 
            String content,
            String asAReplyTo) {
        StringBuilder mimeEmail = new StringBuilder();
        var timestamp = getTimeStampMail(now);
        mimeEmail.append("MIME-Version: 1.0\r\n");
        mimeEmail.append("Date: ").append(timestamp).append("\r\n");
        mimeEmail.append("From: ").append(senderName).append(" <").append(senderEmail).append(">\r\n");
        mimeEmail.append("To: ").append(Joiner.on("; ").join(destinations.stream().map(p -> p.get(0) + " <" + p.get(1) + ">").toList())+ "\r\n");
        mimeEmail.append("Subject: ").append(subject + "\r\n");
    
        var boundary = getBoundary(now);
        mimeEmail.append("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n\r\n");
        
        mimeEmail.append("--" + boundary + "\r\n");
        mimeEmail.append("Content-Type: text/plain; charset=UTF-8\r\n");
	mimeEmail.append("Content-Transfer-Encoding: 7bit\r\n\r\n");
        mimeEmail.append(content + "\r\n");
        
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
	btnGen.setEnabled(v);
	textAreaInput.setEnabled(v);
	comboEndpoints.setEnabled(v);
	comboModel.setEnabled(v);
	listTo.setEnabled(v);
	textAreaOutput.setEnabled(v);
	btnAttachFiles.setEnabled(v);
	btnClearFiles.setEnabled(v);
	textSubject.setEnabled(v);
	tableMails.setEnabled(v);
	comboUserPersona.setEnabled(v);
    }
    
    private static String getFromFromMail(String mail) {
   	var pat = Pattern.compile("From: (?<from>.+)", 0);
   	var matcher = pat.matcher(mail);

   	for(var match :matcher.results().toList()) {
   	    return match.group("from");

   	}
   	return "";
       }
    private static String getToFromMail(String mail) {
   	var pat = Pattern.compile("To: (?<to>.+)", 0);
   	var matcher = pat.matcher(mail);

   	for(var match :matcher.results().toList()) {
   	    return match.group("to");

   	}
   	return "";
       }
    private static String getSubjectFromMail(String mail) {
	var pat = Pattern.compile("Subject: (?<subject>.+)", 0);
	var matcher = pat.matcher(mail);

	for(var match :matcher.results().toList()) {
	    return match.group("subject");

	}
	return "";
    }
    JButton btnClear;
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

    private void interact(String str, String subject, String lastEmail, List<String> userPersona, List<List<String>> agentPersona) {
	interact(str, subject, lastEmail, userPersona, agentPersona, false);
    }
    private int interactLock = 0;
    private void interact(String str, String subject, String lastEmail, List<String> userPersona, List<List<String>> agentPersonas, boolean thenSwapped) {
	new Thread(() -> {
	    endisable(false);
	    interactLock++;
	    var now = new Date();
	    
	    var mail = (
		    (str == null) ? lastEmail : 
			generateMIMEEmail(now, 
				subject, 
				userPersona.get(0), userPersona.get(1), 
				agentPersonas, 
				str, lastEmail));

	    if(str != null) {
		try {
		    saveEmail(
			    now, 
			    mail);
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		}
	    }
        textAreaInput.setText("");
	    attachments.clear();
        textAreaFiles.setText("");
	    for(var agentPersona : agentPersonas) {
		Result<String> answer;
		try {
		    answer = MainService.getAssistant(comboModel.getSelectedItem().toString(),
			    userPersona.get(0),
			    userPersona.get(1),
			    userPersona.get(2),
			    agentPersona.get(0),
			    agentPersona.get(1),
			    agentPersona.get(2), new Tools())
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
		calendar.add(Calendar.SECOND, 1);
		var ans = answer.content();
		if(!ans.contains(MainService.placeholder)) {
		    throw new RuntimeException("The AI ain't cooperating.");
		}

		if(ans.contains("<think>")) {
		    if(ans.contains("<answer>")) {
			ans = ans.substring(ans.indexOf("<answer>")+8,ans.indexOf("</answer>")).trim();
		    } else {
			ans = ans.substring(ans.indexOf("</think>")+8).trim();
		    }
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
		    ans = ans.replaceAll(provided_boundary, getBoundary(now));
		}
		var provided_date = ans.split("\n")[1];
		if(provided_date.startsWith("Date: ")) {
		    ans  = ans.replace(provided_date, "Date: " + getTimeStampMail(now));
		}

		this.setSelectedMail(ans);

		try {
		    saveEmail(
			    now,
			    ans);
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		}

		
		if(thenSwapped) {interact(null, null, textAreaOutput.getText(), agentPersona,List.of(userPersona), false);}
		
	    }
	    if(--interactLock == 0) endisable(true);
	}).start();
    }
    private JButton btnGen;
    private JLabel lblNewLabel_6;
    private JTextField textSubject;
    private JLabel lblNewLabel_8;
    private JComboBox<String> comboModel;
    private JComboBox<String> comboEndpoints;
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
    private JComboBox<String> comboUserPersona = new JComboBox<>(new DefaultComboBoxModel<>(getPersonaNames()));
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
    private JList<String> listTo;
    private static DefaultTableModel getMailsTableModel() {
	return new DefaultTableModel(
		mails.rowCount() == 0 ? new String[0][4] : (String[][])mails.toJaggedList(s -> s).toArray(),
		mailsColumns) {
		    private static final long serialVersionUID = 1L;

		    @Override
		    public boolean isCellEditable(int row, int column) {
			return false;
		    }
	    
	};
    }
    
    private List<List<String>> getSelectedPersona() {
	return listTo.getSelectedValuesList().stream().map(s -> getPersonaFromName(s)).toList();
    }
    
    private DefaultListModel<String> toListModel = new DefaultListModel<String>();
    private JScrollPane scrollPane_2;
    private JTextArea textAreaFiles;
    private void initialize() {
	frmAIMailExchangeSimulator = new JFrame();
	frmAIMailExchangeSimulator.setResizable(false);
	frmAIMailExchangeSimulator.getContentPane().setPreferredSize(new Dimension(550, 550));
	frmAIMailExchangeSimulator.setTitle("AIMailExchangeSimulator");
	frmAIMailExchangeSimulator.setBounds(100, 100, 951, 625);
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
			    getSelectedPersona());
		}
	    }
	});

	JLabel lblNewLabel = new JLabel("User message:");
	
	JLabel lblNewLabel_1 = new JLabel("From:");
	
	JLabel lblNewLabel_2 = new JLabel("To:");
	
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
	
	comboUserPersona.setSelectedIndex(0);;
	
	splitPane = new JSplitPane();
	
	JScrollPane scrollPane_4 = new JScrollPane();
	
	btnClear = new JButton("Clear");
	btnClear.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    textSubject.setText("");
		    textAreaOutput.setText("");
		}
	});
	btnClear.setEnabled(false);
	
	scrollPane_2 = new JScrollPane();
	
	GroupLayout groupLayout = new GroupLayout(frmAIMailExchangeSimulator.getContentPane());
	groupLayout.setHorizontalGroup(
		groupLayout.createParallelGroup(Alignment.LEADING)
			.addGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
							.addComponent(lblNewLabel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 1010, Short.MAX_VALUE)
							.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
								.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
									.addGroup(groupLayout.createSequentialGroup()
										.addComponent(lblNewLabel_6, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(textSubject, GroupLayout.PREFERRED_SIZE, 701, GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(ComponentPlacement.UNRELATED)
										.addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 92, GroupLayout.PREFERRED_SIZE)
										.addGap(115))
									.addGroup(groupLayout.createSequentialGroup()
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
											.addComponent(lblNewLabel_1, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE)
											.addComponent(lblNewLabel_8, GroupLayout.PREFERRED_SIZE, 98, GroupLayout.PREFERRED_SIZE)
											.addComponent(lblNewLabel_7, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(ComponentPlacement.RELATED)
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
											.addComponent(comboEndpoints, 0, 230, Short.MAX_VALUE)
											.addComponent(comboModel, 0, 230, Short.MAX_VALUE)
											.addComponent(comboUserPersona, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
										.addPreferredGap(ComponentPlacement.RELATED)
										.addComponent(lblNewLabel_2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addPreferredGap(ComponentPlacement.UNRELATED)
										.addComponent(scrollPane_4, GroupLayout.PREFERRED_SIZE, 341, GroupLayout.PREFERRED_SIZE)
										.addGap(289)))
								.addGap(18))
							.addGroup(Alignment.LEADING, groupLayout.createParallelGroup(Alignment.TRAILING, false)
								.addComponent(scrollPane_1, Alignment.LEADING, 0, 0, Short.MAX_VALUE)
								.addComponent(btnGen, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
									.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
										.addComponent(btnClearFiles)
										.addComponent(btnAttachFiles))
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(scrollPane_2, GroupLayout.PREFERRED_SIZE, 794, GroupLayout.PREFERRED_SIZE))))
						.addGap(98))
					.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
						.addComponent(splitPane, GroupLayout.PREFERRED_SIZE, 886, GroupLayout.PREFERRED_SIZE)
						.addGap(222)))
				.addContainerGap())
	);
	groupLayout.setVerticalGroup(
		groupLayout.createParallelGroup(Alignment.LEADING)
			.addGroup(groupLayout.createSequentialGroup()
				.addContainerGap()
				.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(lblNewLabel_8)
							.addComponent(comboEndpoints, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
							.addComponent(lblNewLabel_2, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
						.addGap(6)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(lblNewLabel_7, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
							.addComponent(comboModel, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE))
						.addGap(5)
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(lblNewLabel_1)
							.addComponent(comboUserPersona, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)))
					.addComponent(scrollPane_4, GroupLayout.PREFERRED_SIZE, 82, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(splitPane, GroupLayout.PREFERRED_SIZE, 154, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
					.addComponent(textSubject, GroupLayout.PREFERRED_SIZE, 19, GroupLayout.PREFERRED_SIZE)
					.addComponent(lblNewLabel_6)
					.addComponent(btnClear, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(lblNewLabel)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(scrollPane_1, GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(btnGen)
				.addGap(18)
				.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
					.addGroup(groupLayout.createSequentialGroup()
						.addGap(25)
						.addComponent(btnClearFiles))
					.addComponent(btnAttachFiles)
					.addComponent(scrollPane_2, GroupLayout.PREFERRED_SIZE, 82, GroupLayout.PREFERRED_SIZE))
				.addContainerGap())
	);
	
	textAreaFiles = new JTextArea();
	scrollPane_2.setViewportView(textAreaFiles);
	if(toListModel.isEmpty()) {
	    toListModel.addAll(List.of(getPersonaNames()));
	}
	listTo = new JList<String>(toListModel);
	
	scrollPane_4.setViewportView(listTo);
	
	JScrollPane scrollPane_3 = new JScrollPane();
	splitPane.setLeftComponent(scrollPane_3);
	
	tableMails = new JTable();
	tableMails.addKeyListener(new KeyListener() {
	    
	    @Override
	    public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	    }
	    
	    @Override
	    public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	    }
	    
	    @Override
	    public void keyPressed(KeyEvent e) {
		if(e.isConsumed()) return;
		if(e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
		    var r = getSelectedRow();
		    tableMails.getSelectionModel().clearSelection();
		    int im = findSelectedMailIndexInModel(r);
		    
		    if(im >= 0) ((DefaultTableModel)tableMails.getModel()).removeRow(im);
		    im = findSelectedMailIndexInMatrix(r);
		    if(im >= 0) mails.removeRow(im);
		    try {
			writeMails();
		    } catch (FileNotFoundException e1) {
			e1.printStackTrace();
		    }
		}
	    }
	});
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
		if(tableMails.getSelectedRow() < 0) {
		    setSelectedMail(null);
		    return;
		}
		var sr = getSelectedRow();
		setSelectedMail(mails.getRow(findSelectedMailIndexInMatrix(sr)).get(4));
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
	frmAIMailExchangeSimulator.getContentPane().setLayout(groupLayout);
	MainService.setPrintToOutput((s) -> printToOutput(s));
	
	new Thread(() -> {
	    endisable(false);
	    MainService.ingest();
	    textAreaOutput.setText("");
	    endisable(true);

	}).start();

    }
    private List<String> getSelectedRow() {
	var o = new ArrayList<String>();
	for(int i=0;i<4;i++) {
	    o.add((String)tableMails.getValueAt(tableMails.getSelectedRow(), i));
	}
	return o;
    }
    private int findSelectedMailIndexInMatrix(List<String> v) {
	for(int i=0;i<mails.rowCount();i++) {
	    if(
		    mails.get(i, 0).equals(v.get(0)) &&
		    mails.get(i, 1).equals(v.get(1)) &&
		    mails.get(i, 2).equals(v.get(2)) &&
		    mails.get(i, 3).equals(v.get(3))) {
		return i;
	    }
	}
	return -1;
    }
    private int findSelectedMailIndexInModel(List<String> v) {
	for(int i=0;i<tableMails.getModel().getRowCount();i++) {
	    if(
		    tableMails.getModel().getValueAt(i, 0).equals(v.get(0)) &&
		    tableMails.getModel().getValueAt(i, 1).equals(v.get(1)) &&
		    tableMails.getModel().getValueAt(i, 2).equals(v.get(2)) &&
		    tableMails.getModel().getValueAt(i, 3).equals(v.get(3))) {
		return i;
	    }
	}
	return -1;
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
