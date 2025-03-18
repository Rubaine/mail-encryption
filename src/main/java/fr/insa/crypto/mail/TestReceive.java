package fr.insa.crypto.mail;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

public class TestReceive {

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put("mail.pop3s.host", "pop.gmail.com");
        properties.put("mail.pop3s.port", "995");
        properties.put("mail.pop3s.ssl.enable", "true");
        properties.put("mail.pop3s.ssl.protocols", "TLSv1.2");
        // Forcer la confiance pour le serveur Gmail
        properties.put("mail.pop3s.ssl.trust", "pop.gmail.com");


        Session emSession = Session.getDefaultInstance(properties);

        try{
            Store store = emSession.getStore("pop3s");
            store.connect("pop.gmail.com", "vieira.ruben.sp@gmail.com", "sdus tegs qvum tvoa");

            Folder emaiFolder = store.getFolder("INBOX");
            emaiFolder.open(Folder.READ_ONLY);

            Message[] messages = emaiFolder.getMessages();
            for (int i = 0; i < messages.length; i++) {
                Message message = messages[i];
                System.out.println("Email Number " + (i + 1));
                System.out.println("Subject: " + message.getSubject());
                System.out.println("From: " + message.getFrom()[0]);
                System.out.println("Text: " + message.getContent().toString());
            }
            emaiFolder.close(false);
            store.close();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}