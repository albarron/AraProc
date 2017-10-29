package qa.qcri.iyas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

/**
 * A simple class with texts substitutions that aim to remove mARkdown tags from
 * OpenITI files. It also integrates a home-trained model of OpenNLP to 
 * do sentence splitting.
 * 
 * When executed in isolation, it gets an OpenITI file and generates a .plain file
 * with plain text (no mARkdown) and .sent with one sentence per line.
 * 
 * @author albarron
 * @since September 2017
 * @version 0.1
 */
public class MARkdown {

  private final String SENTENCE_MODEL = "resources/open-nlp-seg-models/ar-sent.bin";
  private  static SentenceDetectorME sentenceDetector;
  
  private final ReadFile FILE_READER;
  private final String FIRST_LINE = "######OpenITI#";
  
  private final String SUBJECT_PATTERN = "#####[A-Z]+#[A-Z]+#";
  private final String SUBJECT_SUBST = "";
  
  
  private final String HEADER_PATTERN = "### \\|{1,5}";
  private final String HEADER_SUBST = " ";
  //TODO incorporated into HEADER_PATTERN. MIGHT NOT BE NECESSARY
//  private final String HEADER_CHAPTER = "### |";
//  private final String HEADER_SECTION = "### ||";
//  private final String HEADER_SUBSECTION = "### |||";
  
  private final String EDITORIAL_PATTERN = "### \\|EDITOR\\|";
  private final String EDITORIAL_SUBST = "";
  
  private final String HEADER_META = "#META#";
  
  private final String TAG_PARAGRAPH_BEGINS = "#( %)?";
  private final String TAG_PARAGRAPH_CONTINUES = "~~";
  
  /** The symbol inserted when we want to start a new paragraph  */
  private final String PAR_DIVISOR = "\n";
  
  /** The symbol inserted when we want to continue the paragraph*/
  private final String PAR_CONTINUER = " ";
  
  private final String PAGE_NUMBER_PATTERN = "PageV\\d{2}P\\d{3}";
  private final String PAGE_NUMBER_SUBST = "";
  
  private final String MILESTONE_PATTERN = "Milestone300";
  private final String MILESTONE_SUBST = "";
  
  //TODO what character should I implement instead of this one?
  private final String VERSE_HEMISTICH_PATTERN = "%([~, ]%)?";
  private final String VERSE_HEMISTICH_SUBST = "";
  
  private final String DICTIONARY_PATTERN = "\\$DIC_[A-Z]{3}\\$";
  private final String DICTIONARY_SUBST = ".";
  
  private final String BIOGRAPHY_PATTERN = "### \\${1,4}";
  private final String BIOGRAPHY_SUBST = "";
  
  private final String HISTORY_PATTERN = "### @( RAW)?";
  private final String HISTORY_SUBST = " ";
  
  /**
   * Contains patterns for biographies (e.g., ### $BIO_MAN$), historical events
   * (e.g., ### $CHR_RAW$), doxographical items (e.g., ### $DOX_POS$)
   */
  private final String GENERIC_PATTERN = "### \\$[A-Z]{3}_[A-Z]{3}\\$";
  private final String GENERIC_SUBST = " ";
  
  private final String LINE_NUMBER = "\\%\\s+(\\d)+";
  private final String LINE_SUBST = " ";
  
  //TODO note that bios and dict entries have the same
  //TODO yet to be used
//  private final String EVENTS = "### @";
//  private final String DICT_ENTRIES = "### $";
  
  private static final String FILE_SUFFIX_PLAIN = ".plain";
  private static final String FILE_SUFFIX_SENTENCES = ".sent";
  
