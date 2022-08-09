package com.java_polytech.common;

import com.java_polytech.pipeline_interfaces.RC;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

//class for syntax analysis of config files
public class SyntaxAnalyzer {

    //data
    RC.RCWho who;
    IGrammar grammar;// instance of IGrammar interface(will become Reader/Writer/Executor/ManagerGrammar class
    private final HashMap<String, String[]> configData;// final container for grammar of one of our classes

    //constructor
    public SyntaxAnalyzer(RC.RCWho who, IGrammar ig) {

        this.who = who;
        grammar = ig;
        configData = new HashMap<>();

    }

    //method to get data from config by name
    //will return null if no object with that key is in config
    public String[] getDataByName(String name){
        return configData.get(name);
    }

    //main function for syntax analysis
    public RC Analyze(String fileName){

        File file = new File(fileName);//don't need to check exception, we'll check it in scanner
        Scanner scanner;
        try {

            scanner = new Scanner(file);

        } catch (FileNotFoundException exception) {

            return new RC(who, RC.RCType.CODE_CONFIG_FILE_ERROR, "Could not open config file");

        }

        int numberOfElements = 0;
        while (scanner.hasNext()) {//read while can

            String str = scanner.nextLine().trim();//read string and delete spaces from left and right

            //if it is empty string or comment => read next line
            if(str.length() == 0 || str.startsWith(grammar.GetCommentString()))
                continue;

            String[] parts = str.split(grammar.GetSeparator());
            boolean isCorrect = false;//flag to know if first part of config string is correct

            //check if there are only two elements in line not including =
            if (parts.length != 2)
                return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "Incorrect config format.");


            //check first
            parts[0] = parts[0].trim();
            if (grammar.CheckIfElementIsInGrammar(parts[0])) {

                    if (configData.containsKey(parts[0]))//if that key is already in the map
                        return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "Incorrect data: two elements in config file with the same name.");

                    String[] argumentsArray = parts[1].split(grammar.GetArgumentsSeparator());
                    if(argumentsArray.length == 0)
                        return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "Incorrect data: incorrect format of arguments.");

                    //delete all spaces
                    for (int i = 0; i < argumentsArray.length; i++)
                        argumentsArray[i] = argumentsArray[i].trim();

                    configData.put(parts[0], argumentsArray);
                    numberOfElements++;
                    isCorrect = true;

            }


            //check if name of first element is not pointless
            if (isCorrect == false)
                return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "Incorrect data in config tags.");

        }

        //check that we fill all elements of map
        if (grammar.GetNumberOfElements() != numberOfElements)
            return new RC(who, RC.RCType.CODE_CONFIG_GRAMMAR_ERROR, "Incorrect data: not all tags in config file.");

        scanner.close();//we don't need this anymore
        return RC.RC_SUCCESS;

    }
}
