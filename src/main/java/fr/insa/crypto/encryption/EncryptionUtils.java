package fr.insa.crypto.encryption;

import it.unisa.dia.gas.jpbc.Element;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaires pour les opérations de chiffrement
 */
public class EncryptionUtils {
    
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
