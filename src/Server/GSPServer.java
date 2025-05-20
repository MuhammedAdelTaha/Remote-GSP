package Server;
import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Server implementation for the Graph Shortest Path service.
 * Handles graph operations and shortest path queries.
 */
public class GSPServer extends UnicastRemoteObject implements GSPRemote {
    private final Map<Integer, Set<Integer>> graph;                 // Adjacency list representation of the graph
    private final ReadWriteLock graphLock;                          // Lock for concurrent access to the graph
    private final AtomicInteger nodeCount;                          // Counter for total nodes in the graph
    private final ConcurrentHashMap<String, Integer> counts;        // Map to track counts of operations
    private final ConcurrentHashMap<String, Long> processingTimes;  // Map to track operation processing times

    private final String serverAddress;
    private final int serverPort;
    private final int rmiRegistryPort;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private final String logFilePath;

    /**
     * Constructor for the GSP Server.
     *
     * @param serverAddress The server's IP address
     * @param serverPort The server's port
     * @param rmiRegistryPort The RMI registry port
     * @throws RemoteException If a remote communication error occurs
     */
    public GSPServer(String serverAddress, int serverPort, int rmiRegistryPort) throws RemoteException {
        super();
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.rmiRegistryPort = rmiRegistryPort;
        this.graph = new ConcurrentHashMap<>();
        this.graphLock = new ReentrantReadWriteLock();
        this.nodeCount = new AtomicInteger(0);
        this.counts = new ConcurrentHashMap<>();
        this.processingTimes = new ConcurrentHashMap<>();
        this.logFilePath = "server_log.txt";
        this.isRunning = false;

        // Initialize processing time tracking
        processingTimes.put("query", 0L);
        processingTimes.put("add", 0L);
        processingTimes.put("delete", 0L);

        // Initialize the log file
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath))) {
            writer.println("GSP Server started at " + new Date());
            writer.println("Server Address: " + serverAddress);
            writer.println("Server Port: " + serverPort);
            writer.println("RMI Registry Port: " + rmiRegistryPort);
        } catch (IOException e) {
            System.err.println("Error initializing log file: " + e.getMessage());
        }
    }

    /**
     * Start the server and register with RMI registry.
     */
    public void start() {
        try {
            System.setProperty("java.rmi.server.hostname", serverAddress);

            // Create and start RMI registry if needed
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(rmiRegistryPort);
                log("Created RMI registry on port " + rmiRegistryPort);
            } catch (RemoteException e) {
                // Registry may already exist
                registry = LocateRegistry.getRegistry(serverAddress, rmiRegistryPort);
                log("Using existing RMI registry on port " + rmiRegistryPort);
            }

            // Bind this server to the registry
            registry.rebind("GSPService", this);
            log("GSP Service bound to registry");

            // Start the server socket for non-RMI communications
            serverSocket = new ServerSocket(serverPort);
            isRunning = true;
            log("Server socket listening on port " + serverPort);
        } catch (Exception e) {
            log("Server start error: " + e.getMessage());
        }
    }

    /**
     * Handle the initial graph input from standard input.
     */
    public void handleInitialGraph(String filePath) {
        long millis = System.currentTimeMillis();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            log("Reading initial graph from file: " + filePath);
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("S"))
                    break; // End of initial graph

                String[] parts = line.trim().split("\\s+");
                if (parts.length == 2) {
                    try {
                        int source = Integer.parseInt(parts[0]);
                        int target = Integer.parseInt(parts[1]);
                        addEdgeInternal(source, target, false);
                        log("Added initial edge: " + source + " -> " + target);
                    } catch (NumberFormatException e) {
                        log("Invalid edge format: " + line);
                    }
                }
            }

            millis = System.currentTimeMillis() - millis;
            System.out.println("Initial graph processing complete in " + String.valueOf(millis / 1000.0f) + " seconds");

            log("Initial graph processing complete. Ready for workload.");
        } catch (IOException e) {
            log("Error handling input: " + e.getMessage());
        }
    }

    /**
     * Process a single operation (query, add, delete).
     *
     * @param results List to store results of query operations
     * @param operation The operation to process
     */
    private void processOperation(List<Integer> results, String[] operation) {
        if (operation.length != 3)
            return; // Invalid operation format

        // Extract operation type and parameters
        try {
            char op = operation[0].charAt(0);
            int source = Integer.parseInt(operation[1]);
            int target = Integer.parseInt(operation[2]);

            switch (op) {
                case 'Q':
                    results.add(queryShortestPathInternal(source, target));
                    break;
                case 'A':
                    addEdgeInternal(source, target, true);
                    break;
                case 'D':
                    deleteEdgeInternal(source, target);
                    break;
                default:
                    log("Invalid operation: " + op);
            }
        } catch (NumberFormatException e) {
            log("Invalid operation parameters: " + Arrays.toString(operation));
        }
    }

    /**
     * Query the shortest path distance between two nodes.
     */
    @Override
    public int queryShortestPath(int sourceNode, int targetNode) throws RemoteException {
        return queryShortestPathInternal(sourceNode, targetNode);
    }

    /**
     * Internal implementation of shortest path query using BFS.
     */
    private int queryShortestPathInternal(int sourceNode, int targetNode) {
        counts.put("query", counts.getOrDefault("query", 0) + 1);
        long startTime = System.currentTimeMillis();
        int result = -1;

        // If source and target are the same, distance is 0
        if (sourceNode == targetNode) {
            result = 0;
        } else {
            // Acquire read lock for graph traversal
            graphLock.readLock().lock();
            try {
                // Check if nodes exist in the graph
                if (graph.containsKey(sourceNode) && graph.containsKey(targetNode))
                    result = bfsShortestPath(sourceNode, targetNode); // Breadth-First Search for shortest path
            } finally {
                graphLock.readLock().unlock();
            }
        }

        long endTime = System.currentTimeMillis();
        processingTimes.put("query", processingTimes.getOrDefault("query", 0L) + (endTime - startTime));
        log("Query: " + sourceNode + " -> " + targetNode + " = " + result + " (took " + (endTime - startTime) + "ms)");

        return result;
    }

    /**
     * Perform BFS to find the shortest path between two nodes.
     *
     * @param sourceNode The source node ID
     * @param targetNode The target node ID
     * @return The shortest path distance (number of edges), or -1 if no path exists
     */
    private int bfsShortestPath(int sourceNode, int targetNode) {
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> distances = new HashMap<>();

        queue.add(sourceNode);
        distances.put(sourceNode, 0);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            int currentDistance = distances.get(current);

            // Found target node
            if (current == targetNode)
                return currentDistance;

            // Explore neighbors
            for (int neighbor : graph.getOrDefault(current, Collections.emptySet())) {
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, currentDistance + 1);
                    queue.add(neighbor);
                }
            }
        }

        // No path found
        return -1;
    }

    /**
     * Add an edge from source node to target node in the graph.
     */
    @Override
    public void addEdge(int sourceNode, int targetNode) throws RemoteException {
        addEdgeInternal(sourceNode, targetNode, true);
    }

    /**
     * Internal implementation of adding an edge.
     */
    private void addEdgeInternal(int sourceNode, int targetNode, boolean addTime) {
        if (addTime)
            counts.put("add", counts.getOrDefault("add", 0) + 1);
        long startTime = System.currentTimeMillis();

        graphLock.writeLock().lock();
        try {
            // Add the source node if it doesn't exist
            if (!graph.containsKey(sourceNode)) {
                graph.put(sourceNode, new HashSet<>());
                nodeCount.incrementAndGet();
            }

            // Add the target node if it doesn't exist
            if (!graph.containsKey(targetNode)) {
                graph.put(targetNode, new HashSet<>());
                nodeCount.incrementAndGet();
            }

            // Add the edge (if it doesn't already exist)
            graph.get(sourceNode).add(targetNode);
        } finally {
            graphLock.writeLock().unlock();
        }

        long endTime = System.currentTimeMillis();
        if (addTime)
            processingTimes.put("add", processingTimes.getOrDefault("add", 0L) + (endTime - startTime));
        log("Added edge: " + sourceNode + " -> " + targetNode + " (took " + (endTime - startTime) + "ms)");
    }

    /**
     * Delete an edge from source node to target node in the graph.
     */
    @Override
    public void deleteEdge(int sourceNode, int targetNode) throws RemoteException {
        deleteEdgeInternal(sourceNode, targetNode);
    }

    /**
     * Internal implementation of deleting an edge.
     */
    private void deleteEdgeInternal(int sourceNode, int targetNode) {
        counts.put("delete", counts.getOrDefault("delete", 0) + 1);
        long startTime = System.currentTimeMillis();

        graphLock.writeLock().lock();
        try {
            // If the source node exists, remove the edge to the target
            if (graph.containsKey(sourceNode))
                graph.get(sourceNode).remove(targetNode);
        } finally {
            graphLock.writeLock().unlock();
        }

        long endTime = System.currentTimeMillis();
        processingTimes.put("delete", processingTimes.getOrDefault("delete", 0L) + (endTime - startTime));
        log("Deleted edge: " + sourceNode + " -> " + targetNode + " (took " + (endTime - startTime) + "ms)");
    }

    /**
     * Process a batch of operations (queries, adds, deletes).
     */
    @Override
    public List<Integer> processBatch(List<String[]> operations) throws RemoteException {
        List<Integer> results = new ArrayList<>();
        long batchStartTime = System.currentTimeMillis();

        for (String[] operation : operations)
            processOperation(results, operation);

        long batchEndTime = System.currentTimeMillis();
        log("Processed batch with " + operations.size() + " operations (took " + (batchEndTime - batchStartTime) + "ms)");

        // System.out.println(getPerformanceMetrics());

        return results;
    }

    /**
     * Get performance metrics from the server.
     */
    @Override
    public String getPerformanceMetrics() throws RemoteException {
        int queryCount = counts.getOrDefault("query", 0);
        int addCount = counts.getOrDefault("add", 0);
        int deleteCount = counts.getOrDefault("delete", 0);
        float avgQueryTime = queryCount > 0 ? (float)processingTimes.get("query") / queryCount : 0;
        float avgAddTime = addCount > 0 ? (float)processingTimes.get("add") / addCount : 0;
        float avgDeleteTime = deleteCount > 0 ? (float)processingTimes.get("delete") / deleteCount : 0;
        float totalAvgTime = (float)(processingTimes.get("query") + processingTimes.get("add") + processingTimes.get("delete")) / (queryCount + addCount + deleteCount);
        return "Performance Metrics:\n" +
                "Total Nodes: " + nodeCount.get() + "\n" +
                "Total Operations: " + (queryCount + addCount + deleteCount) + "\n" +
                "Average Query Time: " + avgQueryTime + "ms\n" +
                "Average Add Time: " + avgAddTime + "ms\n" +
                "Average Delete Time: " + avgDeleteTime + "ms\n" + 
                "Total Average Time: " + totalAvgTime + " ms\n";
    }

    /**
     * Log a message to the server log file.
     *
     * @param message The message to log
     */
    private void log(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println("[" + new Date() + "]: " + message);
        } catch (IOException e) {
            System.err.println("Error writing to log: " + e.getMessage());
        }
    }

    /**
     * Stop the server.
     */
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();

            // Unbind from RMI registry
            Naming.unbind("rmi://" + serverAddress + ":" + rmiRegistryPort + "/GSPService");
            log("Server stopped");
        } catch (Exception e) {
            log("Error stopping server: " + e.getMessage());
        }
    }

    /**
     * Check if the server is running.
     *
     * @return true if the server is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
}