package mse.advDB;

import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.ClientException;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


public class Inserter implements Runnable {

    private Driver driver;

    private BlockingQueue<List<Article>> queue;
    private boolean hasMoreData = true;

    private boolean connected = false;


    public Inserter(BlockingQueue<List<Article>> queue, Driver driver) {
        this.driver = driver;
        this.queue = queue;
    }

    public synchronized void noMoreData() {
        hasMoreData = false;
    }

    private synchronized boolean hasMoreData() {
        return hasMoreData;
    }

    public void insertArticle(List<Article> articles) {
        System.out.println("\tInsert new batch of length: "  + articles.size());
        List<Map<String, Object>> articles_list = new ArrayList<>();
        for(Article ar:articles) {
            articles_list.add(ar.objectMap());
        }
        Map<String, Object> articles_map = new HashMap<>();
        articles_map.put("articles", articles_list);
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.writeTransaction(tx -> {
                tx.run(new Query(   "UNWIND $articles as ar " +
                                        "MERGE (a:ARTICLE {_id:ar._id}) " +
                                        "SET a.title = ar.title " +
                                        "WITH ar, a UNWIND ar.authors as ath " +
                                        "MERGE (b:AUTHOR {_id:ath._id}) " +
                                        "SET b.name = ath.name " +
                                        "MERGE (b)-[:AUTHORED]->(a) " +
                                        "WITH ar, a UNWIND ar.references as ref " +
                                        "MERGE (r:ARTICLE {_id:ref}) " +
                                        "MERGE (a)-[:CITES]->(r) ",
                                        articles_map));

                return 1;
            });
        }
    }

    public static void createConstraintAndIndex(Driver driver) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.writeTransaction(tx -> {
                try{
                    tx.run("CREATE CONSTRAINT ON (article:ARTICLE) ASSERT article._id IS UNIQUE");
                    tx.run("CREATE CONSTRAINT ON (author:AUTHOR) ASSERT author._id IS UNIQUE");
                    //tx.run("CREATE INDEX ON :ARTICLE(_id)");
                    //tx.run("CREATE INDEX ON :AUTHOR(_id)");
                } catch(ClientException ignored) {
                    // the index is already created
                    tx.rollback();
                }
                return 1;
            });
        }
    }

    public static void deleteEmptyNode(Driver driver) {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.writeTransaction(tx -> {
                tx.run(new Query("MATCH (a) where a._id = '' DETACH DELETE a"));
                return 1;
            });
        }
    }

    @Override
    public void run(){
        System.out.println("Start Inserter (" + Thread.currentThread().getName()+ ")");
        int stopped_times = -1; // -1 cause the first wait is not really a true wait
        while(!queue.isEmpty() || hasMoreData()) {
            try{
                insertArticle(Objects.requireNonNull(queue.poll(0, TimeUnit.SECONDS)));
            } catch(NullPointerException ignored) {
                try{
                    stopped_times++;
                    insertArticle(Objects.requireNonNull(queue.poll(10, TimeUnit.SECONDS)));
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
            catch(InterruptedException e){

                System.out.println("Interrupted while waiting for new articles");
            }
        }
        try{
            Thread.sleep(5000);
        } catch(InterruptedException e){
            e.printStackTrace();
        }
        System.out.println("Insert (" + Thread.currentThread().getName() +") has been stopped: " + stopped_times);
    }
}