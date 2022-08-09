package com.java_polytech.pipeline;

import com.java_polytech.common.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.IWriter;
import com.java_polytech.pipeline_interfaces.RC;

import java.io.OutputStream;
import java.io.IOException;

//class that writes to output file
public class Writer implements IWriter {

    //data
    private OutputStream outputStream = null;// outputStream that was created from file
    private byte buffer[] = null;// buffer array to keep information from executor
    private int maxBufferSize = 0;// max buffer size
    private int bufferSize = 0;// actual size of the buffer from executor

    //interface methods
    @Override
    public RC setConfig(String cfgFileName) {

        SyntaxAnalyzer sa = new SyntaxAnalyzer(RC.RCWho.WRITER, new WriterGrammar());
        RC returnCode = sa.Analyze(cfgFileName);
        if (!returnCode.isSuccess())
            return returnCode;

        //get buffer size
        String bufferSize = sa.getDataByName(WriterGrammar.GRAMMAR.BUFFER_SIZE.toString());
        if(bufferSize == null)
            return RC.RC_WRITER_CONFIG_FILE_ERROR;

        try {

            maxBufferSize = Integer.parseInt(bufferSize);

        } catch (NumberFormatException exception) {

            return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;

        }

        //check if bufferSize is correct
        if (maxBufferSize <= 0) {

            return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;

        }

        buffer = new byte[maxBufferSize];
        return RC.RC_SUCCESS;

    }

    @Override
    public RC setOutputStream(OutputStream output) {

        outputStream = output;
        return RC.RC_SUCCESS;

    }

    @Override
    public RC consume(byte[] buff) {

        if (buffer == null)
            return new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "Error in setting config.");
        if (outputStream == null)
            return new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "Error in setting output stream");

        if (buff == null) {

            if( bufferSize != 0) {

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