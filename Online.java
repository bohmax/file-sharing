class Online {

    private final String nome;

    //documento e sezione che vorrebbero o stanno modificando
    //questi campi sono rispettivamente null e -1 se non si sta modificando sezioni
    private Documento_Server documento;
    private int sezione;

    /**
     *
     * @param nome nome dell'utente che si è appena connesso
     */
    Online(String nome) {
        this.nome = nome;
        this.documento = null;
        this.sezione = -1;
    }

    /**
     * ritorna il nome dell'utente
     * @return il nome dell'utente
     */
    String getNome() { return nome; }

    /**
     * ritorna il documento del server
     * @return il documento che sta modificando
     */
    Documento_Server getDocumento() { return documento; }

    /**
     * ritorna il valore della sezione
     * @return la sezione che sta modificando
     */
    int getSezione() { return sezione; }

    /**
     * si cerca di prendere la sezione del documento che si vuole acquisire
     * @param doc il documento che si vuole acquisire
     * @param sezione la sezione che si vuole acquisire
     * @return true se si può cercare di acquisire la sezione, false altrimenti
     */
    boolean setEdit(Documento_Server doc,int sezione) {
        if(documento==null) {
            documento = doc;
            this.sezione = sezione;
            return true;
        }
        return false;
    }

    /**
     * imposta ai valori iniziali il documento e la sezione dell'utente online
     */
    void setEndEdit() {
        documento = null;
        sezione=-1;
    }
}