package mse.advDB;

import com.google.gson.stream.JsonReader;
import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ObjectCutter implements Runnable {

    private boolean hasMoreArticle = true;
    private final int numberOfArticlePerRead;
    private final int numberOfArticle;
    private final JsonReader reader;

    private final BlockingQueue<List<Article>> queue;

    public ObjectCutter(String filename, int numberOfArticlePerRead, int numberOfArticle, BlockingQueue<List<Article>> queue) throws IOException{
        this.numberOfArticlePerRead = numberOfArticlePerRead;
        this.numberOfArticle = numberOfArticle;
        InputStream inputStream = Files.newInputStream(Path.of(filename));
        reader = new JsonReader(new InputStreamReader(inputStream));
        reader.beginArray();

        this.queue = queue;
    }

    public List<Article> getNextArticles() {
        List<Article> res = new ArrayList<>(this.numberOfArticlePerRead);
        for(int i = 0; i<numberOfArticlePerRead; i++) {
            try{
                if(!reader.hasNext()) {
                    this.hasMoreArticle = false;
                    break;
                }
            } catch(IOException e){
                e.printStackTrace();
            }
            Article a = new Gson().fromJson(reader, Article.class);
            a.createObjectMap();
            res.add(a);
        }
        return res;
    }

    public boolean hasMoreArticles(){
        return hasMoreArticle;
    }

    @Override
    public void run(){
        int stopped_times = 0;
        for(int i=0; i<numberOfArticle/numberOfArticlePerRead && hasMoreArticles(); i++) {
            List<Article> ar = getNextArticles();

            try {
                queue.add(ar);
            } catch(IllegalStateException ise) {
                stopped_times++;
                try{
                    queue.put(ar);
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
        System.out.println("ObjectCutter has been stopped: " + stopped_times);
    }
}
