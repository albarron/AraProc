package qa.qcri.iyas;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.qcri.farasa.pos.Clitic;
import com.qcri.farasa.pos.FarasaPOSTagger;
import com.qcri.farasa.pos.Sentence;
import com.qcri.farasa.segmenter.Farasa;

import constituencyParser.ConstituencyParser;

public class ArabicProcessor {

  private String INPUT_FILE;
  
  private Farasa FARASA;
  private FarasaPOSTagger farasaPOS;
  private ConstituencyParser p;
  
  private CommandLine cLine; 
  
  private final String DEFAULT_PATH_TO_FARASA_MODEL = "resources/farasa-models/modelIteration10";
  private String PATH_TO_FARASA_MODEL;
  
  private final String FILE_SUFFIX_CLITICS = ".clit";
  private final String FILE_SUFFIX_LEMMAS = ".lem";
//  private final String FILE_SUFFIX_NAMEDENT = "ner";
  private final String FILE_SUFFIX_POS = ".pos";
  private final String FILE_SUFFIX_SEGMENT = ".segm";
  private final String FILE_SUFFIX_TREES = ".tree";
  private final String SEPARATOR = " ";

  
  public ArabicProcessor() throws Exception{
    FARASA = new Farasa();
    
    //TODO with this invocation, I cannot call setPathToFarasaModel becuase if
    //it doesn't exist 
    PATH_TO_FARASA_MODEL = DEFAULT_PATH_TO_FARASA_MODEL;
  }
  
  public void setPathToFarasaModel(String modelFile) {
    File path = new File(modelFile);
    if (!path.isFile()) {
      System.err.println("I cannot read the directory " + path);
      System.exit(1);     
    }
    PATH_TO_FARASA_MODEL = modelFile; 
  }
  
  
 
  

  
//  public void runParser(String text) throws InterruptedException, Exception {
//    //TODO this should be one single sentence
//    Sentence sentence = getFarasaSentence(text);
//  }
//  
  public List<String> getClitics(String text) throws InterruptedException, Exception {
    Sentence segments = getFarasaSentence(text);
    List<String> stems = new ArrayList<String>();
    for (Clitic w : segments.clitics) {
      stems.add(w.surface);
    }
    return stems;
  }
  
  public List<String> getLemmas(String text) {
    ArrayList<String> lemmas =  FARASA.lemmatizeLine(text);
    return lemmas;
  }
  
  public Sentence getFarasaSentence(String text) throws InterruptedException, Exception {
     ArrayList<String> segOutput = FARASA.segmentLine(text);
     return farasaPOS.tagLine(segOutput);
  }
  
//  private void runStemmer() {
//    ReadFile rf = new ReadFile(INPUT_FILE);
//    
//    while (rf.hasNextLine()) {
//      String sen = rf.nextLine();
//    }
//  }
  
  
    
  
  private void runSegmenter() throws InterruptedException, Exception {
    System.err.println("Computing segments");
    final String outputFile = INPUT_FILE + FILE_SUFFIX_SEGMENT; 
    
    ReadFile rf = new ReadFile(INPUT_FILE);

    StringBuffer sb = new StringBuffer();
    while (rf.hasNextLine()) {
      ArrayList<String> segOutput = FARASA.segmentLine(rf.nextLine());
      
      for (String w : segOutput) {
        sb.append(w)
          .append(SEPARATOR);
        }
      sb.append("\n");
    }
      rf.close();
      WriteFile wf = new WriteFile(outputFile);
      wf.write(sb.toString());
      wf.close();
      System.err.println("Segments written to file " + outputFile);
    
  }
  
  private final String SEPARATOR_POS = "/";
  
  
  
  private void runPost() throws InterruptedException, Exception {
    System.err.println("Computing POS");
    final String outputFile = INPUT_FILE + FILE_SUFFIX_POS; 
    
    ReadFile rf = new ReadFile(INPUT_FILE);
    
    StringBuffer sb = new StringBuffer();
    while (rf.hasNextLine()) {
      Sentence segments = getFarasaSentence(rf.nextLine());
      for (Clitic w : segments.clitics) {
        sb.append(w.surface)
          .append(SEPARATOR_POS)
          .append(w.guessPOS)
          .append(    //only add the gender if it exists
              w.genderNumber!=""
                  ? "-"+w.genderNumber
                  : "")
          .append(SEPARATOR);
        }
      sb.append("\n");
    }
      rf.close();
      WriteFile wf = new WriteFile(outputFile);
      wf.write(sb.toString());
      wf.close();
      System.err.println("POS written to file " + outputFile);
  }
 
  private void runClitics() throws InterruptedException, Exception {
    System.err.println("Computing clitics");
    final String outputFile = INPUT_FILE + FILE_SUFFIX_CLITICS; 
    
    ReadFile rf = new ReadFile(INPUT_FILE);
   
    StringBuffer sb = new StringBuffer();
    while (rf.hasNextLine()) {
      for (String stem : getClitics(rf.nextLine()) ) {
        sb.append(stem)
          .append(SEPARATOR);
      }
      sb.append("\n");
    }
    rf.close();
    
    WriteFile wf = new WriteFile(outputFile);
    wf.write(sb.toString());
    wf.close();
    System.err.println("Clitics written to file " + outputFile);
  }
  
