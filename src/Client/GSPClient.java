package Client;

import Server.GSPRemote;
import java.rmi.*;
import java.util.*;
import java.io.*;

public class GSPClient {
    private GSPRemote serverStub;
    private final String clientId;
    private final String logFilePath;

    public GSPClient(String clientId) {
        this.clientId = clientId;
        this.logFilePath = "client_" + clientId + "_log.txt";
        initializeLog();
    }

    private void initializeLog() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath))) {
            writer.println("GSP Client " + clientId + " started at " + new Date());
        } catch (IOException e) {
            System.err.println("Error initializing client log: " + e.getMessage());
        }
    }

    public void connectToServer(String serverAddress, int rmiRegistryPort) {
        try {
            String serviceName = "rmi://" + serverAddress + ":" + rmiRegistryPort + "/GSPService";
            serverStub = (GSPRemote) Naming.lookup(serviceName);
            log("Connected to server at " + serviceName);
            System.out.println("Connected to server at " + serviceName);
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
            log("Connection error: " + e.getMessage());
        }
    }

    public void sendQuery(int source, int target) {
        if (serverStub == null) {
            log("Not connected to server");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            int distance = serverStub.queryShortestPath(source, target);
            long endTime = System.currentTimeMillis();

            log("Query: " + source + "->" + target + " = " + distance +
                    " (took " + (endTime - startTime) + "ms)");
            System.out.println(distance);
            // Simulate random processing delay
            Thread.sleep((long)(Math.random() * 10000));

        } catch (Exception e) {
            log("Query failed: " + e.getMessage());
        }
    }

    public void sendAddEdge(int source, int target) {
        if (serverStub == null) {
            log("Not connected to server");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            serverStub.addEdge(source, target);
            long endTime = System.currentTimeMillis();

            log("Added edge: " + source + "->" + target +
                    " (took " + (endTime - startTime) + "ms)");
            // Simulate random processing delay
            Thread.sleep((long)(Math.random() * 10000));

        } catch (Exception e) {
            log("Add edge failed: " + e.getMessage());
        }
    }

    public void sendDeleteEdge(int source, int target) {
        if (serverStub == null) {
            log("Not connected to server");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            serverStub.deleteEdge(source, target);
            long endTime = System.currentTimeMillis();

            log("Deleted edge: " + source + "->" + target +
                    " (took " + (endTime - startTime) + "ms)");
            // Simulate random processing delay
            Thread.sleep((long)(Math.random() * 10000));
        } catch (Exception e) {
            log("Delete edge failed: " + e.getMessage());
        }
    }

    public void sendBatch(List<String[]> operations) {
        if (serverStub == null) {
            log("Not connected to server");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            List<Integer> results = serverStub.processBatch(operations);
            long endTime = System.currentTimeMillis();

            log("Processed batch with " + operations.size() + " operations" +
                    " (took " + (endTime - startTime) + "ms)");

            // Print query results
            // for (int result : results) {
            //     System.out.println(result);
            // }

            // Simulate random processing delay
            // Thread.sleep((long)(Math.random() * 10000));
        } catch (Exception e) {
            log("Batch processing failed: " + e.getMessage());
        }
    }

    private void log(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println("[" + new Date() + "]: " + message);
        } catch (IOException e) {
            System.err.println("Error writing to client log: " + e.getMessage());
        }
    }
}
