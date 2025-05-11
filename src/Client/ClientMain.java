package Client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ClientMain {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: GSPClient <clientId>");
            return;
        }

        try {
            // Load configuration
            Properties config = new Properties();
            try (InputStream input = new FileInputStream("src/system.properties")) {
                config.load(input);
            }

            String serverAddress = config.getProperty("GSP.server");
            int rmiRegistryPort = Integer.parseInt(config.getProperty("GSP.rmiregistry.port"));

            // Create client
            GSPClient client = new GSPClient(args[0]);
            client.connectToServer(serverAddress, rmiRegistryPort);

            // Read operations from stdin
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            List<String[]> currentBatch = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("F")) {
                    // Process complete batch
                    client.sendBatch(currentBatch);
                    currentBatch.clear();
                    continue;
                }

                String[] parts = line.trim().split("\\s+");
                if (parts.length == 3) {
                    currentBatch.add(parts);
                }
            }

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
