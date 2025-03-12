package fr.insa.crypto.encryption;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Classe gérant le chiffrement basé sur l'identité
 */
public class IdentityBasedEncryption {
    private Pairing pairing;
    private Element systemPublicKey;
    private KeyGeneration keyGeneration;
    
    public IdentityBasedEncryption(Pairing pairing, Element systemPublicKey, KeyGeneration keyGeneration) {
        this.pairing = pairing;
        this.systemPublicKey = systemPublicKey;
        this.keyGeneration = keyGeneration;
    }
    
    /**
     * Chiffrement d'un message pour un destinataire donné
     * @param message Le message à chiffrer
     * @param recipientEmail L'adresse email du destinataire
     * @return Le message chiffré sous forme d'objet EncryptedMessage
     */
    public EncryptedMessage encrypt(String message, String recipientEmail) throws Exception {
        // Génération d'un nombre aléatoire r
        Element r = pairing.getZr().newRandomElement().getImmutable();
        
        // Calcul de l'identité publique du destinataire
        Element qId = keyGeneration.calculateQid(recipientEmail);
        
        // Calcul du premier composant du chiffrement U = rP (P est le générateur)
        Element u = pairing.getG1().newRandomElement().mulZn(r).getImmutable();
        
        // Calcul du secret partagé: gID = e(QID, Ppub)^r
        Element gId = pairing.pairing(qId, systemPublicKey).powZn(r).getImmutable();
        
        // Utilisation du secret partagé comme clé de chiffrement symétrique
        byte[] sessionKeyBytes = gId.toBytes();
        SecretKeySpec secretKey = new SecretKeySpec(EncryptionUtils.deriveKey(sessionKeyBytes, 16), "AES");
        
        // Chiffrement du message avec AES
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes());
        
