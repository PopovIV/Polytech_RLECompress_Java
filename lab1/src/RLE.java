import java.io.*;
import  java.util.Vector;

public class RLE {

    //enum declarations
    enum Repeat{

        DIFFERENT_BYTES,
        SAME_BYTES

    }

    enum MODE{

        UNKNOWN("UNKNOWN"),
        COMPRESS("COMPRESS"),
        DECOMPRESS("DECOMPRESS");

        final private String label;

        MODE(String label){  this.label = label; }

        public String toString(){ return label;}

        static MODE toEnum(String label){

            if(label.equals(COMPRESS.toString()))
                return COMPRESS;
            else
                if(label.equals(DECOMPRESS.toString()))
                    return DECOMPRESS;
                else
                    return UNKNOWN;

        }


    }

    //data from config class
    final private class dataFromConfig {
        public FileInputStream inputStream;
        public FileOutputStream outputStream;
        public MODE mode;
        public int bufferSize;
        public int maxCompressSize;
        public int minCompressSize;
    }

    //data
    private dataFromConfig configData;
    private Errno rleError;
    //constructor
    RLE(Config config){

        //const variables
        final int MAX_LENGTH = 127;
        final int MIN_LENGTH = 1;


        configData = new dataFromConfig();//default constructor with zeros
        if(config.checkError()){//if config wasn't initialized well

            rleError = new Errno( "config was not initialized");
            return;

        }

        //open input file
        File inputFile = new File(config.getDataByName(Config.configKey.INPUT_FILE));
        try{

            configData.inputStream = new FileInputStream(inputFile);

        }
        catch (FileNotFoundException exception){

            rleError = new Errno(exception.toString());
            return;

        }

        //open output file
        File outputFile = new File(config.getDataByName(Config.configKey.OUTPUT_FILE));
        try{

            configData.outputStream = new FileOutputStream(outputFile);

        }
        catch (FileNotFoundException exception){

            rleError = new Errno(exception.toString());
            try {

                configData.inputStream.close();

            }
           catch (IOException ex){

               rleError.addToError(" + error with closing " + config.getDataByName(Config.configKey.INPUT_FILE));

            }
            return;

        }

        //get bufferSize
        try{

            configData.bufferSize = Integer.parseInt(config.getDataByName(Config.configKey.BUFFER_SIZE));

        }
        catch (NumberFormatException exception){

            rleError = new Errno(exception.toString());
            closeStreams();
            return;

        }

        //check if buggerSize is correct
        if(configData.bufferSize <= 0){

            rleError = new Errno("buffer size must be positive");
            closeStreams();
            return;

        }

        //get maximum compress size
        try{

            configData.maxCompressSize = Integer.parseInt(config.getDataByName(Config.configKey.MAX_COMPRESS));

        }
        catch (NumberFormatException exception){

            rleError = new Errno(exception.toString());
            closeStreams();
            return;

        }

        //check if maxSize is correct
        if(configData.maxCompressSize < MIN_LENGTH || configData.maxCompressSize > MAX_LENGTH){

            rleError = new Errno("max compress size must be number between 2 and 127");
            closeStreams();
            return;

        }

        //get minimum compress size
        try{

            configData.minCompressSize = Integer.parseInt(config.getDataByName(Config.configKey.MIN_COMPRESS));

        }
        catch (NumberFormatException exception){

            rleError = new Errno(exception.toString());
            closeStreams();
            return;

        }

        //check if maxSize is correct
        if(configData.minCompressSize <= 0 || configData.minCompressSize > configData.maxCompressSize){

            rleError = new Errno("min compress size must be bigger than 1 and smaller then maximum compress size");
            closeStreams();
            return;

        }

        //get mode
        configData.mode = MODE.toEnum(config.getDataByName(Config.configKey.MODE));

        rleError = new Errno();

    }

