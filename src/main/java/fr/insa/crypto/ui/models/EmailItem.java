package fr.insa.crypto.ui.models;

import javax.mail.Message;
import java.util.Date;

/**
 * Represents an email item for display in the email list
 */
public class EmailItem {
    private Message originalMessage;
    private String sender;
    private String subject;
    private String preview;
    private Date date;
    private String timeText;
    private boolean isUnread;

    public EmailItem(Message originalMessage, String sender, String subject, String preview, 
                     Date date, String timeText, boolean isUnread) {
        this.originalMessage = originalMessage;
        this.sender = sender;
        this.subject = subject;
        this.preview = preview;
        this.date = date;
        this.timeText = timeText;
        this.isUnread = isUnread;
    }

    public Message getOriginalMessage() {
        return originalMessage;
    }

    public String getSender() {
        return sender;
    }

    public String getSubject() {
        return subject;
    }

    public String getPreview() {
        return preview;
    }

    public Date getDate() {
        return date;
    }

    public String getTimeText() {
        return timeText;
    }

    public boolean isUnread() {
        return isUnread;
    }

    @Override
    public String toString() {
        return sender + " - " + subject;
    }
}
