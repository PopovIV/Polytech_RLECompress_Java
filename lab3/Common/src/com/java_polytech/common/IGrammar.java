package com.java_polytech.common;

public interface IGrammar {

    //method to get separator
    String GetSeparator();
    //method to get arguments separator
    String GetArgumentsSeparator();
    //method to get comment string
    String GetCommentString();
    //method to get number of elements in grammar structure
    int GetNumberOfElements();
    //method to check if element is part of grammar structure
    boolean CheckIfElementIsInGrammar(String str);

}
