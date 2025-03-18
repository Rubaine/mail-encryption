package fr.insa.crypto.mail;

import javax.mail.*;
import javax.mail.search.FlagTerm;
import javax.net.ssl.*;

import fr.insa.crypto.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Classe pour la réception des emails chiffrés
 */
public class MailReceiver {
    private String host;
    private String email;
    private String password;
    private Properties properties;
    private Session session;
    private Store store;
    
    /**
     * Constructeur pour initialiser le récepteur d'emails
     * 
     * @param host     Serveur de messagerie (ex: "imap.gmail.com")
     * @param email    Email de l'utilisateur
     * @param password Mot de passe ou clé d'application
     */
    public MailReceiver(String host, String email, String password) {
        this.host = host;
        this.email = email;
        this.password = password;
        
        // Configuration pour IMAP avec paramètres étendus pour compatibilité SSL/TLS
        properties = new Properties();
        properties.setProperty("mail.store.protocol", "imaps");
        properties.setProperty("mail.imaps.host", host);
        properties.setProperty("mail.imaps.port", "993");
        properties.setProperty("mail.imaps.ssl.enable", "true");
        properties.setProperty("mail.imaps.ssl.trust", "*");  // Trust all hosts
        
        // Spécification explicite des protocoles SSL/TLS supportés
        properties.setProperty("mail.imaps.ssl.protocols", "TLSv1.2 TLSv1.3");
        
        try {
            // Utiliser un socket factory qui fait confiance à tous les certificats
            SSLSocketFactory sf = createTrustAllSSLSocketFactory();
            properties.put("mail.imaps.socketFactory", sf);
            properties.put("mail.imaps.socketFactory.fallback", "false");
            properties.put("mail.imaps.socketFactory.port", "993");
        } catch (GeneralSecurityException e) {
            Logger.error("Erreur lors de la création du socket factory: " + e.getMessage());
        }
        
        // Désactiver la vérification du certificat
        properties.setProperty("mail.imaps.ssl.checkserveridentity", "false");
        
        // Gérer les timeouts
        properties.setProperty("mail.imaps.connectiontimeout", "30000");
        properties.setProperty("mail.imaps.timeout", "30000");
        properties.setProperty("mail.imaps.writetimeout", "30000");
        
        // Activation du débogage pour voir les informations de connexion détaillées
        properties.put("mail.debug", "true");
        properties.put("mail.debug.auth", "true");
        
        Logger.info("MailReceiver configuré pour " + email + " avec host " + host);
    }
    
