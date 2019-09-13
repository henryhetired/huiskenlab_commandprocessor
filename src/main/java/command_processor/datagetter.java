package command_processor;

import com.sun.security.ntlm.Server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class datagetter {
    private static ByteBuffer buf;
    private String hostname = null;
    private int port = 0;
    private int framesize = 2048*2048;
    private int numframe = 500;
    private static void ensure (int len, ByteChannel chan) throws IOException
    {
        if (buf.remaining () < len) {
            buf.compact ();
            buf.flip ();
            do {
                buf.position (buf.limit ());
                buf.limit (buf.capacity ());
                chan.read (buf);
                buf.flip ();
            } while (buf.remaining () < len && buf.limit()!=buf.capacity());
        }
    }
    public datagetter(String hostname, int port,int framesize,int numframe){
        this.hostname = hostname;
        this.port = port;
        this.framesize = framesize;
        this.numframe = numframe;
    }
    public void receiveandwrite(String filename) throws IOException{
        buf = ByteBuffer.allocateDirect(framesize);
        FileOutputStream fos = new FileOutputStream(filename);
        ServerSocketChannel chanserv = ServerSocketChannel.open();
        chanserv.socket().bind(new InetSocketAddress(hostname,this.port));
        SocketChannel chan = chanserv.accept();
        buf.limit(0);
        byte[] msg = new byte[framesize];
        long it = (long)numframe*(long)framesize;
        for (int i=0;i<numframe;i++){
            ensure(framesize,chan);
            buf.get(msg,0,framesize);
//            fos.write(msg);
        }
        chanserv.close();

        fos.close();
//        chan.close();
    }
}
