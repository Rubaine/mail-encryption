package fr.insa.crypto.mail;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.owasp.encoder.Encode;

import javax.mail.Address;

/**
 * Mapper pour convertir un javax.mail.Message en objet Mail.
 */
public final class MailMapper {

    private MailMapper() {}

    public static Mail map(Message message, String user) {
        String subject = Encode.forHtml(getSubject(message));
        String content = Encode.forHtml(getContent(message));
        String from = Encode.forHtml(getFrom(message));
        String recipient = Encode.forHtml(getRecipient(message, user));
        return new Mail(subject, content, from, recipient);
    }

    private static String getSubject(Message message) {
        try {
            String subject = message.getSubject();
            return subject != null ? subject : "";
        } catch (MessagingException e) {
            return "";
        }
    }

    private static String getContent(Message message) {
        try {
            Object content = message.getContent();
            if (content instanceof String) {
                return (String) content;
            }
            // Pour simplifier, on retourne une chaîne vide pour les contenus non textuels.
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String getFrom(Message message) {
        try {
            Address[] froms = message.getFrom();
            if (froms != null && froms.length > 0) {
                return froms[0].toString();
            }
            return "";
        } catch (MessagingException e) {
            return "";
        }
    }

    private static String getRecipient(Message message, String user) {
        try {
            Address[] recipients = message.getRecipients(Message.RecipientType.TO);
            if (recipients != null && recipients.length > 0) {
                return recipients[0].toString();
            }
            return user;
        } catch (MessagingException e) {
            return user;
        }
    }
}
