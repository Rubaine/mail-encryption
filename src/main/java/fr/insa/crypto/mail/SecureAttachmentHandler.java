package fr.insa.crypto.mail;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.io.IOException;

import fr.insa.crypto.encryption.IBEcipher;
import fr.insa.crypto.encryption.IdentityBasedEncryption;
import fr.insa.crypto.trustAuthority.KeyPair;
import fr.insa.crypto.utils.Logger;
import it.unisa.dia.gas.jpbc.Element;

import org.json.JSONObject;

/**
 * Gestionnaire sécurisé pour les pièces jointes avec chiffrement/déchiffrement IBE
 */
public class SecureAttachmentHandler extends AttachmentHandler {
    
    private final IdentityBasedEncryption ibeEngine;
    private final KeyPair userKeyPair;
    
    /**
     * Constructeur pour l'envoi de pièces jointes chiffrées
     * @param ibeEngine Moteur de chiffrement IBE
     * @param userKeyPair Paire de clés de l'utilisateur
     */
    public SecureAttachmentHandler(IdentityBasedEncryption ibeEngine, KeyPair userKeyPair) {
        super();
        this.ibeEngine = ibeEngine;
        this.userKeyPair = userKeyPair;
    }
    
    /**
     * Ajoute une pièce jointe chiffrée pour le destinataire
     * @param filePath Chemin du fichier à chiffrer et joindre
     * @param recipientEmail Email du destinataire (utilisé comme identité pour le chiffrement)
     * @throws Exception Si une erreur survient lors du chiffrement
     */
    public void addEncryptedAttachment(String filePath, String recipientEmail) throws Exception {
        File originalFile = new File(filePath);
        if (!originalFile.exists()) {
            throw new IOException("Le fichier n'existe pas: " + filePath);
        }
        
        String fileName = originalFile.getName();
        Logger.info("Chiffrement du fichier " + fileName + " pour " + recipientEmail);
        
        try {
            // Lire le fichier
            byte[] fileContent = Files.readAllBytes(originalFile.toPath());
            
            // Chiffrer le contenu pour le destinataire
            IBEcipher encryptedData = ibeEngine.IBEencryption(fileContent, recipientEmail);
            
            // Sérialiser le contenu chiffré
            JSONObject jsonCipher = new JSONObject();
            jsonCipher.put("U", java.util.Base64.getEncoder().encodeToString(encryptedData.getU().toBytes()));
            jsonCipher.put("V", java.util.Base64.getEncoder().encodeToString(encryptedData.getV()));
            jsonCipher.put("AEScipher", java.util.Base64.getEncoder().encodeToString(encryptedData.getAescipher()));
            jsonCipher.put("originalName", fileName);
            
            // Créer un fichier temporaire contenant le chiffré
            File encryptedFile = File.createTempFile("encrypted_", ".ibe");
            try (FileOutputStream fos = new FileOutputStream(encryptedFile)) {
                fos.write(jsonCipher.toString().getBytes());
            }
            
            // Ajouter le fichier chiffré comme pièce jointe
            super.addAttachment(encryptedFile.getAbsolutePath());
            Logger.info("Fichier chiffré et ajouté comme pièce jointe: " + encryptedFile.getAbsolutePath());
            
        } catch (Exception e) {
            Logger.error("Erreur lors du chiffrement du fichier " + fileName + ": " + e.getMessage());
            throw new Exception("Échec du chiffrement de la pièce jointe: " + e.getMessage());
        }
    }
    
    /**
     * Déchiffre un fichier chiffré IBE
     * @param encryptedFilePath Chemin du fichier chiffré
     * @param outputDirectory Répertoire où sauvegarder le fichier déchiffré
     * @return Le fichier déchiffré
     * @throws Exception Si une erreur survient lors du déchiffrement
     */
    public static File decryptFile(File encryptedFile, String outputDirectory, Element privateKey, 
                                  IdentityBasedEncryption ibeEngine) throws Exception {
        try {
            // Lire et parser le fichier chiffré
            String jsonContent = new String(Files.readAllBytes(encryptedFile.toPath()));
            JSONObject jsonCipher = new JSONObject(jsonContent);
            
            // Récupérer les composants du chiffrement
            byte[] uBytes = java.util.Base64.getDecoder().decode(jsonCipher.getString("U"));
            byte[] vBytes = java.util.Base64.getDecoder().decode(jsonCipher.getString("V"));
            byte[] aesCipherBytes = java.util.Base64.getDecoder().decode(jsonCipher.getString("AEScipher"));
            String originalName = jsonCipher.getString("originalName");
            
            // Reconstruire le chiffrement IBE
            Element uElement = ibeEngine.getParameters().getPairing().getG1().newElementFromBytes(uBytes);
            IBEcipher cipher = new IBEcipher(uElement, vBytes, aesCipherBytes);
            
            // Déchiffrer avec la clé privée
            byte[] decryptedContent = ibeEngine.IBEdecryption(privateKey, cipher);
            
            // Créer le fichier de sortie avec le nom d'origine
            File outputFile = new File(outputDirectory, originalName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(decryptedContent);
            }
            
            Logger.info("Fichier déchiffré avec succès: " + outputFile.getAbsolutePath());
            return outputFile;
            
        } catch (Exception e) {
            Logger.error("Erreur lors du déchiffrement du fichier: " + e.getMessage());
            throw new Exception("Échec du déchiffrement de la pièce jointe: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie si un fichier est un fichier chiffré IBE
     */
    public static boolean isIBEEncryptedFile(File file) {
        if (file.getName().endsWith(".ibe")) {
            return true;
        }
        
        // Vérification plus approfondie si nécessaire
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            JSONObject json = new JSONObject(content);
            return json.has("U") && json.has("V") && json.has("AEScipher") && json.has("originalName");
        } catch (Exception e) {
            return false;
        }
    }
}
