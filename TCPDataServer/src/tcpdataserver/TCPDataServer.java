package tcpdataserver;

import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by capitanfg on 27.10.2017.
 */
public class TCPDataServer {

    private static HashMap<String, String> environmentVariables;

    public static void main(String[] args) {
        ServerSocket server_socket = null;
        try {
            String configFile = "server.config";
            environmentVariables = ServerUtilities.readFromConfigFile(configFile);
            try {
                server_socket = new ServerSocket(7304);
            } catch (BindException e) {
                e.printStackTrace();
            }
            ExecutorService executor = Executors.newFixedThreadPool(5);
            while(true) {
                new Thread(new CheckForAvailability()).start();
                Thread worker = new Thread(new ClientWorker(server_socket.accept(), environmentVariables));
                executor.execute(worker);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientWorker implements Runnable {

    private Socket target_socket;
    private DataInputStream din;
    private DataOutputStream dout;
    private static HashMap<String, String> environmentVariables;

    public ClientWorker(Socket recv_socker, HashMap<String, String> hash) {
        target_socket = recv_socker;

        try {
            din = new DataInputStream(target_socket.getInputStream());
            dout = new DataOutputStream(target_socket.getOutputStream());
            environmentVariables = hash;
            System.out.println(environmentVariables);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String default_folder = environmentVariables.get("ProjDirectory");
        RandomAccessFile rw = null;
        long current_file_pointer = 0;
        boolean loop_break = false;
        boolean program_sent = false;
        String file_name = null;
        String download_message = "";

        Short projType = 0;
        String[] downloadParams = null;

        long downloadTimeStart = 0;
        long downloadTimeStop = 0;

        long compileTimeStart = 0;
        long compileTimeStop = 0;

        while(true) {
            byte[] initialize = new byte[1];
            try {
                try {
                    din.read(initialize, 0, initialize.length);
                    if (initialize[0] == 2) {
                        byte[] cmd_buffer = new byte[3];
                        din.read(cmd_buffer, 0, cmd_buffer.length);
                        byte[] recv_data = ReadStream();
                        File execFile;
                        switch (Integer.parseInt(new String(cmd_buffer))) {
                            case 122:
                                long time = compileTimeStop - downloadTimeStart;
                                dout.write(CreateDataPacket("123".getBytes("UTF8"), String.valueOf(time).getBytes("UTF8")));
                                dout.flush();
                                loop_break = true;
                                target_socket.close();
                                break;
                            case 123:
                                dout.write(CreateDataPacket("124".getBytes("UTF8"), projType.toString().getBytes("UTF8")));
                                dout.flush();
                                break;
                            case 124:
                                downloadTimeStart = System.nanoTime();
                                file_name = new String(recv_data);
                                rw = new RandomAccessFile(default_folder + file_name, "rw");
                                dout.write(CreateDataPacket("125".getBytes("UTF8"), String.valueOf(current_file_pointer).getBytes("UTF8")));
                                dout.flush();
                                break;
                            case 126:
                                rw.seek(current_file_pointer);
                                rw.write(recv_data);
                                System.out.println("Download precentage: " + ((float) current_file_pointer / rw.length()) * 100);
                                current_file_pointer = rw.getFilePointer();
                                dout.write(CreateDataPacket("125".getBytes("UTF8"), String.valueOf(current_file_pointer).getBytes("UTF8")));
                                dout.flush();
                                break;
                            case 127:
                                downloadTimeStop = System.nanoTime();
                                if (new String(recv_data).equals("Compile")) {
                                    // unzip the archive with project received
                                    compileTimeStart = System.nanoTime();
                                    String archName = default_folder + file_name;
                                    String folderToCompile = archName.split(".zip")[0] + "\\";
                                    FolderUtilities.DeleteDirectory(folderToCompile);
                                    new UnZip().unZipIt(archName, default_folder);
                                    rw.close();
                                    FolderUtilities.DeleteFile(archName);
                                    // Compiling faze
                                    CompileProject cp = new CompileProject(folderToCompile, environmentVariables.get("VSCompilerPath"));
                                    cp.startCompiling();
                                    byte[] compileOutput = cp.getOutput();
                                    projType = cp.getProjectType();
                                    dout.write(CreateDataPacket("128".getBytes("UTF8"), compileOutput));
                                    compileTimeStop = System.nanoTime();
                                }
                                break;
                            case 224:
                                downloadParams = new String(recv_data).split(" ");
                                file_name = downloadParams[0];
                                projType = Short.valueOf(downloadParams[1]);
                                if (projType == 5)
                                    execFile = new File(default_folder + file_name.split(".zip")[0]
                                            + "\\" + "program.jar");
                                else
                                    execFile = new File(default_folder + file_name.split(".zip")[0]
                                        + "\\" + "program.exe");
                                download_message = "Executable";
                                rw = new RandomAccessFile(execFile, "r");
                                dout.write(CreateDataPacket("225".getBytes("UTF8"), execFile.getName().getBytes("UTF-8")));
                                dout.flush();
                                break;
                            case 225:
                                downloadParams = new String(recv_data).split(" ");
                                file_name = downloadParams[0];
                                projType = Short.valueOf(downloadParams[1]);
                                if (projType == 1 | projType == 2 | projType == 5) {
                                    download_message = "Objects";
                                    File f = new File(default_folder + file_name.split(".zip")[0] + "\\obj");
                                    ZipUtils addZip = new ZipUtils(f);
                                    addZip.generateFileList(f);
                                    addZip.zipIt(f.getName() + ".zip");
                                    execFile = new File(f.getName() + ".zip");
                                    rw = new RandomAccessFile(execFile, "r");
                                    dout.write(CreateDataPacket("225".getBytes("UTF8"), execFile.getName().getBytes("UTF-8")));
                                    dout.flush();
                                } else if (projType == 3 | projType == 4) {
                                    execFile = new File(default_folder + file_name.split(".zip")[0]
                                            + "\\" + "program.exe");
                                    download_message = "Executable";
                                    rw = new RandomAccessFile(execFile, "r");
                                    dout.write(CreateDataPacket("225".getBytes("UTF8"), execFile.getName().getBytes("UTF-8")));
                                    dout.flush();
                                } else {
                                    String errorMessaje = "Compile error";
                                    dout.write(CreateDataPacket("225".getBytes("UTF8"), errorMessaje.getBytes("UTF-8")));
                                    dout.flush();
                                }
                                break;
                            case 226:
                                current_file_pointer = Long.valueOf(new String(recv_data)).longValue();
                                int buff_len = (int) (rw.length() - current_file_pointer < 2000 ? rw.length() - current_file_pointer : 2000);
                                byte[] temp_buff = new byte[buff_len];
                                if (current_file_pointer != rw.length()) {
                                    rw.seek(current_file_pointer);
                                    rw.read(temp_buff, 0, temp_buff.length);
                                    dout.write(CreateDataPacket("227".getBytes("UTF8"), temp_buff));
                                    dout.flush();
                                    System.out.println("Upload precentage: " + ((float) current_file_pointer / rw.length()) * 100);
                                } else {
                                    rw.close();
                                    if (new File("obj.zip").exists())
                                        FolderUtilities.DeleteFile("obj.zip");
                                    program_sent = true;
                                    dout.write(CreateDataPacket("228".getBytes("UTF8"), download_message.getBytes("UTF8")));
                                    dout.flush();
                                }
                                break;
                        }
                    }
                    if (loop_break) {
                        long downloadTime = downloadTimeStop - downloadTimeStart;
                        long compileTime = compileTimeStop - compileTimeStart;
                        double coefficient = (double) (downloadTime + compileTime) / current_file_pointer;
                        File metricFile = new File("metrics.txt");
                        if (metricFile.exists()) {
                            ArrayList<Double> coefsArray = getCoefs();

                            if (coefsArray.size() < 5) {
                                BufferedWriter buf_w = new BufferedWriter(new FileWriter(metricFile, true));
                                buf_w.write(((Double)coefficient).toString() + " ");
                                buf_w.close();
                            }
                            else {
                                coefsArray.add(coefficient);
                                double execTimeCoef = executionTimePerMillis(coefsArray);
                                int index = 0;
                                double max_difference = execTimeCoef - coefsArray.get(0);
                                for (int i = 1; i < coefsArray.size(); ++i) {
                                    double temp = execTimeCoef - coefsArray.get(i);
                                    if (temp > max_difference) {
                                        max_difference = temp;
                                        index = i;
                                    }
                                }
                                coefsArray.remove(index);

                                BufferedWriter bw = new BufferedWriter(new FileWriter(metricFile));
                                for (Double c : coefsArray) {
                                    bw.write(c.toString() + " ");
                                }
                                bw.close();
                            }
                        } else {
                            metricFile.createNewFile();
                            BufferedWriter buf_w = new BufferedWriter(new FileWriter(metricFile, false));
                            buf_w.write(((Double)coefficient).toString() + " ");
                            buf_w.close();
                        }
                        target_socket.close();
                        break;
                    }
                } catch (SocketException e) {
                    System.out.println("Client socket closed");
                    target_socket.close();
                    break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ArrayList<Double> getCoefs() throws IOException {
        ArrayList<Double> coefsArray = new ArrayList<Double>();

        File metricFile = new File("metrics.txt");

        BufferedReader br = new BufferedReader(new FileReader(metricFile));
        String line = br.readLine();
        br.close();
        if (line != null) {
            String[] coefs = line.split(" ");

            for (String s : coefs)
                coefsArray.add(Double.parseDouble(s));
        }
        return coefsArray;
    }

    private double executionTimePerMillis(ArrayList<Double> coefs) {
        double sum = 0;
        for (double c : coefs)
            sum += c;
        return sum / coefs.size();
    }

    private byte[] ReadStream() {
        byte[] data_buff = null;
        try {
            int b = 0;
            String buff_length = "";
            while((b = din.read()) != 4) {
                buff_length += (char)b;
            }

            int data_length = Integer.parseInt(buff_length);
            data_buff = new byte[data_length];
            int byte_read = 0;
            int byte_offset = 0;

            while (byte_offset < data_length) {
                byte_read = din.read(data_buff, byte_offset, data_length - byte_offset);
                byte_offset += byte_read;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data_buff;
    }

    private byte[] CreateDataPacket(byte[] cmd, byte[] data) {
        byte[] packet = null;
        try {
            byte[] initialize = new byte[1];
            initialize[0] = 2;
            byte[] separator = new byte[1];
            separator[0] = 4;
            byte[] data_length = String.valueOf(data.length).getBytes("UTF8");
            packet = new byte[initialize.length + cmd.length + separator.length + data.length + data_length.length];

            System.arraycopy(initialize, 0, packet, 0, initialize.length);
            System.arraycopy(cmd, 0, packet, initialize.length, cmd.length);
            System.arraycopy(data_length, 0, packet, initialize.length + cmd.length, data_length.length);
            System.arraycopy(separator, 0, packet, initialize.length + cmd.length + data_length.length, separator.length);
            System.arraycopy(data, 0, packet, initialize.length + cmd.length + data_length.length + separator.length, data.length);


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return packet;
    }

}
