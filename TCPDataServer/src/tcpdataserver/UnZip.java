package tcpdataserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by capitanfg on 06.11.2017.
 */
public class UnZip {

    public void unZipIt(String zipFile, String outputFolder) {
        byte[] buffer = new byte[1024];

        try {
            // create directory
            File folder = new File(outputFolder);
            if (!folder.exists())
                folder.mkdir();

            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();

            while(ze != null) {
                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                System.out.println("File unzip: " + newFile.getAbsolutePath());

                String filepath = newFile.getAbsolutePath();
                filepath = filepath.replace(outputFolder, "");
                String[] folderToCreate = filepath.split("\\\\");
                String currentFolder = outputFolder;
                for (int i = 0; i < folderToCreate.length - 1; ++i ) {
                    File folder_from_archive = new File(currentFolder + folderToCreate[i]);
                    if (!folder_from_archive.exists())
                        folder_from_archive.mkdir();
                    currentFolder += folderToCreate[i] + "\\";
                }
                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0)
                    fos.write(buffer, 0, len);

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

            System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
