import java.util.EnumMap;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

//class for parsing config file into final container
public class Config {

    //enum as tags from config file
    public enum configKey{

        BUFFER_SIZE,//size of buffer to read/write to/from file
        INPUT_FILE,//name of input file
        OUTPUT_FILE,//name of output file
        MAX_COMPRESS,//length of max compressed chunk
        MIN_COMPRESS,//length of min compressed chuck
        MODE//mode - COMPRESS or DECOMPRESS

    }

    //data
    private Errno configError;//error tracker
    final private EnumMap<configKey, String> configData;//final container

    //constructor from file
    Config(String fileName){

        configData = new EnumMap<>(configKey.class);
        File file = new File(fileName);//don't need to check exception, we'll check it in scanner
        final String separator = "=";

        Scanner scanner;
        try{

            scanner = new Scanner(file);

        }
        catch(FileNotFoundException exception){

            configError = new Errno(exception.toString());
            return;

        }


        int numberOfElements = 0;
        while(scanner.hasNext()){//read while can

            String str = scanner.nextLine();//read string
            String[] parts = str.split(separator);//separator - "="
            boolean isCorrect = false;//flag to know if first part of config string is correct

            //check if there are only two elements in line not including =
            if(parts.length != 2){

                configError = new Errno("incorrect data");
                return;

            }

            //check first
            for(configKey enumName : configKey.values()){

                if(parts[0].trim().equals(enumName.name())){

                    if(configData.containsKey(parts[0].trim())) {//if that key is already in the map

                        configError = new Errno("incorrect data: two elements in config file with the same name");
                        return;

                    }
                    configData.put(enumName, parts[1].trim());
                    numberOfElements++;
                    isCorrect = true;

                }

            }

            //check if name of first element is not pointless
            if(isCorrect == false) {

               configError = new Errno("incorrect data");
                return;

            }

        }

        //check that we fill all elements of map
        if(configData.size() != numberOfElements){

            configError = new Errno("incorrect data: not all tags in config file");
            return;

        }

        configError = new Errno();//initialization is success

        scanner.close();//we don't need this anymore

    }

    //methods
    //see if error occurred
    //return true if error happened, else false
    public boolean checkError(){

        return configError.error;

    }

    //return error message
    public String getErrorMessage(){

        return configError.message;

    }

    //method to get String data from config by tag
    public String getDataByName(configKey key){

        return configData.get(key);

    }


}
