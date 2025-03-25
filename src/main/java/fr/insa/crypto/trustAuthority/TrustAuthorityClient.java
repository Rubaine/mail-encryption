package fr.insa.crypto.trustAuthority;

import fr.insa.crypto.encryption.IdentityBasedEncryption;
import fr.insa.crypto.utils.Logger;
import fr.insa.crypto.utils.SecureChannelManager;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Client pour interagir avec le serveur de l'autorité de confiance
 */
public class TrustAuthorityClient {
    private final String serverUrl;
    private final SettingParameters parameters;
    private String totpSecret;
    private String currentEmail;
    private final SecureChannelManager secureChannel;
    private final IdentityBasedEncryption ibeEngine;
    private boolean secureChannelEstablished = false;
    
    // Constante pour l'identité du serveur - mise à jour pour utiliser un format d'email valide
    private static final String SERVER_IDENTITY = "server@trust.authority";
    
    // Header pour l'ID de session
    private static final String SESSION_ID_HEADER = "X-Session-ID";
    
    // ID de session pour le canal sécurisé
    private String sessionId;

    /**
     * Constructeur qui récupère les paramètres publics du serveur
     *
     * @param serverUrl L'URL du serveur de l'autorité de confiance
     */
    public TrustAuthorityClient(String serverUrl) throws IOException {
        this.serverUrl = serverUrl;
        this.parameters = fetchParameters();
        this.secureChannel = new SecureChannelManager();
        this.ibeEngine = new IdentityBasedEncryption(parameters);
    }

    /**
     * Récupère les paramètres publics depuis le serveur de manière sécurisée
     * Note: Cette méthode est spéciale car elle est appelée avant que le canal sécurisé ne soit initialisé
     */
    private SettingParameters fetchParameters() throws IOException {
        URL url = new URL(serverUrl + "/public-parameters");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to fetch parameters: HTTP error code " + responseCode);
        }

        String response = readResponse(connection);
        connection.disconnect();

