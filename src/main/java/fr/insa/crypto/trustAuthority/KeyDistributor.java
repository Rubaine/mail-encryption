package fr.insa.crypto.trustAuthority;

import it.unisa.dia.gas.jpbc.Element;

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
     * @return La clé privée encodée en Base64
     */
    public String distributePrivateKey(String email) {
        // Vérification de l'adresse email
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Format d'adresse email invalide");
        }
        
        // Génération de la clé privée
        Element privateKey = trustAuthority.generatePrivateKey(email);
        
        // Conversion de la clé en format transmissible (Base64)
        return Base64.getEncoder().encodeToString(privateKey.toBytes());
    }
    
    /**
     * Vérifie si l'adresse email a un format valide
     */
    private boolean isValidEmail(String email) {
        // Vérification basique du format email
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
