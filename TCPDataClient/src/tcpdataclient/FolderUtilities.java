package tcpdataclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FolderUtilities {
    public static void DeleteFile(String filepath) {
        try {
            Files.delete(Paths.get(filepath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
