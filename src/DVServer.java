import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DVServer implements Runnable {
    final int serverPort = 9080;
    private final Logger logger;
    private final DistanceVector distanceVector;
    private final Socket socket;
    private final Lock masterLock;
    private final String myName;

    DVServer(Logger logger, DistanceVector distanceVector, Socket socket, Lock lock, String myName) {
        this.logger = logger;
        this.distanceVector = distanceVector;
        this.socket = socket;
        this.masterLock = lock;
        this.myName = myName;
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
                            "DISTANCE VECTOR SERVER '%s'> Reading request from %s",
                            this.myName,
                            socket.getInetAddress().toString()));

            // Setting up buffers and stuff
            output = socket.getOutputStream();
            writer = new PrintWriter(output, true);

            input = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(input));

            while(true) {
                String node = reader.readLine().split(":")[1];
                String type = reader.readLine().split(":")[1];

                if(type.equals("HELLO")) {
                    // Sending Welcome Message
                    writer.println("From:" + this.myName + "\n" + "Type:WELCOME");
                    //writer.println("Type:WELCOME");
                    this.logger.log(Level.INFO, String.format("DISTANCE VECTOR SERVER '%s'> Sent WELCOME message to '%s'", this.myName, node));
                } else if(type.equals("DV")) {
                    // Read new Distance Vector received
                    HashMap<String, Integer> distanceVector = new HashMap<String, Integer>();
                    int len = Integer.parseInt(reader.readLine().split(":")[1]);
                    for(int i = 0; i < len; i++) {
                        String[] parameters = reader.readLine().split(":");
                        if(!parameters[0].equals(this.myName)) {
                            distanceVector.put(parameters[0], Integer.parseInt(parameters[1]));
                        }
                    }

                    // Acquiring lock for modifying DistanceVector
                    this.masterLock.lock();
                    this.logger.log(Level.INFO, String.format("DISTANCE VECTOR SERVER '%s'> Updating my Distance Vector...", this.myName));
                    this.distanceVector.update(node, distanceVector);
                    this.logger.log(Level.INFO, String.format("DISTANCE VECTOR SERVER '%s'> Distance Vector Table updated", this.myName));
                    this.distanceVector.printMinimumDistances();
                    this.masterLock.unlock();

                } else if(type.equals("KeepAlive")) {
                    // Do nothing
                    this.logger.log(Level.INFO, String.format("DISTANCE VECTOR SERVER '%s'> Received KeepAlive from '%s'", this.myName, node));
                } else {
                    this.logger.log(Level.SEVERE, String.format("DISTANCE VECTOR SERVER '%s'> Unkown Type found %s", this.myName, type));
                }
            }
        } catch (Exception error) {
            this.logger.log(Level.SEVERE,
                    String.format("DISTANCE VECTOR SERVER '%s'> An error has occurred: %s. Lost connection to a client.",
                            this.myName, error.toString()));
        }

        try {
            // Releasing resources in case an error has occurred
            if(output != null) output.close();
            if(writer != null) writer.close();
            if(input != null) input.close();
            if(reader != null) reader.close();
            this.socket.close();
        } catch (IOException ex) {
            this.logger.log(Level.INFO, String.format("DISTANCE VECTOR SERVER '%s'> Resources have been released", this.myName));
        }
    }
}
