package Client;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ClientFrequencyTest {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: ClientFrequencyTest <propertiesFile> <batchesFile> <clientID>");
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

            int frequency = 1;
            long tsMillis = (long) (1.0f / frequency * 1000.0f);

            long millis = 0l;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue; // Skip empty lines
                
                if (line.equalsIgnoreCase("F")) {
                    // Process complete batch
                    System.out.println("Processing batch of " + currentBatch.size() + " operations...");

                    millis = System.currentTimeMillis() - millis;
                    if (tsMillis - millis > 0l) {
                        Thread.sleep(tsMillis - millis);
                    }

                    client.sendBatch(currentBatch);

                    millis = System.currentTimeMillis();

                    currentBatch.clear();
                    System.out.println("Ready for new batch.");
                    continue;
                }

                String[] parts = line.trim().split("\\s+");
                if (parts.length == 3) {
                    currentBatch.add(parts);
                    System.out.println("Added operation to batch: " + line);
                } else {
                    System.out.println("Invalid operation format (skipping): " + line);
                }
            }

            // Process any remaining operations in the last batch (if the batch didn't end with 'F')
            if (!currentBatch.isEmpty()) {
                System.out.println("Processing final batch of " + currentBatch.size() + " operations...");
                client.sendBatch(currentBatch);
            }

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