package tcpdataclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by capitanfg on 06.11.2017.
 */
public class ZipUtils {

    private List<String> fileList;
    private File sourceFile;

    public ZipUtils(File sf) {
        fileList = new ArrayList<String>();
        sourceFile = sf;
    }

    public void zipIt(String zipFile) {
        byte[] buffer = new byte[1024];
        String source = sourceFile.getName();
        FileOutputStream fos = null;
        ZipOutputStream zos = null;

        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);

            System.out.println("Archieving " + source + " ...");
            FileInputStream in = null;

            for (String file : this.fileList) {
                System.out.println("File added: " + file);
                ZipEntry ze = new ZipEntry(source + File.separator + file);
                zos.putNextEntry(ze);

                try {
                    in = new FileInputStream(sourceFile.toString() + File.separator + file);
                    int len;
                    while ((len = in.read(buffer)) > 0)
                        zos.write(buffer, 0, len);
                } finally {
                    in.close();
                }
            }

            zos.closeEntry();
            System.out.println("Folder successfully compressed");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                zos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void generateFileList(File node) {
        if (node.isFile())
            fileList.add(generateZipEndtry(node.toString()));
        else {// is directory
            String[] subNote = node.list();
            for (String filename : subNote)
                generateFileList(new File(node, filename));
        }
    }

    private String generateZipEndtry(String s) {
        return s.substring(sourceFile.toString().length() + 1, s.length());
    }

}
