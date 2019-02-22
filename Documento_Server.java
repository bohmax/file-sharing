import org.json.simple.JSONObject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class Documento_Server extends Documento{

    private Sezione[] sez;

    //----- riguarda la gestione della chat udp -----
    // numero degli utenti che stanno editando il documento nello stesso momento
    private int num_connessi = 0;
    //classe che genere ip multicast da usare per la chat
    private MulticastIP multicastIP = null;
    private MulticastSocket ms = null;
    private String ipudp; //stringa che rappresenta ip multicast
    //-----------------------------------------------

    /**
     *
     * @param obj json object che rappresenta il documento
     * @param path path su cui creare i file che rappresentano le sezioni
     * @param multicastIP oggetto che genera l'ip
     * @throws IOException se i file non sono creabili
     */
    Documento_Server(JSONObject obj, String path, MulticastIP multicastIP) throws IOException {
        super(obj);
        sez = new Sezione[getNum_sezioni()];
        for(int i=0;i<getNum_sezioni();i++)
            sez[i] = new Sezione(path+(i+1));
        this.multicastIP = multicastIP;
    }

    /**
     * Costruttore usato per confrontarsi con altri documenti
     * @param nome_doc nome del documento
     */
    Documento_Server(String nome_doc){
        super(nome_doc);
    }

    /**
     * restituisce il filechannel associato alla sezione
     * @param index indice della sezione da utilizzare, gli indici vanno da 1 a n
     * @return il FileChannel per modificare il file
     */
    FileChannel getFileChannel(int index){
        int i = index-1;
        if(sez!=null && i<sez.length)
            return sez[i].fc();
        return null;
    }

    //funzione utilizzata per permettere il riusco di tutti gli indirizzi multicast
    //provati prima di poter usare una chat
    private void freelist(LinkedList<String> repeat){
        for (String s: repeat) {
            multicastIP.freeIP(s);
        }
    }

    /**
     * Permette di dare i permessi per modificare un utente alla volta una sezione
     * @param index indice della sezione da utilizzare, gli indici vanno da 1 a n
     * @param nome nome dell'utente che vuole modificare la sezione
     * @return false se la sezione è già occupata, true altrimenti
     */
    boolean takesezione(int index, String nome){
        int i = index-1;
        if(!sez[i].IsOccupied()) {
            if(num_connessi==0){
                try {
                    ms = new MulticastSocket(2000);
                    boolean ip_ok = true;
                    LinkedList<String> repeat = new LinkedList<>();
                    while(ip_ok) {
                            ipudp = multicastIP.getIP();
                        if(ipudp != null) {
                            //se l'acquisizione fallisce più di 10 volte il server non garantisce la chat
                            if(repeat.size()!=20) {
                                System.out.println(getNome() + " voglio creare una chat con ip " + ipudp);
                                try {
                                    InetAddress multicastGroup = InetAddress.getByName(ipudp);
                                    ms.joinGroup(multicastGroup);
                                    new Thread(new ServerUdp(multicastGroup, ms)).start();
                                    ip_ok = false;
                                    freelist(repeat);
                                } catch (IOException e) {
                                    repeat.add(ipudp);
                                    System.out.println("chat con indirizzo "+ ipudp + " fallita");
                                }
                            }
                            else{
                                freelist(repeat);
                                ip_ok = false;
                                ipudp = "";
                            }
                        } else return false;
                    }
                } catch (IOException e){
                    return false;
                }
            }
            sez[i].setOccupied(nome);
            num_connessi ++;
            return true;
        }
        return false;
    }

    /**
     * modifica il testo di una sezione, liberando quest'ultima dalla persona che la modificava
     * @param index indice della sezione da utilizzare, gli indici vanno da 1 a n
     * @param nome nome della persona che occupava la sezione
     * @param client socket per comunicare con il client
     * @return true se il file viene modificato, false altrimenti
     */
    boolean write(int index, String nome, SocketChannel client){
        int i = index-1;
        if(sez[i].WhoOcuupied(nome)){
                if (sez[i].write(client)){
                setFree(index);
                return true;
            } else return false;
        }
        return false;
    }

    /**
     * libera una sezione da un utente
     * @param index indice della sezione da utilizzare, gli indici vanno da 1 a n
     */
    void setFree(int index){
        sez[index-1].freeOccupied();
        num_connessi--;
        if(num_connessi==0){
            ms.close();
            ms = null;
            if(!ipudp.equals("")) {
                multicastIP.freeIP(ipudp);
                ipudp = "";
            }
        }
    }

    /**
     * restituisce ip multicast associato al documento
     * @return la stringa " " se nessuno sta modificando il documento, altrimenti ip multicast associato al documento
     */
    String getIP(){return ipudp+ " ";}

    /**
     * per conoscere l'ultimo istante in cui una sezione del documento è stata modificata o è in fase di modifica
     * @param index il numero della sezione della quale si vuole conoscere il tempo reale
     * @return il tempo del file
     */
    long getRealTime(int index){
        return sez[index-1].getRealTimeofsection();
    }

    /**
     * per conoscere l'ultimo istante in cui una sezione del documento è stata modificata
     * @param index il numero della sezione della quale si vuole conoscere il tempo
     * @return il tempo dell'ultima modifica
     */
    long getTime(int index){
        return sez[index-1].getTimeofsection();
    }

    /**
     * confronta due oggetti Documento_Server
     * @param o deve essere castabile a Documento_Server
     * @return true se i due oggetti hanno lo stesso nome del documento, false se non castabile a documento_server o hanno nomi diversi
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Documento_Server)) return false;

        Documento_Server doc = (Documento_Server) o;
        return getNome().equals(doc.getNome());
    }

}
