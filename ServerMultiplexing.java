import javafx.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

class ServerMultiplexing{

    private int DEFAULT_PORT;
    private JSONParser parser = new JSONParser();
    private ConcurrentMap<String,Utente> hash;
    private TreeMap<String, Online> utenti_online;
    private List<Documento_Server> documenti = new ArrayList<>();

    //classe che genera e gestisce gli ip multicast
    private MulticastIP multicastIP = new MulticastIP();

    /**
     *
     * @param hash la tabella hash contenente gli utenti registrati
     * @param utenti_online la lista degli utenti online, inizialmente sarà vuota
     * @param port la porta su cui eseguire la connessione tcp coi client
     */
    ServerMultiplexing(ConcurrentMap<String,Utente> hash, TreeMap<String, Online> utenti_online,int port){
        this.hash = hash;
        this.utenti_online = utenti_online;
        DEFAULT_PORT = port;
    }

    /**
     * metodo che esegue l'operazione richiesta dal server se possibile
     * @param operation l'operazione da eseguire
     * @param sel la selectionkey associata al client
     * @param message contiene il messaggio da parsare per eseguire l'operazioe
     * @return Il messaggio di risposta da spedire al client
     * @throws ParseException se la stringa passato tramite message non è json
     * @throws IOException quando creo un nuovo documento
     */
    private Invia operation(Risposte operation, SelectionKey sel,String message) throws ParseException, IOException {
        Invia risposta = null;
        SocketChannel client =(SocketChannel) sel.channel();
        System.out.println(operation + " " + message);
        //ogni messaggio che invia il client deve essere parsato
        JSONObject obj = (JSONObject) parser.parse(message);
        switch (operation) {
            case OP_LOGIN: {
                //prendo username e password dal json file
                String username = (String) obj.get("name");
                String password = (String) obj.get("password");
                Utente dati = hash.get(username);
                //controllo se esiste nella tabella hash se le password coincidono e se è online
                if ((dati != null) && dati.getPassword().equals(password)) {
                    if (!dati.Is_online()) {
                        dati.setOnline(sel);
                        //viene inserito tra gli utenti online
                        utenti_online.put(client.getRemoteAddress().toString(), new Online(username));
                        if(dati.getnotifica()){
                            risposta = new Invia(new String[] { Risposte.OP_OK.name() + " " +Risposte.CLIENT_NOTIFICA.name()});
                            dati.setnotifica(false);
                        } else
                            risposta = new Invia(Risposte.OP_OK.name());
                    } else risposta = new Invia(Risposte.ERROR_ALREADYONLINE.name());
                } else risposta = new Invia(Risposte.ERROR.name());
                break;
            }
            case OP_LOGOUT: {
                Online online = utenti_online.get(client.getRemoteAddress().toString());
                //se è online e se ha la modifica di qualche sezione
                if (online != null) {
                    if(online.getDocumento()!=null)
                        online.getDocumento().setFree(online.getSezione());
                    utenti_online.remove(client.getRemoteAddress().toString());
                    hash.get(online.getNome()).setOnline(null);
                    risposta = new Invia(Risposte.CLIENT_CLOSE.name());
                    System.out.println("Disconnesso " + online.getNome());
                } else risposta = new Invia(Risposte.ERROR.name());
                break;
            }
            case OP_CREATE: {
                Documento_Server doc = new Documento_Server((String) obj.get("name"));
                String nome_file = "Documenti_server/" + doc.getNome() + "/";
                Path dirPathObj = Paths.get(nome_file);
                //controllo che non esista un file con lo stesso nome...
                if (!documenti.contains(doc)) {
                    //se non esisto creo la directory e tutte le sezioni
                    try {
                        Files.createDirectory(dirPathObj);
                        try{
                            Documento_Server doc_serv = new Documento_Server(obj,nome_file,multicastIP);
                            documenti.add(doc_serv);
                            Utente u = hash.get(doc_serv.getCreatore());
                            u.addLista(doc_serv);
                            risposta = new Invia(Risposte.OP_OK.name());
                        } catch (IOException e){
                            //cancella la cartella e i file al suo interno
                            Files.walk(dirPathObj).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                            risposta = new Invia(Risposte.ERROR.name());
                        }
                    } catch (FileAlreadyExistsException e) {
                        risposta = new Invia(Risposte.ERROR.name());
                    }
                    break;
                } else risposta = new Invia(Risposte.ERROR_FILEALREADY.name());
                break;
            }
            case OP_ADDTOFILE: {
                String nomedoc = (String)obj.get("file");
                String persona = (String)obj.get("persona");
                Documento_Server doc = new Documento_Server(nomedoc);
                int i = documenti.indexOf(doc);
                //controllo se il documento esiste e anche se la persona invitata esiste e se esiste
                if(i>=0){
                    Utente u = hash.get(persona);
                    if(u!=null){
                        if(u.addLista(documenti.get(i))) {
                            risposta = new Invia(Risposte.OP_OK.name());
                            if(u.Is_online()) {
                                //se è online cambio il suo registro delle operazioni in scrittura e gli notifico il documento
                                Pair<Invia, JSONArray> attach = (Pair<Invia, JSONArray>) u.getOnline().attachment();
                                attach.getValue().add(documenti.get(i).getObj());
                                u.getOnline().interestOps(SelectionKey.OP_WRITE);
                                System.out.println("attach nuovo " + ((Pair<Invia, JSONArray>) u.getOnline().attachment()).getValue().toString());
                            } else u.setnotifica(true);
                        } else risposta = new Invia(Risposte.ERROR_ALREADY.name());
                        System.out.println( hash.get(persona).getListaDocumento().toString());
                    } else risposta = new Invia(Risposte.ERROR_NOTEXIST.name());
                } else risposta = new Invia(Risposte.ERROR.name());
                break;
            }
            case OP_LIST:{
                //invia la lista dei documenti appartenenti a chi ha fatto la richiesta
                String name = utenti_online.get(client.getRemoteAddress().toString()).getNome();
                if(name!=null){
                    JSONArray arr = new JSONArray();
                    //dalla tabella hash si preleva il contenuto dell utente e si controllano tutti i file associati ad esso
                    //dato memorizzato nella tabella hash
                    Utente u = hash.get(name);
                    List<Documento> lista = u.getListaDocumento();
                    for (Documento doc: lista) {
                        arr.add(doc.getObj());
                    }
                    risposta = new Invia(Risposte.OP_OK.name() + " " +  arr.toString());
                } else risposta = new Invia(Risposte.ERROR.name());

                break;
            }
            case OP_SHOW:{
                String nomefile = (String)obj.get("file");
                int sezione =  Integer.valueOf((String) obj.get("sezione"));
                Documento_Server doc = null;
                try {
                    doc = documenti.get(documenti.indexOf(new Documento_Server(nomefile)));
                } catch (IndexOutOfBoundsException ignored) {}
                if(doc!=null) {
                    //se il valore di sezione è un intero valido allora si invia la sezione specificata
                    //altrimenti si invia l intero documento
                    if (sezione > 0) {
                        //controllo se i tempi coincidono, se coincidono non invio ma in OP_OK
                        Long tempo = (Long) obj.get("tempo");
                        if(!tempo.equals(doc.getRealTime(sezione))) {
                            obj.put("tempo", doc.getRealTime(sezione));
                            LinkedList<FileChannel> fc = new LinkedList<>();
                            fc.add(doc.getFileChannel(sezione));
                            if(tempo.equals(doc.getTime(sezione)))
                                risposta = new Invia(Risposte.CLIENT_SHOW.name() + " " + obj.toString());
                            else risposta = new Invia(Risposte.CLIENT_SHOW.name() + " " + obj.toString(), fc);
                        } else
                            risposta = new Invia(Risposte.OP_OK.name());
                    }
                    else {
                        //invio i tempi di tutte le sezioni, se essi coincidono con quelli del client non invio le apposite sezioni
                        LinkedList<FileChannel> fc = new LinkedList<>();
                        for (int i = 1; i <= doc.getNum_sezioni(); i++) {
                            String key = String.valueOf(i);
                            Long tempo = (Long) obj.get(key);
                            if(!tempo.equals(doc.getRealTime(i))){
                                obj.put(key,doc.getRealTime(i));
                                if(!tempo.equals(doc.getTime(i)))
                                fc.add(doc.getFileChannel(i));
                            }
                        }
                        risposta = new Invia(Risposte.CLIENT_SHOW.name() + " " + obj.toString(), fc);
                    }
                } else risposta = new Invia(Risposte.ERROR.name());
                break;
            }
            case OP_EDIT:{
                String nomefile = (String)obj.get("file");
                int sezione =  Integer.valueOf((String) obj.get("sezion"));
                Long time = (Long) obj.get("tempo");
                int index = documenti.indexOf(new Documento_Server(nomefile));
                try {
                    Documento_Server doc = documenti.get(index);
                    Online online = utenti_online.get(client.getRemoteAddress().toString());
                    //controllo che l' utente_online non abbia già acquisito altre sezioni e nel caso
                    // setta nei suoi parametri il documento e la sezione che sta per acquisire
                    if (online.setEdit(doc, sezione)) {
                        //se può prendere la sezione la acquisisce nel documento
                        if (doc.takesezione(sezione, online.getNome())) {
                            obj.put("tempo", doc.getTime(sezione));
                            //invio l'ip multicast per ascoltare e inviare i messaggi udp e poi la sezione da editare
                            //getIP ha uno spazio dopo l'ip
                            LinkedList<FileChannel> fc = new LinkedList<>();
                            fc.add(doc.getFileChannel(sezione));
                            if(!time.equals(doc.getTime(sezione))) {
                                risposta = new Invia(Risposte.CLIENT_OPENUDP.name() + " " + doc.getIP()
                                        + obj.toString(), fc);
                            }
                            else risposta = new Invia(Risposte.CLIENT_OPENUDP.name() + " " + doc.getIP());
                        }
                        else {
                            risposta = new Invia(Risposte.ERROR_OCCUPIED.name());
                            online.setEndEdit();
                        }
                    } else risposta = new Invia(Risposte.ERROR.name());
                } catch (IndexOutOfBoundsException | NullPointerException e){
                    risposta = new Invia(Risposte.ERROR.name());
                }
                break;
            }
            case OP_ENDEDIT:{
                //mi faccio inviare il testo in questo modo per fare in modo tale che se l'utente crasha durante le trasmissione del file
                //la mia sezione non diventi inconsistente
                String nomefile = (String)obj.get("file");
                int sezione =  Integer.valueOf((String) obj.get("sez"));
                try {
                    Documento_Server doc = documenti.get(documenti.indexOf(new Documento_Server(nomefile)));
                    //controllo che l'utente che la ha modificata sia quello corretto e sovrascrivo la sezione
                    if(doc.write(sezione,utenti_online.get((client.getRemoteAddress().toString())).getNome(),client)){
                        utenti_online.get(client.getRemoteAddress().toString()).setEndEdit();
                        risposta = new Invia(Risposte.CLIENT_CLOSEUDP.name());
                    } else risposta = new Invia(Risposte.ERROR_NOTEDIT.name());
                } catch (IndexOutOfBoundsException e) {risposta = new Invia(Risposte.ERROR_NOTEDIT.name());}
                break;
            }
        }
        return risposta;
    }

