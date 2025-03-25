package fr.insa.crypto.utils;

import fr.insa.crypto.encryption.IBEcipher;
import fr.insa.crypto.encryption.IdentityBasedEncryption;
import fr.insa.crypto.trustAuthority.SettingParameters;
import it.unisa.dia.gas.jpbc.Element;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Gestionnaire du canal sécurisé pour les échanges client-autorité
 */
public class SecureChannelManager {
    // Constantes pour AES-GCM
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    // Générateur de nombres aléatoires sécurisé
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // État du canal
    private SecretKey sessionKey;
    
    /**
     * Initialise un canal sécurisé
     */
    public SecureChannelManager() {
        // La clé de session sera générée au besoin
    }
    
    /**
     * Génère une nouvelle clé AES
     * @return Une clé AES 256 bits
     */
    public SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, secureRandom);
        return keyGenerator.generateKey();
    }
    
    /**
     * Définit la clé de session pour ce canal
     */
    public void setSessionKey(SecretKey key) {
        this.sessionKey = key;
    }
    
    /**
     * Chiffre un message avec la clé de session AES
     * @param message Message à chiffrer
     * @return Message chiffré encodé en Base64
     */
    public String encryptWithSessionKey(String message) throws Exception {
        if (sessionKey == null) {
            throw new IllegalStateException("Session key not established");
        }
        
        // Générer un IV aléatoire
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        // Initialiser le cipher AES-GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey, gcmParameterSpec);
        
        // Chiffrer le message
        byte[] ciphertext = cipher.doFinal(message.getBytes());
        
        // Concaténer IV et texte chiffré
        byte[] encrypted = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, encrypted, 0, iv.length);
        System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);
        
        // Encoder en Base64
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    /**
     * Déchiffre un message avec la clé de session AES
     * @param encryptedMessage Message chiffré encodé en Base64
     * @return Message déchiffré
     */
    public String decryptWithSessionKey(String encryptedMessage) throws Exception {
        if (sessionKey == null) {
            throw new IllegalStateException("Session key not established");
        }
        
        // Décoder depuis Base64
        byte[] encrypted = Base64.getDecoder().decode(encryptedMessage);
        
        // Extraire l'IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encrypted, 0, iv, 0, iv.length);
        
        // Extraire le texte chiffré
        byte[] ciphertext = new byte[encrypted.length - GCM_IV_LENGTH];
        System.arraycopy(encrypted, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
        
        // Initialiser le cipher pour le déchiffrement
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, sessionKey, gcmParameterSpec);
        
        // Déchiffrer
        byte[] plaintext = cipher.doFinal(ciphertext);
        
        return new String(plaintext);
    }
    
    /**
     * Chiffre la clé de session avec IBE pour l'échange initial
     * @param serverIdentity Identité du serveur (pour IBE)
     * @param ibeEngine Moteur de chiffrement IBE
     * @return Un objet JSONObject contenant la clé de session chiffrée
     */
    public JSONObject encryptSessionKeyForServer(String serverIdentity, IdentityBasedEncryption ibeEngine) throws Exception {
        // Générer une clé de session si nécessaire
        if (sessionKey == null) {
            sessionKey = generateAESKey();
        }
        
        // Chiffrer la clé AES avec IBE pour le serveur
        IBEcipher encryptedSessionKey = ibeEngine.IBEencryption(sessionKey.getEncoded(), serverIdentity);
        
        // Créer un objet JSON pour transporter la clé chiffrée
        JSONObject keyExchange = new JSONObject();
        keyExchange.put("encryptedKey", Base64.getEncoder().encodeToString(encryptedSessionKey.getAescipher()));
        keyExchange.put("u", Base64.getEncoder().encodeToString(encryptedSessionKey.getU().toBytes()));
        keyExchange.put("v", Base64.getEncoder().encodeToString(encryptedSessionKey.getV()));
        
        return keyExchange;
    }
    
    /**
     * Déchiffre une clé de session avec IBE côté serveur
     * @param encryptedKeyData JSONObject contenant la clé de session chiffrée
     * @param privateKey Clé privée du serveur
     * @param ibeEngine Moteur de chiffrement IBE
     */
    public void decryptSessionKey(JSONObject encryptedKeyData, Element privateKey, IdentityBasedEncryption ibeEngine) throws Exception {
        // Reconstruire l'objet IBEcipher
        byte[] uBytes = Base64.getDecoder().decode(encryptedKeyData.getString("u"));
        byte[] vBytes = Base64.getDecoder().decode(encryptedKeyData.getString("v"));
        byte[] encryptedKeyBytes = Base64.getDecoder().decode(encryptedKeyData.getString("encryptedKey"));
        
        Element uElement = ibeEngine.getParameters().getPairing().getG1().newElementFromBytes(uBytes);
        IBEcipher encryptedSessionKey = new IBEcipher(uElement, vBytes, encryptedKeyBytes);
        
        // Déchiffrer la clé de session avec la clé privée
        byte[] sessionKeyBytes = ibeEngine.IBEdecryption(privateKey, encryptedSessionKey);
        
        // Reconstruire la clé de session
        this.sessionKey = new SecretKeySpec(sessionKeyBytes, "AES");
    }
    
    /**
     * Prépare une requête HTTP sécurisée
     * @param originalMessage Message original
     * @return Message chiffré ou message original si le canal n'est pas établi
     */
    public String prepareSecureMessage(String originalMessage) throws Exception {
        // Si pas de clé de session, retourner le message original
        if (sessionKey == null) {
            return originalMessage;
        }
        
        // Sinon, chiffrer et encapsuler dans un JSON
        JSONObject secureMessage = new JSONObject();
        secureMessage.put("encryptedContent", encryptWithSessionKey(originalMessage));
        secureMessage.put("secured", true);
        
        return secureMessage.toString();
    }
    
    /**
     * Traite une réponse HTTP sécurisée
     * @param response Réponse HTTP
     * @return Message déchiffré ou réponse originale si non chiffrée
     */
    public String processSecureResponse(String response) throws Exception {
        try {
            // Essayer de parser en JSON
            JSONObject jsonResponse = new JSONObject(response);
            
            // Vérifier si c'est un message sécurisé
            if (jsonResponse.has("secured") && jsonResponse.getBoolean("secured")) {
                // Déchiffrer le contenu
                return decryptWithSessionKey(jsonResponse.getString("encryptedContent"));
            }
        } catch (Exception e) {
            // Si ce n'est pas un JSON ou pas un message sécurisé, retourner tel quel
        }
        
        // Par défaut, retourner la réponse originale
        return response;
    }
}
