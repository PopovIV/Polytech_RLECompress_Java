package com.java_polytech.pipeline;

import com.java_polytech.common.IGrammar;

public class ExecutorGrammar implements IGrammar{

    //grammar definition
    enum GRAMMAR {


        MAX_COMPRESS("MAX_COMPRESS"),// length of max compressed chunk
        MIN_COMPRESS("MIN_COMPRESS"),// length of min compressed chunk
        BUFFER_SIZE("BUFFER_SIZE"),// Size of buffer
        MODE("MODE");// mode - COMPRESS or DECOMPRESS or UNKNOWN(for incorrect input)

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

        for(ExecutorGrammar.GRAMMAR grammarElement : ExecutorGrammar.GRAMMAR.values())
            if(grammarElement.toString().equals(str))
                return true;
        return false;

    }

    @Override
    public int GetNumberOfElements() { return ExecutorGrammar.GRAMMAR.values().length; }

}
