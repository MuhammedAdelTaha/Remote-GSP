package Client;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * ClientMain class for running a GSP Client
 */
public class ClientMain {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: ClientMain <propertiesFile> <batchesFile> <clientID>");
            return;
        }

        String propertiesFile = args[0];
        String batchesFile = args[1];
        String clientId = args[2];

        // Load configuration from system.properties
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(propertiesFile)) {
            prop.load(input);

            // Server configuration
            String serverAddress = prop.getProperty("GSP.server");
            int rmiRegistryPort = Integer.parseInt(prop.getProperty("GSP.rmiRegistry.port"));

            // Client configuration
            System.out.println("Starting GSP Client " + clientId);
            System.out.println("Connecting to server at " + serverAddress + ":" + rmiRegistryPort);

            // Create and connect client
            GSPClient client = new GSPClient(clientId);
            client.connectToServer(serverAddress, rmiRegistryPort);

            System.out.println("Client " + clientId + " connected and ready to process batches.");

            // Read operations from file
            BufferedReader reader = new BufferedReader(new FileReader(batchesFile));
            String line;
            List<String[]> currentBatch = new ArrayList<>();
            Random random = new Random();
            int i = 0;
            long totalResponseTime = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue; // Skip empty lines

                if (line.equalsIgnoreCase("F")) {
                    // Process complete batch
                    if (i % 100 == 0)
                        System.out.println("Processing batch #" + i + " of " + currentBatch.size() + " operations...");
                    i++;

                    long responseTime = System.currentTimeMillis();
                    client.sendBatch(currentBatch);
                    totalResponseTime += System.currentTimeMillis() - responseTime;

                    currentBatch.clear();
                    // Thread.sleep(random.nextInt(9000) + 1000); // Simulate network delay
                    // System.out.println("Ready for new batch.");
                    continue;
                }

                String[] parts = line.trim().split("\\s+");
                if (parts.length == 3) {
                    currentBatch.add(parts);
                    // System.out.println("Added operation to batch: " + line);
                } else {
                    // System.out.println("Invalid operation format (skipping): " + line);
                }
            }

            // Process any remaining operations in the last batch (if the batch didn't end with 'F')
            if (!currentBatch.isEmpty()) {
                System.out.println("Processing final batch of " + currentBatch.size() + " operations...");
                client.sendBatch(currentBatch);
            }

            System.out.println("Average response time: " + (float)totalResponseTime / i);

            System.out.println("Finished processing all batches. Client exiting...");
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}