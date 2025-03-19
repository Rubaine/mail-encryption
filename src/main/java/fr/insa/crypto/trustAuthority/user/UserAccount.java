package fr.insa.crypto.trustAuthority.user;

import java.time.Instant;

/**
 * Représente un compte utilisateur dans le système d'autorité de confiance
 */
public class UserAccount {
    private final String email;
    private String totpSecret;
    private boolean isVerified;
    private String pendingOtp;
    private long otpExpirationTime;

    /**
     * Crée un nouveau compte utilisateur
     *
     * @param email L'adresse email de l'utilisateur
     */
    public UserAccount(String email) {
        this.email = email;
        this.isVerified = false;
    }

    /**
     * @return L'adresse email de l'utilisateur
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return Le secret TOTP pour Google Authenticator
     */
    public String getTotpSecret() {
        return totpSecret;
    }

    /**
     * Définit le secret TOTP
     *
     * @param totpSecret Nouveau secret
     */
    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    /**
     * @return true si le compte est vérifié
     */
    public boolean isVerified() {
        return isVerified;
    }

    /**
     * Marque le compte comme vérifié
     */
    public void setVerified() {
        this.isVerified = true;
    }

    /**
     * Stocke un OTP en attente pour ce compte
     *
     * @param otp               Code OTP
     * @param expirationSeconds Délai d'expiration en secondes
     */
    public void storePendingOtp(String otp, int expirationSeconds) {
        this.pendingOtp = otp;
        this.otpExpirationTime = Instant.now().getEpochSecond() + expirationSeconds;
    }

    /**
     * Vérifie si l'OTP fourni correspond à l'OTP en attente et n'est pas expiré
     *
     * @param otp Code OTP à vérifier
     * @return true si l'OTP est valide
     */
    public boolean validateOtp(String otp) {
        if (pendingOtp == null || otp == null) {
            return false;
        }

        // Vérifier expiration
        if (Instant.now().getEpochSecond() > otpExpirationTime) {
            pendingOtp = null;
            return false;
        }

        boolean isValid = pendingOtp.equals(otp);
        if (isValid) {
            pendingOtp = null; // Utilisation unique
        }
        return isValid;
    }
}