    //methods
    private void compress() {

        //const variables
        final int HIGH_BIT_IN_8 = 128;//128 == 10 000 000
        final int MAX_LENGTH_TO_OUTPUT = 255;

        if (rleError.error)//if initialization was not successful
            return;

        byte[] dataChunk = new byte[configData.bufferSize];
        byte[] rleData = new byte[configData.bufferSize * 2];

        int totalNumberOfBytes = 0;
        try {

            totalNumberOfBytes = configData.inputStream.read(dataChunk);

        } catch (IOException exception) {

            rleError = new Errno("error with reading a file");
            return;

        }
        //read while there is something to read
        while (totalNumberOfBytes != -1) {//-1 means no more data == end of file

            //RLE ALGORITHM
            Byte auxByte = 0;
            int countElem = 1;
            int dataSize = 0;
            Repeat repeat = Repeat.SAME_BYTES;
            for(int i = 0; i < totalNumberOfBytes;i++){

                repeat = Repeat.SAME_BYTES;
                int start = i;
                //lets collect min pack of bytes
                while(i + 1 < totalNumberOfBytes){

                    if(repeat == Repeat.SAME_BYTES && dataChunk[i] != dataChunk[i + 1])
                        repeat = Repeat.DIFFERENT_BYTES;


                    countElem++;
                    i++;

                    if((countElem == configData.minCompressSize) && (totalNumberOfBytes - i >= configData.minCompressSize))
                        break;

                }


                switch (repeat){
                    case SAME_BYTES:
                        while(i + 1 < totalNumberOfBytes) {

                            //first condition checks if we still can create min compress size array of bytes
                            if ((totalNumberOfBytes - i) == configData.minCompressSize || countElem == configData.maxCompressSize || dataChunk[i] != dataChunk[i + 1]) {

                                //set highest bit to 1
                                auxByte = (byte) (auxByte | HIGH_BIT_IN_8);
                                //set amount of repeated bytes
                                auxByte = (byte) (auxByte | countElem);

                                rleData[++dataSize] = auxByte;
                                rleData[++dataSize] = dataChunk[i];
                                //restore to begin
                                countElem = 1;
                                auxByte = 0;
                                break;

                            }
                            countElem++;
                            i++;


                        }
                        break;

                        case DIFFERENT_BYTES:

                        while(i + 1 < totalNumberOfBytes) {


                            if ((totalNumberOfBytes - i - 1) == configData.minCompressSize || countElem == configData.maxCompressSize || dataChunk[i] == dataChunk[i + 1]) {

                                countElem--;
                                //set amount of repeated bytes
                                auxByte = (byte) (auxByte | countElem);

                                rleData[++dataSize] = auxByte;
                                for (int j = start; j < start + countElem; j++)
                                    rleData[++dataSize] = dataChunk[j];

                                //restore to begin
                                i--;
                                countElem = 1;
                                auxByte = 0;
                                break;

                            }
                            countElem++;
                            i++;
                        }

                        break;

                }


            }
            //add end
            if(countElem != 1){
                switch (repeat) {
                    case SAME_BYTES:
                        auxByte = (byte) (auxByte | HIGH_BIT_IN_8);
                        //set amount of repeated bytes
                        auxByte = (byte) (auxByte | countElem);

                        rleData[++dataSize] = auxByte;
                        rleData[++dataSize] = dataChunk[totalNumberOfBytes - 1];
                        break;
                    case DIFFERENT_BYTES:
                        auxByte = (byte) (auxByte | countElem) ;

                        rleData[++dataSize] = auxByte;
                        for (int j = totalNumberOfBytes - countElem; j < totalNumberOfBytes; j++)
                            rleData[++dataSize] = dataChunk[j];
                        break;
                }
            }
            //set first elem of rleData - size of chunk to read in decompress
            rleData[0] = (byte)(dataSize++);


            //write to outputStream
            if(!writeToOutStream(rleData, dataSize, configData.bufferSize, configData.outputStream)) {

                rleError = new Errno("error with writing to  file");
                return;

            }

            //read new chunk
            try {

                totalNumberOfBytes = configData.inputStream.read(dataChunk);

            } catch (IOException exception) {

                rleError = new Errno("error with reading a file");
                return;

            }

        }

    }

