package tcpdataserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FolderUtilities {
    public static void DeleteFile(String filepath) {
        try {
            Files.delete(Paths.get(filepath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void DeleteDirectory(String folderpath) {
        try {
            Path directory = Paths.get(folderpath);
            if (Files.exists(directory)) {
                Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file,
                                                     BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                            throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isMainClass(String fileName) {
        boolean tmp = false;
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            tmp = stream
                    .anyMatch(lines -> lines.contains("public static void main"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tmp;
    }

    public static String getPackage(String fileName) {
        String line = null;
        String packageName = "";
        try {
            FileReader fReader = new FileReader(fileName);
            BufferedReader fileBuff = new BufferedReader(fReader);
            while ((line = fileBuff.readLine()) != null) {
                if (line.isEmpty())
                    continue;
                if (line.contains("package")) {
                    packageName = line.replace("package", "");
                    packageName = packageName.trim();
                    packageName = packageName.substring(0, packageName.length() - 1);
                }
                break;
            }
            fileBuff.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return packageName;
    }
}
