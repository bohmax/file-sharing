import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Registrazione extends Remote {

    String SERVICE_NAME = "RegisterOP";

    /**
     * Metodo usato per registrare un utente
     * @param nome nome dell utente
     * @param password password dell utente
     * @return true se l'utente non esiste ancora, false altrimenti
     * @throws RemoteException se viene lanciata l'exception
     */
    boolean register(String nome, String password) throws RemoteException;

}
