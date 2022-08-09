import com.java_polytech.common.IGrammar;

public class ManagerGrammar implements IGrammar {

    //data
    //grammar definition
    enum GRAMMAR {

        INPUT_FILE("INPUT_FILE"),// name of input file
        OUTPUT_FILE("OUTPUT_FILE"),// name of output file
        READER_CONFIG("READER_CONFIG"),// name of reader config file
        EXECUTOR_CONFIG("EXECUTOR_CONFIG"),// name of executor config file
        WRITER_CONFIG("WRITER_CONFIG"),// name of writer config file
        READER_NAME("READER_NAME"),// name of reader
        EXECUTOR_NAME("EXECUTOR_NAME"),// name of executor
        WRITER_NAME("WRITER_NAME");// name of writer

        final private String grammarStr;

        GRAMMAR(String grammarStr) { this.grammarStr = grammarStr; }

        public String toString() { return grammarStr; }

    }

    //separator
    public static final String separator = "=";
    //arguments separator
    public static final String argumentsSeparator = ",";
    //arguments separator
    public static final String commentString = "#";

    //interface methods
    @Override
    public String GetSeparator() { return separator; }

    @Override
    public String GetArgumentsSeparator() { return argumentsSeparator; }

    @Override
    public String GetCommentString(){ return commentString; }

    @Override
    public boolean CheckIfElementIsInGrammar(String str) {

        for(ManagerGrammar.GRAMMAR grammarElement : ManagerGrammar.GRAMMAR.values())
            if(grammarElement.toString().equals(str))
                return true;
        return false;

    }

    @Override
    public int GetNumberOfElements() { return ManagerGrammar.GRAMMAR.values().length; }

}
