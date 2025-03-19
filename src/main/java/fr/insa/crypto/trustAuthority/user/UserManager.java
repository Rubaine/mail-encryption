package fr.insa.crypto.trustAuthority.user;

import fr.insa.crypto.utils.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des comptes utilisateurs pour l'autorité de confiance
 */
public class UserManager {
    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();

    /**
     * Vérifie si l'utilisateur est déjà enregistré
     *
     * @param email Adresse email de l'utilisateur
     * @return true si l'utilisateur existe
     */
    public boolean isUserRegistered(String email) {
        return users.containsKey(email.toLowerCase());
    }

    /**
     * Vérifie si l'utilisateur est vérifié (a complété le processus d'enregistrement)
     *
     * @param email Adresse email de l'utilisateur
     * @return true si l'utilisateur est vérifié
     */
    public boolean isUserVerified(String email) {
        UserAccount account = users.get(email.toLowerCase());
        return account != null && account.isVerified();
    }

    /**
     * Crée un compte utilisateur non vérifié
     *
     * @param email Adresse email de l'utilisateur
     * @return Le compte utilisateur créé ou existant
     */
    public UserAccount createOrGetUser(String email) {
        String lowerEmail = email.toLowerCase();
        return users.computeIfAbsent(lowerEmail, e -> {
            Logger.info("Création d'un nouveau compte pour: " + email);
            return new UserAccount(email);
        });
    }

    /**
     * Récupère un compte utilisateur existant
     *
     * @param email Adresse email de l'utilisateur
     * @return Le compte utilisateur ou null s'il n'existe pas
     */
    public UserAccount getUser(String email) {
        return users.get(email.toLowerCase());
    }

    /**
     * Marque un utilisateur comme vérifié et définit son secret TOTP
     *
     * @param email      Adresse email de l'utilisateur
     * @param totpSecret Secret TOTP pour Google Authenticator
     * @return true si l'opération a réussi
     */
    public boolean verifyUserAndSetTotpSecret(String email, String totpSecret) {
        UserAccount account = users.get(email.toLowerCase());
        if (account == null) {
            return false;
        }

        account.setTotpSecret(totpSecret);
        account.setVerified();
        Logger.info("Utilisateur vérifié avec succès: " + email);
        return true;
    }
}
