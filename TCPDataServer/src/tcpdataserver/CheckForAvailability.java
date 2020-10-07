package tcpdataserver;

import java.io.IOException;
import java.net.*;

public class CheckForAvailability implements Runnable {
    private DatagramSocket socket;

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while(true) {
                System.out.println(getClass().getName() + ">>>Ready to receive broadcast packets!");

                // Receive a packet
                byte[] recvBuf = new byte[1000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                // Packet received
                System.out.println(getClass().getName() + ">>>Discovery packet received from: " + packet.getAddress().getHostAddress());
                System.out.println(getClass().getName() + ">>>Packet received. Data: " + new String(packet.getData()));

                // Check for the right command in the packet
                String message = new String(packet.getData()).trim();
                if (message.equals("DISCOVER_SERVERS_REQUEST")) {
                    String cpuLoad = ServerUtilities.getCPULoad();
                    String freeMemory = ServerUtilities.getAvailableMemoryInProcentage();

                    byte[] sendData = (cpuLoad + " " + freeMemory).getBytes("UTF8");
                    // Send a response
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);

                    System.out.println(getClass().getName() + ">>>Sent packet to: " + sendPacket.getAddress().getHostAddress());
                }
            }
        } catch (SocketException e) {
//            e.printStackTrace();
        } catch (IOException e) {
//            e.printStackTrace();
        }
    }
}
