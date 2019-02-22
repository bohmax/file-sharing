import org.json.simple.JSONObject;

public class Documento {

    private String nome_documento;
    private String creatore;
    private int num_sezioni;
    private JSONObject obj = null;

    /**
     *
     * @param nome nome del documento
     * @param numero_sezioni numero di sezioni del documento
     * @param creatore nome del creatore del documento
     */
    Documento(String nome, int numero_sezioni, String creatore){
        obj = new JSONObject();
        nome_documento = nome;
        num_sezioni = numero_sezioni;
        this.creatore = creatore;
        obj.put("num",numero_sezioni);
        obj.put("name",nome);
        obj.put("creator",creatore);
    }

    /**
     *
     * @param doc json object che descrive un file
     */
    Documento(JSONObject doc){
        obj = doc;
        num_sezioni = Math.toIntExact( (Long) obj.get("num"));
        nome_documento = (String) obj.get("name");
        creatore = (String) obj.get("creator");
    }

    /**
     * crea un documento che ha come unica informazione in nome del documento, può essere utile per la ricerca di altri documenti
     * @param nome_documento il nome del documento
     */
    Documento(String nome_documento){
        this.nome_documento = nome_documento;
    }

    /**
     * restituisce il nome del documento
     * @return nome del documento
     */
    String getNome() {
        return nome_documento;
    }

    /**
     * restituisce il nome del creatore
     * @return nome del creatore
     */
    String getCreatore() {
        return creatore;
    }

    /**
     * restituisce il numero delle sezioni
     * @return il numero delle sezioni
     */
    int getNum_sezioni() {
        return num_sezioni;
    }

    /**
     * restituisce il jsonobject che rappresenta il file
     * @return un json object che rappresenta l'oggetto, null se si è usato il metodo costruttore avente solo il parametro del nome
     */
    JSONObject getObj() {
        return obj;
    }

    /**
     *
     * @return una stringa rappresentante l'oggetto
     */
    @Override
    public String toString() {
        return obj.toString();
    }

    /**
     *
     * @return una stringa che rappresenta meglio un documento
     */
    String toStringDoc() {
        return "Il titolo è " + nome_documento + "\n" +
                "ha " + num_sezioni + " sezioni \n" +
                "ed è stato creato da " + creatore;
    }

    /**
     * metodo usato per confrontare i documenti
     * @param o deve essere castabile a Documento
     * @return false se i nomi dei documenti sono diversi o O non è castabile a Documento, true altrimenti
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Documento)) return false;

        Documento doc = (Documento) o;
        return getNome().equals(doc.getNome());
    }
}
