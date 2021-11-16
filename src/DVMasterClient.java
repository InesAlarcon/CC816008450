// Useful Stuff
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

// Network Stuff
import java.net.Socket;

public class DVMasterClient implements Runnable {
    private final long checkingTime;
    private final Logger logger;
    private final DistanceVector distanceVector;
    private final String myName;
    private final long interval;
    private final HashMap<String, Boolean> connected;

    DVMasterClient(Logger logger, DistanceVector distanceVector, String myName, long interval) {
        this.checkingTime = 10;
        this.logger = logger;
        this.distanceVector = distanceVector;
        this.myName = myName;
        this.interval = interval;
        this.connected = new HashMap<String, Boolean>();

        for (String neighbor: this.distanceVector.getNeighbors().keySet()) {
            this.connected.put(neighbor, false);
        }
    }

    @Override
    public void run() {
        while(true) {
            try{

                // For each 10 seconds, check all connection and reboot the ones with status = false
                Thread.sleep(this.checkingTime*1000);

                // Checking all connections and rebooting the needed ones
                for (String neighbor: this.connected.keySet()) {
                    if(!this.connected.get(neighbor)) {
                        try {
                            // Setting up Connection
                            String ipAddress = this.distanceVector.getNeighbors().get(neighbor).getNetworkData().getIpAddress();
                            int serverPort = this.distanceVector.getNeighbors().get(neighbor).getNetworkData().getRoutingPort();
                            //System.out.println(serverPort);
                            Socket socket = new Socket(ipAddress, serverPort);

                            this.logger.log(Level.INFO,
                                    String.format("MASTER CLIENT> Creating connection to server '%s'",
                                            neighbor)
                            );

                            new Thread(
                                    new DVClient(
                                            this.logger,
                                            this.distanceVector,
                                            socket,
                                            this.myName,
                                            neighbor,
                                            this.interval,
                                            this.connected
                                    )
                            ).start();

                            this.connected.put(neighbor, true);

                        } catch (Exception error) {
                            this.logger.log(
                                    Level.SEVERE,
                                    String.format("MASTER CLIENT> Could not connect to neighbor '%s', error: '%s'", neighbor, error)
                            );
                        }
                    }
                }

            } catch (InterruptedException ex) {
                this.logger.log(Level.SEVERE, "MASTER CLIENT> An internal error has occurred, rebooting");
            }
        }
    }
}
