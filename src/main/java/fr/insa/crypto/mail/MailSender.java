package fr.insa.crypto.mail;

import javax.mail.Session;
import javax.mail.Transport;

import java.util.Date;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import fr.insa.crypto.utils.Config;
import fr.insa.crypto.utils.Logger;

/**
 * Classe pour l'envoi des emails chiffrés
 */
public class MailSender {
    public static void sendEmail(Session session, String toEmail, String subject, String body) {
        try {
            // Valider l'adresse email du destinataire
            if (toEmail == null || toEmail.trim().isEmpty()) {
                Logger.error("L'adresse email du destinataire est null ou vide");
                throw new IllegalArgumentException("L'adresse email du destinataire ne peut pas être null ou vide");
            }
            
            // Vérifier le format de l'adresse email
            if (!Config.isValidEmail(toEmail)) {
                Logger.error("Format d'adresse email invalide: " + toEmail);
                throw new IllegalArgumentException("Format d'adresse email invalide: " + toEmail);
            }
            
            MimeMessage msg = new MimeMessage(session);
            // set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");

            msg.setFrom(new InternetAddress(session.getProperty("mail.smtp.user"),
                    session.getProperty("mail.smtp.username")));

            msg.setReplyTo(InternetAddress.parse(session.getProperty("mail.smtp.user"), false));

            msg.setSubject(subject, "UTF-8");

            msg.setText(body, "UTF-8");

            msg.setSentDate(new Date());

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            Logger.info("Message is ready");
            Transport.send(msg);

            Logger.info("EMail Sent Successfully!!");
        } catch (Exception e) {
            Logger.error("Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Envoyer un email avec des pièces jointes
     * @param session Session mail
     * @param toEmail Destinataire
     * @param subject Sujet
     * @param body Contenu du message
     * @param attachmentHandler Gestionnaire des pièces jointes
     */
    public static void sendEmailWithAttachments(Session session, String toEmail, String subject, 
                                              String body, AttachmentHandler attachmentHandler) {
        try {
            // Valider l'adresse email du destinataire
            if (toEmail == null || toEmail.trim().isEmpty()) {
                Logger.error("L'adresse email du destinataire est null ou vide");
                throw new IllegalArgumentException("L'adresse email du destinataire ne peut pas être null ou vide");
            }
            
            // Vérifier le format de l'adresse email
            if (!Config.isValidEmail(toEmail)) {
                Logger.error("Format d'adresse email invalide: " + toEmail);
                throw new IllegalArgumentException("Format d'adresse email invalide: " + toEmail);
            }
            
            MimeMessage msg = new MimeMessage(session);
            // set message headers
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");

            msg.setFrom(new InternetAddress(session.getProperty("mail.smtp.user"),
                    session.getProperty("mail.smtp.username")));

            System.out.println("user : " + session.getProperty("mail.smtp.user"));

            msg.setReplyTo(InternetAddress.parse(session.getProperty("mail.smtp.user"), false));

            msg.setSubject(subject, "UTF-8");
            
            // Configurer le contenu du message avec les pièces jointes
            attachmentHandler.setMessageBody(body);
            msg.setContent(attachmentHandler.getMultipart());
            
            msg.setSentDate(new Date());

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            
            Logger.info("Message avec " + attachmentHandler.getAttachmentCount() + 
                      " pièce(s) jointe(s) est prêt");
            
            Transport.send(msg);

            Logger.info("EMail avec pièces jointes envoyé avec succès!");
        } catch (Exception e) {
            Logger.error("Erreur lors de l'envoi de l'email avec pièces jointes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
