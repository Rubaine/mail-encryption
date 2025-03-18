package fr.insa.crypto.mail;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.insa.crypto.utils.Logger;

/**
 * Classe pour la gestion des pièces jointes
 */
public class AttachmentHandler {
    private Multipart multipart;
    private List<String> attachmentPaths;
    
    /**
     * Constructeur initialisant un gestionnaire de pièces jointes
     */
    public AttachmentHandler() {
        this.multipart = new MimeMultipart();
        this.attachmentPaths = new ArrayList<>();
    }
    
    /**
     * Ajouter le contenu textuel du message
     * @param messageBody Contenu du message
     * @throws MessagingException
     */
    public void setMessageBody(String messageBody) throws MessagingException {
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(messageBody);
        multipart.addBodyPart(messageBodyPart);
    }
    
    /**
     * Ajouter une pièce jointe au message
     * @param filePath Chemin vers le fichier à attacher
     * @throws MessagingException
     */
    public void addAttachment(String filePath) throws MessagingException {
        File file = new File(filePath);
        if (!file.exists()) {
            Logger.error("Le fichier " + filePath + " n'existe pas");
            return;
        }
        
        MimeBodyPart attachmentPart = new MimeBodyPart();
        DataSource source = new FileDataSource(filePath);
        attachmentPart.setDataHandler(new DataHandler(source));
        attachmentPart.setFileName(file.getName());
        multipart.addBodyPart(attachmentPart);
        attachmentPaths.add(filePath);
        
        Logger.info("Pièce jointe ajoutée: " + file.getName());
    }
    
    /**
     * Obtenir le multipart contenant le message et les pièces jointes
     * @return Multipart pour l'email
     */
    public Multipart getMultipart() {
        return multipart;
    }
    
    /**
     * Vérifier si des pièces jointes ont été ajoutées
     * @return true si des pièces jointes ont été ajoutées
     */
    public boolean hasAttachments() {
        return !attachmentPaths.isEmpty();
    }
    
    /**
     * Obtenir le nombre de pièces jointes
     * @return nombre de pièces jointes
     */
    public int getAttachmentCount() {
        return attachmentPaths.size();
    }
    
    /**
     * Effacer toutes les pièces jointes
     * @throws MessagingException
     */
    public void clearAttachments() throws MessagingException {
        this.multipart = new MimeMultipart();
        this.attachmentPaths.clear();
    }
}
