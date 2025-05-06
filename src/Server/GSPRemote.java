package Server;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface for Graph Shortest Path (GSP) service.
 * Defines operations that can be performed on the graph by remote clients.
 */
public interface GSPRemote extends Remote {

    /**
     * Query the shortest path distance between two nodes.
     *
     * @param sourceNode The source node ID
     * @param targetNode The target node ID
     * @return The shortest path distance (number of edges), or -1 if no path exists
     * @throws RemoteException If a remote communication error occurs
     */
    int queryShortestPath(int sourceNode, int targetNode) throws RemoteException;

    /**
     * Add an edge from source node to target node in the graph.
     *
     * @param sourceNode The source node ID
     * @param targetNode The target node ID
     */
    void addEdge(int sourceNode, int targetNode) throws RemoteException;

    /**
     * Delete an edge from source node to target node in the graph.
     *
     * @param sourceNode The source node ID
     * @param targetNode The target node ID
     */
    void deleteEdge(int sourceNode, int targetNode) throws RemoteException;

    /**
     * Process a batch of operations (queries, adds, deletes).
     *
     * @param operations List of operations in the format: [operation, sourceNode, targetNode]
     *                  where operation is 'Q', 'A', or 'D'
     * @return List of results for query operations (empty for non-query operations)
     */
    List<Integer> processBatch(List<String[]> operations) throws RemoteException;

    /**
     * Get performance metrics from the server.
     *
     * @return String containing performance data
     */
    String getPerformanceMetrics() throws RemoteException;
}