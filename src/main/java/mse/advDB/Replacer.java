package mse.advDB;

import java.io.*;

public class Replacer implements Runnable {
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
        try{
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

        while(!needStopReplace()) {
            String line = null;
            try{
                line = br.readLine();
            } catch(IOException e){
                e.printStackTrace();
            }
            if(line == null) {
                // end of file
                break;
            }
            while(true) {
                try{
                    writer.write(line.replaceAll("NumberInt[(]([0-9]*)[)]", "$1"));
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
