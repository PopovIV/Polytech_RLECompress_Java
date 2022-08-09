import com.java_polytech.common.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.*;

import java.io.*;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Manager implements IConfigurable {

    //data
    private IReader readerClass = null;//reader class instance
    private IExecutor[] executorClasses = null;//executor classes array instance
    private IWriter writerClass = null;//writer class instance
    private boolean isInit = false;//boolean variable to check if initialization was successful
    private FileInputStream inputFileStream = null;//input stream
    private FileOutputStream outputFileStream = null;//output stream


    //helper methods

    //function to create logger
    static final String loggerFileName = "log.txt";

    public static Logger createLogger(){

        Logger logger = Logger.getLogger("Logger");
        FileHandler fileHandler;

        try{

            fileHandler = new FileHandler(loggerFileName);

        }
        catch(IOException exception){

            return null;

        }
        SimpleFormatter simpleFormatter = new SimpleFormatter();
        fileHandler.setFormatter(simpleFormatter);
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);

        return logger;

    }

    //function to reduce usage close input/output streams to 1
    //in this function we get everything from syntax analyzer except streams
    //initialize it
    //and build connections between elements of pipeline
    private RC buildPipeline(SyntaxAnalyzer sa){

        if(sa == null)
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "There can be only 1 element of reader class, not less or more");

        RC returnCode;
        String[] tmp;// Temporary String to store data from config

        //get reader class and initialize it
        try {

            tmp = sa.getDataByName(ManagerGrammar.GRAMMAR.READER_NAME.toString());
            if(tmp == null)
                return RC.RC_MANAGER_CONFIG_FILE_ERROR;
            if(tmp.length != 1)
                return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "There can be only 1 element of reader class, not less or more");
            Class<?> reader = Class.forName(tmp[0]);
            readerClass = (IReader)reader.getDeclaredConstructor().newInstance();

        }
        catch (Exception exception) {

            return RC.RC_MANAGER_INVALID_READER_CLASS;

        }

        //get executor class
        tmp = sa.getDataByName(ManagerGrammar.GRAMMAR.EXECUTOR_NAME.toString());
        if(tmp == null)
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;

        executorClasses = new IExecutor[tmp.length];
        for (int i = 0; i < tmp.length; i++) {

            try{
                Class<?> executor = Class.forName(tmp[i]);
                executorClasses[i] = (IExecutor)executor.getDeclaredConstructor().newInstance();
            }
            catch (Exception exception) {

                return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;

            }


        }

        //get writer class and initialize it
        try {

            tmp = sa.getDataByName(ManagerGrammar.GRAMMAR.WRITER_NAME.toString());

            if(tmp == null)
                return RC.RC_MANAGER_CONFIG_FILE_ERROR;

            if(tmp.length != 1)
                return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "There can be only 1 element of writer class, not less or more");

            Class<?> writer = Class.forName(tmp[0]);
            writerClass = (IWriter)writer.getDeclaredConstructor().newInstance();

        }
        catch (Exception exception) {

            return RC.RC_MANAGER_INVALID_WRITER_CLASS;

        }

        //initialize all classes
        tmp = sa.getDataByName(ManagerGrammar.GRAMMAR.READER_CONFIG.toString());
        if(tmp == null)
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;

        if(tmp.length != 1)
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "There can be only 1 element of reader config, not less or more");

        returnCode = readerClass.setConfig(tmp[0]);
        if(!returnCode.isSuccess())
            return returnCode;

        tmp = sa.getDataByName(ManagerGrammar.GRAMMAR.EXECUTOR_CONFIG.toString());

        if(tmp == null)
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;

        if(tmp.length != executorClasses.length)
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Not match in number of executors and it's configs");

        for(int i = 0; i < tmp.length; i++) {
            returnCode = executorClasses[i].setConfig(tmp[i]);
            if (!returnCode.isSuccess())
                return returnCode;
        }

        tmp = sa.getDataByName(ManagerGrammar.GRAMMAR.WRITER_CONFIG.toString());

        if(tmp == null)
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;

        if(tmp.length != 1)
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "There can be only 1 element of writer config, not less or more");

        returnCode = writerClass.setConfig(tmp[0]);
        if(!returnCode.isSuccess())
            return returnCode;

        //set consumer for classes
        returnCode = readerClass.setConsumer(executorClasses[0]);
        if(!returnCode.isSuccess())
            return returnCode;

        for(int i = 0; i < executorClasses.length - 1; i++) {
            returnCode = executorClasses[i].setConsumer(executorClasses[i + 1]);
            if (!returnCode.isSuccess())
                return returnCode;
        }

        returnCode = executorClasses[executorClasses.length - 1].setConsumer(writerClass);
        if (!returnCode.isSuccess())
            return returnCode;

        //set in/out streams
        returnCode = readerClass.setInputStream(inputFileStream);
        if(!returnCode.isSuccess())
            return returnCode;

        returnCode = writerClass.setOutputStream(outputFileStream);
        if(!returnCode.isSuccess())
            return returnCode;

       return RC.RC_SUCCESS;

    }

    //interface methods
    @Override
    public RC setConfig(String cfgFileName) {

        String[] tmp;// Temporary String to store data from config
        SyntaxAnalyzer sa = new SyntaxAnalyzer(RC.RCWho.MANAGER, new ManagerGrammar());
        RC returnCode = sa.Analyze(cfgFileName);
        if (!returnCode.isSuccess())
            return returnCode;

        //get input file
        tmp = sa.getDataByName(ManagerGrammar.GRAMMAR.INPUT_FILE.toString());
        if(tmp == null)
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;

        if(tmp.length != 1)
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "There can be only 1 input file, not less or more");

        try {

            inputFileStream = new FileInputStream(tmp[0]);

        }
        catch (FileNotFoundException exception) {

            return RC.RC_MANAGER_INVALID_INPUT_FILE;

        }
        //get output file
        tmp = sa.getDataByName(ManagerGrammar.GRAMMAR.OUTPUT_FILE.toString());
        if(tmp == null)
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;

        if(tmp.length != 1)
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "There can be only 1 output file, not less or more");

        try {

            outputFileStream = new FileOutputStream(tmp[0]);

        }
        catch (FileNotFoundException exception) {

            closeInputStream();
            return RC.RC_MANAGER_INVALID_OUTPUT_FILE;

        }
        //build pipeline
        returnCode = buildPipeline(sa);
        if(!returnCode.isSuccess())
            return returnCode;

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

        if(!isInit)
            return new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Could not initialize manager class");

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
