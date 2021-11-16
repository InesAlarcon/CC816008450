import java.nio.file.*;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.*;

public class DVMaster {
    public static void main(String[] args) {
        if (args.length < 1) return;

        try {
            // Creating logger
            Logger loggerDistanceVector = Logger.getLogger(DVMaster.class.getName());
            Logger loggerForwarding = Logger.getLogger(DVMaster.class.getName());

            // Creating consoleHandler and fileHandler
            Handler consoleHandler = new ConsoleHandler();

            Path cwd = Paths.get(System.getProperty("user.dir"));
            Handler fileHandler1  = new FileHandler(cwd.resolve("distance_vector.log").toString());
            Handler fileHandler2  = new FileHandler(cwd.resolve("forwarding.log").toString());

            // Assigning handlers to logger object
            loggerDistanceVector.addHandler(consoleHandler);
            loggerDistanceVector.addHandler(fileHandler1);

            loggerForwarding.addHandler(consoleHandler);
            loggerForwarding.addHandler(fileHandler2);

            // Setting logger levels
            consoleHandler.setLevel(Level.ALL);
            fileHandler1.setLevel(Level.ALL);
            loggerDistanceVector.setLevel(Level.ALL);
            loggerDistanceVector.config("MASTER PROGRAM> Configuration done on Distance Vector Logger.");

            fileHandler2.setLevel(Level.ALL);
            loggerForwarding.setLevel(Level.ALL);
            loggerForwarding.config("MASTER PROGRAM> Configuration done on Forwarding Logger.");

            // Initialization
            String myName = args[0];
            loggerDistanceVector.log(Level.SEVERE, "MASTER PROGRAM> Initializing Distance Vector...");
            DistanceVector distanceVector = new DistanceVector(myName, loggerDistanceVector, "config.txt");
            loggerDistanceVector.log(Level.SEVERE, "MASTER PROGRAM> Distance vector is ready");
            ReentrantLock masterServerLock = new ReentrantLock();
            long masterClientInterval = 20;

            distanceVector.printVectorDistance();
            distanceVector.printMinimumDistances();

            // Run Master Server and Client
            loggerDistanceVector.log(Level.SEVERE, "MASTER PROGRAM> Initializing Master Server...");
            new Thread(new DVMasterServer(loggerDistanceVector, distanceVector, masterServerLock, myName)).start();
            loggerDistanceVector.log(Level.SEVERE, "MASTER PROGRAM> Master server is running");

            Scanner scanner = new Scanner(System.in);
            while(true) {
                System.out.println("Run Clients? (Enter Y to continue)");
                String line = scanner.nextLine();
                if(line.equals("Y")) break;
            }

            loggerDistanceVector.log(Level.SEVERE, "MASTER PROGRAM> Initializing Master Client...");
            new Thread(new DVMasterClient(loggerDistanceVector, distanceVector, myName, masterClientInterval)).start();
            loggerDistanceVector.log(Level.SEVERE, "MASTER PROGRAM> Master client is running");

            // Booting forwarding Module
            loggerForwarding.log(Level.SEVERE, "MASTER PROGRAM> Initializing Forwarding Module...");
            new Thread(new MasterForwarding(1981, distanceVector, myName, loggerForwarding)).start();
            loggerForwarding.log(Level.SEVERE, "MASTER PROGRAM> Forwarding Module is running");

            while (true) {
                Thread.sleep(15*1000);
            }

        } catch( Exception ex) {
            System.out.println("CANNOT INITIALIZE DVMASTER");
        }
    }
}
