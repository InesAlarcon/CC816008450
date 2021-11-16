// Useful Stuff
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MasterForwarding implements Runnable {
    private final int forwardPort;
    private final DistanceVector distanceVector;
    private final String myName;
    private final Logger logger;

    MasterForwarding(int forwardPort, DistanceVector distanceVector, String myName, Logger logger){
        this.forwardPort = forwardPort;
        this.distanceVector = distanceVector;
        this.myName = myName;
        this.logger = logger;
    }

    @Override
    public void run() {

            try(ServerSocket serverSocket = new ServerSocket(this.forwardPort)) {
                while (true){
                    Socket newSocket = serverSocket.accept();
                    this.logger.log(Level.INFO,
                            String.format(
                                    "MASTER FORWARDING> Got new message from %s: creating Thread to dispatch.",
                                    newSocket.getInetAddress().toString()
                            )
                    );

                    new Thread(new ThreadForwarding(this.distanceVector, newSocket, this.myName, this.logger)).start();
                }
            } catch (Exception ex) {
                this.logger.log(Level.SEVERE, String.format("MASTER FORWARDING> Failed to establish connection: %s", ex.getMessage()));
            }

    }
}