    /**
     * Crée un SSLSocketFactory qui accepte tous les certificats
     * Note: À utiliser uniquement dans un environnement de développement/test
     * 
     * @return SSLSocketFactory qui accepte tous les certificats
     */
    private SSLSocketFactory createTrustAllSSLSocketFactory() throws GeneralSecurityException {
        TrustManager[] trustAllCerts = new TrustManager[] { 
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { 
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return sslContext.getSocketFactory();
    }
    
    /**
     * Se connecter au serveur mail
     * 
     * @return true si la connexion est établie
     */
    public boolean connect() {
        try {
            session = Session.getInstance(properties);
            session.setDebug(true);  // Activer le débogage au niveau session
            
            store = session.getStore("imaps");
            store.connect(host, email, password);
            Logger.info("Connexion établie au serveur mail");
            return true;
        } catch (MessagingException e) {
            Logger.error("Erreur lors de la connexion au serveur mail: " + e.getMessage());
            e.printStackTrace();  // Afficher la stack trace complète pour mieux diagnostiquer
            return false;
        }
    }
    
    /**
     * Récupère tous les messages de la boîte de réception
     * 
     * @return Liste des messages
     */
    public List<EmailMessage> getInboxMessages() {
        return getMessagesFromFolder("INBOX");
    }
    
    /**
     * Récupère les messages non lus de la boîte de réception
     * 
     * @return Liste des messages non lus
     */
    public List<EmailMessage> getUnreadMessages() {
        try {
            if (store == null || !store.isConnected()) {
                connect();
            }
            
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            
            // Recherche des messages non lus
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            List<EmailMessage> emailMessages = convertMessages(messages);
            
            inbox.close(false);
            return emailMessages;
        } catch (MessagingException e) {
            Logger.error("Erreur lors de la récupération des messages non lus: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Récupère les messages d'un dossier spécifique
     * 
     * @param folderName Nom du dossier
     * @return Liste des messages
     */
    public List<EmailMessage> getMessagesFromFolder(String folderName) {
        try {
            if (store == null || !store.isConnected()) {
                connect();
            }
            
            Folder folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            
            Message[] messages = folder.getMessages();
            List<EmailMessage> emailMessages = convertMessages(messages);
            
            folder.close(false);
            return emailMessages;
        } catch (MessagingException e) {
            Logger.error("Erreur lors de la récupération des messages du dossier " + folderName + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Convertit un tableau de messages en liste d'objets EmailMessage
     * 
     * @param messages Tableau de messages
     * @return Liste d'objets EmailMessage
     */
    private List<EmailMessage> convertMessages(Message[] messages) {
        List<EmailMessage> emailMessages = new ArrayList<>();
        try {
            for (Message message : messages) {
                String from = message.getFrom()[0].toString();
                String subject = message.getSubject();
                String content = extractContent(message);
                Date receivedDate = message.getReceivedDate();
                boolean hasAttachments = hasAttachments(message);
                boolean isEncrypted = detectEncryption(content);
                
                EmailMessage emailMessage = new EmailMessage(
                    message.getMessageNumber(),
                    from,
                    subject,
                    content,
                    receivedDate,
                    hasAttachments,
                    isEncrypted,
                    message
                );
                
                emailMessages.add(emailMessage);
            }
        } catch (Exception e) {
            Logger.error("Erreur lors de la conversion des messages: " + e.getMessage());
        }
        return emailMessages;
    }
    
    /**
     * Extrait le contenu texte d'un message
     * 
     * @param message Message à traiter
     * @return Contenu texte du message
     */
    private String extractContent(Message message) {
        try {
            Object content = message.getContent();
            if (content instanceof String) {
                return (String) content;
            } else if (content instanceof Multipart) {
                return extractTextFromMultipart((Multipart) content);
            }
            return "Contenu non lisible";
        } catch (Exception e) {
            Logger.error("Erreur lors de l'extraction du contenu: " + e.getMessage());
            return "Erreur lors de l'extraction du contenu";
        }
    }
    
    /**
     * Extrait le texte d'un contenu multipart
     * 
     * @param multipart Contenu multipart
     * @return Texte extrait
     */
    private String extractTextFromMultipart(Multipart multipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = multipart.getCount();
        
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.getContent() instanceof Multipart) {
                result.append(extractTextFromMultipart((Multipart) bodyPart.getContent()));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Détecte si un message contient des pièces jointes
     * 
     * @param message Message à analyser
     * @return true si le message contient des pièces jointes
     */
    private boolean hasAttachments(Message message) {
        try {
            if (message.getContent() instanceof Multipart) {
                Multipart multipart = (Multipart) message.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            Logger.error("Erreur lors de la vérification des pièces jointes: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Télécharge les pièces jointes d'un message
     * 
     * @param message Message contenant des pièces jointes
     * @param downloadDir Répertoire de destination
     * @return Liste des chemins des fichiers téléchargés
     */
    public List<String> downloadAttachments(Message message, String downloadDir) {
        List<String> downloadedFiles = new ArrayList<>();
        try {
            if (message.getContent() instanceof Multipart) {
                Multipart multipart = (Multipart) message.getContent();
                
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                        String fileName = bodyPart.getFileName();
                        String filePath = downloadDir + File.separator + fileName;
                        
                        // S'assurer que le répertoire existe
                        File dir = new File(downloadDir);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        
                        // Téléchargement de la pièce jointe
                        InputStream is = bodyPart.getInputStream();
                        Files.copy(is, Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
                        is.close();
                        
                        downloadedFiles.add(filePath);
                        Logger.info("Pièce jointe téléchargée: " + fileName);
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("Erreur lors du téléchargement des pièces jointes: " + e.getMessage());
        }
        return downloadedFiles;
    }
    
    /**
     * Détecte si un message est chiffré
     * 
     * @param content Contenu du message
     * @return true si le message semble être chiffré
     */
    private boolean detectEncryption(String content) {
        // Cette méthode peut être adaptée selon votre méthode de chiffrement
        return content.contains("-----BEGIN PGP MESSAGE-----") 
            || content.contains("ENCRYPTED") 
            || content.contains("CRYPTED");
    }
    
    /**
     * Déchiffre un message chiffré
     * 
     * @param encryptedContent Contenu chiffré
     * @return Contenu déchiffré
     */
    public String decryptMessage(String encryptedContent) {
        // Cette méthode doit être implémentée selon votre méthode de chiffrement
        // Exemple fictif
        Logger.info("Tentative de déchiffrement du message");
        return "Message déchiffré (à implémenter)";
    }
    
    /**
     * Ferme la connexion au serveur mail
     */
    public void disconnect() {
        try {
            if (store != null && store.isConnected()) {
                store.close();
                Logger.info("Déconnexion du serveur mail réussie");
            }
        } catch (MessagingException e) {
            Logger.error("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }
    
    /**
     * Classe représentant un email pour l'affichage
     */
    public static class EmailMessage {
        private int messageId;
        private String from;
        private String subject;
        private String content;
        private Date receivedDate;
        private boolean hasAttachments;
        private boolean isEncrypted;
        private Message originalMessage;
        
        public EmailMessage(int messageId, String from, String subject, String content,
                           Date receivedDate, boolean hasAttachments, boolean isEncrypted,
                           Message originalMessage) {
            this.messageId = messageId;
            this.from = from;
            this.subject = subject;
            this.content = content;
            this.receivedDate = receivedDate;
            this.hasAttachments = hasAttachments;
            this.isEncrypted = isEncrypted;
            this.originalMessage = originalMessage;
        }
        
        // Getters
        public int getMessageId() { return messageId; }
        public String getFrom() { return from; }
        public String getSubject() { return subject; }
        public String getContent() { return content; }
        public Date getReceivedDate() { return receivedDate; }
        public boolean hasAttachments() { return hasAttachments; }
        public boolean isEncrypted() { return isEncrypted; }
        public Message getOriginalMessage() { return originalMessage; }
        
        @Override
        public String toString() {
            return "De: " + from + "\nSujet: " + subject + 
                   "\nDate: " + receivedDate + 
                   (hasAttachments ? "\n[Avec pièces jointes]" : "") +
                   (isEncrypted ? "\n[Chiffré]" : "");
        }
    }
}
