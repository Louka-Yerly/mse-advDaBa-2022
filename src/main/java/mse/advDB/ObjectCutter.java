package mse.advDB;

import com.google.gson.stream.JsonReader;
import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class ObjectCutter implements Runnable {

    private boolean hasMoreArticle = true;
    private final int numberOfArticlePerRead;
    private final int numberOfArticle;
    private final JsonReader reader;

    private final BlockingQueue<List<Article>> queueArticle;
    private final BlockingQueue<Set<Author>> queueAuthor;
    private final Set<Author> authorSet = new HashSet<>();

    public ObjectCutter(String filename, int numberOfArticlePerRead, int numberOfArticle,
                        BlockingQueue<List<Article>> queueArticle, BlockingQueue<Set<Author>> queueAuthor) throws IOException{
        this.numberOfArticlePerRead = numberOfArticlePerRead;
        this.numberOfArticle = numberOfArticle;
        InputStream inputStream = Files.newInputStream(Path.of(filename));
        reader = new JsonReader(new InputStreamReader(inputStream));
        reader.beginArray();

        this.queueArticle = queueArticle;
        this.queueAuthor = queueAuthor;
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
            if(a.getAuthors() != null) {
                authorSet.addAll(a.getAuthors());
            }

        }
        return res;
    }

    public boolean hasMoreArticles(){
        return hasMoreArticle;
    }

    @Override
    public void run(){
        for(int i=0; i<numberOfArticle/numberOfArticlePerRead && hasMoreArticles(); i++) {
            List<Article> ar = getNextArticles();
            try{
                queueArticle.put(ar);
            } catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        queueAuthor.add(authorSet);
    }
}
