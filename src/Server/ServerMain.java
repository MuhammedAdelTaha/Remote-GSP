package Server;

import java.io.*;
import java.util.Properties;

/**
 * ServerMain class for starting the GSP Server
 */
public class ServerMain {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ServerMain <propertiesFile> <initialGraphFile>");
            return;
        }

        String propertiesFile = args[0];
        String initialGraphFile = args[1];

        // Load configuration from system.properties
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(propertiesFile)) {
            prop.load(input);

            // Server configuration
            String serverAddress = prop.getProperty("GSP.server");
            int serverPort = Integer.parseInt(prop.getProperty("GSP.server.port"));
            int rmiRegistryPort = Integer.parseInt(prop.getProperty("GSP.rmiRegistry.port"));

            // Print server configuration
            System.out.println("Starting GSP Server with configuration:");
            System.out.println("Server Address: " + serverAddress);
            System.out.println("Server Port: " + serverPort);
            System.out.println("RMI Registry Port: " + rmiRegistryPort);

            // Create and start the server
            GSPServer server = new GSPServer(serverAddress, serverPort, rmiRegistryPort);
            server.start();

            // Print server started message
            System.out.println("Server started successfully");

            // Initialize the graph
            server.handleInitialGraph(initialGraphFile);

            // Signal ready to receive workload
            System.out.println("Initial graph loaded. Server is ready to receive workload.");

            // User-Server interaction
            System.out.println("\nEnter 'P' to display the performance of the server.");
            System.out.println("Enter 'E' to stop the server.");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("E")) {
                    System.out.println("Server shutting down...");
                    if (server.isRunning())
                        server.stop();
                    break;
                } else if (line.trim().equalsIgnoreCase("P")) {
                    System.out.println("Displaying server performance...");
                    System.out.println(server.getPerformanceMetrics());
                } else {
                    System.out.println("Invalid command. Enter 'P' to display performance or 'E' to stop the server.");
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number in configuration: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error loading configuration or starting server: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        } finally {
            // Exit the application
            System.exit(0);
        }
    }
}