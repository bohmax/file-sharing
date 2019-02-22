import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.*;

class Sezione {

    //tempo che scandisce quando un file viene creato o modificato,
    //serve per evitare di inviare nuovamente tutto il file a un client che ha già l'ultima versione della sezione
    private long timeofsection;
    //prende oltre i tempi precedenti anche i tempi delle modifiche delle sezioni
    private long realtimeofsection;
    //path del file
    private Path path;
    //FileChannel associato alla sezione
    private FileChannel fc;
    //l'utente che sta modificando la sezione
    private String occupato = "";


    /**
     * Utilizzato per creare una sezione di un documento
     * @param path percorso nel quale viene creato il file, il path deve anche contenere il nome del file
     * @throws IOException nel caso il file non sia creabile
     */
    Sezione(String path) throws IOException {
        this.path = Paths.get(path);
        Files.createFile(this.path);
        RandomAccessFile aFile = new RandomAccessFile(path, "rw");
        fc = aFile.getChannel();
        timeofsection = 0;
        realtimeofsection = 0;
    }

    /**
     * ritorna il filechannel della sezione
     * @return filechannel della sezione
     */
    FileChannel fc(){ return fc; }

    /**
     * Metodo per sapere se la sezione sta venendo modificata da qualche utente
     * @return true se la sezione è in fase di modifica, false altrimenti
     */
    boolean IsOccupied(){return !occupato.equals("");}

    /**
     * permette di sapere se name è la persona che sta modifcando la sezione
     * @param name nome della persona che si pensa stia modificando la sezione
     * @return true se name coincide col nome della persona che sta modificando la sezione
     */
    boolean WhoOcuupied(String name){ return occupato.equals(name);}

    /**
     * si imposta il nome della persona che modifica la sezione
     * @param nome nome della persona a cui si vuole far modificare la sezione
     */
    void setOccupied(String nome){
        occupato = nome;
        realtimeofsection++;
    }

    /**
     * Libera la sezione
     */
    void freeOccupied(){
        //se un utente si disconnette prima di aver fatto lo write faccia finta che non abbia mai richiesto la modifica
        if((realtimeofsection-1)==timeofsection)
            realtimeofsection--;
        else timeofsection = realtimeofsection;
        occupato="";
    }

    /**
     * metodo con cui si modifica il file
     * @param client Socket con cui comunicare per ricevere il file modificato
     * @return true se la scrittura è andata a buon file, false altrimenti
     */
    boolean write(SocketChannel client){
        try {
            Path percorso = Paths.get(path.toString()+"temp");
            Files.createFile(percorso);
            RandomAccessFile aFile = new RandomAccessFile(percorso.toString(), "rw");
            FileChannel fc = aFile.getChannel();
            new Message(client,fc);
            fc.close();
            //il primo parametro è la source il secondo è la destinazione, con questo motedo vado a fare un replace del file
            Files.move(percorso, path, StandardCopyOption.REPLACE_EXISTING);
            this.fc.close();
            RandomAccessFile file = new RandomAccessFile(path.toString(), "rw");
            this.fc = file.getChannel();
            realtimeofsection++;
            return true;
        } catch (IOException e){
            realtimeofsection--;
            return false;
        }
    }

    /**
     * ritorna l'ultimo istante in cui è stato modificato
     * @return l'ultimo istante in cui è stato modificato
     */
    long getTimeofsection(){return timeofsection;}

    /**
     * Questa funzione tiene conto di tutte le possibili operazioni che un utente può fare come tenere la modifica della sezione
     * @return l'istante reale della sezione
     */
    long getRealTimeofsection(){return realtimeofsection;}
}
