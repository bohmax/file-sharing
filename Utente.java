import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

class Utente {

    //password dell'utente
    private String password;
    //lista dei documenti che l'utente è abilitato a modificare
    private List<Documento> listaDocumenti;
    private SelectionKey online;
    //se sono arrivate notifiche mentre l'utente era offline diventa true
    private boolean inviti_da_notificare = false;

    /**
     *
     * @param password password dell'utente
     */
    Utente (String password){
        this.password = password;
        listaDocumenti = new ArrayList<>();
    }

    /**
     * imposta lo stato online dell'utente passando la selectionkey a lui associato
     * @param online SelectionKey associata all'utente che si vuole loggare
     */
    void setOnline(SelectionKey online) {
        this.online = online;
    }

    /**
     * ottieni la password dell'utente
     * @return la password dell'utente
     */
    String getPassword() {
        return password;
    }

    /**
     * ritorna la lista dei documenti che l'utente è abilitato a modificare
     * @return la lista dei documenti
     */
    List<Documento> getListaDocumento() {
        return listaDocumenti;
    }

    /**
     * inserisce nella lista il documento in modo da abilitarne l'utente alla modifica
     * @param doc il documento che si vuole aggiungere alla lista
     * @return true se il documento non è già presente nella lista, false altrimenti
     */
    boolean addLista(Documento doc) {
        if(!listaDocumenti.contains(doc)){
            listaDocumenti.add(doc);
            return true;
        }
        return false;
    }

    /**
     * Per conoscere quando l'utente è online
     * @return true se l'utente è online, false altrimenti
     */
    boolean Is_online() {
        return online!=null;
    }

    /**
     * restituisce la selection key dell'utente
     * @return null se l'utente non è online, la selectionKey altrimenti
     */
    SelectionKey getOnline(){
        return online;
    }

    /**
     * Permette di sapere se un utente mentre era offline è stato invitato a un documento
     * @return true se è stato invitato mentre era offline, false altrimenti
     */
    boolean getnotifica(){ return inviti_da_notificare;}

    /**
     * Per impostare il valore della notifica
     * @param notifica valore da utilizzare per notificare il client al prossimo log in
     */
    void setnotifica(boolean notifica){inviti_da_notificare = notifica;}
}
