package tcpbridgeserver;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class ServerComunication {

    private HashMap<String, String> servers_ip_addresses = new HashMap<>();

    public HashMap<String, String> getServers_ip_addresses() {
        return servers_ip_addresses;
    }

    private void getAvailableServers() {
        try {
            // Deschide un port random pentru a trimite packetul
            DatagramSocket c = new DatagramSocket();

            byte[] sendData = "DISCOVER_SERVERS_REQUEST".getBytes();

            // Incearca sa trimiti la 255.255.255.255
            try {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 8888);
                c.send(sendPacket);
                System.out.println(getClass().getName() + ">>> Request packet sent to: 255.255.255.255 (DEFAULT)");
            } catch (Exception e) {
                // To do
            }

            // Broadcast la mesaj prin toate interfețele de rețea
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp() || networkInterface.isVirtual()) {
                    continue; // Omitem, pentru ca este o rețea de loopback
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    Inet4Address broadcast = (Inet4Address) interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    // Trimiterea pachetului de broadcast
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                        c.send(sendPacket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    System.out.println(getClass().getName() + ">>> Request packet sent to: " + broadcast.getHostAddress()
                            + "; Interface: " + networkInterface.getDisplayName());
                }
            }

            System.out.println(getClass().getName() + ">>> Done looping over all network interfaces. Now waiting for a reply!");

            // Așteptam răspunsul
            byte[] recvBuf = new byte[1000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            c.receive(receivePacket);

            System.out.println(getClass().getName() + ">>> Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

            // Testarea mesajului pentru corectitudine
            String message = new String(receivePacket.getData()).trim();
            servers_ip_addresses.put(receivePacket.getAddress().toString().substring(1), message);


            // Close the port
            c.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Will be sorting algotithm
    public String fitestServer() {
        getAvailableServers();
        MaxHeap heap = new MaxHeap();
        HashMap<Double, String> hm_coefIp = new HashMap<>();
        for (String key : servers_ip_addresses.keySet()) {
            String[] metrics = servers_ip_addresses.get(key).split(" ");
            double coef = 0.6 * Double.parseDouble(metrics[0]) + 0.4 * Double.parseDouble(metrics[1]);
            hm_coefIp.put(coef, key);
            heap.add(coef);
        }
        return hm_coefIp.get(heap.peek());
    }
}
