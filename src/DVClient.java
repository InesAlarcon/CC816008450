import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DVClient implements Runnable {
    private final Logger logger;
    private final DistanceVector distanceVector;
    private final Socket socket;
    private final String myName;
    private final String neighborServerName;
    private final long interval;
    private final HashMap<String, Boolean> connected;

    DVClient(Logger logger, DistanceVector distanceVector, Socket socket, String myName, String serverName, long interval,
             HashMap<String, Boolean> connected) {
        this.logger = logger;
        this.distanceVector = distanceVector;
        this.socket = socket;
        this.myName = myName;
        this.neighborServerName = serverName;
        this.interval = interval;
        this.connected = connected;
    }

    @Override
    public void run() {
        // Preparing buffers
        OutputStream output = null;
        PrintWriter writer = null;
        InputStream input = null;
        BufferedReader reader = null;

        try {
            this.logger.log(Level.INFO,
                    String.format(
                            "DISTANCE VECTOR CLIENT '%s'> Accepted connection with server %s",
                            this.neighborServerName,
                            socket.getInetAddress().toString()
                    )
            );

            // Setting up buffers and stuff
            output = socket.getOutputStream();
            writer = new PrintWriter(output, true);

            input = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(input));

            // Sending Hello Message
            this.logger.log(Level.INFO,
                    String.format(
                        "DISTANCE VECTOR CLIENT '%s'> Sending HELLO to server.",
                        this.neighborServerName
                    )
            );
            writer.println("From:" + myName + "\n" + "Type:HELLO");
            //writer.println("Type:HELLO");

            String from = reader.readLine();
            String welcome = reader.readLine();
            //System.out.println("WELCOME>>" + welcome);

            while(true) {
                if(!this.distanceVector.hasSent(this.neighborServerName)) {
                    this.logger.log(Level.INFO,
                            String.format(
                                    "DISTANCE VECTOR CLIENT '%s'> Sending DV to server.",
                                    this.neighborServerName
                            )
                    );
                    //writer.println("From:" + myName + "\n" + "Type:DV");
                    //writer.println("Type:DV");
                    String dv = "From:" + myName + "\n" + "Type:DV" + "\n";
                    String dvList = "";

                    HashMap<String, Integer> minimumDistances = this.distanceVector.getMinimumDistances();
                    //writer.println("Len:" + minimumDistances.keySet().size());
                    for (String destiny: minimumDistances.keySet()) {
                        dvList = dvList + destiny + ":" + minimumDistances.get(destiny) + "\n";
                    }

                    writer.println(dv + "Len:" + minimumDistances.keySet().size() + "\n" + dvList);

                    this.distanceVector.setSent(this.neighborServerName);

                } else {
                    // Sending KeepAlive Message
                    this.logger.log(Level.INFO,
                            String.format(
                                    "DISTANCE VECTOR CLIENT '%s'> Sending KEEPALIVE to server.",
                                    this.neighborServerName
                            )
                    );
                    writer.println("From:" + myName + "\n" + "Type:KeepAlive");
                    //writer.println("Type:KeepAlive");
                }

                Thread.sleep(this.interval*1000);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            this.logger.log(Level.SEVERE, String.format(
                    "DISTANCE VECTOR CLIENT '%s'> An error has occurred: %s. Lost connection to the server",
                    this.neighborServerName,
                    ex.toString()
                    )
            );
        }

        try {
            // Releasing resources in case an error has occurred
            if(output != null) output.close();
            if(writer != null) writer.close();
            if(input != null) input.close();
            if(reader != null) reader.close();
            this.socket.close();
        } catch (IOException ex) {
            this.logger.log(Level.INFO, String.format(
                    "DISTANCE VECTOR CLIENT '%s'> Resources have been released",
                    this.neighborServerName
            ));
        }

        // Notify Master Client that something happened and must reconnect
        this.connected.put(neighborServerName, false);
        this.logger.log(Level.INFO, String.format(
                "DISTANCE VECTOR CLIENT '%s'> Status has been set to OFFLINE",
                this.neighborServerName
        ));
    }
}
