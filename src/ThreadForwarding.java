// Useful Stuff
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadForwarding implements Runnable {
    private final DistanceVector distanceVector;
    private final Socket socket;
    private final String myName;
    private final Logger logger;

    ThreadForwarding(DistanceVector distanceVector, Socket socket, String myName, Logger logger){
        this.distanceVector = distanceVector;
        this.socket = socket;
        this.myName = myName;
        this.logger = logger;
    }

    @Override
    public void run() {
        // Preparing buffers
        InputStream input = null;
        BufferedReader reader = null;

        try {
            this.logger.log(Level.INFO,
                    String.format(
                            "FORWARDING SERVER '%s'> Reading message from %s...",
                            this.myName,
                            socket.getInetAddress().toString()));

            // Setting up buffers and stuff
            input = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(input));

            // Read line by line
            LinkedList<String> message = new LinkedList<String>();
            while(true){
                String messageLine = reader.readLine();
                message.add(messageLine);
                System.out.println("MESSAGE LINE>>" + messageLine);
                if (messageLine.equals("EOF")) break;
            }

            // Check if the message is for me
            String receptor = message.get(1).split(":")[1];
            if (receptor.equals(this.myName)) {
                this.logger.log(Level.INFO,
                        String.format(
                                "FORWARDING SERVER '%s'> The message's receptor matches our name. Initializing forwarding process...",
                                this.myName
                                ));

                // Connect to AppServer and forward message
                Socket appSocket = new Socket(
                        InetAddress.getLocalHost(),
                        15000
                );

                // Setting up buffers and stuff
                OutputStream appOutput = appSocket.getOutputStream();
                PrintWriter appWriter = new PrintWriter(appOutput, true);

                // Forwarding
                this.logger.log(Level.INFO,
                        String.format(
                                "FORWARDING SERVER '%s'> Forwarding message to App Server version...",
                                this.myName
                        ));

                for (String parameter: message) {
                    appWriter.println(parameter);
                }

                // Releasing resources
                appSocket.close();
                appOutput.close();
                appWriter.close();

                this.logger.log(Level.INFO,
                        String.format(
                                "FORWARDING SERVER '%s'> Message has been forwarded. Resources have been released too.",
                                this.myName
                        ));
            } else {
                this.logger.log(Level.INFO,
                        String.format(
                                "FORWARDING SERVER '%s'> The message's receptor do not match our name. Initializing forwarding process...",
                                this.myName
                        ));

                // Getting registry from Distance Vector
                if(this.distanceVector.getDistanceTable().containsKey(receptor)) {
                    this.logger.log(Level.INFO,
                            String.format(
                                    "FORWARDING SERVER '%s'> Destiny has been found in the Distance Vector Table. Forwarding message.",
                                    this.myName
                            ));

                    HashMap<String, Integer> destinyRegistry = this.distanceVector.getRegistryOfDestiny(receptor);

                    // Get minimum cost and determine Neighbor to forward
                    int minimumCost = 99;
                    String neighborName = "";
                    for (String neighbor: destinyRegistry.keySet()) {
                        int cost = destinyRegistry.get(neighbor);
                        if (cost < minimumCost) {
                            minimumCost = cost;
                            neighborName = neighbor;
                        }
                    }

                    // Connect to AppServer and forward message
                    Socket neighborForwardingSocket = new Socket(
                            this.distanceVector.getNeighbors().get(neighborName).getNetworkData().getIpAddress(),
                            this.distanceVector.getNeighbors().get(neighborName).getNetworkData().getForwardingPort()
                    );

                    // Setting up buffers and stuff
                    OutputStream neighborOutput = neighborForwardingSocket.getOutputStream();
                    PrintWriter neighborWriter = new PrintWriter(neighborOutput, true);

                    // Forwarding
                    this.logger.log(Level.INFO,
                            String.format(
                                    "FORWARDING SERVER '%s'> Forwarding message to Neighbor's Forwarding Module...",
                                    this.myName
                            ));

                    String forwardMessage = "";
                    for (String parameter: message) {
                        forwardMessage = forwardMessage.concat(parameter + "\n");
                    }

                    neighborWriter.println(forwardMessage);

                    // Releasing resources
                    neighborWriter.close();
                    neighborOutput.close();
                    neighborForwardingSocket.close();

                    this.logger.log(Level.INFO,
                            String.format(
                                    "FORWARDING SERVER '%s'> Message has been forwarded. Resources have been released too.",
                                    this.myName
                            ));

                } else {
                    // Dropping message
                    this.logger.log(Level.SEVERE,
                            String.format("FORWARDING SERVER '%s'> Destiny does not contain the Distance Vector Table. Dropping it and sending error message.",
                                    this.myName));
                }
            }

        } catch (Exception error) {
            error.printStackTrace();
            this.logger.log(Level.SEVERE,
                    String.format("FORWARDING SERVER '%s'> An error has occurred: %s. Lost connection to a client.",
                            this.myName, error.toString()));
        }

        try {
            // Releasing resources in case an error has occurred
            if(input != null) input.close();
            if(reader != null) reader.close();
            this.socket.close();
        } catch (IOException ex) {
            this.logger.log(Level.INFO, String.format("FORWARDING SERVER '%s'> Resources have been released", this.myName));
        }
    }
}
