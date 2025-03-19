package fr.insa.crypto.utils;

import fr.insa.crypto.mail.Authentication;

/**
 * Gestionnaire global de session pour conserver l'état d'authentification
 * entre les différentes vues de l'application
 */
public class SessionManager {
    
    // Instance unique (singleton pattern)
    private static SessionManager instance;
    
    // Informations de session
    private String email;
    private String password;
    private Authentication authSession;
    
    // Constructeur privé pour empêcher l'instanciation directe
    private SessionManager() { }
    
    /**
     * Obtient l'instance unique du gestionnaire de session
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Stocke les identifiants de connexion
     */
    public void storeCredentials(String email, String password) {
        Logger.info("SessionManager: Identifiants stockés pour " + email);
        this.email = email;
        this.password = password;
    }
    
    /**
     * Stocke la session d'authentification
     */
    public void storeAuthSession(Authentication authSession) {
        Logger.info("SessionManager: Session d'authentification stockée");
        this.authSession = authSession;
    }
    
    /**
     * Récupère l'email stocké
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * Récupère le mot de passe stocké
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * Récupère la session d'authentification
     */
    public Authentication getAuthSession() {
        return authSession;
    }
    
    /**
     * Vérifie si des identifiants sont disponibles pour l'email spécifié
     */
    public boolean hasCredentialsFor(String email) {
        return this.email != null && this.password != null && this.email.equals(email);
    }
    
    /**
     * Vérifie si une session d'authentification est disponible
     */
    public boolean hasAuthSession() {
        return authSession != null;
    }
    
    /**
     * Efface toutes les informations de session
     */
    public void clearSession() {
        Logger.info("SessionManager: Session effacée");
        this.email = null;
        this.password = null;
        this.authSession = null;
    }
}
