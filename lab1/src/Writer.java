import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

//class that writes to output file
public class Writer {

    //data
    private final FileOutputStream outputStream;//outputStream that was created from file
    private byte buffer[];//buffer that collects data for output
    private final int maxBufferSize;//max buffer size from config file
    private int pos = 0;//position in buffer


    //constructor from path to file and max buffer size from config
    Writer(String fileName, int maxBufSize) throws FileNotFoundException {

        outputStream = new FileOutputStream(fileName);
        buffer = new byte[maxBufSize];
        maxBufferSize = maxBufSize;

    }

    //methods
    //method to add new element to buffer array
    //if it is full => write to actual outStream
    public void writeByte(byte elem)throws IOException{

        buffer[pos++]  = elem;
        //check if need to print out
        if(pos == maxBufferSize){
            outputStream.write(buffer);
            pos = 0;
        }

    }

    //method to close stream + write left data
    public void close() throws IOException{

        //check for remaining bytes in stream
        if(pos != 0)
            outputStream.write(buffer, 0, pos);
        outputStream.close();

    }
}
