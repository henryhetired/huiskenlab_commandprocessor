package command_processor;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class renderingjson {
    private int xsize;
    private int ysize;
    private int zsize;
    private double xpixelsize;
    private double ypixelsize;
    private double zpixelsize;
    private int wavelength;
    private JSONObject timestring = new JSONObject();
    private JSONObject filelocation = new JSONObject();
    JSONObject object = new JSONObject();
    public void updatetime(String newtime,int wavelength){
        this.timestring.put(Integer.toString(wavelength),newtime);
    }
    public void setfilelocation(String newlocation,int wavelength){
        this.filelocation.put(Integer.toString(wavelength),newlocation);
    }
    public renderingjson(int xsize,int ysize,int zsize,double xpixelsize,double ypixelsize,double zpixelsize){
        this.xsize = xsize;
        this.ysize = ysize;
        this.zsize = zsize;
        this.xpixelsize = xpixelsize;
        this.ypixelsize = ypixelsize;
        this.zpixelsize = zpixelsize;
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
    public void readJson(String filename) throws IOException{
        String jsonstring = "";
        jsonstring = new String (Files.readAllBytes(Paths.get(filename)));
        this.object = new JSONObject(jsonstring);
        this.timestring = new JSONObject(this.object.getJSONObject("timestamp").toString());
        this.filelocation = new JSONObject(this.object.getJSONObject("filelocation").toString());
    }
}
