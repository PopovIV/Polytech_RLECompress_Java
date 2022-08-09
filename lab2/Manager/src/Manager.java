import com.java_polytech.common.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.*;

import java.io.*;

public class Manager implements IConfigurable {

    //data
    private IReader readerClass;//reader class instance
    private IExecutor executorClass;//executor class instance
    private IWriter writerClass;//writer class instance
    private boolean isInit = false;//boolean variable to check if initialization was successful
    private FileInputStream inputFileStream = null;//input stream
    private FileOutputStream outputFileStream = null;//output stream

    //interface methods
    @Override
    public RC setConfig(String cfgFileName) {

        String tmp;// Temporary String to store data from config
        SyntaxAnalyzer sa = new SyntaxAnalyzer(RC.RCWho.MANAGER, new ManagerGrammar());
        RC returnCode = sa.Analyze(cfgFileName);
        if (!returnCode.isSuccess())
            return returnCode;

        //get input file
        tmp = sa.getDataByName(ManagerGrammar.GRAMMAR.INPUT_FILE.toString());
        if(tmp == null)
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;

        try {

            inputFileStream = new FileInputStream(tmp);

        }
        catch (FileNotFoundException exception) {

            return RC.RC_MANAGER_INVALID_INPUT_FILE;

        }
        //get output file
        tmp = sa.getDataByName(ManagerGrammar.GRAMMAR.OUTPUT_FILE.toString());
        if(tmp == null)
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;

        try {

            outputFileStream = new FileOutputStream(tmp);

        }
        catch (FileNotFoundException exception) {

            closeInputStream();
            return RC.RC_MANAGER_INVALID_OUTPUT_FILE;

        }

        //get reader class and initialize it
        try {

            String str = sa.getDataByName(ManagerGrammar.GRAMMAR.READER_NAME.toString());
            Class<?> reader = Class.forName(str);
            readerClass = (IReader)reader.getDeclaredConstructor().newInstance();

        }
        catch (Exception exception) {

            closeInputStream();
            closeOutputStream();
            return RC.RC_MANAGER_INVALID_READER_CLASS;

        }

        //get executor class
        try {

            Class<?> executor = Class.forName(sa.getDataByName(ManagerGrammar.GRAMMAR.EXECUTOR_NAME.toString()));
            executorClass = (IExecutor)executor.getDeclaredConstructor().newInstance();

        }
        catch (Exception exception) {

            closeInputStream();
            closeOutputStream();
            return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;

        }


        //get writer class and initialize it
        try {

            Class<?> writer = Class.forName(sa.getDataByName(ManagerGrammar.GRAMMAR.WRITER_NAME.toString()));
            writerClass = (IWriter)writer.getDeclaredConstructor().newInstance();

        }
        catch (Exception exception) {

            closeInputStream();
            closeOutputStream();
            return RC.RC_MANAGER_INVALID_WRITER_CLASS;

        }

        //initialize all classes
        tmp = sa.getDataByName(ManagerGrammar.GRAMMAR.READER_CONFIG.toString());
        if(tmp == null)
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;

        returnCode = readerClass.setConfig(tmp);
        if(!returnCode.isSuccess()) {
            closeInputStream();
            closeOutputStream();
            return returnCode;
        }

        tmp =sa.getDataByName(ManagerGrammar.GRAMMAR.EXECUTOR_CONFIG.toString());
        if(tmp == null)
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;

        returnCode = executorClass.setConfig(tmp);
        if(!returnCode.isSuccess()) {
            closeInputStream();
            closeOutputStream();
            return returnCode;
        }

        tmp =sa.getDataByName(ManagerGrammar.GRAMMAR.WRITER_CONFIG.toString());
        if(tmp == null)
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;

        returnCode = writerClass.setConfig(sa.getDataByName(ManagerGrammar.GRAMMAR.WRITER_CONFIG.toString()));
        if(!returnCode.isSuccess()) {
            closeInputStream();
            closeOutputStream();
            return returnCode;
        }

        //set consumer for classes
        returnCode = readerClass.setConsumer(executorClass);
        if(!returnCode.isSuccess()) {
            closeInputStream();
            closeOutputStream();
            return returnCode;
        }

        returnCode = executorClass.setConsumer(writerClass);
        if(!returnCode.isSuccess()) {
            closeInputStream();
            closeOutputStream();
            return returnCode;
        }

        //set in/out streams
        returnCode = readerClass.setInputStream(inputFileStream);
        if(!returnCode.isSuccess()) {
            closeInputStream();
            closeOutputStream();
            return returnCode;
        }

        returnCode = writerClass.setOutputStream(outputFileStream);
        if(!returnCode.isSuccess()) {
            closeInputStream();
            closeOutputStream();
            return returnCode;
        }

        isInit = true;
        return RC.RC_SUCCESS;

    }

    //methods
    //method to close input stream
    private RC closeInputStream(){

        try{

            inputFileStream.close();

        }
        catch(IOException exception){

            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Can't close input stream");

        }

        return RC.RC_SUCCESS;

    }

    //method to close output stream
    private RC closeOutputStream(){

        try{

            outputFileStream.close();

        }
        catch(IOException exception){

            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Can't close output stream");

        }

        return RC.RC_SUCCESS;

    }

    //main method that starts up pipeline
    public RC run(){

        if(!isInit) {
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Could not initialize manager class");
        }

        RC returnCode = readerClass.run();
        if(!returnCode.isSuccess()){
            closeInputStream();
            closeOutputStream();
            return returnCode;
        }

        returnCode = closeInputStream();
        if(!returnCode.isSuccess()) {
            closeOutputStream();
            return returnCode;
        }

        returnCode = closeOutputStream();
        if(!returnCode.isSuccess())
            return returnCode;

        return RC.RC_SUCCESS;

    }

}
