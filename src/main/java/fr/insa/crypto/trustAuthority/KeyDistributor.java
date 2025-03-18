package fr.insa.crypto.trustAuthority;

import java.util.Base64;

/**
 * Distributeur de clés privées aux utilisateurs
 */
public class KeyDistributor {
    private final TrustAuthority trustAuthority;
    
    public KeyDistributor(TrustAuthority trustAuthority) {
        this.trustAuthority = trustAuthority;
    }
    
    /**
     * Distribue une clé privée basée sur l'adresse email
     * @param email L'adresse email servant d'identité
     * @return La paire de clés contenant l'identité et la clé privée
     */
    public KeyPair distributePrivateKey(String email) {
        // Vérification de l'adresse email
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Format d'adresse email invalide");
        }
        
        // Génération de la clé privée
        return trustAuthority.generatePrivateKey(email);
    }
    
    /**
     * Vérifie si l'adresse email a un format valide
     */
    private boolean isValidEmail(String email) {
        // Vérification basique du format email
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
