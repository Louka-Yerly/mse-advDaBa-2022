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
        //String jsonPath = System.getenv("JSON_FILE");
        String jsonPath = "D:\\Louka\\MSE\\AdvDaBa\\mse-advDaBa-2022\\dblpv13.json";
        String jsonCleanPath = "D:\\Louka\\MSE\\AdvDaBa\\mse-advDaBa-2022\\dblpv13_clean.json";

        System.out.println("Path to JSON file is " + jsonPath);
        int nbArticles = 10000;//Integer.max(1000,Integer.parseInt(System.getenv("MAX_NODES")));
        int nbAriclePerRead = 1000;
        System.out.println("Number of articles to consider is " + nbArticles);
        String neo4jIP = "localhost"; //System.getenv("NEO4J_IP");
        System.out.println("IP address of neo4j server is " + neo4jIP);

        int nbInserterThread = 1;


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
