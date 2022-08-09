package com.java_polytech.pipeline;

import com.java_polytech.common.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.*;
import com.java_polytech.pipeline_interfaces.RC;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;

//class that writes to output file
public class Writer implements IWriter {

    //data
    private OutputStream outputStream = null;// outputStream that was created from file
    private byte buffer[] = null;// buffer array to keep information from executor
    private int maxBufferSize = 0;// max buffer size
    private int bufferSize = 0;// actual size of the buffer from executor
    private IProvider provider = null;// in fact, executor
    private IMediator mediator = null;// inner class of mainType

    //supported types
    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};
    //type for data exchange
    private TYPE mainType = null;

    //private helper methods
    private byte[] getByteArray(){

        if(mainType == TYPE.BYTE_ARRAY)
            return (byte[])mediator.getData();
        else
            if(mainType == TYPE.CHAR_ARRAY) {

                char [] charArray = (char[]) mediator.getData();
                if (charArray == null)
                    return null;

                ByteBuffer bytes = ByteBuffer.allocate(charArray.length * Character.BYTES);
                CharBuffer chars = bytes.asCharBuffer();
                chars.put(charArray);
                return bytes.array();

            }
            else
                if(mainType == TYPE.INT_ARRAY){

                    int [] intArray = (int[]) mediator.getData();
                    if (intArray == null)
                        return null;

                    ByteBuffer bytes = ByteBuffer.allocate(intArray.length * Integer.BYTES);
                    IntBuffer ints = bytes.asIntBuffer();
                    ints.put(intArray);
                    return bytes.array();

                }
                else
                    return null;

    }

    //interface methods
    @Override
    public RC setConfig(String cfgFileName) {

        SyntaxAnalyzer sa = new SyntaxAnalyzer(RC.RCWho.WRITER, new WriterGrammar());
        RC returnCode = sa.Analyze(cfgFileName);
        if (!returnCode.isSuccess())
            return returnCode;

        //get buffer size
        String bufferSize[] = sa.getDataByName(WriterGrammar.GRAMMAR.BUFFER_SIZE.toString());
        if(bufferSize == null)
            return RC.RC_WRITER_CONFIG_FILE_ERROR;

        if(bufferSize.length != 1)
            return new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "There can be only 1 element of buffer size, not less or more");
        
        try {

            maxBufferSize = Integer.parseInt(bufferSize[0]);

        } catch (NumberFormatException exception) {

            return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;

        }

        //check if bufferSize is correct
        if (maxBufferSize <= 0)
            return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;

        buffer = new byte[maxBufferSize];
        return RC.RC_SUCCESS;

    }

    @Override
    public RC setProvider(IProvider provider){

        this.provider = provider;
        TYPE[] providerTypes = provider.getOutputTypes();

        //find match
        for (TYPE consumerType: supportedTypes) {
            for (TYPE providerType: providerTypes) {
              if(consumerType.equals(providerType)){
                  mainType = consumerType;
                  break;
              }
              if(mainType != null)
                  break;
            }
        }

        //if didn't find match
        if(mainType == null)
            return RC.RC_WRITER_TYPES_INTERSECTION_EMPTY_ERROR;

        mediator = this.provider.getMediator(mainType);
        return RC.RC_SUCCESS;

    }

    @Override
    public RC setOutputStream(OutputStream output) {

        outputStream = output;
        return RC.RC_SUCCESS;

    }

    @Override
    public RC consume() {

        if (buffer == null)//can check only this to know if other stuff in config is correct
            return new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "Error in setting config.");
        if(mainType == null || mediator == null || provider == null)
            return new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "Error in handshake.");
        if (outputStream == null)
            return new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "Error in setting output stream");

        byte[] buff = getByteArray();

        if (buff == null) {

            if(bufferSize != 0) {

                try {

                    outputStream.write(buffer, 0, bufferSize);

                } catch (IOException exception) {

                    return RC.RC_WRITER_FAILED_TO_WRITE;

                }
            }
            return RC.RC_SUCCESS;
        }

        //add bytes from ba to output buffer
        for (int i = 0; i < buff.length; i++) {

            if (bufferSize == maxBufferSize) {

                try {

                    outputStream.write(buffer);

                } catch (IOException exception) {

                    return RC.RC_WRITER_FAILED_TO_WRITE;

                }
                bufferSize = 0;

            }
            buffer[bufferSize++] = buff[i];

        }

        return RC.RC_SUCCESS;

    }
}