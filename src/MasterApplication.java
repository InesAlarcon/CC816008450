import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.*;

public class MasterApplication {
    public static void main(String[] args) {
        try {
            if (args.length < 1) return;

            String myName = args[0];

            // Creating logger
            Logger loggerApp = Logger.getLogger(DVMaster.class.getName());

            // Creating consoleHandler and fileHandler
            Path cwd = Paths.get(System.getProperty("user.dir"));
            Handler fileHandler  = new FileHandler(cwd.resolve("application_server.log").toString());

            // Assigning handlers to logger object
            loggerApp.addHandler(fileHandler);

            // Setting logger levels
            fileHandler.setLevel(Level.ALL);
            loggerApp.setLevel(Level.ALL);
            loggerApp.config("MASTER APPLICATION> Configuration done on App logger.");

            // Run Master Server and Client
            loggerApp.log(Level.INFO, "MASTER APPLICATION> Initializing Server Application Server...");
            int serverPort = 15000;
            HashMap<String, AppFileHandler> fileHandlers = new HashMap<String, AppFileHandler>();
            new Thread(new AppServer(serverPort, loggerApp, myName, fileHandlers)).start();
            loggerApp.log(Level.INFO, "MASTER APPLICATION> Server Application is running");

            /* Running Client Side */
            // Setting up connection to forward Module
            loggerApp.log(Level.INFO, "MASTER APPLICATION> Client connecting to Forwarding Module...");
            Socket socket = new Socket(
                    InetAddress.getLocalHost(),
                    1981
            );
            loggerApp.log(Level.INFO, "MASTER APPLICATION> Client side connected to Forwarding Module");

            // Setting up buffers and stuff
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            Scanner sc = new Scanner(System.in);

            // print Files
            File dir = new File("./files");
            String[] children = dir.list();
            if (children == null) {
                System.out.println("No hay archivos");
            } else {
                System.out.println("\n"+" ".repeat(10) + "Listado de archivos\n");
                for (int i = 0; i < children.length; i++) {
                    String filename = children[i];
                    File temp = new File("./files/"+filename);
                    String texto = (i+ 1) + " > " + filename;
                    String espacio = "_".repeat(30-texto.length());
                    System.out.println(texto + espacio + temp.length() + " bytes");
                }
                System.out.println();
            }

            // Indefinite cycle
            while(true) {
                while(true) {
                    System.out.print("Proceed? (Type Y)");
                    if(sc.nextLine().equals("Y")) break;
                }

                loggerApp.log(Level.INFO, "CLIENT APPLICATION> Creating request message");

                // Get node to
                System.out.print("Ask file to who? (Enter Node Nickname): ");
                String toNode = sc.nextLine();

                // Getting file name
                System.out.print("Enter file name: ");
                String filename = sc.nextLine();

                // Getting file size
                System.out.print("Enter file size: ");
                int fileSize = Integer.parseInt(sc.nextLine());

                while(true) {
                    System.out.print("Send? (Type Y, type N to reset message construction)");
                    String response = sc.nextLine();
                    if(response.equals("Y")) {
                        // Constructing message
                        loggerApp.log(Level.INFO, "CLIENT APPLICATION> Sending request message...");
                        writer.println(
                                "From:" + myName + "\n" +
                                        "To:" + toNode + "\n" +
                                        "Name:" + filename + "\n" +
                                        "Size:" + fileSize + "\n" +
                                        "EOF"
                        );
                        loggerApp.log(Level.INFO, "CLIENT APPLICATION> Managing the rest of the request to the server");
                        System.out.print("Message has been sent. The rest of the request, AppServer will manage it");
                    } else if(response.equals("N")) {
                        // Do nothing, reset everything
                        System.out.print("Message has been dropped. Reseting construction");
                        loggerApp.log(Level.INFO, "CLIENT APPLICATION> Resseting message. Not sending previous one");
                        break;
                    }
                }
            }
        } catch (Exception error) {
            System.out.println("CANNOT INITIALIZE MASTER APPLICATION");
        }
    }
}
