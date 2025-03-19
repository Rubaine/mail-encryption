package fr.insa.crypto.trustAuthority;

/**
 * Représente le statut d'un compte utilisateur dans le système d'autorité de confiance
 */
public class AccountStatus {
    private final boolean exists;
    private final boolean verified;

    /**
     * @param exists   Si le compte existe dans le système
     * @param verified Si le compte est vérifié (a complété le processus d'enregistrement)
     */
    public AccountStatus(boolean exists, boolean verified) {
        this.exists = exists;
        this.verified = verified;
    }

    /**
     * @return true si le compte existe
     */
    public boolean exists() {
        return exists;
    }

    /**
     * @return true si le compte est vérifié
     */
    public boolean isVerified() {
        return verified;
    }

    /**
     * @return true si le compte n'existe pas ou n'est pas vérifié
     */
    public boolean needsRegistration() {
        return !exists || !verified;
    }
}