  private void runLemmatizer() {
    System.err.println("Computing lemmas");
    final String outputFile = INPUT_FILE + FILE_SUFFIX_LEMMAS; 
    
    ReadFile rf = new ReadFile(INPUT_FILE);
    StringBuffer sb = new StringBuffer();
    while (rf.hasNextLine()) {
      for (String lemma : getLemmas(rf.nextLine())) {
        sb.append(lemma)
          .append(SEPARATOR);
      }
      sb.append("\n");
    }
    rf.close();
    
    WriteFile wf = new WriteFile(outputFile);
    wf.write(sb.toString());
    wf.close();
    System.err.println("Lemmas written to file " + outputFile);
  }
  
//  public void runNer() {
//    System.err.println("Computing NEs");
//    final String outputFile = INPUT_FILE + FILE_SUFFIX_NAMEDENT; 
//    ArabicNER ner = new ArabicNER(FARASA, farasaPOS);
//    ArrayList output = ner.tagLine("النص المراد معالجته");
//
//  int loc = 0;
//  for (String s : output)
//    {
//  String plusSign = " ";
//  if (loc == 0)
//  {
//      plusSign = "";
//  }
//              System.out.println(plusSign + s.trim());
//  
//  loc++;
//    }
//
//
//    }
  

  
  public void runParser() throws InterruptedException, IOException, Exception {
    System.err.println("Computing parse trees");
    final String outputFile = INPUT_FILE + FILE_SUFFIX_TREES; 
    
    ReadFile rf = new ReadFile(INPUT_FILE);
   
    
    StringBuffer sb = new StringBuffer();
    while (rf.hasNextLine()) {
      Sentence sentence = farasaPOS.tagLine(
          FARASA.segmentLine(rf.nextLine()));
      System.out.println(p.generateParserFormat(sentence));
      sb.append(p.generateParserFormat(sentence))
        .append("\n");
    }
    rf.close();
    WriteFile wf = new WriteFile(outputFile);
    wf.write(sb.toString());
    wf.close();
    System.err.println("Parse trees written to file " + outputFile);
    

  }
  public void run() throws InterruptedException, Exception {

    if (cLine.hasOption("c")) {
      runClitics();
    }
    
    if (cLine.hasOption("l")) {
      runLemmatizer();
    }
    if (cLine.hasOption("s")) {
      runSegmenter();
    }

    if (cLine.hasOption("p")) {
      runPost();
    }
    
    if (cLine.hasOption("t")) {
      runParser();
    }
//    
//    if (cLine.hasOption("n")) {
//      runNer();
//    }
//    
   
//    for (String sen : sentences) {
//      System.out.println(sen);
//      Sentence sentence = getFarasaSentence(sen);
//      if (cLine.hasOption("t")) {
//        String s = p.generateParserFormat(sentence);
//      System.out.println(p.parseSentence(s));
//      } 

////    
////    //Parsetrees
////    String s = p.generateParserFormat(sentence);
////    System.out.println(p.parseSentence(s));
////    System.out.println();


  }
  
  public void setInputFile(File path) {
    if (!path.isFile()) {
      System.err.println("I cannot read the file at " + path);
      System.exit(1);     
    }
    INPUT_FILE = path.toString();
  }
  
  private void setup(String[] args) throws FileNotFoundException, ClassNotFoundException, UnsupportedEncodingException, IOException, InterruptedException {
    HelpFormatter formatter = new HelpFormatter();
//    int widthFormatter = 88;    
    
    Options options= new Options();
    CommandLineParser parser = new DefaultParser();

    options.addOption("f", "input", true, 
        "Input file");
    
    options.addOption("m", "model", true, 
        "Path to the farasa model; if not set: " + DEFAULT_PATH_TO_FARASA_MODEL);
    options.addOption("l", "lemmas", false, "Compute lemmas");
    options.addOption("c", "clitics", false, "Compute clitics");
    options.addOption("s", "segments", false,  "Compute segments");
    options.addOption("p", "pos", false,    "Compute POS");
//    options.addOption("n", "nes", false, "Compute NEs");
    options.addOption("t", "tree", false,   "Compute parse tree");
    
    options.addOption("h", "help", false, "This help");
    
//    options.addOption("o", "outputFile", true,
//              "Output file, where the similarities are going to be stored");

    cLine = null;
    
    try {     
        cLine = parser.parse( options, args );
    } catch( ParseException exp ) {
      System.err.println( "Unexpected exception:" + exp.getMessage() );     
    }
    
    if (cLine == null || ! cLine.hasOption("f") ) {
      System.err.println("Please, provide the necessary parameters");
      formatter.printHelp(ArabicProcessor.class.getSimpleName(),options);
      System.exit(1);
    }
    
    setInputFile(new File(cLine.getOptionValue("f")));
    
    if (cLine.hasOption("m")) {
      setPathToFarasaModel(cLine.getOptionValue("m"));
    }
    farasaPOS = new FarasaPOSTagger(FARASA);
    if (cLine.hasOption("p")) {
      
      p = new ConstituencyParser(PATH_TO_FARASA_MODEL);
    } else if (cLine.hasOption("t")) {
      p = new ConstituencyParser(PATH_TO_FARASA_MODEL);
    }
   
  }
  
  
  public static void main(String[] args) throws Exception {
    
    ArabicProcessor erc = new ArabicProcessor();
    erc.setup(args);
    erc.run();
  }
  
}
