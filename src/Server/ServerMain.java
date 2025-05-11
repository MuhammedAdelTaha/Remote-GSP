package Server;

import java.io.*;
import java.util.Properties;

public class ServerMain {
    public static void main(String[] args) {
        // Load configuration from system.properties
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("src/system.properties")) {
            prop.load(input);

            String serverAddress = prop.getProperty("GSP.server");
            int serverPort = Integer.parseInt(prop.getProperty("GSP.server.port"));
            int rmiRegistryPort = Integer.parseInt(prop.getProperty("GSP.rmiregistry.port"));

            System.out.println("Starting GSP Server with configuration:");
            System.out.println("Server Address: " + serverAddress);
            System.out.println("Server Port: " + serverPort);
            System.out.println("RMI Registry Port: " + rmiRegistryPort);

            // Create and start the server
            GSPServer server = new GSPServer(serverAddress, serverPort, rmiRegistryPort);
            server.start();

            System.out.println("Server started successfully");

            // Clean shutdown
//            server.stop();
//            System.out.println("Server stopped.");

        } catch (NumberFormatException e) {
            System.err.println("Invalid port number in configuration: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error loading configuration or starting server: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}