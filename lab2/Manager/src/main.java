import com.java_polytech.pipeline_interfaces.RC;

public class main {
    public static void main(String[] args) {

        if(args.length != 1)
            System.out.println("ERROR: error with parameters in command line");

        RC returnCode;
        //create instance of manager
        Manager manager = new Manager();
        returnCode = manager.setConfig(args[0]);
        if(!returnCode.isSuccess()) {

            System.out.println("ERROR in " + returnCode.who + ": " + returnCode.info);
            return;

        }

        returnCode = manager.run();
        if(!returnCode.isSuccess())
            System.out.println("ERROR in " + returnCode.who + ":" + returnCode.info);
        else
            System.out.println("RLE successful");

    }
}