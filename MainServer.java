import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MainServer {
    public static void main(String[] args) throws IOException {
        ConcurrentMap<String,Utente> hash = new ConcurrentHashMap<>();
        TreeMap<String, Online> utenti_online = new TreeMap<>();
        //ATTENZIONE ELIMINO LA CARTELLA DOCUMENTI E TUTTO IL SUO CONTENUTO
        try {
            Files.walk(Paths.get("Documenti_server"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }catch (IOException ignored){}
        try {
            Files.walk(Paths.get("Documenti_client"))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        }catch (IOException ignored){}
        Files.createDirectory(Paths.get("Documenti_client"));
        Files.createDirectory(Paths.get("Documenti_server"));
        ServerMultiplexing multi = new ServerMultiplexing(hash, utenti_online,5000);

        //rmi
        try {
            Registrazione registrazione = new Registrazione_Impl(hash);
            LocateRegistry.createRegistry(9999);
            Registry r = LocateRegistry.getRegistry(9999);
            r.rebind(Registrazione.SERVICE_NAME, registrazione);
        } catch (Exception e){
            System.exit(-1);
        }

        try {
            multi.multiplexing();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
