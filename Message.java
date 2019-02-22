import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

class Message {

    //range per capire se spedire un messaggio usando il wrapper oppure no
    private final static int range = 8;
    //utilizzato per leggere il messaggio ad esclusione della lunghezza del messaggio
    private static ByteBuffer buffer = ByteBuffer.allocate(range);
    //utilizzato per sapere quanti byte leggere dall'intero messaggio
    private static ByteBuffer header = ByteBuffer.allocate(8);
    //lunghezza del messaggio
    private long lenght=0;
    //messaggio da inviare
    private byte[] message = null;
    //bytebuffer con cui si spedisce il messaggio
    private ByteBuffer writer;
    //se si vogliono spedire file
    private FileChannel fc = null;
    //Stringa che rappresente il contenuto di messagge
    private String mess = "";
    //serve per sapere se un messaggio è stato spedito
    private boolean spedito = false;

    /* ho quattro metodi costruttori,
    il primo si usa quando si deve prelevare il messaggio dal socket
    il secondo quando si legge un messaggio generico
    il terzo viene utilizzato per spedire file e il quarto per riceverne
    in alcuni casi decido la dimensione del bytebuffer perchè voglio ottenere le migliori prestazioni possibili
    facendo il wrap in genere si hanno le prestazioni migliori ma rischio di finire la memoria se molti client si connettono
    contemporaneamente, perciò tengo il valore di range vicino a 4mb, che rappresenta un ottimo compromesso */

    /**
     * Quando si utilizza questo metodo costruttore si va a leggere un messaggio da dal client
     * @param client Socket channel da cui si vuole ottenere il messaggio
     * @throws IOException Se il client chiude la connessione
     */
    Message(SocketChannel client) throws IOException {
        getHeader(client);
        System.out.println(lenght);
        writer = buffer;
        message = getBodyMess(client);
        if(lenght<=range)
            writer = ByteBuffer.wrap(message);
    }

    /**
     * da utilizzare quando si vuole inviare un messaggio, si deve usare a seguire il metodo sendmessage ovviamente
     * @param message il messaggio da spedire
     */
    Message(String message){
        this.message = message.getBytes(StandardCharsets.UTF_8);
        mess = new String(this.message, StandardCharsets.UTF_8);
        lenght = message.length();
        if(lenght<=range)
            writer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
        else
            writer = buffer;
    }

    /**
     * Da utilizzare quando si vuole spedire un file, anche qua si deve accompagnare con un messaggio
     * @param fc FileChannel del file che si vuole spedire
     * @throws IOException se c'è un  errore di IO
     */
    Message(FileChannel fc) throws IOException {
        this.fc = fc;
        lenght = fc.size();
        writer = buffer;
    }

    /**
     * Quando si vuole ricevere un file dal client specificato
     * @param client Socket channel che si vuole ascoltare
     * @param fc filechannel su cui deve essere trasferito il file
     * @throws IOException Se il client si disconnetto o c'è un errore di IO
     */
    Message(SocketChannel client, FileChannel fc) throws IOException {
        this.fc = fc;
        writer = buffer;
        getHeader(client);
        getBodyFile(client);
    }

    /*-------------------------metodi per spedire il messaggio-------------------------------*/


    //spedisce il numero di byte da leggere
    private void sendHeader(SocketChannel socket) throws IOException {
        int i = 0,j;
        header.putLong(0,lenght);
        while(i != 8) {
            if((j = socket.write(header))==-1)
                throw new IOException();
            i += j;
        }
        header.clear();
    }

    //spedisce il messaggio, ho usato l'approccio della wrap perchè ho notato che per file medio piccoli è
    //più efficiente
    private long sendBodyMess(SocketChannel socket) throws IOException {
        long spediti = 0, mandati;
        if (writer != buffer) {
            while (spediti != message.length)
                if((mandati = socket.write(writer))!=-1)
                    spediti += mandati;
                else throw new IOException();
            writer.rewind();
        } else {
            for (int i = 0; i < message.length; i += range) {
                try {
                    writer.put(message, i, range);
                } catch (IndexOutOfBoundsException e) {
                    writer.put(message, i, message.length - i);
                }
                writer.flip();
                while (writer.hasRemaining())
                    if((mandati = socket.write(writer))!=-1)
                        spediti+=mandati;
                    else throw new IOException();
                writer.clear();
            }
        }
        spedito = true;
        return spediti;
    }

    //caso in cui spedire un file
    private long sendBodyFile(SocketChannel socket) throws IOException {
        long spediti=0,x=0;
        fc.position(0);
        while (x != -1) {
            x = fc.read(writer);
            spediti+=x;
            writer.flip();
            while(writer.hasRemaining())
                if(socket.write(writer)==-1)
                    throw new IOException();
            writer.clear();
        }
        spedito = true;
        return spediti+1;
    }

    /**
     * metodo da utilizzare per spedire un Message
     * @param socket del quale si vuole inviare il messaggio
     * @return i byte spediti
     * @throws IOException se il client si disconnette
     */
    long sendMessage(SocketChannel socket) throws IOException {
        long i = 0;
        header.clear();
        writer.clear();
        sendHeader(socket);
        if(message != null)
            i = sendBodyMess(socket);
        if(fc != null)
            i += sendBodyFile(socket);
        return i;
    }

    /*--------------------------------fine per spedire---------------------------------------*/


    /*-------------------------metodi per ricevere un messaggio------------------------------*/

    //riceve il numero di byte da leggere
    private void getHeader(SocketChannel socket) throws IOException {
        int letti=0,i;
        header.clear();
        while( letti != 8) {
           if((i = socket.read(header))==-1)
               throw new IOException();
           letti += i;
        }
        lenght = header.getLong(0);
    }

    //metodo usato per ricevere il messaggio
    private byte[] getBodyMess(SocketChannel socket) throws IOException {
        int i = (int)lenght, pos = 0, readed;
        message = new byte[i];
        writer.clear();
        while (i>0) {
            if(i<range)
                writer.limit(i);
            readed = socket.read(writer);
            if(readed==-1)
                throw new IOException();
            i -= readed;
            writer.flip();
            writer.get(message, pos, readed);
            pos += readed;
            writer.clear();
        }
        mess = new String(message, StandardCharsets.UTF_8);
        return message;
    }

    //per ricevere un file
    private void getBodyFile(SocketChannel socket) throws IOException{
        long i = lenght, readed;
        writer.clear();
        while (i > 0){
            if(i<range)
                writer.limit((int)i);
            readed = socket.read(writer);
            if (readed==-1)
                throw new IOException();
            i -= readed;
            writer.flip();
            while (writer.hasRemaining())
                fc.write(writer);
            writer.clear();
        }
    }

    /*------------------------fine metodi per ricevere messaggio-----------------------------*/

    /**
     * per sapere che messaggio si è ricevuto se non si è ricevuto un file
     * @return il messaggio
     */
    String getText() {
        return mess;
    }

    /**
     * per sapere se un messaggio è già stato spedito
     * @return true se è già stato spedito, false altrimenti
     */
    boolean wassend(){ return spedito;}

    long getLenght(){
        return lenght;
    }
}