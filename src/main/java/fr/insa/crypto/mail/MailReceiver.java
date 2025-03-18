package fr.insa.crypto.mail;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * Classe permettant de recevoir des emails depuis un serveur POP3S
 */
public class MailReceiver {
    
    private Properties properties;
    private Session session;
    private Store store;
    private Folder emailFolder;
    
    private String host;
    /**
     * Constructeur avec paramètres par défaut pour Gmail
     */
    public MailReceiver() {
        this("pop.gmail.com", "995");
    }
    
    /**
     * Constructeur avec paramètres personnalisés
     * @param host Nom du serveur POP3
     * @param port Port du serveur POP3
     */
    public MailReceiver(String host, String port) {
        this.host = host;
        
        properties = new Properties();
        properties.put("mail.pop3s.host", host);
        properties.put("mail.pop3s.port", port);
        properties.put("mail.pop3s.ssl.enable", "true");
        properties.put("mail.pop3s.ssl.protocols", "TLSv1.2");
        properties.put("mail.pop3s.ssl.trust", host);
        
        session = Session.getDefaultInstance(properties);
    }
    
    /**
     * Se connecte au serveur de messagerie
     * @param username Nom d'utilisateur (adresse email)
     * @param password Mot de passe ou mot de passe d'application
     * @throws MessagingException En cas d'erreur de connexion
     */
    public void connect(String username, String password) throws MessagingException {
        store = session.getStore("pop3s");
        store.connect(host, username, password);
    }
    
    /**
     * Ouvre le dossier spécifié
     * @param folderName Nom du dossier (ex: "INBOX")
     * @param readOnly Mode lecture seule si true
     * @throws MessagingException En cas d'erreur
     */
    public void openFolder(String folderName, boolean readOnly) throws MessagingException {
        emailFolder = store.getFolder(folderName);
        emailFolder.open(readOnly ? Folder.READ_ONLY : Folder.READ_WRITE);
    }
    
    /**
     * Récupère tous les messages du dossier ouvert
     * @return Tableau des messages
     * @throws MessagingException En cas d'erreur
     */
    public Message[] getMessages() throws MessagingException {
        return emailFolder.getMessages();
    }
    
    /**
     * Ferme les connexions
     * @throws MessagingException En cas d'erreur
     */
    public void close() throws MessagingException {
        if (emailFolder != null && emailFolder.isOpen()) {
            emailFolder.close(false);
        }
        if (store != null && store.isConnected()) {
            store.close();
        }
    }
}