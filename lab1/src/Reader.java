import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

//class that reads information from inputStream
public class Reader {

    //data
    private final FileInputStream inputStream;//inputStream that was created from file
    private byte buffer[];//buffer array to keep information from input file
    private int bufferSize = 0;//actual buffer size from read function
    private int pos = 0;//position in buffer


    //constructor from path to file and max buffer size from config
    Reader(String fileName, int maxBufSize) throws FileNotFoundException {

        inputStream = new FileInputStream(fileName);
        buffer = new byte[maxBufSize];

    }

    //return one byte from buffer
    public byte readByte()throws IOException{

        updateReader();
        byte byteToReturn = buffer[pos++];
        return byteToReturn;

    }

    //if we gave away all data from buffer => read new one
    public void updateReader()throws IOException{

        if(bufferSize == pos){
            bufferSize = inputStream.read(buffer);
            pos = 0;
        }

    }

    //fill array with arraySize elements of buffer(or less if we don't have that much)
    //return size of array
    public int fillArray(byte[] array, int arraySize) throws  IOException{

        for(int i = 0; i < arraySize; i++){

            updateReader();

            if(isRead())
                return i;

            array[i] = buffer[pos++];

        }

        return arraySize;

    }

    //method to check if we read all data from inputStream
    //return true if we read all data
    public boolean isRead(){

        if(bufferSize > 0)
            return false;
        else
            return true;

    }

    //method to close inputStream
    public void close() throws  IOException{

        inputStream.close();

    }

}
