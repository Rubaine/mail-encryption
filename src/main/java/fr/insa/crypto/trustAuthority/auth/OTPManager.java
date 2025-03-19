package fr.insa.crypto.trustAuthority.auth;

import java.security.SecureRandom;

/**
 * Gestionnaire des codes OTP (One-Time Password)
 */
public class OTPManager {
    private static final String OTP_CHARS = "0123456789";
    private static final int DEFAULT_OTP_LENGTH = 6;
    private static final int DEFAULT_EXPIRATION_SECONDS = 300; // 5 minutes
    
    private final SecureRandom random = new SecureRandom();
    
    /**
     * Génère un code OTP aléatoire
     * @return Code OTP
     */
    public String generateOtp() {
        return generateOtp(DEFAULT_OTP_LENGTH);
    }
    
    /**
     * Génère un code OTP aléatoire de longueur spécifiée
     * @param length Longueur du code
     * @return Code OTP
     */
    public String generateOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(OTP_CHARS.charAt(random.nextInt(OTP_CHARS.length())));
        }
        return sb.toString();
    }
    
    /**
     * @return Délai d'expiration par défaut en secondes
     */
    public int getDefaultExpirationSeconds() {
        return DEFAULT_EXPIRATION_SECONDS;
    }
}
