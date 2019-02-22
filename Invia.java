import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * Astrazione che contiene un array di Message e permette l'invio di questi
 */
class Invia {

    private Message[] message;

    /**
     *
     * @param mess quando bisogna spedire una singola string
     */
    Invia(String mess){
        message = new Message[]{new Message(mess)};
    }

    /**
     *
     * @param mess quando bisogna spedire un singolo Message
     */
    Invia(Message mess) {
        message = new Message[]{mess};
    }

    /**
     * Quando spedire più messaggi in un colpo solo
     * @param  stringa dei messaggi da spedire
     * @throws IOException se c'è un errore di IO
     */
    Invia(String[] stringa) throws IOException {
        message = new Message[stringa.length];
        for (int i = 0;i<stringa.length;i++)
            message[i] = new Message(stringa[i]);
    }

    /**
     * Per spedire un messaggio e un file tutto insieme
     * @param header Il messaggio da spedire
     * @param fc FileChannel del file con cui spedire i dati del file
     * @throws IOException se c'è un errore di IO
     */
    Invia(String header, LinkedList<FileChannel> fc) throws IOException {
        int size= fc.size()+1;
        message = new Message[size];
        message[0] = new Message(header);
        for (int i = 1; i <  size; i++) {
            message[i] = new Message(fc.removeFirst());
        }
    }

    /**
     * Per sapere se sono stati spediti tutti i messaggi
     * @return true se sono stati spedititi tutti i messaggi, false altrimenti
     */
    boolean wassend(){
        for (Message mess: message) {
            if(!mess.wassend())
                return false;
        }
        return true;
    }

    /**
     * Utilizzato per spedire tutti i messaggi in un colpo solo
     * @param client Utente a cui spedire i messaggi
     * @return Il numero di Byte spediti
     * @throws IOException se non si è riusciti a inviare il messaggio
     */
    long sendMessage(SocketChannel client) throws IOException{
        long send = 0;
        for (Message mess: message) {
            if(!mess.wassend()) {
                send += mess.sendMessage(client);
            }
        }
        return send;
    }
}
