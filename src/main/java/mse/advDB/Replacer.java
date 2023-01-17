package mse.advDB;

import java.io.*;
import java.util.regex.Pattern;

public class Replacer implements Runnable {
    private static final int NB_LINES = 1000;

    private static final Pattern pattern = Pattern.compile("NumberInt[(]([0-9]*)[)]", Pattern.CASE_INSENSITIVE);

    private String src;
    private String dst;

    private boolean stopReplace = false;

    public Replacer(String src, String dst) {
        this.src = src;
        this.dst = dst;
    }

    public synchronized void stopReplace() {
        stopReplace = true;
    }

    private synchronized boolean needStopReplace() {
        return stopReplace;
    }

    public void replace() {
        FileReader fr;
        FileWriter writer = null;
        File file = new File(this.dst);

        try{
            file.createNewFile();
            writer = new FileWriter(this.dst);
        } catch(IOException e){
            e.printStackTrace();
        }
        if(writer == null) throw new RuntimeException("Cannot create FileWriter");

        BufferedReader br;

        try{
            fr = new FileReader(this.src);
        } catch(FileNotFoundException e){
            e.printStackTrace();
            return;
        }
        br = new BufferedReader(fr);

        StringBuilder lines = new StringBuilder();
        while(!needStopReplace()) {
            try{
                for(int i=0; i<NB_LINES; i++){
                    String line = br.readLine();
                    if(line == null) {
                        // end of file
                        stopReplace();
                        break;
                    }
                    lines.append(line);
                }

            } catch(IOException e){
                e.printStackTrace();
                continue;
            }
            while(true) {
                try{
                    //writer.write(lines.replaceAll("NumberInt[(]([0-9]*)[)]", "$1"));
                    writer.write((pattern.matcher(lines).replaceAll("$1")));
                    lines.setLength(0);
                    break;
                } catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
        try{
            writer.close();
            br.close();
            fr.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        replace();
    }
}
