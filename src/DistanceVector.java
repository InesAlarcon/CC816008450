// Logging
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.*;
import java.nio.file.Path;

public class DistanceVector {
    private final Logger logger;
    private final HashMap<String, Neighbor> neighbors;
    private final HashMap<String, Boolean> hasSentDV;
    private final HashMap<String, Integer> minimumDistances;
    private final HashMap<String, HashMap<String, Integer>> distanceTable;    // Destinies, [Direct neighbors, cost exiting from neighbor]
    private final String myName;

    DistanceVector(String myName, Logger logger, String configFile){
        this.myName = myName;
        this.logger = logger;
        this.logger.log(Level.INFO, "DISTANCE VECTOR> Initializing...");
        this.distanceTable = new HashMap<String, HashMap<String, Integer>>();
        this.minimumDistances = new HashMap<String, Integer>();
        this.hasSentDV = new HashMap<String, Boolean>();

        // Reading config.txt file and creating neighbors
        this.neighbors = new HashMap<String, Neighbor>();
        try {
            Path cwd = Paths.get(System.getProperty("user.dir"));
            File file = new File(cwd.resolve(configFile).toString());
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                // Reading line by line
                String[] neighborInformation = line.split(":");

                // Creating Neighbor
                Neighbor neighbor = new Neighbor(
                        neighborInformation[0], // Nickname
                        Integer.parseInt(neighborInformation[1]), // Initial Cost
                        neighborInformation[2], // IP address
                        Integer.parseInt(neighborInformation[3]), // Routing Port
                        Integer.parseInt(neighborInformation[4]) // Forwarding Port
                );

                this.logger.log(Level.INFO, String.format(
                        "DISTANCE VECTOR> Found Neighbor '%s' in the configuration file with an initial cost of %d and IP address <%s>",
                        neighbor.getName(), neighbor.getCost(),
                        neighbor.getNetworkData().getIpAddress()
                        )
                );

                // Adding Neighbor to table
                this.neighbors.put(neighbor.getName(), neighbor);
                this.logger.log(Level.INFO, "DISTANCE VECTOR> Neighbor configured successfully");
            }

            this.logger.log(Level.INFO, "DISTANCE VECTOR> Finished initialization of neighbors");

            // Initializing Distance Table
            this.logger.log(Level.INFO, "DISTANCE VECTOR> Creating first instance of Distance Table...");
            for (String destiny: this.neighbors.keySet()) {
                // Creating registry
                HashMap<String, Integer> neighborRow = new HashMap<String, Integer>();

                // Creating columns and setting everything to infinite
                for (Neighbor neighbor: this.neighbors.values()) {
                    neighborRow.put(neighbor.getName(), 99);
                    this.distanceTable.put(destiny, neighborRow);
                }

                // Setting up diagonal values
                this.distanceTable.get(destiny).put(this.neighbors.get(destiny).getName(), this.neighbors.get(destiny).getCost());

                // Setting up Minimum distances table
                this.minimumDistances.put(destiny, this.neighbors.get(destiny).getCost());
            }
            this.logger.log(Level.INFO, "DISTANCE VECTOR> Table has been initialized");

            for (Neighbor neighbor: this.neighbors.values()) {
                this.hasSentDV.put(neighbor.getName(), false);
            }

        } catch (Exception e) {
            this.logger.log(Level.SEVERE,
                    String.format(
                            "DISTANCE VECTOR> An eror ocurred while initializing neighbors: %s",
                            e.getMessage()
                    )
            );
        }
    }

    // Getters
    public HashMap<String, Neighbor> getNeighbors() {
        return this.neighbors;
    }

    public int getMinimumFromDestiny(String destiny) { return this.minimumDistances.get(destiny); }

    public LinkedList<NeighborNetworkData> getAllNetowrkData(){
        LinkedList<NeighborNetworkData> allNetworkData = new LinkedList<NeighborNetworkData>();
        for (Neighbor neighbor: this.neighbors.values()) {
            allNetworkData.add(neighbor.getNetworkData());
        }

        return allNetworkData;
    }

    public int getMinimumFromRegistry(HashMap<String, Integer> destinyRegistry){
        int minValue = 99;
        for (Integer cost: destinyRegistry.values()) {
            if (cost < minValue) {
                minValue = cost;
            }
        }
        return minValue;
    }

    public HashMap<String, Integer> getMinimumDistances() {
        return minimumDistances;
    }

    public boolean hasSent(String neighborName) {
        return this.hasSentDV.get(neighborName);
    }

    public HashMap<String, HashMap<String, Integer>> getDistanceTable() { return this.distanceTable; }

    public HashMap<String, Integer> getRegistryOfDestiny(String destiny) {
        return this.distanceTable.get(destiny);
    }

    // Setter
    public void setSent(String neighborName) {
        this.hasSentDV.put(neighborName, true);
    }

    // Distance Vector Algorithm
    public void update(String node, HashMap<String, Integer> distanceVector) {
        if(distanceVector != null) {
            this.logger.log(Level.INFO, "DISTANCE VECTOR> A new Distance Vector has been received... PROCESSING");
            for (String destiny: distanceVector.keySet()) {
                // First step: Add new Destinies if they aren't in the current state of my distance table
                if(!this.distanceTable.containsKey(destiny)) {
                    this.logger.log(Level.INFO, String.format("DISTANCE VECTOR> A new destiny has been discovered %s. Initializing every cost as infinite", destiny));
                    HashMap<String, Integer> newDestinyRegistry = new HashMap<String, Integer>();
                    for (String neighbor: this.neighbors.keySet()) {
                        newDestinyRegistry.put(neighbor, 99);
                    }
                    this.distanceTable.put(destiny, newDestinyRegistry);
                }

                // Updating new value
                this.logger.log(Level.INFO, String.format("DISTANCE VECTOR> Setting up new cost from Node '%s'", node));
                int costFromMeToNode = this.distanceTable.get(node).get(node);
                int newCost = costFromMeToNode + distanceVector.get(destiny);

                if(newCost < this.distanceTable.get(destiny).get(node)) {
                    // Get previous minimum to compare
                    int previousMinimum = this.getMinimumFromRegistry(this.distanceTable.get(destiny));

                    // Update destiny registry
                    this.distanceTable.get(destiny).put(node, newCost);

                    // Get new minimum
                    int newMinimum = this.getMinimumFromRegistry(this.distanceTable.get(destiny));

                    // If new Minimum is smaller than the previous one, change destiny status as a changed
                    if(newMinimum < previousMinimum) {
                        this.minimumDistances.put(destiny, newMinimum);
                    }

                    for (Neighbor neighbor: this.neighbors.values()) {
                        this.hasSentDV.put(neighbor.getName(), false);
                    }
                }

                this.logger.log(Level.INFO, String.format("DISTANCE VECTOR> New cost is: %d", newCost));
            }
            this.logger.log(Level.INFO, "DISTANCE VECTOR> Distance Vector Table must be sent to every neighbor. Distance Vector Table has been marked as dirty");
        }
    }

    // Prints
    public void printVectorDistance() {
        String message = "---\tMinimum Distances Table\t---\n";
        for (String neighbor: this.minimumDistances.keySet()) {
            message += "\t" + neighbor + ":\t" + this.minimumDistances.get(neighbor);
        }

        this.logger.log(Level.INFO, "DISTANCE VECTOR>" + message);
    }

    public void printMinimumDistances() {
        String message = "---\tVector Distance Table\t---\n";
        message += "D(" + this.myName + ")\t";

        // Neighbors
        for (String neighbor: this.neighbors.keySet()) {
            message = message.concat("\t|\t" + neighbor);
        }
        message += "\n";

        for (String destiny: this.distanceTable.keySet()) {
            message = message.concat(destiny + "\t");
            HashMap<String, Integer> registry = this.distanceTable.get(destiny);
            for (int neighborCost: registry.values()) {
                message = message.concat("\t|\t" + neighborCost);
            }
            message += "\n";
        }

        for (String neighbor: this.minimumDistances.keySet()) {
            message += "\t" + neighbor + ":\t" + this.minimumDistances.get(neighbor);
        }

        this.logger.log(Level.INFO, "DISTANCE VECTOR>" + message);
    }

    public void printNeighbors() {}
}