        // Création du message chiffré
        return new EncryptedMessage(
            Base64.getEncoder().encodeToString(u.toBytes()),
            Base64.getEncoder().encodeToString(encryptedBytes)
        );
    }
    
    /**
     * Déchiffrement d'un message avec la clé privée
     * @param encryptedMessage Le message chiffré
     * @param privateKeyBase64 La clé privée du destinataire encodée en Base64
     * @return Le message en clair
     */
    public String decrypt(EncryptedMessage encryptedMessage, String privateKeyBase64) throws Exception {
        // Décodage de U
        Element u = pairing.getG1().newElementFromBytes(
            Base64.getDecoder().decode(encryptedMessage.getU())
        ).getImmutable();
        
        // Décodage de la clé privée
        Element privateKey = keyGeneration.decodePrivateKey(privateKeyBase64);
        
        // Calcul du secret partagé: gID = e(dID, U)
        Element gId = pairing.pairing(privateKey, u).getImmutable();
        
        // Utilisation du secret partagé comme clé de déchiffrement
        byte[] sessionKeyBytes = gId.toBytes();
        SecretKeySpec secretKey = new SecretKeySpec(EncryptionUtils.deriveKey(sessionKeyBytes, 16), "AES");
        
        // Déchiffrement du message avec AES
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(
            Base64.getDecoder().decode(encryptedMessage.getV())
        );
        
        return new String(decryptedBytes);
    }
    
    /**
     * Chiffrement d'un fichier pour un destinataire donné
     * @param inputFile Le fichier à chiffrer
     * @param outputFile Le fichier de sortie qui contiendra le contenu chiffré
     * @param recipientEmail L'adresse email du destinataire
     * @throws Exception Si une erreur survient lors du chiffrement
     */
    public void encryptFile(File inputFile, File outputFile, String recipientEmail) throws Exception {
        byte[] fileContent = Files.readAllBytes(inputFile.toPath());
        EncryptedData encryptedData = encryptData(fileContent, recipientEmail);
        
        // Format: u_length + u_bytes + v_bytes
        byte[] uBytes = Base64.getDecoder().decode(encryptedData.getU());
        byte[] vBytes = Base64.getDecoder().decode(encryptedData.getV());
        
        // Write the encrypted file format
        byte[] outputContent = new byte[4 + uBytes.length + vBytes.length];
        
        // 4 bytes for u_length (int)
        outputContent[0] = (byte) ((uBytes.length >> 24) & 0xFF);
        outputContent[1] = (byte) ((uBytes.length >> 16) & 0xFF);
        outputContent[2] = (byte) ((uBytes.length >> 8) & 0xFF);
        outputContent[3] = (byte) (uBytes.length & 0xFF);
        
        // Copy u_bytes and v_bytes
        System.arraycopy(uBytes, 0, outputContent, 4, uBytes.length);
        System.arraycopy(vBytes, 0, outputContent, 4 + uBytes.length, vBytes.length);
        
        Files.write(outputFile.toPath(), outputContent);
    }
    
    /**
     * Déchiffrement d'un fichier avec la clé privée
     * @param inputFile Le fichier chiffré
     * @param outputFile Le fichier de sortie qui contiendra le contenu déchiffré
     * @param privateKeyBase64 La clé privée du destinataire encodée en Base64
     * @throws Exception Si une erreur survient lors du déchiffrement
     */
    public void decryptFile(File inputFile, File outputFile, String privateKeyBase64) throws Exception {
        byte[] encryptedContent = Files.readAllBytes(inputFile.toPath());
        
        // Parse the encrypted file format
        int uLength = ((encryptedContent[0] & 0xFF) << 24) | 
                      ((encryptedContent[1] & 0xFF) << 16) | 
                      ((encryptedContent[2] & 0xFF) << 8) | 
                      (encryptedContent[3] & 0xFF);
        
        byte[] uBytes = new byte[uLength];
        System.arraycopy(encryptedContent, 4, uBytes, 0, uLength);
        
        int vLength = encryptedContent.length - 4 - uLength;
        byte[] vBytes = new byte[vLength];
        System.arraycopy(encryptedContent, 4 + uLength, vBytes, 0, vLength);
        
        // Create EncryptedData object
        EncryptedData encryptedData = new EncryptedData(
            Base64.getEncoder().encodeToString(uBytes),
            Base64.getEncoder().encodeToString(vBytes)
        );
        
        // Decrypt the data
        byte[] decryptedContent = decryptData(encryptedData, privateKeyBase64);
        
        // Write the decrypted content to the output file
        Files.write(outputFile.toPath(), decryptedContent);
    }
    
    /**
     * Chiffrement de données binaires pour un destinataire donné
     * @param data Les données à chiffrer
     * @param recipientEmail L'adresse email du destinataire
     * @return Les données chiffrées
     */
    public EncryptedData encryptData(byte[] data, String recipientEmail) throws Exception {
        // Génération d'un nombre aléatoire r
        Element r = pairing.getZr().newRandomElement().getImmutable();
        
        // Calcul de l'identité publique du destinataire
        Element qId = keyGeneration.calculateQid(recipientEmail);
        
        // Calcul du premier composant du chiffrement U = rP (P est le générateur)
        Element u = pairing.getG1().newRandomElement().mulZn(r).getImmutable();
        
        // Calcul du secret partagé: gID = e(QID, Ppub)^r
        Element gId = pairing.pairing(qId, systemPublicKey).powZn(r).getImmutable();
        
        // Utilisation du secret partagé comme clé de chiffrement symétrique
        byte[] sessionKeyBytes = gId.toBytes();
        SecretKeySpec secretKey = new SecretKeySpec(EncryptionUtils.deriveKey(sessionKeyBytes, 16), "AES");
        
        // Chiffrement des données avec AES
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data);
        
        // Retour des données chiffrées
        return new EncryptedData(
            Base64.getEncoder().encodeToString(u.toBytes()),
            Base64.getEncoder().encodeToString(encryptedBytes)
        );
    }
    
    /**
     * Déchiffrement de données binaires avec la clé privée
     * @param encryptedData Les données chiffrées
     * @param privateKeyBase64 La clé privée du destinataire encodée en Base64
     * @return Les données déchiffrées
     */
    public byte[] decryptData(EncryptedData encryptedData, String privateKeyBase64) throws Exception {
        // Décodage de U
        Element u = pairing.getG1().newElementFromBytes(
            Base64.getDecoder().decode(encryptedData.getU())
        ).getImmutable();
        
        // Décodage de la clé privée
        Element privateKey = keyGeneration.decodePrivateKey(privateKeyBase64);
        
        // Calcul du secret partagé: gID = e(dID, U)
        Element gId = pairing.pairing(privateKey, u).getImmutable();
        
        // Utilisation du secret partagé comme clé de déchiffrement
        byte[] sessionKeyBytes = gId.toBytes();
        SecretKeySpec secretKey = new SecretKeySpec(EncryptionUtils.deriveKey(sessionKeyBytes, 16), "AES");
        
        // Déchiffrement avec AES
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        
        return cipher.doFinal(Base64.getDecoder().decode(encryptedData.getV()));
    }
    
    /**
     * Adapte le format EncryptedMessage pour les données binaires
     */
    public static class EncryptedData {
        private String u; // Premier composant (rP)
        private String v; // Deuxième composant (données chiffrées)
        
        public EncryptedData(String u, String v) {
            this.u = u;
            this.v = v;
        }
        
        public String getU() {
            return u;
        }
        
        public String getV() {
            return v;
        }
        
        public EncryptedMessage toEncryptedMessage() {
            return new EncryptedMessage(u, v);
        }
    }
    
    /**
     * Classe interne pour représenter un message chiffré
     */
    public static class EncryptedMessage {
        private String u; // Premier composant (rP)
        private String v; // Deuxième composant (message chiffré)
        
        public EncryptedMessage(String u, String v) {
            this.u = u;
            this.v = v;
        }
        
        public String getU() {
            return u;
        }
        
        public String getV() {
            return v;
        }
        
        @Override
        public String toString() {
            return "U: " + u + "\nV: " + v;
        }
    }
}
