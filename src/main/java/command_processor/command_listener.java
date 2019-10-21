package command_processor;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.plugin.Macro_Runner;
import ij.plugin.ZProjector;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;


public class command_listener implements Runnable{
    protected config config = null;
    protected boolean isStopped= false;
    private boolean renderinginit = false;
    private byte[][] dsframe = null;
    private ReentrantLock renderinglock = new ReentrantLock();
    private long lastrendered;
    private BlockingQueue<String> renderinglist = new ArrayBlockingQueue<String>(3);
    protected ServerSocket server = null;
    protected Thread runningThread = null;
    protected int ServerPort = 53705;
    protected int threadport_start = 50400;
    protected int dsfactor = 4; //downsampling factor in x-y for rendering
    protected ReentrantLock printlock = new ReentrantLock();
    protected BlockingQueue<commands> commands= new ArrayBlockingQueue<commands>(50);
    protected class commands{
        private String commands;
        private int port;
        public commands(String commandsin,int portin){
            this.commands = commandsin;
            this.port = portin;
        }
    }
    public command_listener(){};
    protected BlockingQueue<Integer> ports = new ArrayBlockingQueue<Integer>(50);
    protected ExecutorService commandexecutor = Executors.newFixedThreadPool(5);
    protected ExecutorService scriptexecutor = Executors.newFixedThreadPool(5);
    //TCP/IP server class that listen to command for processing
    public command_listener(config configin){
        this.config = configin;
        this.ServerPort = configin.port;
        lastrendered = System.currentTimeMillis();
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
    protected synchronized boolean isStopped(){
        return this.isStopped;
    }

    protected void command_accept(Socket acceptedClient){
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
                    writefile(Integer.parseInt(commandlist[2]),Integer.parseInt(commandlist[3]),Integer.parseInt(commandlist[4]),commandlist[5],this.port, Boolean.parseBoolean(commandlist[6]));
                }
                catch (IOException e){
                    throw new RuntimeException("Cannot close port" + this.port);
                }
                printlock.lock();
                System.out.println("Command finished:"+command);
                printlock.unlock();
            }
            else if (this.commandlist[1].startsWith("ometiffwritingrequest")){
                try {
                    writefile_ome(Integer.parseInt(commandlist[2]),Integer.parseInt(commandlist[3]),Integer.parseInt(commandlist[4]),commandlist[5],this.port);
                }
                catch (IOException e){
                    throw new RuntimeException("Cannot close port" + this.port);
                }
                printlock.lock();
                System.out.println("Command finished:"+command);
                printlock.unlock();
            }
            //TODO:Implement other potential functions
            else if (this.commandlist[1].startsWith("convertandrender")){
                try {
                    convert4rendering(Integer.parseInt(commandlist[2]),Integer.parseInt(commandlist[3]),Integer.parseInt(commandlist[4]),Integer.parseInt(commandlist[5]),commandlist[6],this.port,Boolean.parseBoolean(commandlist[7]));
                }
                catch (IOException e){
                    throw new RuntimeException("Cannot close port" + this.port);
                }
                printlock.lock();
                System.out.println("Command finished:"+command);
                printlock.unlock();
            }
            else if (this.commandlist[1].startsWith("addrender")){
                add_to_queue(commandlist[2]);
            }
            else if (this.commandlist[1].startsWith("gettimestamp")){
                try {
                    sendtimestamp(this.port);
                }
                catch (IOException e){
                    throw new RuntimeException("Cannot close port" + this.port);
                }
                printlock.lock();
                System.out.println("Command finished:"+command);
                printlock.unlock();
            }
            else if (this.commandlist[1].startsWith("getdata2render")){
                try {
                    sendrenderingdata(this.port);
                }
                catch (IOException e){
                    throw new RuntimeException("Cannot close port" + this.port);
                }
                printlock.lock();
                System.out.println("Command finished:"+command);
                printlock.unlock();
            }
        }
    }

    protected void rawzproject(int zsize, int ysize, int xsize, String filename) throws IOException{
        //Function to generate a MIP z projection of a given image
        FileInfo fi = new FileInfo();
        fi.fileName = filename;
        fi.intelByteOrder = true;
        fi.nImages = zsize;
        fi.width = xsize;
        fi.height = ysize;
        fi.fileType = FileInfo.GRAY16_UNSIGNED;
        FileOpener fo = new FileOpener(fi);
        ImagePlus img = fo.open(false);
        ZProjector projector = new ZProjector(img);
        projector.setMethod(ZProjector.MAX_METHOD);
        projector.doProjection();
        ImagePlus projected = projector.getProjection();
        IJ.saveAsTiff(projected, FilenameUtils.removeExtension(filename)+"_mip.tiff");
        printlock.lock();
        System.out.println("Zprojection generated");
        printlock.unlock();
    }
    private void tiffzproject(int zsize, int ysize, int xsize, String filename) throws IOException{
        //Function to generate a MIP z projection of a given image
        ImagePlus img = new ImagePlus(filename);
        ZProjector projector = new ZProjector(img);
        projector.setMethod(ZProjector.MAX_METHOD);
        projector.doProjection();
        ImagePlus projected = projector.getProjection();
        IJ.saveAsTiff(projected, FilenameUtils.removeExtension(filename)+"_mip.tiff");
        printlock.lock();
        System.out.println("Zprojection generated");
        printlock.unlock();
    }
    protected void writefile(int zsize,int ysize,int xsize,String filename,int port) throws IOException{
        InetAddress add = InetAddress.getByName(config.ipadd);
        ServerSocket socket = new ServerSocket(port, 10, add);
        Socket clientsocket;
        clientsocket = socket.accept();
        FileOutputStream fos = new FileOutputStream(filename);
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
        long t1 = System.currentTimeMillis();
        printlock.lock();
        System.out.printf("Data transfer speed: %f MB/s\n", (long)zsize*(long)xsize*(long)ysize*2d/((double)(t1-t0)/1000d)/1024d/1024d);
        printlock.unlock();
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
    protected void writefile(int zsize,int ysize, int xsize,String filename,int port, boolean zproject) throws IOException{
        if (zproject){
            writefile(zsize,ysize,xsize,filename,port);
            rawzproject(zsize,ysize,xsize,filename);
        }
        else{
            writefile(zsize,ysize,xsize,filename,port);
        }
    }
    protected void writefile_ome(int zsize,int ysize,int xsize,String filename,int port) throws IOException{
        InetAddress add = InetAddress.getByName(config.ipadd);
        ServerSocket socket = new ServerSocket(port, 10, add);
        Socket clientsocket;
        clientsocket = socket.accept();
        ometiffwriter fos = new ometiffwriter(filename);
        fos.initialize(xsize,ysize,zsize,0.65f,0.65f,2f);
        int pixelType = FormatTools.UINT16;
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
            try{
                fos.writer.saveBytes(i,frame);}
            catch (FormatException e){
                e.printStackTrace();
            }
        }
        fos.cleanup();
        long t1 = System.currentTimeMillis();
        printlock.lock();
        System.out.println((long)zsize*(long)xsize*(long)ysize*2d/(double)(t1-t0));
        System.out.printf("Data transfer speed: %f MB/s\n", (long)zsize*(long)xsize*(long)ysize*2d/((double)(t1-t0)/1000d)/1024d/1024d);
        printlock.unlock();
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
    protected void convert4rendering(int zsize,int ysize,int xsize,int wavelength,String filename,int port,boolean save) throws IOException{
        String filepath = Paths.get(filename).getParent().toString()+"/";
        InetAddress add = InetAddress.getByName(config.ipadd);
        ServerSocket socket = new ServerSocket(port, 10, add);
        Socket clientsocket;
        clientsocket = socket.accept();
        DataInputStream in = new DataInputStream(new BufferedInputStream(clientsocket.getInputStream()));
        String renderingname = FilenameUtils.removeExtension(filename)+".rendering.raw";
        System.out.println(renderingname);
        FileOutputStream fos = new FileOutputStream(renderingname,true);
        long t0 = System.currentTimeMillis();
        if (save) {
            FileOutputStream fosraw = new FileOutputStream(filename);
            int chunksize = 2 * xsize * ysize;
            byte[] frame = new byte[chunksize];
            if (!renderinginit) {
                dsframe = new byte[zsize][chunksize / dsfactor];
                renderinginit = true;
            }
            renderinglock.lock();
            for (int i = 0; i < zsize; i++) {
                int pos = 0;
                while (pos < chunksize - 1) {
                    int len = in.read(frame, pos, chunksize - pos);
                    pos += len;
                }
                fosraw.write(frame);
                dsframe[i] = downsamplearray(frame, dsfactor, 2, xsize, ysize);
                fos.write(dsframe[i]);
            }
            fosraw.close();
        }
        else{
            int chunksize = 2 * xsize * ysize;
            byte[] frame = new byte[chunksize];
            if (!renderinginit) {
                dsframe = new byte[zsize][chunksize / dsfactor];
                renderinginit = true;
            }
            renderinglock.lock();
            for (int i = 0; i < zsize; i++) {
                int pos = 0;
                while (pos < chunksize - 1) {
                    int len = in.read(frame, pos, chunksize - pos);
                    pos += len;
                }
                dsframe[i] = downsamplearray(frame, dsfactor, 2, xsize, ysize);
                fos.write(dsframe[i]);
            }
        }
        try{
            String tempcommand = "internal addrender"+" "+renderingname;
            commands.put(new commands(tempcommand,0)); //0 reserved for internal commands
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        long t1 = System.currentTimeMillis();
        lastrendered = t1;
        Date date = new Date(lastrendered);
        DateFormat simple = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
        File jsonfile = new File(filepath+"rendering.json");
        renderingjson json = new renderingjson(xsize/dsfactor,ysize/dsfactor,zsize,0.65/dsfactor,0.65/dsfactor,2);
        if (jsonfile.exists()){
            json.readJson(filepath+"rendering.json");
        }
        System.out.println("yep");
        json.updatetime(simple.format(date),wavelength);
        json.setfilelocation(renderingname,wavelength);

        try{
            json.writeJson(filepath+"rendering.json");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        renderinglock.unlock();
        printlock.lock();
        System.out.println((long)zsize*(long)xsize*(long)ysize*2d/(double)(t1-t0));
        System.out.printf("Data transfer and conversion speed: %f MB/s\n", (long)zsize*(long)xsize*(long)ysize*2d/((double)(t1-t0)/1000d)/1024d/1024d);
        printlock.unlock();
        in.close();
        fos.close();
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
    protected void sendtimestamp(int port) throws IOException{
        //send the timestamp to the client
        InetAddress add = InetAddress.getByName(config.ipadd);
        ServerSocket socket = new ServerSocket(port, 10, add);
        Socket clientsocket;
        clientsocket = socket.accept();
        OutputStream os = clientsocket.getOutputStream();
        renderinglock.lock();
        Date date = new Date(lastrendered);
        DateFormat simple = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
        os.write(simple.format(date).getBytes());
        os.write("\n".getBytes());
        renderinglock.unlock();
        os.close();
        try{
            ports.put(port);
        }
        catch (InterruptedException it){
            it.printStackTrace();
        }
    }
    protected void sendrenderingdata(int port) throws IOException{
        //send the data for rendering to the client
        InetAddress add = InetAddress.getByName(config.ipadd);
        ServerSocket socket = new ServerSocket(port, 10, add);
        Socket clientsocket;
        clientsocket = socket.accept();
        OutputStream os = clientsocket.getOutputStream();
        renderinglock.lock();
        for (int i=0;i<dsframe.length;i++){
            os.write(dsframe[i]);
        }
        renderinglock.unlock();
        os.close();
        try{
            ports.put(port);
        }
        catch (InterruptedException it){
            it.printStackTrace();
        }
    }
    protected void add_to_queue(String filename){
        //Function to add a filename to the list to indicate that the file is alive on the storage, remove the oldest file
        try {
            if (renderinglist.size() < 3) {
                renderinglist.put(filename);
            } else {
                String temp = renderinglist.take();
                File filetobedeleted = new File(temp);
                filetobedeleted.delete();
                renderinglist.put(filename);
            }
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
        return;
    }
    private byte[] downsamplearray(byte[] input,int dsfactor,int bitwidth,int xsize,int ysize){
        byte[] output = new byte[input.length/dsfactor/dsfactor];
        int newysize = ysize/dsfactor;
        int newxsize = xsize/dsfactor;
        int pos = 0;
        for (int i=0;i<newysize;i++){
            for (int j=0;j<newxsize;j++){
                for (int k=0;k<bitwidth;k++){
                    output[i*newxsize*bitwidth+j*bitwidth+k] = input[i*xsize*bitwidth*dsfactor+j*bitwidth*dsfactor+k];
                }
            }
        }
        return(output);
    }
    private void writefilefast(int zsize,int ysize,int xsize,String filename,int port) throws IOException{
        long t0 = System.currentTimeMillis();
        datagetter dg = new datagetter(this.config.ipadd,port,xsize*ysize*2,zsize);
        dg.receiveandwrite(filename);
        long t1 = System.currentTimeMillis();
        printlock.lock();
        System.out.println((long)zsize*(long)xsize*(long)ysize*2d/(double)(t1-t0));
        System.out.printf("Data transfer speed: %f MB/s\n", (long)zsize*(long)xsize*(long)ysize*2d/((double)(t1-t0)/1000d)/1024d/1024d);
        printlock.unlock();
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
                if (current_command[1].endsWith(".ijm")){
                    String pluginsDir = config.workspace;
                    System.setProperty("plugins.dir", pluginsDir);
                    ImageJ session = new ImageJ(ImageJ.NO_SHOW);
                    Macro_Runner mr = new Macro_Runner();
                    mr.runMacroFile(config.workspace + current_command[1]+".ijm",args);
                    session.quit();
                }
                else if (current_command[1].endsWith(".py")){
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


