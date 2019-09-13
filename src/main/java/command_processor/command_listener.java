package command_processor;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
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
    protected BlockingQueue<commands> commands= new ArrayBlockingQueue<commands>(50);
    private class commands{
        private String commands;
        private int port;
        public commands(String commandsin,int portin){
            this.commands = commandsin;
            this.port = portin;
        }
    }
    protected BlockingQueue<Integer> ports = new ArrayBlockingQueue<Integer>(50);
    private ExecutorService commandexecutor = Executors.newFixedThreadPool(5);
    private ExecutorService scriptexecutor = Executors.newFixedThreadPool(5);
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
            System.out.println("Listener socket closed");
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
            printlock.lock();
            System.out.println("New connection");
            System.out.println("Message receieved at "+acceptedClient.getPort()+":"+messagein);
            printlock.unlock();
            if (messagein.startsWith("runscript")){
                //Script command running, no need to maintain connection
                OutputStream os = acceptedClient.getOutputStream();
                os.write("Script processing command received.".getBytes());
                os.close();
                is.close();
                process_script_command(messagein);
                acceptedClient.close();
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
                    acceptedClient.close();
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
        public command_worker(int port,String commandin){
            this.command = commandin;
            this.commandlist = command.split(" ");
            this.port = port;
        }
        public void run() {
            if (this.commandlist[1].startsWith("filewritingrequest")){
                try {
                    writefile(Integer.parseInt(commandlist[2]),Integer.parseInt(commandlist[3]),Integer.parseInt(commandlist[4]),commandlist[5],this.port);
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
    private void writefile(int zsize,int ysize,int xsize,String filename,int port) throws IOException{
        InetAddress add = InetAddress.getByName(config.ipadd);
        ServerSocket socket = new ServerSocket(port, 10, add);
        Socket clientsocket;
        clientsocket = socket.accept();
        FileOutputStream fos = new FileOutputStream(filename);
//        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientsocket.getOutputStream()));
        DataInputStream in = new DataInputStream(new BufferedInputStream(clientsocket.getInputStream()));
        int chunksize = 2*xsize*ysize;
        byte[] frame = new byte[chunksize];
        long t0 = System.currentTimeMillis();
        for (int i=0;i<zsize;i++){
            int pos = 0;
            while (pos<chunksize-1){
                int len = in.read(frame,pos,chunksize-pos);
                pos+= len;
            }
            fos.write(frame);
        }
        fos.close();
//        out.write("Data received".getBytes());
//        out.close();
        long t1 = System.currentTimeMillis();
        System.out.println((long)zsize*(long)xsize*(long)ysize*2d/(double)(t1-t0));
        System.out.printf("Data transfer speed: %f MB/s\n", (long)zsize*(long)xsize*(long)ysize*2d/(double)(t1-t0)/1000d/1024d/1024d);
        in.close();
        socket.close();
        clientsocket.close();
        try{
            ports.put(port);
        }
        catch (InterruptedException it){
            it.printStackTrace();
        }
        return;
    }
    private void writefilelargebuffer(int zsize,int ysize,int xsize,String filename,int port) throws IOException{

        byte[][] data = new byte[zsize][zsize*ysize*2];
        InetAddress add = InetAddress.getByName(config.ipadd);
        ServerSocket socket = new ServerSocket(port, 10, add);
        Socket clientsocket;
        clientsocket = socket.accept();
        long t0 = System.currentTimeMillis();
        FileOutputStream fos = new FileOutputStream(filename);
//        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientsocket.getOutputStream()));
        DataInputStream in = new DataInputStream(new BufferedInputStream(clientsocket.getInputStream()));
        int chunksize = 2*xsize*ysize;
        for (int i=0;i<zsize;i++){
            int pos = 0;
            while (pos<chunksize-1){
                int len = in.read(data[i],pos,chunksize-pos);
                pos+= len;
            }
        }
        long t1 = System.currentTimeMillis();
        System.out.printf("Data transfer speed: %f MB/s\n", (long)zsize*(long)xsize*(long)ysize*2d/(double)(t1-t0)/1000d/1024d/1024d);
        for (int i=0;i<zsize;i++){
            fos.write(data[i]);
        }
        fos.close();
        long t2 = System.currentTimeMillis();
        System.out.printf("Data write speed: %f MB/s\n", (long)zsize*(long)xsize*(long)ysize*2d/(double)(t2-t1)/1000d/1024d/1024d);
//        out.write("Data received".getBytes());
//        out.close();
        in.close();
        socket.close();
        clientsocket.close();
        try{
            ports.put(port);
        }
        catch (InterruptedException it){
            it.printStackTrace();
        }
        return;
    }
    private void writefilefast(int zsize,int ysize,int xsize,String filename,int port) throws IOException{
        datagetter dg = new datagetter(this.config.ipadd,port,xsize*ysize*2,zsize);
        dg.receiveandwrite(filename);
        try{
            ports.put(port);
        }
        catch (InterruptedException it){
            it.printStackTrace();
        }
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
                printlock.lock();
                System.out.println("Command handler ready");
                printlock.unlock();

                while (true) {
                    try {
                        if (isStopped()){
                            break;
                        }
                        commands current_command = commands.take();
                        commandexecutor.submit(new command_worker(current_command.port, current_command.commands));
                        printlock.lock();
                        System.out.println("Command started:" + current_command.commands+ "@" + current_command.port);
                        printlock.unlock();
                    } catch (InterruptedException e) {
                        printlock.lock();
                        System.out.println("Command handler interrupted");
                        printlock.unlock();
                    }

                }
                printlock.lock();
                System.out.println("command_handler stopped");
                printlock.unlock();
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
                    System.out.println("caught");
                    System.out.println("Server Stopped.") ;
                    break;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }
            command_accept(clientSocket);
        }
        handlerthread.interrupt();
        this.commandexecutor.shutdown();
        this.scriptexecutor.shutdown();
        try{
            handlerthread.join();}
        catch (InterruptedException it){
            it.printStackTrace();
        }
        printlock.lock();
        System.out.println("Server Stopped.") ;
        printlock.unlock();
    }
    }


