package com.java_polytech.pipeline;

import com.java_polytech.common.SyntaxAnalyzer;
import com.java_polytech.pipeline_interfaces.IExecutor;
import com.java_polytech.pipeline_interfaces.IConsumer;
import com.java_polytech.pipeline_interfaces.RC;
import java.util.Arrays;

public class Executor implements IExecutor {

    //enum for type of sequence
    enum REPEAT{
        SAME_BYTES,
        DIFFERENT_BYTES
    }
    //enum to know what to do compress or decompress
    enum MODE{

        UNKNOWN("UNKNOWN"),
        COMPRESS("COMPRESS"),
        DECOMPRESS("DECOMPRESS");

        final private String label;

        MODE(String label){  this.label = label; }

        public String toString(){ return label;}

        static MODE toEnum(String label){

            if(label == null)
                return UNKNOWN;

            if(label.equals(COMPRESS.toString()))
                return COMPRESS;
            else
            if(label.equals(DECOMPRESS.toString()))
                return DECOMPRESS;
            else
                return UNKNOWN;

        }


    }

    //data
    private int maxCompressSize = 0;// length of max compressed chunk
    private int minCompressSize = 0;// length of min compressed chunk
    private int bufferSize = 0;// actual buffer size
    private int maxBufferSize = 0; // max buffer size
    private int remainingToDecompress = 0;// if first symbol in buffer is not control byte, this variable will show shift to it
    private REPEAT pastSequenceState = null;// used in decompress; shows type of remaining sequence
    private MODE mode = null;//  mode - COMPRESS or DECOMPRESS or UNKNOWN(for incorrect input)
    private IConsumer consumer = null;// in fact, writer
    private byte buffer[] = null;// array of bytes to work with
    //interface methods


    @Override
    public RC setConfig(String cfgFileName) {

        //const variables
        final int MAX_LENGTH = 127;// Max size of sequence to encode
        final int MIN_LENGTH = 1;// Min size of sequence to encode
        String tmp;// Temporary String to store data from config

        SyntaxAnalyzer sa = new SyntaxAnalyzer(RC.RCWho.EXECUTOR, new ExecutorGrammar());
        RC returnCode = sa.Analyze(cfgFileName);
        if(!returnCode.isSuccess())
            return returnCode;

        //get buffer size
        tmp = sa.getDataByName(ExecutorGrammar.GRAMMAR.BUFFER_SIZE.toString());
        if(tmp == null)
            return RC.RC_EXECUTOR_CONFIG_FILE_ERROR;

        try{

            maxBufferSize = Integer.parseInt(tmp);

        }
        catch (NumberFormatException exception){

            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Can't parse buffer size to integer");

        }

        //check if buffer size is correct
        if(maxBufferSize <= 0){

            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Incorrect buffer size.");

        }

        //get maximum compress size
        tmp = sa.getDataByName(ExecutorGrammar.GRAMMAR.MAX_COMPRESS.toString());
        if(tmp == null)
            return RC.RC_EXECUTOR_CONFIG_FILE_ERROR;

        try{

            maxCompressSize = Integer.parseInt(tmp);

        }
        catch (NumberFormatException exception){

            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Can't parse max compress size to integer");

        }

        //check if maxSize is correct
        if(maxCompressSize < MIN_LENGTH || maxCompressSize > MAX_LENGTH){

            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Incorrect max compress size.");

        }

        //get minimum compress size
        tmp = sa.getDataByName(ExecutorGrammar.GRAMMAR.MIN_COMPRESS.toString());
        if(tmp == null)
            return RC.RC_EXECUTOR_CONFIG_FILE_ERROR;

        try{

            minCompressSize = Integer.parseInt(tmp);

        }
        catch (NumberFormatException exception){

            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Can't parse min compress size to integer");
        }

        //check if minSize is correct
        if(minCompressSize < MIN_LENGTH ||minCompressSize > maxCompressSize){

            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Incorrect min compress size.");

        }

        //get mode
        tmp = sa.getDataByName(ExecutorGrammar.GRAMMAR.MODE.toString());
        if(tmp == null)
            return RC.RC_EXECUTOR_CONFIG_FILE_ERROR;

        mode = MODE.toEnum(tmp);

        //check if mode is correct
        if(mode == MODE.UNKNOWN)
            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Unknown mode.");

        buffer = new byte[maxBufferSize];

        return RC.RC_SUCCESS;

    }

