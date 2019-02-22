import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Comparator;
import java.util.Optional;

class Guisupport{

    Guisupport(){

    }

    //crea un label e ne indica la posizione nella gridpane
    Label setLabel(String text, int index){
        Label label = new Label(text);
        GridPane.setConstraints(label,0,index);
        return label;
    }

    //crea una Textfield e ne indica la posizione nella gridpane
    TextField setTextField(String message,int index) {
        TextField text = new TextField();
        text.setPromptText(message);
        GridPane.setConstraints(text, 0, index);
        return text;
    }

    //crea un PasswordField e ne indica la posizione nella gridpane
    PasswordField setPassField(String message,int index) {
        PasswordField pass = new PasswordField();
        pass.setPromptText(message);
        GridPane.setConstraints(pass, 0, index);
        return pass;
    }

    //crea una Button e ne indica la posizione nella gridpane
    Button setButton(String message, int index, HPos pos){
        Button bottone = new Button(message);
        GridPane.setConstraints(bottone,0,index);
        GridPane.setHalignment(bottone, pos);
        return bottone;
    }

    void Allerta(String message){
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.FINISH);
        alert.showAndWait();
    }

    void Successo(String message){
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message
                , ButtonType.FINISH);
        alert.showAndWait();
    }

    Optional<String> taxtallert(String title, String header,String content){
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        return dialog.showAndWait();
    }
}

public class MainClient extends Application {

    private static Registrazione rmi = null;
    private static SocketChannel server;
    private Scene scena, scena2;

    public static void main(String[] args) {
        //per l'interfaccia
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        Guisupport gu = new Guisupport();

        ////connessio a rmi e server
        Registry reg = null;
        while (reg == null)
            try {
                reg = LocateRegistry.getRegistry(9999);
            } catch (IOException e) {
                gu.Allerta("registro non disponibile al momento, per riprovare premere il tasto fine");
            }
        while (rmi == null)
            try {
                rmi = (Registrazione) reg.lookup(Registrazione.SERVICE_NAME);
            } catch (NotBoundException | RemoteException e) {
                gu.Allerta("Operazione di lookup fallita, per riprovare premere il tasto fine");
            }

        SocketAddress address = new InetSocketAddress("localhost", 5000);
        while (server == null)
            try {
                server = SocketChannel.open();
                boolean connesso = false;
                while (!connesso)
                    try {
                        connesso = server.connect(address);
                    } catch (IOException e){
                        gu.Allerta("Connessione fallita, per riprovare premere il tasto fine");
                    }
            } catch (IOException e){
                gu.Allerta("Impossibile aprire il server, per riprovare premere il tasto fine");
            }

        //--------------------------------------------------------------


        //--------------------------- grafica ----------------------------
        primaryStage.setTitle("Log In");
        StackPane layout = new StackPane(),layout2 = new StackPane();
        primaryStage.setAlwaysOnTop(false);
        //è una classe di supporto che uso per creare allertbox e i vari elementi della Gui

        GridPane gp = new GridPane();
        gp.setPadding(new Insets( 10,10,10,10));
        gp.setHgap(10);
        gp.setVgap(10);

        Label userLabel = gu.setLabel("Username",0);
        Label passwordLabel = gu.setLabel("Password",2);
        Label conferma = gu.setLabel("Conferma Password",4);

        TextField username = gu.setTextField("Username",1);

        PasswordField password = gu.setPassField("Password",3);
        PasswordField confermapassword = gu.setPassField("Conferma la Password",5);

        Button registrati = gu.setButton("Registrati",4,HPos.RIGHT);
        Button accedi = gu.setButton("Accedi",4,HPos.LEFT);
        Button confermaregistrazione = gu.setButton("Conferma",6,HPos.RIGHT);
        Button indietro = gu.setButton("Indietro",6,HPos.LEFT);
        //-------------------------------------------------------------

        //------------------------ eventi -----------------------------
        registrati.setOnAction(e -> {
            //aggiungo e rimuovo nodi nella scena, per potermi registrare
            gp.getChildren().removeAll(accedi,registrati);
            gp.getChildren().addAll(conferma,confermapassword,confermaregistrazione,indietro);
            layout2.getChildren().add(gp);
            primaryStage.setTitle("Registrati");
            primaryStage.setScene(scena2);
        });

        indietro.setOnAction(e -> {
            //aggiungo e rimuovo nodi nella scena per tornare al og in
            gp.getChildren().removeAll(conferma,confermapassword,confermaregistrazione,indietro);
            gp.getChildren().addAll(accedi,registrati);
            layout.getChildren().add(gp);
            accedi.setDefaultButton(true);
            primaryStage.setTitle("Log In");
            primaryStage.setScene(scena);
        });

        accedi.setOnAction(e -> {
            String[] risp = null;
            Risposte richiesta = Risposte.OP_LOGIN;
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", username.getText());
                obj.put("password", password.getText());

                //invio la richiesta e ricevo la risposta
                Message LogInRequest = new Message(Risposte.OP_LOGIN.name() + " " + obj.toString());
                System.out.println(LogInRequest.getText());
                LogInRequest.sendMessage(server);
                Message answer = new Message(server);
                System.out.println(answer.getText());
                risp = answer.getText().split(" ",2);
                richiesta = Risposte.valueOf(risp[0]);
                //---------------------------------------
            } catch (IOException i){
                System.exit(-1);
            }
            switch (richiesta){
                case OP_OK: {
                    //cancello cartelle con lo stesso nome dell'utente e creo la cartella utente
                    try {
                        Files.walk(Paths.get("Documenti_client/"+username.getText()))
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    }catch (IOException ignored){}
                    try {
                        Files.createDirectory(Paths.get("Documenti_client/"+username.getText()));
                    }catch (IOException i){ gu.Allerta("Impossibile creare la cartella utente, cambiare directory o permessi");
                        System.exit(-1);}
                    //--------------------------------------------------------------------------
                    primaryStage.hide();
                    //per far apparire all' utente la notifiche di nuovi inviti
                    if(risp.length == 1)
                        MainViewClient.display(username.getText(), primaryStage,server, false);
                    else MainViewClient.display(username.getText(), primaryStage,server, true);
                    username.setText("");
                    password.setText("");
                    confermapassword.setText("");
                    username.requestFocus();
                    break;
                }
                case ERROR: {
                    gu.Allerta("Nome utente o password errato");
                    break;
                }
                case ERROR_ALREADYONLINE: {
                    gu.Allerta("Questo utente è già loggato");
                    break;
                }
            }

        });

