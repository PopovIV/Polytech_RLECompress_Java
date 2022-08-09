package com.java_polytech.pipeline;

import com.java_polytech.common.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.*;

import javax.print.DocFlavor;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

public class Reader implements IReader {

    //data
    private InputStream inputStream = null;// inputStream that was created from file
    private byte buffer[] = null;// buffer array to keep information from input file
    private int maxBufferSize = 0;// max buffer size
    private int bufferSize = 0;// actual size of the buffer from read function
    private IConsumer consumer = null;// in fact, executor

    //supported types
    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};
    //interface methods

    //class that implements IMediator for bytes array
    private class byteMediator implements IMediator {
        //interface methods
        @Override
        public Object getData() {

            if(bufferSize <= 0)
              return null;

            byte[] buf = new byte[bufferSize];
            System.arraycopy(buffer, 0, buf, 0, bufferSize);
            return buf;

        }
    }
    //class that implements IMediator for chars array
    private class charMediator implements IMediator {
        //interface methods
        @Override
        public Object getData() {

            if(bufferSize <= 0)
                return null;

            byte[] buf = new byte[bufferSize];
            System.arraycopy(buffer, 0, buf, 0, bufferSize);

            CharBuffer charBuf = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN).asCharBuffer();
            char[] charArray = new char[charBuf.remaining()];
            charBuf.get(charArray);
            return charArray;

        }
    }

    //class that implements IMediator for int array
    private class integerMediator implements IMediator {
        //interface methods
        @Override
        public Object getData() {

            if(bufferSize <= 0)
                return null;

            byte[] buf = new byte[bufferSize];
            System.arraycopy(buffer, 0, buf, 0, bufferSize);

            IntBuffer intBuf = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
            int[] intArray = new int[intBuf.remaining()];
            intBuf.get(intArray);
            return intArray;

        }
    }

    @Override
    public RC setConfig(String cfgFileName) {

        SyntaxAnalyzer sa = new SyntaxAnalyzer(RC.RCWho.READER, new ReaderGrammar());
        RC returnCode = sa.Analyze(cfgFileName);
        if(!returnCode.isSuccess())
            return returnCode;

        //get buffer size
        String[] bufferSize = sa.getDataByName(ReaderGrammar.GRAMMAR.BUFFER_SIZE.toString());
        if(bufferSize == null)
            return RC.RC_READER_CONFIG_FILE_ERROR;

        if(bufferSize.length != 1)
            return new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "There can be only 1 element of buffer size, not less or more");

        try{

            maxBufferSize = Integer.parseInt(bufferSize[0]);

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
        return consumer.setProvider(this);

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
            returnCode = consumer.consume();
            if(!returnCode.isSuccess()) {
                buffer = null;
                bufferSize = 0;
                consumer.consume();//
                return returnCode;
            }
            //read new chunk
            try{

                bufferSize = inputStream.read(buffer);

            }
            catch(IOException exception){

                return RC.RC_READER_FAILED_TO_READ;

            }

        }

        //tell consumer that it is the end of the work
        bufferSize = 0;
        returnCode =  consumer.consume();
        if(!returnCode.isSuccess())
            return returnCode;

        return RC.RC_SUCCESS;

    }

    @Override
    public TYPE[] getOutputTypes() { return supportedTypes; }

    @Override
    public IMediator getMediator(TYPE type) {

        if(type == TYPE.CHAR_ARRAY)
            return new charMediator();
        else if(type == TYPE.BYTE_ARRAY)
            return new byteMediator();
        else if(type == TYPE.INT_ARRAY)
            return new integerMediator();
        else
            return null;

    }

}
