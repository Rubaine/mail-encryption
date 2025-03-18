package fr.insa.crypto.mail;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Flags;

/**
 * Client pour la réception des mails.
 */
public class MailReceiver2 {
    private final String protocol;
    private final String host;
    private final String port;
    private final String user;
    private final String password;

    public MailReceiver2(String protocol, String host, String port, String user, String password) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    /**
     * Récupère les mails non lus de la boîte de réception.
     * @return une liste d'objets Mail
     */
    public List<Mail> receive() {
        Store emailStore = null;
        Folder emailFolder = null;
        List<Mail> mails = new ArrayList<>();
        Properties properties = new Properties();
        properties.put("mail.store.protocol", protocol);
        properties.put("mail." + protocol + ".host", host);
        properties.put("mail." + protocol + ".port", port);
        properties.put("mail.imaps.ssl.enable", "true");
        properties.put("mail.imaps.ssl.protocols", "TLSv1.2");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
        properties.put("mail.imap.starttls.enable", "true");
        Session emailSession = Session.getInstance(properties);

        try {
            emailStore = emailSession.getStore();
            emailStore.connect(user, password);
            emailFolder = emailStore.getFolder("INBOX");
            emailFolder.open(Folder.READ_WRITE);
            mails = getNewMails(emailFolder);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de la réception des mails", e);
        } finally {
            try {
                if (emailFolder != null && emailFolder.isOpen()) {
                    emailFolder.close(false);
                }
                if (emailStore != null && emailStore.isConnected()) {
                    emailStore.close();
                }
            } catch (MessagingException e) {
                throw new RuntimeException("Erreur lors de la fermeture des connexions", e);
            }
        }
        return mails;
    }

    /**
     * Parcourt les messages du dossier et retourne ceux non lus.
     */
    private List<Mail> getNewMails(Folder emailFolder) throws MessagingException {
        List<Mail> mails = new ArrayList<>();
        for (Message message : emailFolder.getMessages()) {
            if (!message.getFlags().contains(Flags.Flag.SEEN)) {
                message.setFlags(new Flags(Flags.Flag.SEEN), true);
                Mail mail = MailMapper.map(message, user);
                mails.add(mail);
            }
        }
        return mails;
    }
}
