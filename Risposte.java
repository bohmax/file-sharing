public enum Risposte {

    //-------- richieste dal client al server --------
    OP_LOGIN,
    OP_LOGOUT,
    OP_CREATE,
    OP_ADDTOFILE,
    OP_LIST,
    OP_SHOW,
    OP_EDIT,
    OP_ENDEDIT,
    //------------------------------------------------

    //-------- messaggi di errore inviati dal server --------
    ERROR, //errore generico
    ERROR_ALREADYONLINE, //indica che il client sta provando a fare il login di un utente già online
    ERROR_FILEALREADY, //indica che il client sta provando a creare un file che ha lo stesso nome di uno creato precedentemente
    ERROR_ALREADY, //errore generico per qualcosa che è già avvenuto
    ERROR_NOTEXIST, //indica che si sta provando ad accedere a qualcosa non presente nel server
    ERROR_OCCUPIED, //indica che la sezione che si sta cercando di occupare è già occupata
    ERROR_NOTEDIT, //indica che il server non è riuscito a registrare la modifica di una sezione
    //-------------------------------------------------------

    //-------- il server indica cosa fare al clientlistener ---------
    OP_OK,
    /**
     * notifica al client che ha nuovi documenti a cui è stato invitato
     */
    CLIENT_NOTIFICA,
    CLIENT_CLOSE,
    /**
     * aggiunge un documento alla lista dei
     * documenti selezionabili dall'utente
     */
    CLIENT_ADDTOTREE,
    /**
     * operazione di edit accettata dal client facendo partire il thread che si occupa dell udp
     */
    CLIENT_OPENUDP,
    /**
     * operazione di endedit accettata dal client facendo chiudere il thread che si occupa dell udp
     */
    CLIENT_CLOSEUDP,
    /**
     * per notificare al listener che deve scaricare le sezioni
     */
    CLIENT_SHOW
    //-------------------------------------------------------
}
