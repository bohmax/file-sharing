import javafx.scene.control.TextArea;

import java.io.IOException;
import java.net.*;

public class ClientListenerUdp implements Runnable{

    private MulticastSocket ms;
    private TextArea text;

    /**
     *
     * @param ms multicastSocket in cui ricevo i messaggi
     * @param text textArea in cui visualizzo i messaggi ricevuti
     */
    ClientListenerUdp(MulticastSocket ms, TextArea text){
        this.text = text;
        this.ms = ms;
    }


    @Override
    public void run() {
        boolean cicla = true;
        byte[] buf = new byte[128];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        while (cicla) {
            //quindo si chiude il multicast socket dal listener dei pacchetti tcp esco dal thread
            try {
                ms.receive(dp);
                String s = new String(buf, 0, dp.getLength());
                text.appendText(s);
            } catch (IOException e) {
                cicla = false;
            }
        }
        System.out.println("close udp");
    }
}
