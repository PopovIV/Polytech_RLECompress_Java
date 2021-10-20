//class for tracking errors
public class Errno {

    //data
    public boolean error;//if error occurred => true, else => false
    public String message;//message about error

    //default constructor(for no error)
    Errno(){

        error = false;
        message = "";

    }
    //constructor from String(if error happened)
    Errno(String msg){

        error = true;
        message = msg;

    }

    //method to add new error to existing message
    public void addToError(String msg){

        if(!error){

            error = true;
            message = msg;

        }
        else {
            message += "+" + msg;
        }
    }


}