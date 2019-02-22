import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentMap;

public class Registrazione_Impl extends UnicastRemoteObject implements Registrazione{

    private ConcurrentMap<String,Utente> hash;

    /**
     *
     * @param table la tabella hash su cui registrare gli utenti
     * @throws RemoteException
     */
    Registrazione_Impl(ConcurrentMap<String,Utente> table) throws RemoteException {
        hash = table;
    }

    /**
     * Metodo usato per registrare un utente
     * @param nome nome dell utente
     * @param password password dell utente
     * @return true se l'utente non esiste ancora, false altrimenti
     */
    public boolean register(String nome, String password){
        Utente utente = new Utente(password);
        return hash.putIfAbsent(nome, utente) == null;
    }
}
