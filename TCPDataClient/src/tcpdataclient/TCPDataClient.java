package tcpdataclient;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * Created by capitanfg on 27.10.2017.
 */
public class TCPDataClient {

    private TCPDataClient obj;
    private Socket obj_client;
    private DataInputStream din;
    private DataOutputStream dout;
    String folder = "";
    private File target_file;
    private HashMap<String, String> environmentVariables;
    private Controller controller;
    short projectType = 0;

    public TCPDataClient(Controller ctr) {
        environmentVariables = ClientUtilities.readFromConfigFile("client.config");
        controller = ctr;
    }

    public String chooseFolder() {
        // choose project directory
        try {
            DirectoryChooser jfc = new DirectoryChooser();
            folder = jfc.showDialog(new Stage()).getAbsolutePath();

            return folder;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String compileProject() {
        try {
            // connect to TCP server
            obj = new TCPDataClient(controller);
            obj_client = new Socket(InetAddress.getByName(environmentVariables.get("ServerIPAddr")), 6578);
            din = new DataInputStream(obj_client.getInputStream());
            dout = new DataOutputStream(obj_client.getOutputStream());
            String output = "";
            String estimatedTime = "";

            if (!folder.equals("")) {
                File file = new File(folder);

                // archive directory
                ZipUtils addZip = new ZipUtils(file);
                addZip.generateFileList(file);
                addZip.zipIt(file.getName() + ".zip");
                target_file = new File(file.getName() + ".zip");

                RandomAccessFile rw = new RandomAccessFile(target_file, "r");

                dout.write(obj.CreateDataPacket("124".getBytes("UTF8"), target_file.getName().getBytes("UTF-8")));
                dout.flush();

                long current_file_pointer = 0;
                boolean project_send = false;
                boolean loop_close = false;
                while (true) {
                    if (din.read() == 2) {
                        byte[] cmd_buffer = new byte[3];
                        din.read(cmd_buffer, 0, cmd_buffer.length);
                        byte[] recv_buffer = obj.ReadStream(din);
                        switch (Integer.parseInt(new String(cmd_buffer))) {
                            case 123:
                                estimatedTime = new String(recv_buffer, StandardCharsets.UTF_8);
                                System.out.println("Estimated time: " + estimatedTime);
                                String time = formatTime(Long.parseLong(estimatedTime));
                                controller.setTextLabel_Time(time);
                                loop_close = true;
                                break;
                            case 124:
                                projectType = Short.valueOf(new String(recv_buffer));
                                dout.write(CreateDataPacket("122".getBytes("UTF8"), String.valueOf(file.length()).getBytes("UTF8")));
                                dout.flush();
                                break;
                            case 125:
                                current_file_pointer = Long.valueOf(new String(recv_buffer)).longValue();
                                int buff_len = (int) (rw.length() - current_file_pointer < 2000 ? rw.length() - current_file_pointer : 2000);
                                byte[] temp_buff = new byte[buff_len];
                                if (current_file_pointer != rw.length()) {
                                    rw.seek(current_file_pointer);
                                    rw.read(temp_buff, 0, temp_buff.length);
                                    dout.write(obj.CreateDataPacket("126".getBytes("UTF8"), temp_buff));
                                    dout.flush();
                                    System.out.println("Upload precentage: " + ((float) current_file_pointer / rw.length()) * 100);
                                } else {
                                    rw.close();
                                    FolderUtilities.DeleteFile(target_file.getAbsolutePath());
                                    project_send = true;
                                }
                                break;
                            case 128:
                                byte[] compilingOutput = recv_buffer;
                                output = new String(compilingOutput, StandardCharsets.UTF_8);
                                System.out.println(output);
                                dout.write(CreateDataPacket("123".getBytes("UTF8"), String.valueOf(file.length()).getBytes("UTF8")));
                                dout.flush();
                                break;
                        }
                    }
                    if (project_send) {
                        System.out.println("Server is processing your data ...");
                        dout.write(obj.CreateDataPacket("127".getBytes("UTF8"), "Compile".getBytes("UTF8")));
                        dout.flush();
                        project_send = false;
                    }
                    if (loop_close) {
                        System.out.println("Closing connection with server");
                        obj_client.close();
                        System.out.println("Client Socket Closed");
                        target_file.delete();

                        break;
                    }
                }
            }
            return output;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String downloadExe(boolean withartefacts) {
        String output = "";
        try {
            try {
                boolean file_sent = false;
                String messaje = "";
                obj = new TCPDataClient(controller);
                obj_client = new Socket(InetAddress.getByName(environmentVariables.get("ServerIPAddr")), 6578);
                din = new DataInputStream(obj_client.getInputStream());
                dout = new DataOutputStream(obj_client.getOutputStream());
                String dowloadParams = target_file.getName() + " " + Short.toString(projectType);
                if (projectType == 1 | projectType == 2 | projectType == 5) {
                    if (withartefacts)
                        dout.write(obj.CreateDataPacket("224".getBytes("UTF8"),
                                dowloadParams.getBytes("UTF-8")));
                    else
                        dout.write(obj.CreateDataPacket("225".getBytes("UTF8"),
                                dowloadParams.getBytes("UTF-8")));
                }
                else if (projectType == 3 | projectType == 4) {
                    dout.write(obj.CreateDataPacket("224".getBytes("UTF8"),
                            dowloadParams.getBytes("UTF-8")));
                }
                dout.flush();
                RandomAccessFile rw = null;
                long current_file_pointer = 0;
                String file_name = null;

                while (true) {
                    if (din.read() == 2) {
                        byte[] cmd_buffer = new byte[3];
                        din.read(cmd_buffer, 0, cmd_buffer.length);
                        byte[] recv_data = ReadStream(din);

                        switch (Integer.parseInt(new String(cmd_buffer))) {
                            case 225:
                                file_name = new String(recv_data);
                                rw = new RandomAccessFile(folder + "\\" + file_name, "rw");
                                current_file_pointer = 0;
                                dout.write(obj.CreateDataPacket("226".getBytes("UTF8"),
                                        String.valueOf(current_file_pointer).getBytes("UTF8")));
                                dout.flush();
                                break;
                            case 227:
                                rw.seek(current_file_pointer);
                                rw.write(recv_data);
                                if (recv_data.length < 2000 && recv_data.length > 100)
                                    System.out.println("debug");
                                System.out.println("Downloasd precentsge: " +
                                        ((float) current_file_pointer / rw.length()) * 100);
                                current_file_pointer = rw.getFilePointer();
                                dout.write(obj.CreateDataPacket("226".getBytes("UTF8"),
                                        String.valueOf(current_file_pointer).getBytes("UTF8")));
                                dout.flush();
                                break;
                            case 228:
                                messaje = new String(recv_data);
                                if (messaje.equals("Executable")) {
                                    output = "Gasesti fisierul executabil la locatia " + folder;
                                    file_sent = true;
                                    rw.close();
                                }
                                else if (messaje.equals("Objects")) {
                                    file_sent = true;
                                    String archName = folder + "\\" + file_name;
                                    new UnZip().unZipIt(archName ,folder);
                                    rw.close();
                                    FolderUtilities.DeleteFile(archName);
                                    output += "Gasesti fisierele *.o la locatia " + folder + "\\obj";
                                }
                                break;

                        }
                    }
                    if (file_sent) {
                        obj_client.close();
                        System.out.println(messaje);
                        System.out.println("Client Socket Closed");
                        break;
                    }
                }
            } catch (SocketException e) {
                System.out.println("Server socket closed");
                obj_client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    // A method that converts the nano-seconds to Seconds-Minutes-Hours form
    private String formatTime(long nanoSeconds)
    {
        int hours, minutes, remainder, totalSecondsNoFraction;
        double totalSeconds, seconds;

        totalSeconds = (double) nanoSeconds / 1000000000.0;

        hours = (int) totalSeconds / 3600;
        totalSeconds -= hours * 3600;

        minutes = (int) totalSeconds / 60;
        totalSeconds -= minutes * 60;

        seconds = totalSeconds;

        // Formatting the string that conatins hours, minutes and seconds
        StringBuilder result = new StringBuilder("");
        if (hours > 0) {
            if (hours < 10)
                result.append("0");
            result.append(hours);
        }
        else
            result.append("00");
        result.append(":");
        if (minutes > 0) {
            if (minutes < 10)
                result.append("0");
            result.append(minutes);
        }
        else
            result.append("00");
        result.append(":");
        if (seconds > 0) {
            if (seconds < 10)
                result.append("0");
            DecimalFormat df = new DecimalFormat("##.00");
            result.append(df.format(seconds));
        }
        else
            result.append("00");
        return result.toString();
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
            System.arraycopy(separator, 0, packet, initialize.length + cmd.length +
                    data_length.length, separator.length);
            System.arraycopy(data, 0, packet, initialize.length + cmd.length +
                    data_length.length + separator.length, data.length);


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return packet;
    }

    private byte[] ReadStream(DataInputStream din) {
        byte[] data_buff = null;
        try {
            int b = 0;
            String buff_length = "";
            while ((b = din.read()) != 4) {
                buff_length += (char) b;
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
}