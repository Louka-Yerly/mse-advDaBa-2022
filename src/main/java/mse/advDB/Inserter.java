package mse.advDB;

import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.ClientException;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


public class Inserter implements Runnable {

    private Driver driver;

    private final BlockingQueue<List<Article>> queue;
    private final BlockingQueue<Set<Author>> authorQueue;
    private boolean hasMoreData = true;
    private int batchSize;
    private boolean connected = false;


    public Inserter(BlockingQueue<List<Article>> queue, BlockingQueue<Set<Author>> authorQueue, Driver driver,
                    int batchSize) {
        this.driver = driver;
        this.queue = queue;
        this.authorQueue = authorQueue;
        this.batchSize = batchSize;
    }

    public synchronized void noMoreData() {
        hasMoreData = false;
    }

    private synchronized boolean hasMoreData() {
        return hasMoreData;
    }


    public void createRelationships() {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.writeTransaction(tx -> {
                System.out.println("Create AUTHORED relationship");
                /*
                tx.run(new Query("MATCH (a:ARTICLE) " +
                                     "WITH a UNWIND a.authors as a_id " +
                                     "MATCH(b:AUTHOR {_id:a_id}) " +
                                     "CREATE (b)-[:AUTHORED]->(a)"));
                */
                tx.run(new Query("CALL apoc.periodic.iterate(" +
                                      "\"MATCH (a:ARTICLE) return a\", " +
                                      "\"UNWIND a.authors as a_id MATCH (b:AUTHOR {_id:a_id}) WHERE a_id <> '' CREATE (b)-[:AUTHORED]->(a) \"," +
                                      "{batchSize:1000, parallel:false})"));

                System.out.println("Create CITES relationship");
                /*
                tx.run(new Query("MATCH (a:ARTICLE) " +
                        "WITH a UNWIND a.references as ref " +
                        "MATCH(b:ARTICLE {_id:ref}) " +
                        "CREATE (a)-[:CITES]->(b)"));
                */
                tx.run(new Query("CALL apoc.periodic.iterate(" +
                        "\"MATCH (a:ARTICLE) return a\", " +
                        "\"UNWIND a.references as a_ref MATCH (b:ARTICLE {_id:a_ref}) WHERE a_ref <> '' CREATE (a)-[:CITES]->(b) \"," +
                        "{batchSize:1000, parallel:false})"));

                return 1;
            });
        }
    }

    public void insertAuthors(Set<Author> authors) {
        List<Author> authorList = new ArrayList<>(authors.size());
        List<Map<String, Object>> subList = new ArrayList<>(batchSize);
        authorList.addAll(authors);
        for(int i=0; i<authorList.size(); i++) {
            subList.add(authorList.get(i).toMap());
            if(i%batchSize == 0 && i>0) {
                System.out.println("\tInsert authors batch of size : " + subList.size());
                insertSubListAuthor(subList);
                subList.clear();
            }
        }
        System.out.println("\tInsert authors batch of size : " + subList.size());
        insertSubListAuthor(subList);
    }

    private void insertSubListAuthor(List<Map<String, Object>> subList){
        Map<String, Object> map = new HashMap<>();
        map.put("authors", subList);
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            session.writeTransaction(tx -> {
                tx.run(new Query(   "UNWIND $authors as a " +
                        "CREATE (:AUTHOR {_id:a._id, name:a.name}) ",
                        map));
                return 1;
            });
        }
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
                try {
                    tx.run(new Query(   "UNWIND $articles as ar " +
                            "CREATE (a:ARTICLE {_id:ar._id, title:ar.title, authors: ar.authors, " +
                            "references: ar.references}) ",
                            //"SET a.title = ar.title " +
                            //"SET a.authors = authors",
                            //"WITH ar, a UNWIND ar.authors as ath " +
                            //"MERGE (b:AUTHOR {_id:ath._id}) " +
                            //"SET b.name = ath.name " +
                            //"MERGE (b)-[:AUTHORED]->(a) " +
                            //"WITH ar, a UNWIND ar.references as ref " +
                            //"MERGE (r:ARTICLE {_id:ref}) " +
                            //"MERGE (a)-[:CITES]->(r) ",
                            articles_map));
                } catch(Exception e) {
                    System.out.println(articles_map);
                }


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
        while(!queue.isEmpty() || hasMoreData()) {
            try{
                insertArticle(Objects.requireNonNull(queue.poll(10, TimeUnit.SECONDS)));
            } catch(NullPointerException ignored) {
            }
            catch(InterruptedException ie){
                ie.printStackTrace();
            }
        }
        System.out.println("Inserter: Finish inserting Articles");

        try{
            insertAuthors(authorQueue.take());
        } catch(InterruptedException e){
            e.printStackTrace();
        }
        System.out.println("Inserter: Finish inserting Authors");

        createRelationships();
        System.out.println("Inserter: Finish inserting relationship");
    }
}