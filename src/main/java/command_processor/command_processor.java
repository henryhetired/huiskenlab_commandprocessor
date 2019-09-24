package command_processor;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.IOException;

public class command_processor {
    public static void main(String args[]) throws IOException{
        config configuration = new config();
        configuration.read("/home/henryhe/Documents/huiskenlab_commandprocessor/");
//        configuration.read(args[0]);
        command_listener cl = new command_listener(configuration);
        Thread main = new Thread(cl);
        main.start();
        try {
            main.join();
        }
        catch (InterruptedException it){
            it.printStackTrace();
        }
        return;
    }
}
