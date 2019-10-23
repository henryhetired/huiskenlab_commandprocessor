package command_processor;

import java.io.*;

public class config {
    //workspace is the location of the macro scipts
    public String workspace = "/mnt/";
    //port is the port to listen for command
    public int port = 53705;
    public String ipadd = "10.129.11.254";
    public String renderingpath = "C:\\";
    public void create(String filepath) throws IOException {
        //create a config file with the default parameters at filepath
        File fout = new File(filepath + "config.txt");
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write("This is the configuration file for the Command processor");
        bw.newLine();
        bw.write(new String(new char[30]).replace("\0", "-"));
        bw.newLine();
        bw.write("Workspace is the location of the macro scripts");
        bw.newLine();
        bw.write("workspace=" + workspace);
        bw.newLine();
        bw.write(new String(new char[30]).replace("\0", "-"));
        bw.newLine();
        bw.write("***");
        bw.write("Network configurations");
        bw.write("***");
        bw.newLine();
        bw.write("Port number is the port to listen to command for");
        bw.newLine();
        bw.write("port="+port);
        bw.newLine();
        bw.write("IP address="+ipadd);
        bw.newLine();
        bw.write(new String(new char[30]).replace("\0", "-"));
        bw.close();
        System.out.println("Config File successfully created: " + filepath + "config.txt");
    }
    public void read(String filepath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath + "config.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("=")) {
                    if (line.contains("workspace")) {
                        workspace = line.substring(line.indexOf("=") + 1);
                    }
                    else if (line.contains("port")) {

                        port = Integer.parseInt(line.substring(line.indexOf("=") + 1));
                    }
                    else if (line.contains("IP")){
                        ipadd = line.substring(line.indexOf("=")+1);
                    }
                    else if (line.contains("renderingpath")){
                        renderingpath = line.substring(line.indexOf("=")+1);
                    }
                }
            }

        }
    }

}