        return new SettingParametersClient(response);
    }

    /**
     * Vérifie si l'utilisateur a un compte sur le serveur d'autorité
     *
     * @param email Adresse email de l'utilisateur
     * @return Un objet contenant les informations sur l'existence et la vérification du compte
     */
    public AccountStatus checkAccountStatus(String email) throws IOException {
        try {
            // Établir un canal sécurisé si nécessaire
            ensureSecureChannel();
            
            // Création des données JSON pour la requête
            JSONObject jsonInput = new JSONObject();
            jsonInput.put("email", email);
            
            // Envoyer la requête sécurisée
            String response = sendSecureRequest("/auth/check-account", "POST", jsonInput.toString());
            
            // Analyse de la réponse JSON
            JSONObject jsonResponse = new JSONObject(response);
            boolean exists = jsonResponse.getBoolean("exists");
            boolean verified = jsonResponse.getBoolean("verified");
            
            return new AccountStatus(exists, verified);
        } catch (Exception e) {
            Logger.error("Erreur lors de la vérification du compte: " + e.getMessage());
            throw new IOException("Failed to check account status: " + e.getMessage());
        }
    }

    /**
     * Demande l'envoi d'un code OTP à l'adresse email pour commencer l'enregistrement
     *
     * @param email Adresse email de l'utilisateur
     * @return true si l'OTP a été envoyé avec succès
     */
    public boolean requestRegistration(String email) throws IOException {
        try {
            // Établir un canal sécurisé si nécessaire
            ensureSecureChannel();
            
            // Création des données JSON pour la requête
            JSONObject jsonInput = new JSONObject();
            jsonInput.put("email", email);
            
            // Envoyer la requête sécurisée
            String response = sendSecureRequest("/auth/register", "POST", jsonInput.toString());
            
            // Vérifier la réponse
            return response.contains("OTP sent successfully");
        } catch (Exception e) {
            Logger.error("Erreur lors de la demande d'enregistrement: " + e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie l'OTP et configure le TOTP (Google Authenticator)
     *
     * @param email Adresse email de l'utilisateur
     * @param otp   Code OTP reçu par email
     * @return Le QR code à scanner avec Google Authenticator, ou null en cas d'échec
     */
    public String verifyOtpAndSetupTOTP(String email, String otp) throws IOException {
        try {
            // Établir un canal sécurisé si nécessaire
            ensureSecureChannel();
            
            // Préparer les données JSON
            JSONObject jsonInput = new JSONObject();
            jsonInput.put("email", email);
            jsonInput.put("otp", otp);
            
            // Envoyer la requête sécurisée
            String response = sendSecureRequest("/auth/verify-otp", "POST", jsonInput.toString());
            
            // Analyse de la réponse JSON
            JSONObject jsonResponse = new JSONObject(response);
            if ("success".equals(jsonResponse.getString("status"))) {
                this.totpSecret = jsonResponse.getString("totpSecret");
                this.currentEmail = email;
                return jsonResponse.getString("qrCodeUri");
            } else {
                return null;
            }
        } catch (Exception e) {
            Logger.error("Erreur lors de la vérification OTP: " + e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie si le code TOTP est valide
     *
     * @param email    Adresse email de l'utilisateur
     * @param totpCode Code TOTP généré par Google Authenticator
     * @return true si le code est valide
     */
    public boolean verifyTOTP(String email, String totpCode) throws IOException {
        try {
            // Établir un canal sécurisé si nécessaire
            ensureSecureChannel();
            
            // Préparer les données JSON
            JSONObject jsonInput = new JSONObject();
            jsonInput.put("email", email);
            jsonInput.put("totp", totpCode);
            
            // Envoyer la requête sécurisée
            String response = sendSecureRequest("/auth/verify-totp", "POST", jsonInput.toString());
            
            // Analyse de la réponse JSON
            JSONObject jsonResponse = new JSONObject(response);
            return jsonResponse.getBoolean("authenticated");
        } catch (Exception e) {
            Logger.error("Erreur lors de la vérification TOTP: " + e.getMessage());
            return false;
        }
    }

    /**
     * Demande une clé privée au serveur pour l'identité spécifiée (avec authentification TOTP)
     */
    public KeyPair requestPrivateKey(String identity, String totpCode) throws IOException {
        Logger.debug("requestPrivateKey appelé pour " + identity);
        
        try {
            // Établir un canal sécurisé si nécessaire
            if (!secureChannelEstablished) {
                establishSecureChannel();
            }
            
            // Préparer les données JSON
            JSONObject jsonInput = new JSONObject();
            jsonInput.put("email", identity);
            jsonInput.put("totpCode", totpCode);
            String jsonInputString = jsonInput.toString();
            
            // Envoyer la requête sécurisée
            String response = sendSecureRequest("/get-private-key", "POST", jsonInputString);
            Logger.debug("Réponse JSON reçue du serveur (longueur: " + response.length() + ")");
            
            try {
                // Analyse de la réponse JSON
                JSONObject jsonResponse = new JSONObject(response);
                
                // Vérifier que la réponse contient les champs attendus
                if (!jsonResponse.has("identity") || !jsonResponse.has("privateKey")) {
                    throw new IOException("Invalid server response, missing required fields");
                }
                
                // Récupération de l'identité et de la clé privée
                String identityFromServer = jsonResponse.getString("identity");
                String privateKeyBase64 = jsonResponse.getString("privateKey");
                
                byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
                
                // Recréer l'élément JPBC pour la clé privée
                Element privateKey = parameters.getPairing().getG1().newElementFromBytes(privateKeyBytes);
                
                return new KeyPair(identityFromServer, privateKey);
            } catch (Exception e) {
                Logger.error("Exception lors du traitement de la réponse JSON: " + e.getMessage());
                throw new IOException("Failed to parse server response: " + e.getMessage());
            }
        } catch (Exception e) {
            Logger.error("Erreur lors de la demande de clé privée: " + e.getMessage());
            throw new IOException("Failed to request private key: " + e.getMessage());
        }
    }

    /**
     * Demande une clé privée au serveur (méthode originale, conservée pour compatibilité)
     */
    public KeyPair requestPrivateKey(String identity) throws IOException {
        // Cette méthode est gardée pour la compatibilité, mais ne devrait plus être utilisée
        // car elle ne passe pas par le mécanisme d'authentification 2FA
        Logger.warning("ATTENTION: Méthode legacy requestPrivateKey appelée sans code TOTP pour " + identity);
        Logger.warning("Cette méthode est obsolète et pourrait échouer si le serveur requiert une authentification 2FA");

        // Vérifier si nous avons un code TOTP pour cet utilisateur
        if (identity.equals(currentEmail) && totpSecret != null) {
            throw new IOException("Cette méthode est obsolète, utilisez requestPrivateKey(identity, totpCode) pour les comptes protégés par 2FA");
        }

        URL url = new URL(serverUrl + "/get-private-key");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // Envoi de l'identité au serveur
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = identity.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == 401) {
            throw new IOException("L'authentification 2FA est requise pour cet utilisateur. Utilisez la méthode requestPrivateKey(identity, totpCode)");
        } else if (responseCode != 200) {
            String errorMessage = readResponse(connection);
            throw new IOException("Failed to get private key: HTTP error code " + responseCode + " - " + errorMessage);
        }

        String response = readResponse(connection);
        connection.disconnect();

        // Analyse de la réponse JSON
        JSONObject jsonResponse = new JSONObject(response);
        String privateKeyB64 = jsonResponse.getString("privateKey");
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyB64);

        // Reconstruction de la clé privée
        Element privateKey = parameters.getPairing().getG1().newElementFromBytes(privateKeyBytes);

        return new KeyPair(identity, privateKey);
    }

    /**
     * Établit un canal sécurisé avec le serveur
     */
    public boolean establishSecureChannel() throws IOException {
        if (secureChannelEstablished) {
            return true;
        }
        
        try {
            // Générer une clé de session et la chiffrer pour le serveur
            JSONObject keyExchange = secureChannel.encryptSessionKeyForServer(SERVER_IDENTITY, ibeEngine);
            
            // Envoyer la clé au serveur
            URL url = new URL(serverUrl + "/establish-secure-channel");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = keyExchange.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }
            
            int responseCode = connection.getResponseCode();
            String response = readResponse(connection);
            
            // Récupérer le Session-ID depuis les headers
            sessionId = connection.getHeaderField(SESSION_ID_HEADER);
            if (sessionId != null) {
                Logger.info("Session-ID reçu: " + sessionId);
            } else {
                Logger.warning("Aucun Session-ID reçu dans la réponse");
            }
            
            connection.disconnect();
            
            if (responseCode == 200 && response.equals("secure-channel-established")) {
                secureChannelEstablished = true;
                Logger.info("Canal sécurisé établi avec le serveur d'autorité");
                return true;
            } else {
                Logger.error("Échec de l'établissement du canal sécurisé: " + response);
                return false;
            }
            
        } catch (Exception e) {
            Logger.error("Erreur lors de l'établissement du canal sécurisé: " + e.getMessage());
            return false;
        }
    }

    /**
     * Méthode utilitaire pour envoyer une requête HTTP sécurisée
     */
    private String sendSecureRequest(String endpoint, String method, String data) throws IOException {
        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        
        // Ajouter le Session-ID dans les headers si disponible
        if (sessionId != null) {
            connection.setRequestProperty(SESSION_ID_HEADER, sessionId);
        }
        
        if (data != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            
            try {
                // Tenter de sécuriser la communication si possible
                String securedData = data;
                if (secureChannelEstablished) {
                    securedData = secureChannel.prepareSecureMessage(data);
                }
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = securedData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }
            } catch (Exception e) {
                Logger.error("Erreur lors de la préparation de la requête sécurisée: " + e.getMessage());
                // En cas d'erreur, envoyer la requête non sécurisée
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = data.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                }
            }
        }
        
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            String errorResponse = readResponse(connection);
            connection.disconnect();
            throw new IOException("HTTP error " + responseCode + ": " + errorResponse);
        }
        
        // Vérifier si nous avons reçu un nouveau Session-ID
        String newSessionId = connection.getHeaderField(SESSION_ID_HEADER);
        if (newSessionId != null && !newSessionId.equals(sessionId)) {
            Logger.info("Mise à jour du Session-ID: " + newSessionId);
            sessionId = newSessionId;
        }
        
        String response = readResponse(connection);
        connection.disconnect();
        
        // Tenter de déchiffrer la réponse si le canal est sécurisé
        if (secureChannelEstablished) {
            try {
                return secureChannel.processSecureResponse(response);
            } catch (Exception e) {
                Logger.error("Erreur lors du déchiffrement de la réponse: " + e.getMessage());
                // En cas d'erreur, retourner la réponse non déchiffrée
            }
        }
        
        return response;
    }

    /**
     * S'assure qu'un canal sécurisé est établi, en créant un nouveau si nécessaire
     */
    private void ensureSecureChannel() throws IOException {
        if (!secureChannelEstablished) {
            boolean success = establishSecureChannel();
            if (!success) {
                throw new IOException("Failed to establish secure channel with trust authority");
            }
        }
    }

    /**
     * Lit la réponse HTTP
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        connection.getResponseCode() < 400 ? connection.getInputStream() : connection.getErrorStream(),
                        StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        }
    }

    /**
     * Renvoie les paramètres récupérés
     */
    public SettingParameters getParameters() {
        return parameters;
    }

    /**
     * Retourne le secret TOTP stocké en mémoire
     *
     * @return Le secret TOTP ou null s'il n'est pas disponible
     */
    public String getTotpSecret() {
        return totpSecret;
    }

    /**
     * Classe interne pour encapsuler les paramètres publics du client
     */
    private static class SettingParametersClient extends SettingParameters {
        private final Pairing pairing;
        private final Element generator;
        private final Element publicKey;

        public SettingParametersClient(String jsonString) {
            // Ne pas appeler super() car nous allons redéfinir tous les paramètres
            super(false); // Ajout d'un constructeur spécial pour éviter l'initialisation par défaut

            JSONObject jsonParams = new JSONObject(jsonString);

            // Récupérer le chemin des paramètres de pairing
            String pairingParamsPath = jsonParams.getString("pairingParams");

            // Initialiser le pairing avec les mêmes paramètres que le serveur
            this.pairing = PairingFactory.getPairing(pairingParamsPath);

            // Reconstruire le générateur à partir des bytes
            byte[] generatorBytes = Base64.getDecoder().decode(jsonParams.getString("generator"));
            this.generator = pairing.getG1().newElementFromBytes(generatorBytes);

            // Reconstruire la clé publique à partir des bytes
            byte[] publicKeyBytes = Base64.getDecoder().decode(jsonParams.getString("publicKey"));
            this.publicKey = pairing.getG1().newElementFromBytes(publicKeyBytes);
        }

        @Override
        public Pairing getPairing() {
            return pairing;
        }

        @Override
        public Element getGenerator() {
            return generator;
        }

        @Override
        public Element getPublicKey() {
            return publicKey;
        }

        @Override
        public Element getMasterKey() {
            throw new UnsupportedOperationException("La clé maître n'est pas disponible côté client");
        }
    }
}
