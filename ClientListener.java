import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.TreeMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.ReentrantLock;

public class ClientListener implements Runnable{

    private SocketChannel server;
    private TreeMap<String,Long> sezioni;
    //la synchronousqueue è fatta in modo tale che il client aspetti senza poter mandare altre richieste
    private SynchronousQueue<String> risp;
    //per evitare la concorrenza della treeMap e della TreeTableView
    private ReentrantLock lock;
    private TreeTableView<Pair<String, Documento>> treetableview;
    private String nomeutente;
    private volatile boolean termina;

    //-------- udp ------------
    private Thread udpthread = null;
    private TextArea udpchat;
    private InetAddress ia = null;
    private MulticastSocket ms = null;
    //-------------------------

    private void createandfillFile(String file,long time) throws IOException{
        lock.lock();
        Long oldtime = sezioni.get(file);
        lock.unlock();
        Path path = Paths.get("Documenti_client/" + file);
        if(oldtime.equals(-1L))
            Files.createFile(path);
        else if(oldtime.equals(time)) return;
        FileChannel fc = FileChannel.open(path, StandardOpenOption.WRITE);
        fc.truncate(0);
        new Message(server, fc);
        fc.close();
        System.out.println("file "+ file + " scaricato");
        lock.lock();
        sezioni.put(file,time);
        lock.unlock();
    }


    ClientListener (SocketChannel server, SynchronousQueue<String> risp, TreeTableView<Pair<String, Documento>> treetableview,
                    ReentrantLock lock,String nomeutente, TextArea udpchat, TreeMap<String,Long> sezioni ){
        this.server = server;
        this.risp = risp;
        this.treetableview = treetableview;
        this.lock = lock;
        this.sezioni = sezioni;
        this.nomeutente = nomeutente;
        this.udpchat =udpchat;
    }

    @Override
    public void run() {
        System.out.println(nomeutente);
        String[] split;
        Message m;
        Risposte op = Risposte.OP_OK;
        JSONParser parser = new JSONParser();
        while (!termina){
            try {
                try {
                    m = new Message(server);
                    split = m.getText().split(" ", 2);
                    System.out.println("ricevuto " + m.getText());
                    try {
                        //se finisco nel catch vuol dire che il server mi ha inviato una sezione su cui fare la show
                        op = Risposte.valueOf(split[0]);
                    } catch (IllegalArgumentException ignored) {
                    }
                    switch (op) {
                        case CLIENT_ADDTOTREE: {
                            //aggiunge un documento alla treetableview selezionabile dall utente
                            try {
                                System.out.println("addtree " + split[1]);
                                JSONArray arr = (JSONArray) parser.parse(split[1]);
                                for (Object o : arr) {
                                    Documento doc = new Documento((JSONObject) o);
                                    System.out.println(doc.getCreatore());
                                    lock.lock();
                                    TreeItem<Pair<String, Documento>> ins = new TreeItem<>(new Pair<>(doc.getNome(), doc));
                                    String path = nomeutente + "/" + doc.getNome();
                                    for (int i = 1; i <= doc.getNum_sezioni(); i++) {
                                        TreeItem<Pair<String, Documento>> listadocumenti = new TreeItem<>(new Pair<>("sezione " + i, doc));
                                        ins.getChildren().add(listadocumenti);
                                        sezioni.put(path + i, -1L);
                                    }
                                    treetableview.getRoot().getChildren().add(ins);
                                    lock.unlock();
                                    if (doc.getCreatore().equals(nomeutente))
                                        risp.put(m.getText());
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                        case CLIENT_SHOW: {
                            try {
                                JSONObject obj = (JSONObject) parser.parse(split[1]);
                                String nomefile = (String) obj.get("file");
                                int sezione = Integer.valueOf((String) obj.get("sezione"));
                                StringBuilder edited = new StringBuilder();
                                long time;
                                if (sezione != -1) {
                                    time = (Long) obj.get("tempo");
                                    //serve per indicare che la sezione è in fase di modifica
                                    if((time%2)==1){
                                        time--;
                                        edited.append(" ").append(sezione);
                                    }
                                    createandfillFile( nomeutente + "/" + nomefile + sezione, time);

                                } else {
                                    int num_sez = treetableview.getSelectionModel().getSelectedItem().getValue().getValue().getNum_sezioni();
                                    for (int i = 1; i <= num_sez; i++) {
                                        time = (Long) obj.get(Integer.toString(i));
                                        //serve per indicare che la sezione è in fase di modifica
                                        if((time%2)==1){
                                            time--;
                                            edited.append(" ").append(i);
                                        }
                                        createandfillFile(nomeutente + "/" + nomefile + i, time);
                                    }
                                }
                                risp.put(Risposte.OP_OK.name() + edited);
                            } catch (ParseException e) {
                                risp.put(Risposte.ERROR.name());
                            }
                            break;
                        }
                        case CLIENT_OPENUDP: {
                            //l'operazione di edit è stata accettata e apre quindi un nuovo thread che ascolta sulla porta 3000 i messaggi udp
                            String[] split2 = split[1].split(" ", 2);
                            if (!split2[1].equals("")) {
                                try {
                                    JSONObject obj = (JSONObject) parser.parse(split2[1]);
                                    String nome = (String) obj.get("file");
                                    String sez = (String) obj.get("sezion");
                                    long time = (Long) obj.get("tempo");
                                    if (time != -1)
                                        createandfillFile( nomeutente + "/" + nome + sez, time);
                                } catch (ParseException e) {
                                    split[1] = Risposte.ERROR.name();
                                }
                            }
                            if(!split2[0].equals(""))
                            try {
                                ms = new MulticastSocket(3000);
                                ia = InetAddress.getByName(split2[0]);
                                udpthread = new Thread(new ClientListenerUdp(ms, udpchat));
                                udpthread.start();
                                ms.joinGroup(ia);
                            } catch (IOException e){
                                ms = null;
                                ia = null;
                                split[1] = split[1].replaceFirst(split2[0], " ");
                            }
                            risp.put(split[1]);
                            break;
                        }
                        case ERROR_NOTEDIT:
                        case CLIENT_CLOSEUDP: {
                            if(ms != null) {
                                ms.leaveGroup(ia);
                                ms.close();
                                ms = null;
                                udpthread.join();
                            }
                            udpchat.setText("");
                            risp.put(split[0]);
                            break;
                        }
                        case CLIENT_CLOSE: {
                            termina = true;
                            if (ms != null) {
                                ms.leaveGroup(ia);
                                ms.close();
                            }
                            risp.put(m.getText());
                            break;
                        }
                        default: {
                            risp.put(m.getText());
                            break;
                        }
                    }
                    op = Risposte.OP_OK;
                } catch (InterruptedException e) {
                    //chiudo il listener
                    if (ms != null) ms.close();
                    termina = true;
                }
            } catch (IOException e){
                Runtime.getRuntime().halt(-1);
            }
        }
        System.out.println("listener chiuso");
    }
}