    void multiplexing () throws IOException {
        int port = DEFAULT_PORT;
        ServerSocketChannel serverChannel;
        Selector selector;
        try {
            serverChannel = ServerSocketChannel.open();
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);
            ss.bind(address);
            serverChannel.configureBlocking(false);
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        while (true) {
            try {
                selector.select();
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                // rimuove la chiave dal Selected Set, ma non dal registered Set
                try {
                    if (key.isAcceptable()) {
                        //accetta la connessione e cambia il registro delle operazioni in lettura
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        System.out.println("Accepted connection from " + client);
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        Message mess = new Message(client);
                        Invia messaggio = new Invia(mess);
                        if(!mess.getText().equals("")) {
                            String[] split = mess.getText().split(" ", 2);
                            Risposte work = Risposte.valueOf(split[0]);
                            try {
                                messaggio = operation(work, key, split[1]);
                            } catch (ParseException e) {
                                messaggio = new Invia(Risposte.ERROR.name());
                            }
                        }
                        //nell attach inserisco una coppia contenente la risposta del messaggio e dati supplementari da mandare
                        //come ad esempio l'invito a un documento. L'invito sarà però aggiunto successivamente.
                        key.attach(new Pair<>(messaggio, new JSONArray()));
                        key.interestOps(SelectionKey.OP_WRITE);
                    } else if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        Pair<Invia, JSONArray> attach = (Pair<Invia, JSONArray>) key.attachment();
                        //controllo che non abbia già spedito il messaggio e in caso affermativo lo invio
                        if(!attach.getKey().wassend()){
                            attach.getKey().sendMessage(client);
                            //se non ho inviti cambia il registro delle operazioni
                            if (attach.getValue().size()==0) key.interestOps(SelectionKey.OP_READ);
                        }
                        else {
                            new Message(Risposte.CLIENT_ADDTOTREE.name() + " " + attach.getValue().toString()).sendMessage(client);
                            attach.getValue().clear();
                            key.interestOps(SelectionKey.OP_READ);
                        }
                        System.out.println("inviato");
                    }
                } catch (IOException ex) {
                    //se un utente crasha o chiude l app lo disconnetto e controllo che non avesse nessuna sezione
                    Online online = utenti_online.get(((SocketChannel) key.channel()).getRemoteAddress().toString());
                    if(online != null){
                        if(online.getDocumento()!=null)
                            online.getDocumento().setFree(online.getSezione());
                        utenti_online.remove(((SocketChannel) key.channel()).getRemoteAddress().toString());
                        hash.get(online.getNome()).setOnline(null);
                    }
                    System.out.println("chiudo " + key.channel());
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

}
