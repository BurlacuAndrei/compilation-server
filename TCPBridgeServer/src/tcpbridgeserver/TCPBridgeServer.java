package tcpbridgeserver;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

/**
 * Created by capitanfg on 27.10.2017.
 */
public class TCPBridgeServer {
    public static HashMap<Socket, String> bindedHosts = new HashMap<>();

    public static void main(String[] args) {
        ServerSocket server_socket = null;

        try {
            server_socket = new ServerSocket(6578);

            while(true) {
                new Thread(new ClientWorker(server_socket.accept())).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientWorker implements Runnable {

    private Socket target_socket;
    private DataInputStream client_din;
    private DataOutputStream client_dout;

    private Socket server_socket;
    private DataInputStream server_din;
    private DataOutputStream server_dout;

    public ClientWorker(Socket recv_socket) {
        target_socket = recv_socket;

        try {
            client_din = new DataInputStream(target_socket.getInputStream());
            client_dout = new DataOutputStream(target_socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            //server_socket = new Socket(InetAddress.getByName(new ServerComunication().fitestServer()), 6579);
            ServerComunication sc = new ServerComunication();

            String serverIPAddres;
            if (TCPBridgeServer.bindedHosts.containsKey(target_socket))
                serverIPAddres = TCPBridgeServer.bindedHosts.get(target_socket);
            else {
                serverIPAddres = sc.fitestServer();
                TCPBridgeServer.bindedHosts.put(target_socket, serverIPAddres);
            }
            server_socket = new Socket(InetAddress.getByName(serverIPAddres), 7304);
            server_din = new DataInputStream(server_socket.getInputStream());
            server_dout = new DataOutputStream(server_socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        while(true) {
            boolean download = false;
            byte[] initialize = new byte[1];
            try {
                try {
                    client_din.read(initialize, 0, initialize.length);
                    if (initialize[0] == 2) {
                        byte[] cmd_buffer = new byte[3];
                        client_din.read(cmd_buffer, 0, cmd_buffer.length);
                        byte[] recv_data = ReadStream(client_din);
                        switch (Integer.parseInt(new String(cmd_buffer))) {
                            case 122:
                                server_dout.write(CreateDataPacket("122".getBytes("UTF8"), recv_data));
                                server_dout.flush();
                                break;
                            case 123:
                                server_dout.write(CreateDataPacket("123".getBytes("UTF8"), recv_data));
                                server_dout.flush();
                                break;
                            case 124:
                                download = false;
                                server_dout.write(CreateDataPacket("124".getBytes("UTF8"), recv_data));
                                server_dout.flush();
                                break;
                            case 126:
                                server_dout.write(CreateDataPacket("126".getBytes("UTF8"), recv_data));
                                server_dout.flush();
                                break;
                            case 127:
                                server_dout.write(CreateDataPacket("127".getBytes("UTF8"), recv_data));
                                server_dout.flush();
                                break;
                            case 224:
                                download = true;
                                server_dout.write(CreateDataPacket("224".getBytes("UTF8"), recv_data));
                                server_dout.flush();
                                break;
                            case 225:
                                server_dout.write(CreateDataPacket("225".getBytes("UTF8"), recv_data));
                                server_dout.flush();
                                break;
                            case 226:
                                server_dout.write(CreateDataPacket("226".getBytes("UTF8"), recv_data));
                                server_dout.flush();
                                break;
                        }
                    }

                    if (server_din.read() == 2) {
                        byte[] cmd_buffer = new byte[3];
                        server_din.read(cmd_buffer, 0, cmd_buffer.length);
                        byte[] recv_buffer = null;
                        recv_buffer = ReadStream(server_din);
                        switch (Integer.parseInt(new String(cmd_buffer))) {
                            case 123:
                                client_dout.write(CreateDataPacket("123".getBytes("UTF8"), recv_buffer));
                                client_dout.flush();
                                break;
                            case 124:
                                client_dout.write(CreateDataPacket("124".getBytes("UTF8"), recv_buffer));
                                client_dout.flush();
                                break;
                            case 125:
                                client_dout.write(CreateDataPacket("125".getBytes("UTF8"), recv_buffer));
                                client_dout.flush();
                                break;
                            case 128:
                                client_dout.write(CreateDataPacket("128".getBytes("UTF8"), recv_buffer));
                                client_dout.flush();
                                break;
                            case 225:
                                client_dout.write(CreateDataPacket("225".getBytes("UTF8"), recv_buffer));
                                client_dout.flush();
                                break;
                            case 227:
                                client_dout.write(CreateDataPacket("227".getBytes("UTF8"), recv_buffer));
                                client_dout.flush();
                                break;
                            case 228:
                                client_dout.write(CreateDataPacket("228".getBytes("UTF8"), recv_buffer));
                                client_dout.flush();
                                break;
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("Client socket closed");
                    target_socket.close();
                    System.out.println("Server socket closed");
                    server_socket.close();
                    if (download)
                        TCPBridgeServer.bindedHosts.remove(target_socket);
                    break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private byte[] ReadStream(DataInputStream din) {
        byte[] data_buff = null;
        try {
            int b;
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
