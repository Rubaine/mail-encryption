package fr.insa.crypto.mail;

import javax.mail.Session;
import javax.mail.Transport;

import java.util.Date;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import fr.insa.crypto.utils.Logger;

/**
 * Classe pour l'envoi des emails chiffrés
 */
public class MailSender {
    public static void sendEmail(Session session, String toEmail, String subject, String body) {
        try {
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
            e.printStackTrace();
        }
    }
}
