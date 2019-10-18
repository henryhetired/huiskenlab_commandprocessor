package command_processor;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class renderingjson {
    private int xsize;
    private int ysize;
    private int zsize;
    private double xpixelsize;
    private double ypixelsize;
    private double zpixelsize;
    private int wavelength;
    private String timestring;
    private String filelocation;
    JSONObject object = new JSONObject();
    public void updatetime(String newtime){
        timestring = newtime;
    }
    public void setfilelocation(String filelocation){
        this.filelocation = filelocation;
    }
    public renderingjson(int xsize,int ysize,int zsize,double xpixelsize,double ypixelsize,double zpixelsize,int wavelength){
        this.xsize = xsize;
        this.ysize = ysize;
        this.zsize = zsize;
        this.xpixelsize = xpixelsize;
        this.ypixelsize = ypixelsize;
        this.zpixelsize = zpixelsize;
        this.wavelength = wavelength;
    }
    public void writeJson(String filename) throws Exception {
        object.put("xsize",xsize);
        object.put("ysize",ysize);
        object.put("zsize",zsize);
        object.put("xpixelsize",xpixelsize);
        object.put("ypixelsize",ypixelsize);
        object.put("zpixelsize",zpixelsize);
        object.put("timestamp",timestring);
        object.put("filelocation",filelocation);
        FileOutputStream fos = new FileOutputStream(filename);
        fos.write(object.toString().getBytes());
        fos.close();
    }
}
