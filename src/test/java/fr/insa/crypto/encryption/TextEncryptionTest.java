package fr.insa.crypto.encryption;

import fr.insa.crypto.trustAuthority.TrustAuthority;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour le chiffrement et le déchiffrement de messages texte
 */
public class TextEncryptionTest {
    
    private TrustAuthority trustAuthority;
    private IdentityBasedEncryption ibe;
    private KeyGeneration keyGeneration;
    private Pairing pairing;
    private Element systemPublicKey;
    private String aliceEmail = "alice@example.com";
    private String bobEmail = "bob@example.com";
    private String charlieEmail = "charlie@example.com";
    private String alicePrivateKey;
    private String bobPrivateKey;
    private String charliePrivateKey;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Initialisation de l'autorité de confiance
        trustAuthority = new TrustAuthority();
        pairing = trustAuthority.getPairing();
        systemPublicKey = trustAuthority.getPublicKey();
        keyGeneration = new KeyGeneration(pairing, trustAuthority.getParameters());
        ibe = new IdentityBasedEncryption(pairing, systemPublicKey, keyGeneration);
        
        // Génération des clés privées
        Element alicePrivateKeyElement = trustAuthority.generatePrivateKey(aliceEmail);
        Element bobPrivateKeyElement = trustAuthority.generatePrivateKey(bobEmail);
        Element charliePrivateKeyElement = trustAuthority.generatePrivateKey(charlieEmail);
        
        alicePrivateKey = Base64.getEncoder().encodeToString(alicePrivateKeyElement.toBytes());
        bobPrivateKey = Base64.getEncoder().encodeToString(bobPrivateKeyElement.toBytes());
        charliePrivateKey = Base64.getEncoder().encodeToString(charliePrivateKeyElement.toBytes());
    }
    
    @Test
    public void testEncryptDecryptSimpleMessage() throws Exception {
        String originalMessage = "Bonjour Bob, ceci est un message secret.";
        
        // Alice chiffre un message pour Bob
        IdentityBasedEncryption.EncryptedMessage encryptedMessage = 
            ibe.encrypt(originalMessage, bobEmail);
        
        // Bob déchiffre le message
        String decryptedByBob = ibe.decrypt(encryptedMessage, bobPrivateKey);
        
        // Vérification
        assertEquals(originalMessage, decryptedByBob, "Le message déchiffré doit être identique au message original");
    }
    
    @Test
    public void testEncryptDecryptUnicodeMessage() throws Exception {
        String originalMessage = "こんにちは Bob, 这是一个秘密消息. Привет! 🚀🔐";
        
        // Alice chiffre un message pour Bob
        IdentityBasedEncryption.EncryptedMessage encryptedMessage = 
            ibe.encrypt(originalMessage, bobEmail);
        
        // Bob déchiffre le message
        String decryptedByBob = ibe.decrypt(encryptedMessage, bobPrivateKey);
        
        // Vérification
        assertEquals(originalMessage, decryptedByBob, "Le message Unicode déchiffré doit être identique au message original");
    }
    
    @Test
    public void testWrongRecipient() throws Exception {
        String originalMessage = "Secret message pour Bob uniquement!";
        
        // Alice chiffre un message pour Bob
        IdentityBasedEncryption.EncryptedMessage encryptedMessage = 
            ibe.encrypt(originalMessage, bobEmail);
        
        // Charlie essaie de déchiffrer le message destiné à Bob
        Exception exception = assertThrows(Exception.class, () -> {
            ibe.decrypt(encryptedMessage, charliePrivateKey);
        });
        
        assertTrue(exception.getMessage().contains("pad block corrupted") ||
                  exception.getMessage().contains("padding") ||
                  exception.getMessage().contains("decrypt"),
                  "L'exception devrait indiquer une erreur de déchiffrement");
    }
    
    @Test
    public void testLongMessage() throws Exception {
        // Création d'un message long
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            builder.append("Ligne ").append(i).append(": C'est un test de message long pour vérifier la performance.\n");
        }
        String longMessage = builder.toString();
        
        // Mesure du temps de chiffrement
        long startEncrypt = System.currentTimeMillis();
        IdentityBasedEncryption.EncryptedMessage encryptedMessage = 
            ibe.encrypt(longMessage, aliceEmail);
        long endEncrypt = System.currentTimeMillis();
        
        // Mesure du temps de déchiffrement
        long startDecrypt = System.currentTimeMillis();
        String decryptedMessage = ibe.decrypt(encryptedMessage, alicePrivateKey);
        long endDecrypt = System.currentTimeMillis();
        
        // Vérification
        assertEquals(longMessage, decryptedMessage, "Le message long déchiffré doit être identique au message original");
        
        // Affichage des temps d'exécution
        System.out.println("Temps de chiffrement pour un message long : " + (endEncrypt - startEncrypt) + " ms");
        System.out.println("Temps de déchiffrement pour un message long : " + (endDecrypt - startDecrypt) + " ms");
    }
    
    @Test
    public void testMultipleMessages() throws Exception {
        // Alice envoie plusieurs messages à Bob et Charlie
        String[] messages = {
            "Premier message pour Bob",
            "Message confidentiel pour Charlie",
            "Autre message pour Bob",
            "Information importante pour Charlie"
        };
        
        String[] recipients = {
            bobEmail,
            charlieEmail,
            bobEmail,
            charlieEmail
        };
        
        String[] privateKeys = {
            bobPrivateKey,
            charliePrivateKey,
            bobPrivateKey,
            charliePrivateKey
        };
        
        // Chiffrement et déchiffrement de plusieurs messages
        for (int i = 0; i < messages.length; i++) {
            IdentityBasedEncryption.EncryptedMessage encrypted = 
                ibe.encrypt(messages[i], recipients[i]);
            
            String decrypted = ibe.decrypt(encrypted, privateKeys[i]);
            
            assertEquals(messages[i], decrypted, 
                "Le message " + (i+1) + " déchiffré doit être identique au message original");
        }
    }
}
