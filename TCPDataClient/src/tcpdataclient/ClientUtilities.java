package tcpdataclient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class ClientUtilities {

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
}
