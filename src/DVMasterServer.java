// Useful Stuff
import java.util.logging.Level;
import java.util.logging.Logger;

// Network Stuff
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

public class DVMasterServer implements Runnable {
    final int serverPort = 9080;
    private final Logger logger;
    private final DistanceVector distanceVector;
    private final Lock masterLock;
    private final String myName;

    DVMasterServer(Logger logger, DistanceVector distanceVector, Lock lock, String myName) {
        this.logger = logger;
        this.distanceVector = distanceVector;
        this.masterLock = lock;
        this.myName = myName;
    }

    @Override
    public void run() {
        while(true) {
            try(ServerSocket serverSocket = new ServerSocket(this.serverPort)) {
                Socket newSocket = serverSocket.accept();
                this.logger.setLevel(Level.INFO);
                this.logger.log(Level.INFO,
                        String.format(
                                "MASTER SERVER> Got new connection: %s,creating Thread to dispatch.",
                                newSocket.getInetAddress().toString()
                        )
                );

                new Thread(new DVServer(this.logger, this.distanceVector, newSocket, this.masterLock, this.myName)).start();

            } catch (Exception ex) {
                this.logger.log(Level.SEVERE, String.format("MASTER SERVER> Failed to establish connection: %s", ex.getMessage()));
            }
        }
    }
}
