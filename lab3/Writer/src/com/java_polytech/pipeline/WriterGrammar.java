package com.java_polytech.pipeline;

import com.java_polytech.common.IGrammar;

public class WriterGrammar implements IGrammar {

    //grammar definition
    enum GRAMMAR {

        BUFFER_SIZE("BUFFER_SIZE");// Size of buffer

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

        for(WriterGrammar.GRAMMAR grammarElement : WriterGrammar.GRAMMAR.values())
            if(grammarElement.toString().equals(str))
                return true;
        return false;

    }

    @Override
    public int GetNumberOfElements() { return WriterGrammar.GRAMMAR.values().length; }

}
