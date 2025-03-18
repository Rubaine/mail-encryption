package fr.insa.crypto.mail;

import javax.mail.*;

import fr.insa.crypto.utils.Logger;

import java.util.Properties;

/**
 * The Authentication class is responsible for configuring and providing an
 * authenticated
 * email session using the provided email and application key. It supports both
 * SSL and TLS
 * configurations for connecting to the SMTP server.
 */
public class Authentication {
    private String email;
    private String appKey;
    private Properties properties;

    public Authentication(String email, String appKey) {
        this(email, appKey, false);
    }

    /**
     * Constructs an Authentication object with the specified email, appKey, and SSL
     * usage.
     *
     * @param email  the email address to be used for authentication
     * @param appKey the application key or password for the email account
     * @param useSSL a boolean indicating whether to use SSL for the connection
     *
     *               This constructor initializes the SMTP properties based on the
     *               useSSL parameter.
     *               If useSSL is true, it configures the properties for SSL
     *               connection on port 465.
     *               Otherwise, it configures the properties for TLS connection on
     *               port 587.
     *               Additionally, it sets the connection timeout and read timeout
     *               to 10 seconds.
     *
     *               The configuration details are logged using the Logger.
     */
    public Authentication(String email, String appKey, boolean useSSL) {
        this.email = email;
        this.appKey = appKey;
        this.properties = new Properties();

        if (useSSL) {
            properties.put("mail.smtp.host", "smtp.gmail.com");
            properties.put("mail.smtp.socketFactory.port", "465");
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.port", "465");
            properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        } else {
            properties.put("mail.smtp.host", "smtp.gmail.com");
            properties.put("mail.smtp.port", "587");
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
        }

        // Ajout de l'adresse email comme propriété mail.smtp.user
        properties.put("mail.smtp.user", email);
        properties.put("mail.smtp.username", "Ruben");

        // Increase timeouts from 10 to 30 seconds
        properties.put("mail.smtp.connectiontimeout", "30000");
        properties.put("mail.smtp.timeout", "30000");
        properties.put("mail.smtp.writetimeout", "30000");

        Logger.info("Authentication configured for " + email + " with host " + properties.getProperty("mail.smtp.host")
                + ":" + properties.getProperty("mail.smtp.port") +
                (useSSL ? " using SSL" : " using TLS"));
    }

    /**
     * Creates and returns an authenticated email session using the provided email
     * and app key.
     * 
     * @return a Session object that is authenticated with the provided credentials.
     */
    public Session getAuthenticatedSession() {
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, appKey);
            }
        };

        return Session.getInstance(properties, auth);
    }

    /**
     * Logs out from the email session by clearing stored credentials.
     */
    public void logout() {
        email = null;
        appKey = null;
        Logger.info("Logged out successfully.");
    }

    /**
     * Retrieves the email address associated with this authentication.
     *
     * @return the email address as a String
     */
    public String getEmail() {
        return email;
    }

    /**
     * Retrieves the properties for the authentication.
     *
     * @return the properties used for authentication
     */
    public Properties getProperties() {
        return properties;
    }
}