  /**
   * Loads the file and checks that it is openITI by checking its first
   * line
   * @param file
   *      input file in openITI format
   * @throws IOException 
   * @throws FileNotFoundException 
   * @throws InvalidFormatException 
   */
  public MARkdown(String file) throws InvalidFormatException, FileNotFoundException, IOException {
    FILE_READER = new ReadFile(file);
    String firstLine = FILE_READER.nextLine();
    if (! firstLine.trim().startsWith(FIRST_LINE)) {
      FILE_READER.close();
      System.err.println("The file does not start with the expected line '" 
                          +FIRST_LINE+ "'");
      System.exit(1);
    }
    SentenceModel model = new SentenceModel(new FileInputStream(SENTENCE_MODEL));
    sentenceDetector = new SentenceDetectorME(model);
  }  
  
  /**
   * Removes all the metadata (i.e. mARkdown) from the file
   * @return
   *        plain text version of the file contents
   */
  public String getPlainText() {
    StringBuffer sb = new StringBuffer();
    String currentLine;
    int counter = 0;
    while (FILE_READER.hasNextLine()) {
      if ((++ counter ) % 50 == 0) {
        System.err.println("Processing line " + counter);
      }
      //What to do with the lines...
      currentLine = FILE_READER.nextLine();
      if (! lineIsValid(currentLine)) {
        continue;
      }
      if (lineBeginsParagraph(currentLine)) {
        //do the preprocessing for new paragraph
        sb.append(PAR_DIVISOR);
        
      }
      if (lineContinuesParagraph(currentLine)) {
      //do the preprocessing for continues par
        sb.append(PAR_CONTINUER);
        
      }
      sb.append(cleanLine(currentLine));
    } 
    FILE_READER.close();
    return sb.toString();
  }
  
  /**
   * Identifies sentences in the text with a model trained with OpenNLP.
   * The method uses \n to split the text into (potential) paragraphs 
   * before starting with sentences.
   * 
   * @param text  
   *          plain text
   * @return
   *          a list withthe sentences in the text. 
   */
  public List<String> getSentences(String text) {
    System.err.println("Identifying sentences");
    List<String> sentences = new ArrayList<String>();
    String[] paragraphs = text.split(PAR_DIVISOR);
    for (int i = 0; i < paragraphs.length; i++) {
      Span[] spans = sentenceDetector.sentPosDetect(paragraphs[i]);
      for (int j = 0; j < spans.length; j++) {
        if (spans[j].getStart() < spans[j].getEnd()) {
          sentences.add(
              paragraphs[i].substring(spans[j].getStart(), spans[j].getEnd()));
        } else {
          System.out.println("this one is empty");
        }
      }
    }
    return sentences;
  }
  
  /**
   * Substitutes mARkdown for spaces. The new paragraph marker is substituted by 
   * linebreak
   * @param line
   * @return
   */
  private String cleanLine(String line) {
    line = line
        .replaceAll(SUBJECT_PATTERN, SUBJECT_SUBST)         // "#####[A-Z]+#[A-Z]+#";
        .replaceAll(HEADER_PATTERN, HEADER_SUBST)           // "### |{1,5}"
        .replaceAll(EDITORIAL_PATTERN, EDITORIAL_SUBST)     // "### \\|EDITOR\\|"
        .replaceAll(DICTIONARY_PATTERN, DICTIONARY_SUBST)   // "\\$DIC_[A-Z]{3}\\$"
        .replaceAll(HISTORY_PATTERN, HISTORY_SUBST)         // "### @( RAW)?";
        .replaceAll(GENERIC_PATTERN, GENERIC_SUBST)         // "### \\$[A-Z]{3}_[A-Z]{3}\\$";
        .replaceAll(BIOGRAPHY_PATTERN, BIOGRAPHY_SUBST)     // "### \\${1,4}";  TODO not sure it this is necessary as well
        .replaceAll(PAGE_NUMBER_PATTERN, PAGE_NUMBER_SUBST) // "PageV\\d{2}P\\d{3}"

        .replaceAll(TAG_PARAGRAPH_BEGINS, PAR_DIVISOR)              //  "#( %)?" (it was originally "#")
        .replaceAll(TAG_PARAGRAPH_CONTINUES, " ")           // "~~"
        .replaceAll("( ){2,}", " ")                         // space normalization
        .replaceAll(LINE_NUMBER, LINE_SUBST)                // "\\% (\\d)+"
        .replaceAll(VERSE_HEMISTICH_PATTERN, VERSE_HEMISTICH_SUBST) //"%([~, ]%)?";
        .replaceAll(MILESTONE_PATTERN, MILESTONE_SUBST)     //"Milestone300"
        // 
        .replaceAll(" % ", " ")        
        ;

    return line;
  }
  
