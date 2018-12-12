package command_processor;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ij.plugin.Macro_Runner;
class command_listener {
    private static boolean keeprunning = true;
    private static ServerSocket server = null;
    private static Socket client = null;
    private String messagein = "start";
    private String workspace = "/mnt/";
    private static DataInputStream is;
    private static PrintStream os;
    private static Queue<String> commands;
    private static ExecutorService myexecutor;
    private static ExecutorService commandparser;
    private static boolean initialized = false;
    //TCP/IP server class that listen to command for processing

    public void init(config configin) {
        try {
            server = new ServerSocket(configin.port);
            workspace = configin.workspace;
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
                String current_command[] = command.split("\\ ");
                Macro_Runner mr = new Macro_Runner();
                String args ="";
                for (int i=1;i<current_command.length;i++){
                    args+=current_command[i];
                    args+=" ";
                }
                mr.runMacroFile(workspace + current_command[0]+".ijm",args);
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
                        Thread.sleep(20);
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