    @Override
    public RC consume(byte[] buff) {

        if(mode == null || buffer == null)
            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Error in setting config.");
        if(consumer == null)
            return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Error in setting consumer.");

        RC returnCode;
        if(buff == null){
            //send remaining data
            if(bufferSize != 0) {
                buffer = Arrays.copyOf(buffer, bufferSize);
                returnCode = consumer.consume(buffer);
                if(!returnCode.isSuccess())
                    return returnCode;

                bufferSize = 0;
            }
            //tell that it is end
            returnCode =  consumer.consume(null);
            if(!returnCode.isSuccess())
                return returnCode;

            return RC.RC_SUCCESS;
        }

        switch (mode){
            case DECOMPRESS:
                return decompress(buff);
            case COMPRESS:
                return compress(buff);
            case UNKNOWN:
                return new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Unknown mode.");
        }

        return RC.RC_SUCCESS;

    }

    @Override
    public RC setConsumer(IConsumer consumer) {

        this.consumer = consumer;
        return RC.RC_SUCCESS;

    }

    //methods
    //method to write byte to output array
    private RC writeByte(byte b){

        buffer[bufferSize++] = b;
        if(bufferSize == maxBufferSize){

            bufferSize = 0;
            return consumer.consume(buffer);

        }

        return RC.RC_SUCCESS;

    }

    //method to compress information using rle
    private RC compress(byte[] ba) {

        //const variables
        final byte HIGH_BIT_IN_8 = (byte)128;// 128(decimal) == 10 000 000(binary)
        int pos = 0;// index of byte array
        RC returnCode;// variable to store return code from other methods
        //RLE ALGORITHM
        byte controlByte = 0;
        int countElem = 1;//count elem of sequence
        REPEAT sequenceType = REPEAT.SAME_BYTES;//variable to know what type of sequence we have right now

        //read while there is something to read or we need to process last bytes

        for (pos = 0; pos < ba.length; pos++) {

            int start = pos;
            sequenceType = REPEAT.SAME_BYTES;

            //lets collect min pack of bytes
            while (pos + 1 < ba.length) {

                if (sequenceType == REPEAT.SAME_BYTES && ba[pos] != ba[pos + 1])
                    sequenceType = REPEAT.DIFFERENT_BYTES;

                countElem++;
                pos++;

                //when collected and can collect new min size sequence after this=> leave
                if ((countElem == minCompressSize) && (ba.length - pos >= minCompressSize))
                    break;

            }

            switch (sequenceType) {

                case SAME_BYTES:

                    while (pos + 1 < ba.length) {

                        //first condition checks if sequence is still have repeating symbols in it
                        //second condition checks that we reach max elements in sequence
                        //third condition checks if we can only create min compress size sequence
                        if (ba[pos] != ba[pos + 1] || countElem == maxCompressSize || (ba.length - pos) == minCompressSize) {

                            //set highest bit to 1
                            controlByte = (byte) (controlByte | HIGH_BIT_IN_8);
                            //set amount of repeated bytes
                            controlByte = (byte) (controlByte | countElem);

                            //write control byte to outputStream
                            returnCode = writeByte(controlByte);
                            if (!returnCode.isSuccess())
                                return returnCode;

                            //write repeated byte to outputStream
                            returnCode = writeByte(ba[pos]);
                            if (!returnCode.isSuccess())
                                return returnCode;

                            //restore to begin
                            countElem = 1;
                            controlByte = 0;
                            break;

                        }
                        countElem++;
                        pos++;

                    }
                    break;

                    case DIFFERENT_BYTES:

                        while (pos + 1 < ba.length) {

                            //first condition checks if sequence is still have different symbols in it
                            //second condition checks that we reach max elements in sequence
                            //third condition checks if we can only create min compress size sequence
                            if (ba[pos] == ba[pos + 1] || countElem == maxCompressSize || (ba.length - pos - 1) == minCompressSize) {

                                countElem--;
                                //set amount of repeated bytes
                                controlByte = (byte) (controlByte | countElem);

                                //write control byte to outputStream
                                returnCode = writeByte(controlByte);
                                if (!returnCode.isSuccess())
                                    return returnCode;


                                //write sequence bytes to outputStream
                                for (int j = start; j < start + countElem; j++) {

                                    returnCode = writeByte(ba[j]);
                                    if (!returnCode.isSuccess())
                                        return returnCode;

                                }

                                //restore to begin
                                pos--;
                                countElem = 1;
                                controlByte = 0;
                                break;

                            }
                            countElem++;
                            pos++;

                        }
                        break;

                }

        }
        //add end
        if(countElem != 1){
            switch (sequenceType) {
                case SAME_BYTES:
                    controlByte = (byte) (controlByte | HIGH_BIT_IN_8);
                    //set amount of repeated bytes
                    controlByte = (byte) (controlByte | countElem);

                    returnCode = writeByte(controlByte);
                    if(!returnCode.isSuccess())
                        return returnCode;

                    returnCode = writeByte(ba[ba.length - 1]);
                    if(!returnCode.isSuccess())
                        return returnCode;

                    break;

                    case DIFFERENT_BYTES:
                        controlByte = (byte) (controlByte | countElem) ;

                        returnCode = writeByte(controlByte);
                        if(!returnCode.isSuccess())
                            return returnCode;

                        for (int j = ba.length - countElem; j < ba.length; j++) {

                            returnCode = writeByte(ba[j]);
                            if(!returnCode.isSuccess())
                                return returnCode;

                        }
                        break;
            }
        }


        return RC.RC_SUCCESS;

    }