  /**
   * @param line
   * @return
   *        true if this line of text starts with the paragraph identifier
   */
  private boolean lineBeginsParagraph(String line) {
    return line.startsWith(TAG_PARAGRAPH_BEGINS);
  }
  
  /**
   * @param line
   * @return
   *      true if this line of text starts with the continuation identifier
   */
  private boolean lineContinuesParagraph(String line) {
    return line.startsWith(TAG_PARAGRAPH_CONTINUES);
  }
  
  /**
   * Checks if the current line contains text. It will be considered invalid if
   * (a) it's empty (b) it's metadata
   * @param line
   *            a line from the openITI file
   * @return
   *          true if not empty nor metadata
   */
  private boolean lineIsValid(String line) {
    boolean valid = true;
    line = line.trim();
    if (line.isEmpty()) {
      valid = false;
    } else if (line.startsWith(HEADER_META)) {
      valid = false;
    }
    return valid; 
  }
  
  /**
   * Parses the parameters and, if provided, checks if the file exists and
   * can be read
   * @param args
   * @return
   *          the provided file (if valid)
   */
  private static String setup(String[] args)  {
    HelpFormatter formatter = new HelpFormatter();
//    int widthFormatter = 88;    
    
    Options options= new Options();
    CommandLineParser parser = new DefaultParser();

    options.addOption("f", "input", true, 
        "Input file");
    
    CommandLine cLine = null; 
    
    try {     
        cLine = parser.parse( options, args );
    } catch( ParseException exp ) {
      System.err.println( "Unexpected exception:" + exp.getMessage() );    
    }
    
    if (cLine == null || ! cLine.hasOption("f") ) {
      System.err.println("Please, provide the input file");
      formatter.printHelp(ArabicProcessor.class.getSimpleName(),options);
      System.exit(1);
    }
  
    File f = new File(cLine.getOptionValue("f"));
    if (! ( f.isFile() && f.canRead()) ) {
      System.err.println("I cannot read the file at " + f);
      System.exit(1);     
    }

    return f.toString();

  }

  
  
  public static void main (String[] args) throws InvalidFormatException, FileNotFoundException, IOException {
//    String input = "/Users/albarron/workspace/abc/qatar/qcri/shamela/0001AbuTalibCabdManaf.Diwan.JK007501-ara1";
//    String input = "/Users/albarron/workspace/abc/qatar/qcri/shamela/0834IbnHamzaFanari.MisbahUns.Shia004692-ara1";
    String input = setup(args);
    final String filePlain =  input + FILE_SUFFIX_PLAIN;
    final String fileSent = input + FILE_SUFFIX_SENTENCES;
    
    MARkdown mark = new MARkdown(input);
    String text = mark.getPlainText();
    
    
    WriteFile wf = new WriteFile(filePlain);
    wf.write(text);
    wf.close();
    System.err.println("Plain text file saved");
    
    List<String> sentences = mark.getSentences(text);

    WriteFile wf2 = new WriteFile(fileSent);

    int counter = 0;
    for (String sent : sentences) {
      wf2.writeLn(sent); 
      
      if ((++ counter ) % 50 == 0) {
        System.err.println("Writing sentence " + counter);
      }

    }
    wf2.close();
    System.err.println("Plain text file saved to " + filePlain);
    System.err.println("Sentences file saved to " + fileSent);
  }
  
  
  
}
