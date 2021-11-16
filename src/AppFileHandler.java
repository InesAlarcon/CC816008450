import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppFileHandler {
    private final String node, filename;
    private final int totalChunks;
    private final HashMap<Integer, String> fileController;
    private final Logger logger;

    AppFileHandler(String node, String filename, int totalChunks, Logger logger) {
        this.node = node;
        this.filename = filename;
        this.totalChunks = totalChunks;
        this.fileController = new HashMap<Integer, String>();
        this.logger = logger;
    }

    // Asux method
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    // Getters
    public String getNode() {
        return node;
    }

    public String getFilename() {
        return filename;
    }

    // Setter
    public boolean pushChunk(int chunkNumber, String hexString) {
        // Pushing chunk
        fileController.put(chunkNumber, hexString);
        this.logger.log(Level.INFO,
                String.format("FILE CONTROLLER FOR %s> Chunk has been added", this.filename));

        // Check if all chunks have been received
        if(chunkNumber == totalChunks) {
            this.logger.log(Level.INFO,
                    String.format("FILE CONTROLLER FOR %s> Creating and writing file", this.filename));
            String cwd = System.getProperty("user.dir");

            try {
                // 3. Get all chunks
                FileOutputStream fileOutputStream = new FileOutputStream(cwd + "\\recv_files\\" + filename);

                for (int chunk = 0; chunk < this.totalChunks; chunk++) {
                    fileOutputStream.write(
                            this.hexStringToByteArray(this.fileController.get(chunk+1))
                    );
                }
                fileOutputStream.close();

                this.logger.log(Level.INFO,
                        String.format("FILE CONTROLLER FOR %s> File has been created successfully", this.filename));

                return true;

            } catch (Exception ex) {
                this.logger.log(Level.SEVERE,
                        String.format("FILE CONTROLLER FOR %s> File creation has failed. Error is: %s", this.filename, ex.getMessage()));
                return false;
            }
        }

        return false;
    }
}
