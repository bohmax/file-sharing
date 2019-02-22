import java.util.LinkedList;
import java.util.NoSuchElementException;

class MulticastIP {

    //inserisco qui gli ip usati precedentemente ma non più in uso in modo da poterli riutilizzare
    private LinkedList<String> recicleIP = new LinkedList<>();
    //indica l'ultimo ip generato
    private String lastIPgenerated;
    //impongo un limite sugli ip da generare
    private String stopgenerating = "239.0.0.0";

    MulticastIP(){
        lastIPgenerated = "224.0.1.1";
    }

    /**
     * restituisce un ip multicast utilizzabile
     * @return un ip multicast utilizzabile
     */
    String getIP(){
        String ip;
        try {
            //provo a prendere l'ip da quelli generati precedentemente,
            // se non è possibile nel catch ne viene creato uno nuovo
            ip = recicleIP.removeFirst();
        } catch (NoSuchElementException e){
            ip = getNextIPV4Address(lastIPgenerated);
            System.out.println(ip);
            lastIPgenerated = ip;
        }
        return ip;
    }

    /**
     * viene reciclato un ip
     * @param ip l'ip da reciclare
     */
    void freeIP(String ip){
        recicleIP.add(ip);
    }

    //funzione presa da https://stackoverflow.com/questions/6295645/how-to-get-the-next-ip-address-from-a-given-ip-in-java

    /**
     * funzione che genera un nuovo ip multicast da utilizzare
     * @param ip l'ultimo ip generato
     * @return null se non è più possibile generare nuovi ip, l'ip nuovo altrimenti
     */
    private String getNextIPV4Address(String ip) {
        if (!ip.equals(stopgenerating)) {
            String[] nums = ip.split("\\.");
            int i = (Integer.parseInt(nums[0]) << 24 | Integer.parseInt(nums[2]) << 8
                    | Integer.parseInt(nums[1]) << 16 | Integer.parseInt(nums[3])) + 1;

            // If you wish to skip over .255 addresses.
            if ((byte) i == -1) i++;

            return String.format("%d.%d.%d.%d", i >>> 24 & 0xFF, i >> 16 & 0xFF,
                    i >> 8 & 0xFF, i & 0xFF);
        } else return null;
    }
}
