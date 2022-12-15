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
        /*
        String query = "UNWIND $papers AS article" +
                               "CREATE (n:ARTICLE {_id: $id, }) " +
                               "SET n = properties";
         */
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.writeTransaction(tx -> {
                for(Article article : articles){
                    Map<String, Object>  data = article.objectMap();

                    tx.run(new Query(  "MERGE (a:ARTICLE {_id:$_id}) " +
                                            "SET a.title = $title " +
                                            "WITH a UNWIND $authors as ath " +
                                            "MERGE (b:AUTHOR {_id:ath._id}) " +
                                            "SET b.name = ath.name " +
                                            "MERGE (b)-[:AUTHORED]->(a) " +
                                            "WITH a UNWIND $references as ref " +
                                            "MERGE (r:ARTICLE {_id:ref}) " +
                                            "MERGE (a)-[:CITES]->(r) ",
                                            data));
                }

                return 1;
            });
        }
    }

    public static void createConstraintAndIndex(Driver driver) {
        //CREATE CONSTRAINT ON (movie:Movie) ASSERT movie.title IS UNIQUE
        //CREATE INDEX ON :Actor(name)
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.writeTransaction(tx -> {
                try{
                    tx.run("CREATE CONSTRAINT ON (article:ARTICLE) ASSERT article._id IS UNIQUE");
                    tx.run("CREATE CONSTRAINT ON (author:AUTHOR) ASSERT author._id IS UNIQUE");
                    //tx.run("CREATE INDEX ON :ARTICLE(_id)");
                    //tx.run("CREATE INDEX ON :AUTHOR(_id)");
                } catch(ClientException ignored) {
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
                    insertArticle(Objects.requireNonNull(queue.poll(1, TimeUnit.SECONDS)));
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
// "CREATE (:ARTICLE {_id:'test', title:'test'})"
                /*
                Map<String, Object>  data = new HashMap<>();
                data.put("_id", articles.get(0).get_id());
                Query q = new Query("CREATE (:ARTICLE {_id:$_id})", data);
                tx.run(q);
                tx.run("CREATE (:ARTICLE {name:'test2'})");
                return 1;
                 */

/**
 * Map<String,Object> props = new HashMap<>();
 * props.put( "name", "Andy" );
 * props.put( "position", "Developer" );
 *
 * Map<String,Object> params = new HashMap<>();
 * params.put( "props", props );
 *
 * String query = "CREATE ($props)";
 *
 * transaction.execute( query, params );
 */