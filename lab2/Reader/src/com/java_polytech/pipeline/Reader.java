package com.java_polytech.pipeline;

import com.java_polytech.common.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.IReader;
import com.java_polytech.pipeline_interfaces.IConsumer;
import com.java_polytech.pipeline_interfaces.RC;

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;

public class Reader implements IReader {

    //data
    private InputStream inputStream = null;// inputStream that was created from file
    private byte buffer[] = null;// buffer array to keep information from input file
    private int maxBufferSize = 0;// max buffer size
    private int bufferSize = 0;// actual size of the buffer from read function
    private IConsumer consumer = null;// in fact, executor
    //interface methods

    @Override
    public RC setConfig(String cfgFileName) {

        SyntaxAnalyzer sa = new SyntaxAnalyzer(RC.RCWho.READER, new ReaderGrammar());
        RC returnCode = sa.Analyze(cfgFileName);
        if(!returnCode.isSuccess())
            return returnCode;

        //get buffer size
        String bufferSize = sa.getDataByName(ReaderGrammar.GRAMMAR.BUFFER_SIZE.toString());
        if(bufferSize == null)
            return RC.RC_READER_CONFIG_FILE_ERROR;

        try{

            maxBufferSize = Integer.parseInt(bufferSize);

        }
        catch (NumberFormatException exception){

            return RC.RC_READER_CONFIG_SEMANTIC_ERROR;

        }

        //check if bufferSize is correct
        if(maxBufferSize <= 0){

            return RC.RC_READER_CONFIG_SEMANTIC_ERROR;

        }

        buffer = new byte[maxBufferSize];
        return RC.RC_SUCCESS;

    }

    @Override
    public RC setInputStream(InputStream input) {

        inputStream = input;
        return RC.RC_SUCCESS;

    }

    @Override
    public RC setConsumer(IConsumer consumer) {

        this.consumer = consumer;
        return RC.RC_SUCCESS;

    }

    @Override
    public RC run() {

        RC returnCode;

        if(buffer == null)
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Error in setting config.");
        if(inputStream == null)
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Error in setting input stream.");
        if(consumer == null)
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Error in setting consumer.");

        try{

            bufferSize = inputStream.read(buffer);

        }
        catch(IOException exception){

            return RC.RC_READER_FAILED_TO_READ;

        }
        while(bufferSize > 0){

            //send data to consumer
            if(bufferSize != maxBufferSize)
                buffer = Arrays.copyOf(buffer, bufferSize);
            returnCode = consumer.consume(buffer);
            if(!returnCode.isSuccess())
                return returnCode;
            //read new chunk
            try{

                bufferSize = inputStream.read(buffer);

            }
            catch(IOException exception){

                return RC.RC_READER_FAILED_TO_READ;

            }

        }

        //tell consumer that it is the end of the work
        returnCode =  consumer.consume(null);
        if(!returnCode.isSuccess())
            return returnCode;

        return RC.RC_SUCCESS;

    }


}
