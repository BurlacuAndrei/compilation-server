package tcpdataserver;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;

public class ServerUtilities {

    public static HashMap<String, String> readFromConfigFile(String filepath) {
        HashMap<String, String> result = new HashMap<>();
        BufferedReader inputFile = null;
        try {
            inputFile = new BufferedReader(new FileReader(filepath));
            StringBuilder key = new StringBuilder();
            StringBuilder value = new StringBuilder();
            boolean word = false;
            int c;
            while ((c = inputFile.read()) != -1) {
                if ((char) c == '$') {
                    word = true;
                }
                else if ((char) c == '\n') {
                    word = false;
                    result.put(key.toString().trim(), value.toString().trim());
                    key = new StringBuilder();
                    value = new StringBuilder();
                }
                else if (!word && (char) c != '$') {
                    key.append((char) c);
                }
                else if (word && (char) c != '\n') {
                    value.append((char) c);
                }
            }
            if (key.length() > 0 && value.length() > 0)
                result.put(key.toString().trim(), value.toString().trim());
        } catch(IOException ex){
            ex.printStackTrace();
        }
        return result;
    }

    public static String getCommandOutput(String command) {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", command
        );
        builder.redirectErrorStream(true);
        Process p = null;
        String output = "";
        try {
            p = builder.start();
            InputStream is = p.getInputStream();
            Scanner s = new Scanner(is).useDelimiter("\\A");
            output = s.hasNext() ? s.next() : "";
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    public static String getCPULoad() {
        final String command = "wmic cpu get loadpercentage";
        String output = getCommandOutput(command);
        output = output.split("\n")[1].trim();
        return output;
    }

    public static String getAvailableMemory() {
        final String command = "wmic OS get FreePhysicalMemory";
        String output = getCommandOutput(command);
        output = output.split("\n")[1].trim();
        return output;
    }

    public static String getTotalMemory() {
        final String command = "wmic OS get TotalVisibleMemorySize";
        String output = getCommandOutput(command);
        output = output.split("\n")[1].trim();
        return output;
    }

    public static String getAvailableMemoryInProcentage() {
        int freeMemory = Integer.parseInt(getAvailableMemory());
        int totalMemory = Integer.parseInt(getTotalMemory());
        int procentage = freeMemory / totalMemory * 100;

        return Integer.toString(procentage);
    }
}
