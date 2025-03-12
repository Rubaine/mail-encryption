package fr.insa.crypto.encryption;

import fr.insa.crypto.trustAuthority.TrustAuthority;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour le chiffrement et le déchiffrement de fichiers
 */
public class FileEncryptionTest {
    
    private TrustAuthority trustAuthority;
    private IdentityBasedEncryption ibe;
    private KeyGeneration keyGeneration;
    private Pairing pairing;
    private Element systemPublicKey;
    private String aliceEmail = "alice@example.com";
    private String bobEmail = "bob@example.com";
    private String alicePrivateKey;
    private String bobPrivateKey;
    
    @TempDir
    Path tempDir;
    
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
        
        alicePrivateKey = Base64.getEncoder().encodeToString(alicePrivateKeyElement.toBytes());
        bobPrivateKey = Base64.getEncoder().encodeToString(bobPrivateKeyElement.toBytes());
    }
    
    @Test
    public void testEncryptDecryptTextFile() throws Exception {
        // Création d'un fichier texte de test
        String testContent = "Ceci est un test de chiffrement de fichier texte.";
        File testFile = FileUtils.createTempFileWithContent(testContent, "test_text_", ".txt");
        
        // Fichiers pour le contenu chiffré et déchiffré
        File encryptedFile = new File(tempDir.toFile(), "encrypted_text.bin");
        File decryptedFile = new File(tempDir.toFile(), "decrypted_text.txt");
        
        // Test de chiffrement et déchiffrement
        ibe.encryptFile(testFile, encryptedFile, bobEmail);
        ibe.decryptFile(encryptedFile, decryptedFile, bobPrivateKey);
        
        // Vérification du contenu déchiffré
        String decryptedContent = FileUtils.readFileAsString(decryptedFile);
        assertEquals(testContent, decryptedContent, "Le contenu déchiffré devrait correspondre au contenu original");
    }
    
    @Test
    public void testEncryptDecryptBinaryFile() throws Exception {
        // Création d'un fichier binaire de test (image simple)
        byte[] testBinaryData = new byte[1024];
        for (int i = 0; i < testBinaryData.length; i++) {
            testBinaryData[i] = (byte) (i % 256);
        }
        
        File testFile = FileUtils.createTempFileWithData(testBinaryData, "test_binary_", ".bin");
        
        // Fichiers pour le contenu chiffré et déchiffré
        File encryptedFile = new File(tempDir.toFile(), "encrypted_binary.bin");
        File decryptedFile = new File(tempDir.toFile(), "decrypted_binary.bin");
        
        // Test de chiffrement et déchiffrement
        ibe.encryptFile(testFile, encryptedFile, aliceEmail);
        ibe.decryptFile(encryptedFile, decryptedFile, alicePrivateKey);
        
        // Vérification du contenu déchiffré
        byte[] decryptedData = Files.readAllBytes(decryptedFile.toPath());
        assertArrayEquals(testBinaryData, decryptedData, "Les données déchiffrées devraient correspondre aux données originales");
    }
    
    @Test
    public void testWrongRecipient() throws Exception {
        // Création d'un fichier texte de test
        String testContent = "Message confidentiel pour Bob.";
        File testFile = FileUtils.createTempFileWithContent(testContent, "test_wrong_", ".txt");
        
        // Fichiers pour le contenu chiffré et déchiffré
        File encryptedFile = new File(tempDir.toFile(), "encrypted_wrong.bin");
        File decryptedFile = new File(tempDir.toFile(), "decrypted_wrong.txt");
        
        // Chiffrement pour Bob
        ibe.encryptFile(testFile, encryptedFile, bobEmail);
        
        // Tentative de déchiffrement avec la clé d'Alice (devrait échouer)
        Exception exception = assertThrows(Exception.class, () -> {
            ibe.decryptFile(encryptedFile, decryptedFile, alicePrivateKey);
        });
        
        assertTrue(exception.getMessage().contains("pad block corrupted") ||
                  exception.getMessage().contains("padding") ||
                  exception.getMessage().contains("decrypt"),
                  "L'exception devrait indiquer une erreur de déchiffrement");
    }
    
    @Test
    public void testLargeFile() throws Exception {
        // Création d'un fichier de grande taille pour tester les performances
        byte[] largeData = new byte[1024 * 1024]; // 1 MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i & 0xFF);
        }
        
        File largeFile = FileUtils.createTempFileWithData(largeData, "large_file_", ".bin");
        
        // Fichiers pour le contenu chiffré et déchiffré
        File encryptedFile = new File(tempDir.toFile(), "encrypted_large.bin");
        File decryptedFile = new File(tempDir.toFile(), "decrypted_large.bin");
        
        // Mesure du temps de chiffrement
        long startEncrypt = System.currentTimeMillis();
        ibe.encryptFile(largeFile, encryptedFile, bobEmail);
        long endEncrypt = System.currentTimeMillis();
        
        // Mesure du temps de déchiffrement
        long startDecrypt = System.currentTimeMillis();
        ibe.decryptFile(encryptedFile, decryptedFile, bobPrivateKey);
        long endDecrypt = System.currentTimeMillis();
        
        // Vérification du contenu déchiffré
        byte[] decryptedData = Files.readAllBytes(decryptedFile.toPath());
        assertArrayEquals(largeData, decryptedData, "Les données déchiffrées devraient correspondre aux données originales");
        
        // Affichage des temps d'exécution
        System.out.println("Temps de chiffrement pour un fichier de 1 MB : " + (endEncrypt - startEncrypt) + " ms");
        System.out.println("Temps de déchiffrement pour un fichier de 1 MB : " + (endDecrypt - startDecrypt) + " ms");
    }
}