    //method to decompress information
    private RC decompress(byte[] ba) {

        //const variables
        final byte HIGH_BIT_IN_8 = (byte)128;//128 == 10 000 000

        REPEAT sequenceType;//variable to know what type of sequence we have right now
        int i = 0;// index of byte array
        byte controlByte;
        int sequenceSize;//length of encode sequence
        RC returnCode;// variable to store return code from other methods
        int bytesToWrite = remainingToDecompress;
        remainingToDecompress = 0;

        //read remaining
        if(bytesToWrite >= ba.length && pastSequenceState == REPEAT.DIFFERENT_BYTES){
            remainingToDecompress = bytesToWrite - ba.length;
            bytesToWrite = ba.length;
        }

        if(bytesToWrite != 0){
            switch (pastSequenceState) {
                case DIFFERENT_BYTES:
                    for (i = 0; i < bytesToWrite; i++) {
                        returnCode = writeByte(ba[i]);
                        if (!returnCode.isSuccess())
                            return returnCode;
                    }
                    break;
                case SAME_BYTES:
                    for(int j = 0; j < bytesToWrite; j++){
                        returnCode = writeByte(ba[i]);
                        if (!returnCode.isSuccess())
                            return returnCode;
                    }
                    i++;
                    break;
            }
        }

        //read while there is something to read
        while (i < ba.length) {

            controlByte = ba[i++];
            //need to know what type of chunk
            sequenceType = (Byte.toUnsignedInt((byte)(controlByte & HIGH_BIT_IN_8)) == 0) ? REPEAT.DIFFERENT_BYTES :REPEAT.SAME_BYTES;
            sequenceSize = Byte.toUnsignedInt((byte)(controlByte & ~HIGH_BIT_IN_8));
            pastSequenceState = sequenceType;

            if(i + sequenceSize >= ba.length && sequenceType == REPEAT.DIFFERENT_BYTES) {
                remainingToDecompress = (i + sequenceSize) - ba.length;
                sequenceSize = ba.length - i;
            }
            if(i == ba.length && sequenceType == REPEAT.SAME_BYTES){
                remainingToDecompress = sequenceSize;
                break;
            }

            switch (sequenceType){

                case DIFFERENT_BYTES:

                    int t = i + sequenceSize;
                    for (; i < t; i++) {

                        returnCode = writeByte(ba[i]);
                        if(!returnCode.isSuccess())
                            return returnCode;
                    }
                    break;

                case SAME_BYTES:

                    for(int j = 0; j < sequenceSize; j++){

                        returnCode = writeByte(ba[i]);
                        if(!returnCode.isSuccess())
                            return returnCode;

                    }
                    i++;
                    break;
            }


        }

        return RC.RC_SUCCESS;

    }


}
