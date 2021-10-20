public class Main {

    public static void main(String[] args) {


        if(args.length != 1)
            System.out.println("ERROR: error with parameters in command line");

        //create instance of Config class from file
        Config config = new Config(args[0]);
        if(config.checkError()) {

            System.out.println("ERROR: " + config.getErrorMessage());
            return;

        }

        //create instance of RLE class from config
        RLE rle = new RLE(config);
        if(rle.checkError()) {

            System.out.println("ERROR: " + rle.getErrorMessage());
            return;

        }
        // encoding/decoding
        rle.process();
        if(rle.checkError()) {

            System.out.println("ERROR: " + rle.getErrorMessage());
            return;

        }


    }




}