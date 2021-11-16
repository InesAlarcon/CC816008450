import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppServer implements Runnable {
    private final int serverPort;
    private final Logger logger;
    private final String myName;
    private final HashMap<String, AppFileHandler> fileHandlers;

    AppServer(int serverPort, Logger logger, String myName, HashMap<String, AppFileHandler> fileHandlers){
        this.serverPort = serverPort;
        this.logger = logger;
        this.myName = myName;
        this.fileHandlers = fileHandlers;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(this.serverPort)) {
            try {
                this.logger.log(Level.INFO, "SERVER APPLICATION> Reading message from %s...");

                while (true) {
                    // Creating socket
                    Socket socket = serverSocket.accept();

                    this.logger.log(Level.INFO,
                            "SERVER APPLICATION> New client connected with address: %s");

                    // Setting up buffers and stuff
                    InputStream input = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                    // Awaiting request, read line by line
                    this.logger.log(Level.INFO,
                            "SERVER APPLICATION> Reading request line by line...");
                    LinkedList<String> message = new LinkedList<String>();
                    while (true) {
                        String messageLine = reader.readLine();
                        message.add(messageLine);
                        System.out.println("MESSAGE LINE>>" + messageLine);
                        if (messageLine.equals("EOF")) break;
                    }
                    this.logger.log(Level.INFO,
                            "SERVER APPLICATION> Request has been read");


                    // Awaiting request, read line by line
                    this.logger.log(Level.INFO,
                            "SERVER APPLICATION> Releasing forwarding socket resources");
                    // Releasing resources
                    socket.close();
                    input.close();
                    reader.close();

                    // Checking if request is a file chunk or a file request
                    if (message.get(4).equals("EOF")) {
                        /* Dispatch File request */
                        this.logger.log(Level.INFO,
                                "SERVER APPLICATION> Message asks to dispatch File. Initializing...");


                        // Reading file
                        this.logger.log(Level.INFO,
                                String.format("SERVER APPLICATION> Reading file %s", message.get(2).split(":")[1]));
                        String filename = message.get(2).split(":")[1];
                        Path cwd = Paths.get(System.getProperty("user.dir"));
                        File file = new File(cwd + "\\files\\" + filename);
                        FileInputStream in = new FileInputStream(file);

                        // Calculate total chunks
                        int totalChunks = (int) Math.ceil((double) file.length() / 730.0);
                        this.logger.log(Level.INFO,
                                String.format("SERVER APPLICATION> Total chunks to send: %d", totalChunks));

                        // Creating Message
                        String from = "From:" + this.myName;
                        String to = "To:" + message.get(0).split(":")[1];
                        String name = message.get(2);
                        String size = message.get(3);

                        byte[] buffer = new byte[730];
                        for (int chunkNumber = 0; chunkNumber < totalChunks; chunkNumber++) {
                            String data = "Data:";
                            String frag = "Frag:" + (chunkNumber + 1);

                            // Setting up connection to forwarding module
                            Socket forwardingSocket = new Socket(
                                    InetAddress.getLocalHost(),
                                    1981
                            );

                            // Setting up buffers and stuff
                            OutputStream outputForwarding = forwardingSocket.getOutputStream();
                            PrintWriter writerForwarding = new PrintWriter(outputForwarding, true);

                            // Constructing chunk
                            int rc = in.read(buffer);
                            int i = 0;
                            for (byte b : buffer) {
                                data = data.concat(String.format("%02X", b));
                                if(i == rc ){
                                    break;
                                }
                                i += 1;
                            }

                            this.logger.log(Level.INFO,
                                    String.format("SERVER APPLICATION> Sending chunk number: %d", (chunkNumber + 1)));
                            writerForwarding.println(
                                    from + "\n" +
                                            to + "\n" +
                                            name + "\n" +
                                            data + "\n" +
                                            frag + "\n" +
                                            size + "\n" +
                                            "EOF"

                            );

                            // Releasing resources
                            forwardingSocket.close();
                            outputForwarding.close();
                            writerForwarding.close();
                        }

                        this.logger.log(Level.INFO,
                                String.format("SERVER APPLICATION> File has been sent completely to '%s'",
                                        message.get(0).split(":")[1]));
                    } else {
                        // Dispatch File chunk → ASSUMING ALL CHUNKS START WITH 1
                        String node = message.get(0).split(":")[1];
                        String data = message.get(3).split(":")[1];
                        String fragmentNumber = message.get(4).split(":")[1];
                        if (fragmentNumber.equals("1")) {
                            // Creating new File Handler
                            String filename = message.get(2).split(":")[1];
                            String fileSize = message.get(5).split(":")[1];
                            int totalChunks = (int) Math.ceil(Double.parseDouble(fileSize) / 730.0);
                            AppFileHandler newFileHandler = new AppFileHandler(node, filename, totalChunks, this.logger);

                            // Add initial chunk
                            newFileHandler.pushChunk(1, data);

                            // Add File Handler to File Handlers
                            this.fileHandlers.put(node, newFileHandler);
                        } else {
                            // Adding the rest of the chunks
                            int chunkNumber = Integer.parseInt(message.get(4).split(":")[1]);
                            boolean fileHasBeenCreated = this.fileHandlers.get(node).pushChunk(chunkNumber, data);

                            // Removing File Handler once file has been created
                            if (fileHasBeenCreated) this.fileHandlers.remove(node);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                this.logger.log(Level.SEVERE,
                        String.format("SERVER APPLICATION> An error has occurred: %s. Lost connection to the forwarding module",
                                ex.getMessage()));
            }
        } catch(Exception exe) {
            exe.printStackTrace();
            this.logger.log(Level.SEVERE,
                    String.format("SERVER APPLICATION> Could not create Server Socket",
                            exe.getMessage()));
        }
    }
}
