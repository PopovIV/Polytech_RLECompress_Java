import java.io.*;
//class for RLE encoding/decoding
public class RLE {

    //enum declarations
    //enum to know what type of sequence
    enum Repeat{

        DIFFERENT_BYTES,
        SAME_BYTES

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
        public Reader input;
        public Writer output;
        public MODE mode;
        public int bufferSize;
        public int maxCompressSize;
        public int minCompressSize;
    }

    //data
    private dataFromConfig configData;
    private Errno rleError;
    //constructor from config class
    RLE(Config config){

        //const variables
        final int MAX_LENGTH = 127;//Max size of sequence to encode
        final int MIN_LENGTH = 1;//Min size of sequence to encode


        configData = new dataFromConfig();//default constructor with zeros
        if(config.checkError()){//if config wasn't initialized well

            rleError = new Errno( "config was not initialized");
            return;

        }

        //get bufferSize
        try{

            configData.bufferSize = Integer.parseInt(config.getDataByName(Config.configKey.BUFFER_SIZE));

        }
        catch (NumberFormatException exception){

            rleError = new Errno(exception.toString());
            return;

        }

        //check if bufferSize is correct
        if(configData.bufferSize <= 0){

            rleError = new Errno("buffer size must be positive");
            return;

        }

        //create Reader from file
        try{

            configData.input = new Reader(config.getDataByName(Config.configKey.INPUT_FILE), configData.bufferSize);

        }
        catch (FileNotFoundException exception){

            rleError = new Errno(exception.toString());
            return;

        }

        //create Writer from file
        try{

            configData.output = new Writer(config.getDataByName(Config.configKey.OUTPUT_FILE), configData.bufferSize);

        }
        catch (FileNotFoundException exception){

            rleError = new Errno(exception.toString());

            try{

                configData.input.close();

            }
            catch (IOException ex){

                rleError.addToError(" error with closing output file");

            }

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

        //check if minSize is correct
        if(configData.minCompressSize <  MIN_LENGTH || configData.minCompressSize > configData.maxCompressSize){

            rleError = new Errno("min compress size must be bigger than 1 and smaller then maximum compress size");
            closeStreams();
            return;

        }

        //get mode
        configData.mode = MODE.toEnum(config.getDataByName(Config.configKey.MODE));

        //means no error
        rleError = new Errno();

    }

    //methods
    //method to compress information using rle
    private void compress() {

        //const variables
        final byte HIGH_BIT_IN_8 = (byte)128;//128(decimal) == 10 000 000(binary)

        if (rleError.error) {//if initialization was not successful

            rleError = new Errno( "construction of RLE class was not successful");
            return;

        }

        byte[] dataChunk = new byte[configData.bufferSize];//array to store data from Reader
        int chunkSize;//size of dataChunk(will change in run-time)

        try {

            configData.input.updateReader();//read in Reader from file

        } catch (IOException exception) {

            rleError = new Errno("error with reading a file");
            return;

        }

        try {

            chunkSize = configData.input.fillArray(dataChunk, configData.bufferSize);//fill our array with data for compressing

        }
        catch (IOException exception){

            rleError = new Errno("error with reading a file");
            return;

        }
        //read while there is something to read or we need to process last bytes
        while (!configData.input.isRead() || chunkSize != 0) {

            //RLE ALGORITHM
            byte controlByte = 0;
            int countElem = 1;//count elem of sequence
            Repeat sequenceType = Repeat.SAME_BYTES;//variable to know what type of sequence we have right now
            for(int pos = 0; pos < chunkSize; pos++){

                sequenceType = Repeat.SAME_BYTES;
                int start = pos;

                //lets collect min pack of bytes
                while(pos + 1 < chunkSize){

                    if(sequenceType == Repeat.SAME_BYTES && dataChunk[pos] != dataChunk[pos + 1])
                        sequenceType = Repeat.DIFFERENT_BYTES;


                    countElem++;
                    pos++;

                    //when collected and can collect new min size sequence after this=> leave
                    if((countElem == configData.minCompressSize) && (chunkSize - pos >= configData.minCompressSize))
                        break;

                }


                switch (sequenceType){

                    case SAME_BYTES:

                        while(pos + 1 < chunkSize) {

                            //first condition checks if sequence is still have repeating symbols in it
                            //second condition checks that we reach max elements in sequence
                            //third condition checks if we can only create min compress size sequence
                            if (dataChunk[pos] != dataChunk[pos + 1] || countElem == configData.maxCompressSize || (chunkSize - pos) == configData.minCompressSize) {

                                //set highest bit to 1
                                controlByte = (byte)(controlByte | HIGH_BIT_IN_8);
                                //set amount of repeated bytes
                                controlByte = (byte)(controlByte | countElem);

                                //write control byte to outputStream
                                try {

                                    configData.output.writeByte(controlByte);

                                }
                                catch (IOException exception){

                                    rleError = new Errno("error with writing to a file");
                                    return;

                                }

                                //write repeated byte to outputStream
                                try {

                                    configData.output.writeByte(dataChunk[pos]);

                                }
                                catch (IOException exception){

                                    rleError = new Errno("error with writing to a file");
                                    return;

                                }

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

                        while(pos + 1 < chunkSize) {

                            //first condition checks if sequence is still have different symbols in it
                            //second condition checks that we reach max elements in sequence
                            //third condition checks if we can only create min compress size sequence
                            if (dataChunk[pos] == dataChunk[pos + 1] || countElem == configData.maxCompressSize || (chunkSize - pos - 1) == configData.minCompressSize) {

                                countElem--;
                                //set amount of repeated bytes
                                controlByte = (byte) (controlByte | countElem);

                                //write control byte to outputStream
                                try {

                                    configData.output.writeByte(controlByte);

                                }
                                catch (IOException exception){

                                    rleError = new Errno("error with writing to a file");
                                    return;

                                }

                                //write sequence bytes to outputStream
                                for (int i = start; i < start + countElem; i++) {

                                    try {

                                        configData.output.writeByte(dataChunk[i]);

                                    }
                                    catch (IOException exception){

                                        rleError = new Errno("error with writing to a file");
                                        return;

                                    }

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

                        try {

                            configData.output.writeByte(controlByte);

                        }
                        catch (IOException exception){

                            rleError = new Errno("error with writing to a file");
                            return;

                        }

                        try {

                            configData.output.writeByte(dataChunk[chunkSize - 1]);

                        }
                        catch (IOException exception){

                            rleError = new Errno("error with writing to a file");
                            return;

                        }

                        break;
                    case DIFFERENT_BYTES:
                        controlByte = (byte) (controlByte | countElem) ;

                        try {

                            configData.output.writeByte(controlByte);

                        }
                        catch (IOException exception){

                            rleError = new Errno("error with writing to a file");
                            return;

                        }

                        for (int i = chunkSize - countElem; i < chunkSize; i++) {

                            try {

                                configData.output.writeByte(dataChunk[i]);

                            } catch (IOException exception) {

                                rleError = new Errno("error with writing to a file");
                                return;

                            }

                        }
                        break;
                }
            }

            //fill array with new data
            try {

                chunkSize = configData.input.fillArray(dataChunk, configData.bufferSize);

            }
            catch (IOException exception){

                rleError = new Errno("error with reading a file");
                return;

            }

        }

    }

    //method to decompress information
    private void decompress() {

        if (rleError.error) {//if initialization was not successful

            rleError = new Errno( "construction of RLE class was not successful");
            return;

        }

        try {

            configData.input.updateReader();//read in Reader from file

        }
        catch (IOException exception) {

            rleError = new Errno("error with reading a file");
            return;

        }

        //const variables
        final byte HIGH_BIT_IN_8 = (byte)128;//128 == 10 000 000

        Repeat sequenceType;//variable to know what type of sequence we have right now
        byte controlByte;
        int sequenceSize;//length of encode sequence

        //read while there is something to read
        while (!configData.input.isRead()) {

            try{

                controlByte = configData.input.readByte();

            }
            catch (IOException exception){

                rleError = new Errno("error with reading a file");
                return;

            }

            //need to know what type of chunk
            sequenceType = (Byte.toUnsignedInt((byte)(controlByte & HIGH_BIT_IN_8)) == 0) ? Repeat.DIFFERENT_BYTES : Repeat.SAME_BYTES;
            sequenceSize = Byte.toUnsignedInt((byte)(controlByte & ~HIGH_BIT_IN_8));

            byte[] dataChunk = new byte[sequenceSize];

            switch (sequenceType){

                case DIFFERENT_BYTES:

                    try {

                        configData.input.fillArray(dataChunk, sequenceSize);

                    }
                    catch (IOException exception) {

                        rleError = new Errno("error with reading a file");
                        return;

                    }

                    for (int i = 0; i < sequenceSize; i++) {

                        try {

                            configData.output.writeByte(dataChunk[i]);

                        }
                        catch (IOException exception) {

                            rleError = new Errno("error with writing to a file");
                            return;
                        }
                    }
                    break;
               case SAME_BYTES:

                   byte byteToPrint;
                   try{

                       byteToPrint = configData.input.readByte();

                   }
                   catch (IOException exception){

                       rleError = new Errno("error with reading a file");
                       return;

                   }

                   for(int i = 0; i < sequenceSize; i++){

                       try {

                           configData.output.writeByte(byteToPrint);

                       }
                       catch (IOException exception) {

                           rleError = new Errno("error with writing to a file");
                           return;

                       }

                   }
                   break;
            }

            //read new chunk if needed
            try {

                configData.input.updateReader();

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

            configData.input.close();

        }
        catch (IOException ex){

            rleError.addToError(" error with closing input file");

        }

        try {

            configData.output.close();

        }
        catch (IOException ex){

            rleError.addToError(" error with closing output file");

        }

    }

    //method to check if error happened
    public boolean checkError(){

        return  rleError.error;

    }

    //method to get error message
    public String getErrorMessage(){

        return rleError.message;

    }

}
