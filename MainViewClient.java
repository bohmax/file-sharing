import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.input.KeyCode;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.*;
import javafx.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MainViewClient {

    private static void fillText(ByteBuffer buff, TextArea text, String path) throws IOException{
        FileChannel fc = FileChannel.open(Paths.get(path), StandardOpenOption.READ);
        while (fc.read(buff) != -1) {
            buff.flip();
            text.appendText(StandardCharsets.UTF_8.decode(buff).toString());
            buff.clear();
        }
        fc.close();
    }

    static void display(String nomeutente, Stage start, SocketChannel server, boolean pendenti) {
        Stage window = new Stage();
        Guisupport gu = new Guisupport();
        String defaultlabelstring = "Selezionare il file e nel caso la sezione da modificare";
        JSONParser parser = new JSONParser();
        //uso questa coda per aspettare messaggi dal listener
        SynchronousQueue<String> risposta = new SynchronousQueue<>();
        //usato per quando creo un file, per assicurarsi che non ci siano caratteri speciali
        Pattern pattern = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        //Usato per sapere che documento devo modificare
        final ArrayList<Pair<String,Documento>> toedit = new ArrayList<>();
        //Usato per sapere l'ultimo tempo di modifica di una sezione
        TreeMap<String,Long> sezioni = new TreeMap<>();
        //ipmulticast su cui ricevo e spedisco i messaggi
        StringBuilder ipmulticast = new StringBuilder();
        //Lock per poter aggiornare la treetableview
        ReentrantLock lock = new ReentrantLock();
        //avevo bisogno di una variabile temporanea per fare questo assegnamento
        MulticastSocket multi = null;
        try {
            multi = new MulticastSocket(2000);
        } catch (IOException ignored) { System.exit(-1);}
        final MulticastSocket ms = multi;
        //per le operazioni di show
        final int range = 4096;
        ByteBuffer buff = ByteBuffer.allocate(range);

        //--------------------------------- tree view -----------------------------
        Documento documento = new Documento("",0,"");
        final TreeItem<Pair<String, Documento>> root = new TreeItem<>(new Pair<>("", documento));
        TreeTableColumn<Pair<String, Documento>,String> column = new TreeTableColumn<>("Documenti");
        column.setPrefWidth(147);
        column.setCellValueFactory((TreeTableColumn.CellDataFeatures<Pair<String,Documento>,String> p) ->
                new ReadOnlyStringWrapper(p.getValue().getValue().getKey()));
        final TreeTableView<Pair<String, Documento>> treeTableView = new TreeTableView<>();
        treeTableView.getColumns().add(column);
        treeTableView.setPrefWidth(149);
        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);
        //-------------------------------------------------------------------------

        //---------------------------menu e layout------------------------------
        BorderPane bp = new BorderPane();
        StackPane sp = new StackPane();
        HBox topMenu = new HBox(10);
        HBox bottomMenu = new HBox(10);
        HBox endeditmenu = new HBox(10);
        HBox udpchathorizontal = new HBox(0);
        VBox vbox = new VBox(5);
        VBox udpchat = new VBox(5);
        topMenu.setPadding(new Insets(10, 7, 10, 7));
        bottomMenu.setPadding(new Insets(10, 7, 10, 7));
        endeditmenu.setPadding(new Insets(10, 0, 10, 0));
        udpchathorizontal.setPadding(new Insets(10, 0, 10, 0));
        topMenu.setStyle("-fx-background-color: #336699;");
        //-----------------------------------------------------------------------

        //------------------------------label, button e text---------------------
        TextArea text = new TextArea();
        TextField udpfield = new TextField();
        TextArea udptext = new TextArea();
        udptext.setEditable(false);

        Label label = new Label();

        Button logout = new Button("Log out");
        Button show = new Button("Show");
        Button edit = new Button("Edit");
        Button create = new Button("Create document");
        Button addperson = new Button("Share");
        Button list = new Button("List");
        Button endedit = new Button("End Edit");
        Button udpsend = new Button("Send");

        logout.setMinSize(70,20);
        show.setMinSize(70,20);
        edit.setMinSize(70,20);
        create.setMinSize(100,20);
        addperson.setMaxSize(100,20);
        list.setMinSize(70,20);
        endedit.setMinSize(100,20);
        udpsend.setMinSize(50,20);
        udptext.setPrefColumnCount(1);
        udpchat.setPrefWidth(180);
        text.setVisible(false);
        label.setText(defaultlabelstring);
        //----------------------------------------------------------------------

        //Viene avviato il Thread che ascolterà tutti i messaggi tcp
        Thread thread = new Thread(new ClientListener(server,risposta,treeTableView,lock,nomeutente,udptext,sezioni));
        thread.start();

        //Block events to other windows
        window.setOnCloseRequest( e -> {
            ms.close();
            thread.interrupt();
        });
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(nomeutente);

        //--------------------- Allineamento -------------------------
        topMenu.setAlignment(Pos.CENTER_RIGHT);
        bottomMenu.setAlignment(Pos.BOTTOM_CENTER);
        udpchathorizontal.setAlignment(Pos.BOTTOM_CENTER);
        label.setAlignment(Pos.CENTER);
        endedit.setAlignment(Pos.BOTTOM_CENTER);
        BorderPane.setAlignment(bottomMenu, Pos.BOTTOM_CENTER);
        BorderPane.setAlignment(label,Pos.CENTER);
        VBox.setVgrow(text, Priority.ALWAYS);
        VBox.setVgrow(udptext, Priority.ALWAYS);
        vbox.setAlignment(Pos.CENTER);
        HBox.setHgrow(endedit, Priority.ALWAYS);
        endedit.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        //------------------------------------------------------------


        //---------------------- Add ---------------------------------
        topMenu.getChildren().add(logout);
        bottomMenu.getChildren().addAll(show,edit,list,addperson,create);
        endeditmenu.getChildren().add(endedit);
        vbox.getChildren().addAll(text,bottomMenu);
        udpchathorizontal.getChildren().addAll(udpfield,udpsend);
        udpchat.getChildren().addAll(udptext,udpchathorizontal);
        sp.getChildren().addAll(label,vbox);
        //------------------------------------------------------------

        //-------------------- BorderPane insert ---------------------
        bp.setCenter(sp);
        bp.setTop(topMenu);
        bp.setLeft(treeTableView);
        //------------------------------------------------------------


        //------------------------- event ----------------------------
        logout.setOnAction(e -> {
            Message mess = new Message(Risposte.OP_LOGOUT.name() + " " + new JSONObject().toString());
            try {
                mess.sendMessage(server);
                if(Risposte.valueOf(risposta.take())==Risposte.CLIENT_CLOSE){
                    System.out.println("Log out success");
                    start.show();
                    window.close();
                } else gu.Allerta("Prova a uscire di nuovo");
            } catch (IOException | InterruptedException e1) {
                gu.Allerta("Errore, il programma si chiuderà");
                Platform.exit();
            }
        });

        create.setOnAction(e -> {
            Risposte richiesta;
            Matcher matcher = null;
            String answer;
            String nome = "";
            Documento doc;
            Optional<String> ris = gu.taxtallert("Insert file name","Create the file, no special character are allowed,\nNote trime will be applid to the name file", "Insert file name");
            //se l'utente non ha premuto su annulla entra nell if
            if(ris.isPresent()) {
                nome = ris.get().trim();
                matcher = pattern.matcher(nome);
            }
            //se è presente un nome non vuoto e se il file non ha caratteri speciali
            if (!nome.equals("") && !matcher.find()) {
                Optional<String> ris1 = gu.taxtallert("Insert the number of section", "Insert the number of section, no more than 100", "Insert section number");
                int num_sez = 0;
                if(ris1.isPresent()) {
                    try {
                        num_sez = Integer.parseInt(ris1.get().trim());
                    } catch (NumberFormatException ignored) {}
                }
                if (num_sez <= 100 && num_sez > 0) {
                    //creo un riferimento lato client per il file
                    doc = new Documento(nome, num_sez, nomeutente);
                    Message filerequest = new Message(Risposte.OP_CREATE.name() + " " + doc.toString());
                    try {
                        filerequest.sendMessage(server);
                        answer = risposta.take();
                        System.out.println(answer);
                        richiesta = Risposte.valueOf(answer.split(" ",2)[0]);
                        //se la richiesta ha avuto successo inseirsco il file nel treetablelist
                        if (richiesta == Risposte.OP_OK) {
                            lock.lock();
                            final TreeItem<Pair<String, Documento>> ins = new TreeItem<>(new Pair<>(doc.getNome(), doc));
                            String path = nomeutente+"/"+doc.getNome();
                            for (int i = 1; i <= num_sez; i++) {
                                final TreeItem<Pair<String, Documento>> listadocumenti = new TreeItem<>(new Pair<>("sezione " + i, doc));
                                ins.getChildren().add(listadocumenti);
                                sezioni.put(path+i,-1L);
                            }
                            root.getChildren().add(ins);
                            lock.unlock();
                            gu.Successo("File " + nome + " creato con successo");
                        }
                        else gu.Allerta("File già esistente");
                    } catch (IOException | InterruptedException e1) {
                        gu.Allerta("Richiesta fallita se il problema si ripresenta riavviare il sistema");
                    }
                } else if(ris1.isPresent()) gu.Allerta("Hai inserito un numero non compreso tra 1 e 100, oppure non hai inserito un numero");
            } else if(ris.isPresent()) gu.Allerta("Il nome del tuo file conteneva caratteri speciali o era vuoto");
        });

        addperson.setOnAction( e -> {
            //controllo che ci sia un elemento selezionato
            if(treeTableView.getSelectionModel().getSelectedItem()!=null) {
                //prendo l'elemento selezionato
                Documento doc = treeTableView.getSelectionModel().getSelectedItem().getValue().getValue();
                //se sono il creatore del file posso far partire la richiesta
                if(doc.getCreatore().equals(nomeutente)) {
                Optional<String> risp = gu.taxtallert("Insert person to add", "Add a person to make them able to modify the file", "Insert person name");
                    if (risp.isPresent()) {
                        String name = risp.get().trim();
                        //non permetto di inviare file senza nome
                        if (!name.equals("")) {
                            JSONObject obj = new JSONObject();
                            if (!doc.getCreatore().equals(name)){
                                Risposte ris1 = Risposte.ERROR;
                                //creo un oggetto json che rappresenta nominativamente il documento
                                obj.put("file", doc.getNome());
                                obj.put("persona", name);
                                Message mess = new Message(Risposte.OP_ADDTOFILE.name() + " " + obj.toString());
                                try {
                                    mess.sendMessage(server);
                                    ris1 = Risposte.valueOf(risposta.take());
                                } catch (IOException | InterruptedException e1) {
                                    gu.Allerta("Richiesta fallita se il problema si ripresenta riavviare il sistema");
                                }
                                if (ris1 == Risposte.OP_OK) gu.Successo("L'utente è abilitato alla modifica");
                                else if (ris1 == Risposte.ERROR_NOTEXIST)
                                    gu.Allerta("L'utente " + name + " non esiste");
                                else gu.Allerta("Utente già abilitato");
                            } else gu.Allerta("Non puoi invitare te stesso");
                        } else gu.Allerta("inserire un nome valido");
                    }
                } else gu.Allerta("Solo il creatore del file può invitare persone");
            } else gu.Allerta("Selezionare prima il file su cui aggiungere la persona");
        });

        list.setOnAction( e -> {
            Message mess = new Message(Risposte.OP_LIST + " " + new JSONObject().toString());
            String risp = "";
            JSONArray objarr;
            try {
                mess.sendMessage(server);
                risp = risposta.take().split(" ",2)[1];
                System.out.println(risp);
            } catch (IOException | InterruptedException e1) {
                gu.Allerta("Richiesta fallita se il problema si ripresenta riavviare il sistema");
            }
            try {
                //controllo tutti i documenti passati dal server se qualcuno non è presente lo aggiungo
                objarr = (JSONArray) parser.parse(risp);
                for (Object o : objarr) {
                    JSONObject obj = (JSONObject) o;
                    Documento doc = new Documento(obj);
                    Pair<String, Documento> pair = new Pair<>(doc.getNome(), doc);
                    String path = nomeutente+"/"+doc.getNome();
                    final TreeItem<Pair<String, Documento>> ins = new TreeItem<>(pair);
                    lock.lock();
                    if(!sezioni.containsKey(path+1)) {
                        for (int j = 1; j <= doc.getNum_sezioni(); j++) {
                            final TreeItem<Pair<String, Documento>> listadocumenti = new TreeItem<>(new Pair<>("sezione " + j, doc));
                            ins.getChildren().add(listadocumenti);
                            sezioni.put(path + j, -1L);
                        }
                        root.getChildren().add(ins);
                    }
                    lock.unlock();
                }
            } catch (ParseException e1) {
                gu.Allerta("Richiesta fallita se il problema si ripresenta riavviare il sistema");
            }
        });

        show.setOnAction( e -> {
            //controllo che un elemento sia selezionato
            if(treeTableView.getSelectionModel().getSelectedItem()!=null) {
                //a seconda del testo su show decido se far vedere la text area oppure no
                if (show.getText().equals("Show")) {
                    text.setVisible(true);
                    text.setEditable(false);
                    TreeItem<Pair<String,Documento>> tree = treeTableView.getSelectionModel().getSelectedItem();
                    Documento doc = tree.getValue().getValue();
                    JSONObject obj = new JSONObject();
                    obj.put("file", tree.getValue().getValue().getNome());
                    String path = nomeutente+"/"+doc.getNome();
                    text.setText("");
                    try {
                        if (doc.getNome().equals(tree.getParent().getValue().getKey())) {
                            String sezione = tree.getValue().getKey().split(" ")[1];
                            obj.put("sezione", sezione);
                            lock.lock();
                            obj.put("tempo", sezioni.get(path+sezione));
                            lock.unlock();
                            new Message(Risposte.OP_SHOW.name() + " " + obj.toString()).sendMessage(server);
                            String testo = risposta.take();
                            String[] risp = testo.split(" ",2);
                            if(risp.length==2)
                                gu.Successo("La sezione è in fase di modifica da parte di un altro utente");
                            if(Risposte.valueOf(risp[0])==Risposte.OP_OK)
                                fillText(buff,text,"Documenti_client/"+nomeutente+"/"+doc.getNome()+sezione);
                            else gu.Allerta("Qualcosa di veramente brutto è successo, si consiglia di chiudere e riaprire l'applicazione");
                        }
                        else {
                            obj.put("sezione","-1");
                            lock.lock();
                            for (int i = 1; i <= doc.getNum_sezioni(); i++) {
                               obj.put(i,sezioni.get(path+(i)));
                            }
                            lock.unlock();
                            new Message(Risposte.OP_SHOW.name() + " " + obj.toString()).sendMessage(server);
                            String testo = risposta.take();
                            String[] risp = testo.split(" ",2);
                            if(risp.length==2)
                                gu.Successo("Le seguenti sezioni sono in fase di modifica "+ risp[1]);
                            if(Risposte.valueOf(risp[0])==Risposte.OP_OK)
                                for(int i = 0; i<doc.getNum_sezioni(); i++){
                                    fillText(buff,text,"Documenti_client/"+nomeutente+"/"+doc.getNome()+(i+1));
                                    text.appendText("\n");
                                }
                            else gu.Allerta("Qualcosa di veramente brutto è successo, si consiglia di chiudere e riaprire l'applicazione");
                        }
                    } catch (IOException | InterruptedException e1) {
                        gu.Allerta("Richiesta fallita se il problema si ripresenta riavviare il sistema");
                    }
                    text.requestFocus();
                    show.setText("EndShow");
                } else {
                    text.setVisible(false);
                    show.setText("Show");
                }
            } else gu.Allerta("Selezionare il file o la sezione del file da visualizzare ");
        });

        edit.setOnAction( e -> {
            //controllo che sia selezionato un elemento
            if(treeTableView.getSelectionModel().getSelectedItem()!=null) {
                TreeItem<Pair<String,Documento>> tree = treeTableView.getSelectionModel().getSelectedItem();
                Documento doc = tree.getValue().getValue();
                if (doc.getNome().equals(tree.getParent().getValue().getKey())) {
                    String sezione = tree.getValue().getKey().split(" ",2)[1];
                    JSONObject obj = new JSONObject();
                    lock.lock();
                    long tempo = sezioni.get(nomeutente+"/"+ doc.getNome()+sezione);
                    lock.unlock();
                    obj.put("file", doc.getNome());
                    obj.put("sezion", sezione);
                    obj.put("tempo",tempo);
                    try {
                        new Message(Risposte.OP_EDIT.name() + " " + obj.toString()).sendMessage(server);
                        try {
                            String[] mess = risposta.take().split(" ",2);
                            try {
                                Risposte.valueOf(mess[0]);
                                gu.Allerta("Sezione occupata da un altro utente");
                            } catch (IllegalArgumentException i) {
                                text.setVisible(true);
                                vbox.getChildren().remove(bottomMenu);
                                vbox.getChildren().add(endeditmenu);
                                toedit.add(tree.getValue());
                                text.setEditable(true);
                                text.setText("");
                                //faccio visualizzare il testo presente nel documento
                                fillText(buff,text,"Documenti_client/"+ nomeutente + "/" + doc.getNome()+sezione);
                                if(!mess[0].equals("")) {
                                    ipmulticast.append(mess[0]);
                                    bp.setRight(udpchat);
                                } else gu.Allerta("Servizio chat disabilitato");
                            }
                        } catch (InterruptedException e1) {
                            gu.Allerta("ERRORE");
                        }
                    } catch (IOException e1){ gu.Allerta("Errore");}

                } else gu.Allerta("Puoi editare solo una sezione, non un intero file");
            } else gu.Allerta("Selezionare la sezione del file da visualizzare ");

        });

        endedit.setOnAction( e -> {
            text.setVisible(false);
            vbox.getChildren().remove(endeditmenu);
            vbox.getChildren().add(bottomMenu);
            bp.setRight(null);
            Pair<String, Documento> dati = toedit.remove(0);
            String name = nomeutente+"/"+dati.getValue().getNome() +dati.getKey().split(" ")[1];
            String path = "Documenti_client/"+name;
            JSONObject obj = new JSONObject();
            obj.put("file", dati.getValue().getNome());
            obj.put("sez", dati.getKey().split(" ",2)[1]);
            //porto il testo dalla textarea al file
            try {
                RandomAccessFile aFile = new RandomAccessFile(path, "rw");
                FileChannel fc = aFile.getChannel();
                fc.truncate(0);
                //dalla textarea al documento
                int len = text.getText().length(), pos = 0;
                String stringa;
                while(pos<len) {
                    if(len - pos < range)
                         stringa = text.getText(pos, len);
                    else stringa = text.getText(pos, range);
                    buff.put(stringa.getBytes(StandardCharsets.UTF_8));
                    buff.flip();
                    while (buff.hasRemaining())
                        fc.write(buff);
                    buff.clear();
                    pos += range;
                }
                fc.position(0);
                new Message(Risposte.OP_ENDEDIT.name() + " " + obj.toString()).sendMessage(server);
                new Message(fc).sendMessage(server);
                fc.close();
                aFile.close();
                Risposte risp = Risposte.valueOf(risposta.take());
                if(risp == Risposte.CLIENT_CLOSEUDP) {
                    lock.lock();
                    sezioni.put(name, sezioni.get(name) + 2);
                    lock.unlock();
                    if(ipmulticast.length()!=0)
                        ipmulticast.delete(0, ipmulticast.length());
                }
                else gu.Allerta("Errore");
            } catch (IOException | InterruptedException ignored){ gu.Allerta("Errore sezione non modificata, ci dispiace per le ore che hai perso, ma può comunque copiare il file modificato recandosi nella sua cartella, spero non la licenzino, un saluto");}
            show.setText("Show");
        });



        udpsend.setOnAction( e -> {
            //per spedire un messaggio alle altre persone che stanno editando lo stesso documento
            if(!udpfield.getText().trim().equals("")){
                try {
                    InetAddress ia = InetAddress.getByName(ipmulticast.toString());
                    String name = nomeutente+ ":";
                    int totlenghtmess = 128;
                    int dim = udpfield.getText().length(), lengh = totlenghtmess - name.length() -1, pos = 0;
                    for(int i = dim; i > 0; i-= lengh) {
                        DatagramPacket p;
                        String string;
                        if(i > lengh)
                            string = name + udpfield.getText(pos,pos+lengh) + "\n";
                        else
                            string = name + udpfield.getText(pos,dim) + "\n";
                        p = new DatagramPacket(string.getBytes(StandardCharsets.UTF_8), 0,
                                    string.getBytes(StandardCharsets.UTF_8).length, ia, 2000);
                        pos+=lengh;
                        ms.send(p);
                    }
                } catch (IOException e1) {
                    gu.Allerta("Errore chat disabilitata");
                    bp.setRight(null);
                }

            }
            udpfield.setText("");
        });

        udpfield.setOnKeyPressed( e -> {
            if(e.getCode() == KeyCode.ENTER) udpsend.fire();
        });

        //Display window and wait for it to be closed before returning
        treeTableView.getSelectionModel().selectedItemProperty().addListener( (v, oldvalue, newvalue) -> {
            if(newvalue!=null) {
                Documento doc = newvalue.getValue().getValue();
                if (doc.getNome().equals(newvalue.getParent().getValue().getKey()))
                    label.setText(doc.toStringDoc() + "\n" + "Hai selezionato la " + newvalue.getValue().getKey());
                else
                    label.setText(doc.toStringDoc());
            }
            else label.setText(defaultlabelstring);
        });

        //-----------------------------------------------------------------------
        Scene scene = new Scene(bp, 750,550);
        window.setScene(scene);
        if(pendenti) gu.Successo("Attenzione mentre eri offline sei stato invitato a modificare nuovi documenti " +
                "premi List per visualizzarli");
        window.showAndWait();
    }
}
