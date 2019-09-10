package command_processor;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import ij.ImageJ;
import ij.plugin.Macro_Runner;
import javafx.util.Pair;

public class command_listener implements Runnable{
    protected config config = null;
    protected boolean isStopped= false;
    protected ServerSocket server = null;
    protected Thread runningThread = null;
    protected int ServerPort = 53705;
    protected int threadport_start = 50400;
    protected ReentrantLock printlock = new ReentrantLock();
    protected BlockingQueue<commands> commands= null;
    private class commands{
        private String commands;
        private int port;
        public commands(String commandsin,int portin){
            this.commands = commandsin;
            this.port = portin;
        }
    }
    protected BlockingQueue<Integer> ports = null;
    private static ExecutorService commandexecutor = Executors.newFixedThreadPool(5);
    private static ExecutorService scriptexecutor = Executors.newFixedThreadPool(5);
    //TCP/IP server class that listen to command for processing
    public command_listener(config configin){
        this.config = configin;
        this.ServerPort = configin.port;
    }
    public void openServerSocket() {
        try {
            InetAddress add = InetAddress.getByName(this.config.ipadd);
            this.server = new ServerSocket(config.port,10,add);
            for (int i=0;i<5;i++){
                try {
                    this.ports.put(this.threadport_start+i);
                }
                catch (InterruptedException in) {
                    in.printStackTrace();
                }
            }
            System.out.println("Server Started");
        } catch (IOException e) {
            throw new RuntimeException(("Cannot open port" + config.port));
        }
    }
    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.server.close();
        }catch (IOException e){
            throw new RuntimeException("Error closing server",e);
        }
    }
    private synchronized boolean isStopped(){
        return this.isStopped;
    }

    private void command_accept(Socket acceptedClient){
        //Accept commands from a socket and put it in the command processing queue
        try{
            InputStream is = acceptedClient.getInputStream();
            BufferedReader d = new BufferedReader(new InputStreamReader(is));
            String messagein = d.readLine();
            if (messagein.startsWith("runscript")){
                //Script command running, no need to maintain connection
                OutputStream os = acceptedClient.getOutputStream();
                os.write("Script processing command received.".getBytes());
                os.close();
                is.close();
                process_script_command(messagein);
            }
            else if (messagein.startsWith("runcommand")){
                try{
                    //TODO:Implement cases where there is no workers available, ask it to wait
                    int port = ports.take();
                    commands.put(new commands(messagein,port));
                    OutputStream os = acceptedClient.getOutputStream();
                    String response = String.format("Connect to %d\n",port);
                    os.write(response.getBytes());
                    os.close();
                    is.close();
                }
                catch (InterruptedException i){
                    i.printStackTrace();
                }
            }
            else if (messagein.startsWith("Terminate server")){
                this.stop();
            }
        } catch (IOException e){
            e.printStackTrace();
        }

    }
    class command_worker implements Runnable{
        private String command;
        private String[] commandlist;
        private int port;
        private ServerSocket socket= null;
        public command_worker(int port,String commandin){
            this.command = commandin;
            this.commandlist = command.split(" ");
            this.port = port;
            try {
                InetAddress add = InetAddress.getByName(config.ipadd);
                this.socket = new ServerSocket(this.port, 10, add);
            }
            catch(IOException e) {
                throw new RuntimeException(("Cannot open port" + this.port));
            }
        }
        public void run() {
            if (this.commandlist[1].startsWith("file_writing_request")){
                try {
                    writefile(Integer.parseInt(commandlist[2]),Integer.parseInt(commandlist[3]),Integer.parseInt(commandlist[4]),commandlist[5],this.socket);
                    this.socket.close();
                }
                catch (IOException e){
                    throw new RuntimeException("Cannot close port" + this.port);
                }
                printlock.lock();
                System.out.println("Command finished:"+command);
                printlock.unlock();
            }
            //TODO:Implement other potential functions

        }
    }
    private void writefile(int zsize,int ysize,int xsize,String filename,ServerSocket socket) throws IOException{
        Socket clientsocket = null;
        clientsocket = socket.accept();
        FileOutputStream fos = new FileOutputStream(filename);
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientsocket.getOutputStream()));
        DataInputStream in = new DataInputStream(new BufferedInputStream(clientsocket.getInputStream()));
        byte[] bytes = new byte[4096];
        int chunksize = 2*xsize*ysize;
        for (int i=0;i<zsize;i++){
            int pos = 0;
            while (pos<chunksize-1){
                in.read(bytes);
            }
            fos.write(bytes);
        }
        fos.close();
        out.write("Data received".getBytes());
        out.close();
        in.close();
        return;

    }
    public void process_script_command(final String command) {
        scriptexecutor.execute(new Runnable(){
            @Override
            public void run(){
                String[] current_command = command.split("\\ ");
                String args ="";
                for (int i=2;i<current_command.length;i++){
                    args+=current_command[i];
                    args+=" ";
                }
                args = args.trim();
                if (current_command[0].endsWith(".ijm")){
                    String pluginsDir = config.workspace;
                    System.setProperty("plugins.dir", pluginsDir);
                    ImageJ session = new ImageJ(ImageJ.NO_SHOW);
                    Macro_Runner mr = new Macro_Runner();
                    mr.runMacroFile(config.workspace + current_command[0]+".ijm",args);
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

    class command_handler implements Runnable{
            public void run() {
                while (!isStopped()) {
                    try {
                        commands current_command = commands.take();
                        commandexecutor.execute(new command_worker(current_command.port, current_command.commands));
                        printlock.lock();
                        System.out.println("Command started" + current_command.commands);
                        printlock.unlock();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        command_handler ch = new command_handler();
        Thread handlerthread = new Thread(ch);
        handlerthread.start();
        while(! isStopped()){
            Socket clientSocket = null;
            try {
                clientSocket = this.server.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    break;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }
            command_accept(clientSocket);
        }
        this.commandexecutor.shutdown();
        printlock.lock();
        System.out.println("Server Stopped.") ;
        printlock.unlock();
    }
    }