        confermaregistrazione.setOnAction(e -> {
            boolean bool = false;
            //se il nome è diverso dalla stringa vuota
            String nome = username.getText().trim();
            if(nome.length()>0 && nome.length()<16) {
                //se la password è almeno di 4 carattero
                if(password.getText().length()>3 && password.getText().length()<21) {
                    //la password e la conferma password devono coincidere
                    if (password.getText().equals(confermapassword.getText())) {
                        try {
                            bool = rmi.register(nome, password.getText());
                        } catch (RemoteException e1) {
                            gu.Allerta("Errore, Chiusura applicazione");
                            System.exit(-1);
                        }
                        if (bool) {
                            gu.Successo("Registrazione avvenuta con successo");
                            indietro.fire();
                        } else
                            gu.Allerta("Nome utente già in uso");
                    } else {
                        gu.Allerta("Password diverse");
                    }
                }
                else gu.Allerta("La password deve essere di almeno 4 caratteri e non deve avere più di 20 caratteri");
            }
            else
                gu.Allerta("L'username deve contenere almeno un carattere e non deve avere più di 15 caratteri, gli spazi iniziali e finali non sono contati");
        });

        //faccio questo per poter premere invio e potersi registrare
        confermapassword.setOnKeyPressed(e ->{
            if(e.getCode() == KeyCode.ENTER)
                confermaregistrazione.fire();
        });

        //per poter fare il login premendo invio
        password.setOnKeyPressed(e ->{
            accedi.setDefaultButton(false);
            if(e.getCode() == KeyCode.ENTER && primaryStage.getTitle().equals("Log In"))
                accedi.fire();
        });

        //--------------------------------------------------------------

        gp.getChildren().addAll(userLabel,passwordLabel,username,password,registrati,accedi);
        layout.getChildren().addAll(gp);
        scena = new Scene(layout,190, 180);
        scena2 = new Scene(layout2,190,250);
        primaryStage.setScene(scena);
        primaryStage.show();
    }

}
