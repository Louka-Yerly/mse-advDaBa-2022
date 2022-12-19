package mse.advDB;


import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TP2Main {

    private static final String USERNAME = "neo4j";
    private static final String PASSWORD = "test";
    private static final String PORT = "7687";


    public static void main(String[] args) throws IOException, InterruptedException {

        int nbInserterThread = 1;

        String jsonPath = System.getenv("JSON_FILE");
        if(jsonPath == null) {
            // run on host
            jsonPath = "D:\\Louka\\MSE\\AdvDaBa\\mse-advDaBa-2022\\dblpv13.json";
        }
        System.out.println("Path to JSON file is " + jsonPath);

        String jsonCleanPath = System.getenv("JSON_CLEAN_FILE");
        if(jsonCleanPath == null) {
            //run on host
            jsonCleanPath = "D:\\Louka\\MSE\\AdvDaBa\\mse-advDaBa-2022\\dblpv13_clean.json";
        }
        System.out.println("Path to JSON clean file is " + jsonCleanPath);

        int nbArticles;
        try {
            nbArticles = Integer.max(1000,Integer.parseInt(System.getenv("MAX_NODES")));
        } catch(NumberFormatException nfe) {
            // run on host
            nbArticles = 10000;
        }
        System.out.println("Number of articles to consider is " + nbArticles);

        int nbAriclePerRead;
        try {
            nbAriclePerRead = Integer.max(1,Integer.parseInt(System.getenv("NODES_PER_READ")));
        } catch(NumberFormatException nfe) {
            // run on host
            nbAriclePerRead = 100;
        }
        System.out.println("Nodes batch size is " + nbAriclePerRead);




        String neo4jIP = System.getenv("NEO4J_IP");
        if(neo4jIP == null) {
            // run on host
            neo4jIP = "localhost";
        }
        System.out.println("IP address of neo4j server is " + neo4jIP);


        Driver driver = GraphDatabase.driver("bolt://" + neo4jIP + ":" + PORT, AuthTokens.basic(USERNAME, PASSWORD));

        boolean connected = false;
        do {
            try {
                System.out.println("Sleeping a bit waiting for the db");
                Thread.yield();
                Thread.sleep(1000); // let some time for the neo4j container to be up and running

                driver.verifyConnectivity();
                connected = true;
            }
            catch(Exception e) {
            }
        } while(!connected);

        System.out.println("Connected to the database...\n Start inserting elements");
        Inserter.createConstraintAndIndex(driver);

        /*
        Replacer replacer = new Replacer(jsonPath, jsonCleanPath);
        Thread replacerThread = new Thread(replacer);
        replacerThread.setPriority(Thread.MAX_PRIORITY);
        replacerThread.start();
        // let time to replacer to start the replacement job
        Thread.sleep(10000);
        */

        BlockingQueue<List<Article>> queue = new ArrayBlockingQueue<>(1);
        ObjectCutter cutter = new ObjectCutter(jsonCleanPath, nbAriclePerRead, nbArticles, queue);
        Inserter[] inserters = new Inserter[nbInserterThread];
        for(int i=0; i<nbInserterThread; i++) {
            inserters[i] = new Inserter(queue, driver);
        }

        Thread cutterThread = new Thread(cutter);
        Thread[] inserterThreads = new Thread[nbInserterThread];
        for(int i=0; i<nbInserterThread; i++) {
            inserterThreads[i] = new Thread(inserters[i]);
            inserterThreads[i].start();
        }
        long start = System.currentTimeMillis();
        cutterThread.start();



        /*
        int i=1;
        long waiting_time = 1000;
        while(cutterThread.getState() != Thread.State.TERMINATED) {
            Thread.sleep(waiting_time);
            System.out.print("\rWaiting since: " + waiting_time/1000*i++ + " seconds");
        }
        System.out.println();
        */



        cutterThread.join();
        for(Inserter inserter : inserters) {
            inserter.noMoreData();
        }
        //replacer.stopReplace();
        for(Thread t: inserterThreads) {
            t.join();
        }
        long stop = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (stop-start)/1000.0 + " (seconds)");

        //replacerThread.join();

        Inserter.deleteEmptyNode(driver);
        driver.close();
        System.out.println("Finish inserting all nodes");
    }
}
