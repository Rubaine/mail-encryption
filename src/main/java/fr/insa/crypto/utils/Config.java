package fr.insa.crypto.utils;

/**
 * Configuration de l'application
 */
public class Config {
    // Paramètres de configuration pour le serveur d'autorité de confiance
    public static final String TRUST_AUTHORITY_URL = "http://localhost:8080";
    public static final int TRUST_AUTHORITY_PORT = 8080;
    
    // Paramètres email
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final String SMTP_PORT = "587";
    public static final String IMAP_HOST = "imap.gmail.com";
    public static final String IMAP_PORT = "993";
    public static final String POP3_HOST = "pop.gmail.com";
    public static final String POP3_PORT = "995";
    
    // Paramètres de validation
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    
    // Paramètres de chiffrement
    public static final String PAIRING_PARAMETERS_PATH = "params/curves/a.properties";
    
    // Mode débogage
    public static boolean DEBUG_MODE = false;
    
    /**
     * Valide une adresse email selon le format standard
     * @param email Adresse email à valider
     * @return true si l'email est valide, false sinon
     */
    public static boolean isValidEmail(String email) {
        return email != null && !email.trim().isEmpty() && email.matches(EMAIL_REGEX);
    }
}
