package fr.insa.crypto.encryption;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaires pour les opérations de chiffrement
 */
public class EncryptionUtils {
    /**
     * Dérive une clé de taille fixe à partir des données fournies
     * @param input Les données d'entrée
     * @param keyLength La longueur de clé souhaitée en octets
     * @return La clé dérivée
     */
    public static byte[] deriveKey(byte[] input, int keyLength) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest(input);
            
            // Tronquer ou répéter le hash pour obtenir la longueur désirée
            if (hash.length == keyLength) {
                return hash;
            }
            
            byte[] result = new byte[keyLength];
            int copied = 0;
            while (copied < keyLength) {
                int toCopy = Math.min(hash.length, keyLength - copied);
                System.arraycopy(hash, 0, result, copied, toCopy);
                copied += toCopy;
                
                // Si besoin de plus d'octets, hacher à nouveau
                if (copied < keyLength) {
                    sha.update(hash);
                    hash = sha.digest();
                }
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithme de hachage non disponible", e);
        }
    }
    
    /**
     * Vérifie si une chaîne est une adresse email valide
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
