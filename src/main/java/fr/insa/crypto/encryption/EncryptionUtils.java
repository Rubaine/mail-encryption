package fr.insa.crypto.encryption;

import it.unisa.dia.gas.jpbc.Element;

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
    
    /**
     * Encode un élément JPBC en Base64
     * @param element L'élément à encoder
     * @return L'élément encodé en Base64
     */
    public static String encodeElement(Element element) {
        if (element == null) {
            return null;
        }
        return java.util.Base64.getEncoder().encodeToString(element.toBytes());
    }
    
    /**
     * Opération XOR entre deux tableaux d'octets
     * @param a Premier tableau d'octets
     * @param b Second tableau d'octets
     * @return Le résultat de l'opération XOR
     */
    public static byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[Math.min(a.length, b.length)];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte)((int)a[i] ^ (int)b[i]);
        }
        return result;
    }
}
