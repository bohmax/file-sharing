import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ServerUdp implements Runnable{

    private final int LENGTH=128;
    private MulticastSocket socket;
    private InetAddress ip;


    ServerUdp(InetAddress ip, MulticastSocket multi){
        this.ip = ip;
        socket = multi;
    }

    @Override
    public void run() {
        //quando viene chiuso il multicastsocket esce dal thread
        try {
            DatagramPacket packet = new DatagramPacket(new byte[LENGTH], LENGTH, ip, 2000);
            while (true) {
                socket.receive(packet);
                System.out.println("server ha ricevuto: " + new String(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getLength(),
                        StandardCharsets.UTF_8) + " da " + packet.getAddress());
                System.out.println("ricevuto dal listener " + ip);
                DatagramPacket multicastPacket =
                        new DatagramPacket(packet.getData(),
                                packet.getOffset(),
                                packet.getLength(),
                                ip, 3000);
                socket.send(multicastPacket);
            }
        } catch (IOException e) {
            System.out.println("chiudo udp "+ip);
        }
    }
}