    static private boolean writeToOutStream(byte[] rleData, int dataSize, int bufferSize, FileOutputStream outputStream){

        byte[] outputChunk = new byte[bufferSize];
        int index = 0, pos;
        for (pos = 0; pos < dataSize; pos++) {

            outputChunk[index++] = rleData[pos];

            if(index == bufferSize) {

                try {

                    outputStream.write(outputChunk);

                }
                catch (IOException exception) {

                    return false;

                }

                index = 0;

            }

        }
        //write end
        if(index != 0) {

            try {

                outputStream.write(outputChunk, 0, index );

            }
            catch (IOException exception) {

                return false;

            }

        }

        return true;

    }

    private void decompress() {

        if (rleError.error)//if initialization was not successful
            return;


        int numberOfBytesToRead;
        try {

            numberOfBytesToRead = configData.inputStream.read();

        } catch (IOException exception) {

            rleError = new Errno("error with reading a file");
            return;

        }
        //read while there is something to read
        int mostSignByte;
        while (numberOfBytesToRead != -1) {//-1 means no more data == end of file

            byte[] dataChunk = new byte[numberOfBytesToRead];/////
            Vector<Byte> result = new Vector<>();
            try {

                configData.inputStream.read(dataChunk);

            } catch (IOException exception) {

                rleError = new Errno("error with reading a file");
                return;

            }


            for(int i = 0; i < numberOfBytesToRead;){

                byte byt = dataChunk[i];//get our byte
                mostSignByte = Byte.toUnsignedInt((byte)(byt & 128));//
                int shift = Byte.toUnsignedInt((byte)(byt & ~128));


                if(mostSignByte == 0){//if there is no repeat
                    for(int j = i + 1; j < i + 1 + shift; j++)
                          result.add(dataChunk[j]);
                    i = i + shift + 1;
                }
                else{
                    for(int t = 0; t < shift; t++)
                          result.add(dataChunk[i + 1]);
                    i = i + 2;
                }

            }


            //write to outputStream
            byte[] outputChunk = new byte[configData.bufferSize];
            int index = 0, pos;
            for (pos = 0; pos < result.size(); pos++) {

                outputChunk[index] = result.elementAt(pos);
                index++;
                if(index == configData.bufferSize) {

                    try {

                        configData.outputStream.write(outputChunk, 0, index);

                    }
                    catch (IOException exception) {

                        rleError = new Errno("error with writing to a file");
                        return;

                    }

                    index = 0;

                }

            }
            //write end
            if(index != 0) {

                try {

                    configData.outputStream.write(outputChunk, 0, index);

                }
                catch (IOException exception) {

                    rleError = new Errno("error with writing to a file");
                    return;

                }

            }

            //read new size of chunk
            try {

                numberOfBytesToRead = configData.inputStream.read();


            } catch (IOException exception) {

                rleError = new Errno("error with reading a file");
                return;

            }

        }


    }

    //main class method
    public void process(){

        if(rleError.error)
            return;

        switch (configData.mode) {
            case COMPRESS:
                compress();
                break;
            case DECOMPRESS:
                decompress();
                break;
        }

        closeStreams();

    }

    //function to not repeat the same code
    //tries to close input and output streams
    //if not success -> error
    private void closeStreams(){

        try {

            configData.inputStream.close();

        }
        catch (IOException ex){

            rleError.addToError(" error with closing input file");

        }

        try {

            configData.outputStream.close();

        }
        catch (IOException ex){

            rleError.addToError(" error with closing output file");

        }

    }

    public boolean checkError(){

        return  rleError.error;

    }

    public String getErrorMessage(){

        return rleError.message;

    }


}
