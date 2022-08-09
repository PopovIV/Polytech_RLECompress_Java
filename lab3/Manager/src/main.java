import com.java_polytech.pipeline_interfaces.RC;

import java.util.logging.Logger;

public class main {
    public static void main(String[] args) {

        Logger  logger = Manager.createLogger();

        logger.severe("RLE start");

        if(args.length != 1) {

            logger.severe("ERROR: error with parameters in command line");
            System.out.println("Error happened. For more info check logger.");
            return;

        }

        RC returnCode;
        //create instance of manager
        Manager manager = new Manager();
        returnCode = manager.setConfig(args[0]);

        if(!returnCode.isSuccess()) {

            logger.severe("ERROR in " + returnCode.who + ": " + returnCode.info);
            System.out.println("Error happened. For more info check logger.");
            return;

        }
        logger.severe("RLE manager config set");

        returnCode = manager.run();
        if(!returnCode.isSuccess()) {

            logger.severe("ERROR in " + returnCode.who + ": " + returnCode.info);
            System.out.println("Error happened. For more info check logger.");
            return;

        }

        logger.severe("RLE finish");
        System.out.println("RLE successful");

    }
}