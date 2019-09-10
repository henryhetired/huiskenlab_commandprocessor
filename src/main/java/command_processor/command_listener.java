package command_processor;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ij.ImageJ;
import ij.plugin.Macro_Runner;
public class command_listener implements Runnable{
    protected boolean isStopped= false;
    protected ServerSocket server = null;
    protected int ServerPort = 53705;
    private static Queue<String> commands;
    private static ExecutorService myexecutor = Executors.newFixedThreadPool(5);
    private static ExecutorService commandparser = Executors.newFixedThreadPool(2);
    //TCP/IP server class that listen to command for processing
    public void init(config configin) {
        try {
            InetAddress add = InetAddress.getByName(configin.ipadd)
            server = new ServerSocket(configin.port,10,add);
            String workspace = configin.workspace;
            System.out.println("Server Started");
        } catch (IOException e) {
            e.printStackTrace();
        }
        myexecutor = Executors.newFixedThreadPool(5);
        commandparser = Executors.newFixedThreadPool(2);
        initialized = true;
        commands = new LinkedList<String>();
    }

    public void process_command(final String command) {
        myexecutor.execute(new Runnable(){
            @Override
            public void run(){
                String[] current_command = command.split("\\ ");
                String args ="";
                for (int i=1;i<current_command.length;i++){
                    args+=current_command[i];
                    args+=" ";
                }
                if (current_command[0].endsWith(".ijm")){
                    String pluginsDir = workspace;
                    System.setProperty("plugins.dir", pluginsDir);
                    ImageJ session = new ImageJ(ImageJ.NO_SHOW);
                    Macro_Runner mr = new Macro_Runner();
                    mr.runMacroFile(workspace + current_command[0]+".ijm",args);
                    session.quit();
                }
                else if (current_command[0].endsWith(".py")){
                    String command = "python "+current_command[0]+" ";
                    try{
                    Process p = Runtime.getRuntime().exec(command+args);}
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                System.gc();
                System.out.println("Command finished");
                return;
            }
        });
    }

    public void command_handler() {
        commandparser.execute(new Runnable() {
            @Override
            public void run() {
                String current_command;
                while (keeprunning) {
                    try {
                        current_command = commands.remove();
                        process_command(current_command);
                        System.out.println(current_command);
                        System.out.println("Command started");
                    } catch (NoSuchElementException e) {
                        continue;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                return;
            }
        });
    }

    public void run() {
        command_handler();
        if (initialized) {
            while (!messagein.contains("disconnect")) {
                try {
                    client = server.accept();
                    System.out.println("client connected" + client.getInetAddress());
                    is = new DataInputStream(client.getInputStream());
                    BufferedReader d = new BufferedReader(new InputStreamReader(is));
                    messagein = d.readLine();
                    commands.add(messagein);
                    System.out.println("commands queued");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            keeprunning = false;
            commandparser.shutdown();
            myexecutor.shutdown();
            System.out.println("Server stopped");
            return;
        }
    }
}
