package fr.insa.crypto.mail;

import java.util.Properties;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import fr.insa.crypto.trustAuthority.KeyPair;
import fr.insa.crypto.trustAuthority.SettingParameters;
import fr.insa.crypto.trustAuthority.TrustAuthorityClient;
import fr.insa.crypto.encryption.IdentityBasedEncryption;
import fr.insa.crypto.utils.Config;
import fr.insa.crypto.utils.Logger;

/**
 * Gère l'authentification email et la récupération des clés cryptographiques
 */
public class Authentication {
    private final String email;
    private final String password;
    private Session session;
    
    // Paramètres pour la cryptographie IBE
    private TrustAuthorityClient trustClient;
    private KeyPair userKeyPair;
    private IdentityBasedEncryption ibeEngine;

    /**
     * Constructeur qui initialise l'authentification email et cryptographique
     * @param email L'adresse email de l'utilisateur
     * @param password Le mot de passe ou clé d'application
     * @throws Exception En cas d'erreur d'authentification
     */
    public Authentication(String email, String password) throws Exception {
        this.email = email;
        this.password = password;
        
        // Validation de l'adresse email
        if (!Config.isValidEmail(email)) {
            throw new IllegalArgumentException("Format d'adresse email invalide: " + email);
        }
        
        // 1. Authentifier l'utilisateur pour l'email
        authenticateEmail();
        
        // 2. Se connecter à l'autorité de confiance
        try {
            trustClient = new TrustAuthorityClient(Config.TRUST_AUTHORITY_URL);
            
            // Note: La récupération des clés est reportée jusqu'à ce que l'authentification 2FA soit terminée
            // userKeyPair et ibeEngine seront initialisés après la validation 2FA
            
            Logger.info("Connexion à l'autorité de confiance réussie, en attente d'authentification 2FA");
        } catch (Exception e) {
            Logger.error("Erreur lors de la connexion à l'autorité de confiance: " + e.getMessage());
            throw new Exception("Impossible de se connecter à l'autorité de confiance: " + e.getMessage());
        }
    }

    /**
     * Finalise l'authentification cryptographique avec un code TOTP
     * @param totpCode Le code TOTP pour l'authentification à deux facteurs
     * @throws Exception En cas d'erreur d'authentification
     */
    public void completeAuthentication(String totpCode) throws Exception {
        try {
            userKeyPair = trustClient.requestPrivateKey(email, totpCode);
            ibeEngine = new IdentityBasedEncryption(trustClient.getParameters());
            Logger.info("Authentification cryptographique réussie pour " + email);
        } catch (Exception e) {
            Logger.error("Erreur lors de l'authentification cryptographique: " + e.getMessage());
            throw new Exception("Impossible de récupérer les clés cryptographiques: " + e.getMessage());
        }
    }
    
    /**
     * Authentification auprès du serveur email
     */
    private void authenticateEmail() throws Exception {
        Properties props = new Properties();
        
        // Configuration pour Gmail
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.user", email);
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        // Ajout de l'adresse email comme propriété mail.smtp.user
        props.put("mail.smtp.user", email);
        props.put("mail.smtp.username", email);

        // Increase timeouts from 10 to 30 seconds
        props.put("mail.smtp.connectiontimeout", "30000");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.writetimeout", "30000");

        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        
        try {
            session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(email, password);
                }
            });
            
            Logger.info("Session email créée pour " + email);
        } catch (Exception e) {
            Logger.error("Erreur lors de l'authentification email: " + e.getMessage());
            throw new Exception("Échec de l'authentification email: " + e.getMessage());
        }
    }
    
    /**
     * @return La session email authentifiée
     */
    public Session getAuthenticatedSession() {
        return session;
    }
    
    /**
     * @return Le client de l'autorité de confiance
     */
    public TrustAuthorityClient getTrustClient() {
        return trustClient;
    }
    
    /**
     * @return La paire de clés de l'utilisateur
     */
    public KeyPair getUserKeyPair() {
        return userKeyPair;
    }
    
    /**
     * @return Le moteur de chiffrement IBE
     */
    public IdentityBasedEncryption getIbeEngine() {
        return ibeEngine;
    }
    
    /**
     * @return L'adresse email authentifiée
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * Déconnexion et nettoyage des ressources
     */
    public void logout() {
        session = null;
        userKeyPair = null;
        Logger.info("Déconnexion effectuée pour " + email);
    }
}
