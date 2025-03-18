package fr.insa.crypto.encryption;

import fr.insa.crypto.trustAuthority.*;
import it.unisa.dia.gas.jpbc.Element;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests complets pour le chiffrement et le déchiffrement basés sur l'identité
 */
public class Test {
    
    // Identités des utilisateurs pour la simulation
    private static final String ALICE_EMAIL = "alice@example.com";
    private static final String BOB_EMAIL = "bob@example.com";
    private static final String CHARLIE_EMAIL = "charlie@example.com";
    
    // Classe pour simuler un utilisateur avec son identité et sa clé privée
    private static class User {
        private final String identity;
        private final KeyPair keyPair;
        
        public User(String identity, KeyPair keyPair) {
            this.identity = identity;
            this.keyPair = keyPair;
        }
        
        public String getIdentity() {
            return identity;
        }
        
        public KeyPair getKeyPair() {
            return keyPair;
        }
    }
    
    public static void main(String[] args) {
        try {
            System.out.println("\n======= DÉMARRAGE DES TESTS DE CHIFFREMENT BASÉ SUR L'IDENTITÉ =======\n");
            
            // Simulation de l'autorité de confiance
            simulateFullTrustAuthorityScenario();
            
            System.out.println("\n======= TOUS LES TESTS SONT TERMINÉS AVEC SUCCÈS =======\n");
            
        } catch (Exception e) {
            System.err.println("Erreur lors des tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Simule un scénario complet avec l'autorité de confiance
     */
    private static void simulateFullTrustAuthorityScenario() throws Exception {
        System.out.println("=== Simulation d'un scénario complet avec l'autorité de confiance ===\n");
        
        // 1. Initialisation de l'autorité de confiance et démarrage du serveur
        System.out.println("1. Initialisation de l'autorité de confiance et démarrage du serveur...");
        TrustAuthority trustAuthority = new TrustAuthority();
        int serverPort = 8081; // Port pour les tests
        TrustAuthorityServer server = new TrustAuthorityServer(trustAuthority, serverPort);
        
        try {
            server.start();
            System.out.println("   - Serveur d'autorité démarré sur le port " + serverPort);
            
            // Créer un client pour communiquer avec le serveur
            String serverUrl = "http://localhost:" + serverPort;
            TrustAuthorityClient client = new TrustAuthorityClient(serverUrl);
            System.out.println("   - Client connecté au serveur d'autorité");
            
            IdentityBasedEncryption ibe = new IdentityBasedEncryption(client.getParameters());
            System.out.println("   - Paramètres publics récupérés du serveur");
            
            // 2. Génération des clés privées pour les utilisateurs via le serveur
            System.out.println("2. Distribution des clés privées aux utilisateurs via le serveur...");
            Map<String, User> users = new HashMap<>();
            
            // Alice obtient sa clé privée via le serveur
            KeyPair aliceKeyPair = client.requestPrivateKey(ALICE_EMAIL);
            users.put(ALICE_EMAIL, new User(ALICE_EMAIL, aliceKeyPair));
            System.out.println("   - Alice (" + ALICE_EMAIL + ") a reçu sa clé privée du serveur");
            
            // Bob obtient sa clé privée via le serveur
            KeyPair bobKeyPair = client.requestPrivateKey(BOB_EMAIL);
            users.put(BOB_EMAIL, new User(BOB_EMAIL, bobKeyPair));
            System.out.println("   - Bob (" + BOB_EMAIL + ") a reçu sa clé privée du serveur");
            
            // Charlie obtient sa clé privée via le serveur
            KeyPair charlieKeyPair = client.requestPrivateKey(CHARLIE_EMAIL);
            users.put(CHARLIE_EMAIL, new User(CHARLIE_EMAIL, charlieKeyPair));
            System.out.println("   - Charlie (" + CHARLIE_EMAIL + ") a reçu sa clé privée du serveur");
            
            // 3-6. Exécuter les mêmes tests qu'avant
            // Test de chiffrement et déchiffrement de texte
            System.out.println("\n3. Test de chiffrement et déchiffrement de texte:");
            testTextEncryption(ibe, users);
            
            // Test de chiffrement et déchiffrement de petits fichiers
            System.out.println("\n4. Test de chiffrement et déchiffrement de petits fichiers:");
            testSmallFileEncryption(ibe, users);
            
            // Test de chiffrement et déchiffrement de fichiers plus grands
            System.out.println("\n5. Test de chiffrement et déchiffrement de fichiers plus grands:");
            testLargeFileEncryption(ibe, users);
            
            // Test de chiffrement croisé
            System.out.println("\n6. Test de chiffrement croisé entre utilisateurs:");
            testCrossUserEncryption(ibe, users);
            
        } finally {
            // Arrêt du serveur après les tests
            server.stop();
            System.out.println("\nServeur d'autorité arrêté");
        }
    }
    
    /**
     * Teste le chiffrement et déchiffrement de texte simple entre utilisateurs
     */
    private static void testTextEncryption(IdentityBasedEncryption ibe, Map<String, User> users) throws Exception {
        // Alice chiffre un message pour Bob
        String messageFromAliceToBob = "Bonjour Bob, ceci est un message secret d'Alice.";
        System.out.println("   - Alice envoie à Bob: \"" + messageFromAliceToBob + "\"");
        
        // Chiffrement du message
        IBEcipher encryptedMessageForBob = ibe.IBEencryption(
            messageFromAliceToBob.getBytes(StandardCharsets.UTF_8), 
            BOB_EMAIL
        );
        
        // Bob déchiffre le message
        Element bobPrivateKey = users.get(BOB_EMAIL).getKeyPair().getSk();
        byte[] decryptedByBob = ibe.IBEdecryption(bobPrivateKey, encryptedMessageForBob);
        String decryptedMessageByBob = new String(decryptedByBob, StandardCharsets.UTF_8);
        
        System.out.println("   - Bob déchiffre: \"" + decryptedMessageByBob + "\"");
        assert messageFromAliceToBob.equals(decryptedMessageByBob) : "Le message déchiffré par Bob n'est pas identique à celui envoyé par Alice";
        
        // Bob envoie un message à Charlie
        String messageFromBobToCharlie = "Salut Charlie, c'est Bob. J'ai bien reçu le message d'Alice.";
        System.out.println("   - Bob envoie à Charlie: \"" + messageFromBobToCharlie + "\"");
        
        // Chiffrement du message
        IBEcipher encryptedMessageForCharlie = ibe.IBEencryption(
            messageFromBobToCharlie.getBytes(StandardCharsets.UTF_8), 
            CHARLIE_EMAIL
        );
        
        // Charlie déchiffre le message
        Element charliePrivateKey = users.get(CHARLIE_EMAIL).getKeyPair().getSk();
        byte[] decryptedByCharlie = ibe.IBEdecryption(charliePrivateKey, encryptedMessageForCharlie);
        String decryptedMessageByCharlie = new String(decryptedByCharlie, StandardCharsets.UTF_8);
        
        System.out.println("   - Charlie déchiffre: \"" + decryptedMessageByCharlie + "\"");
        assert messageFromBobToCharlie.equals(decryptedMessageByCharlie) : "Le message déchiffré par Charlie n'est pas identique à celui envoyé par Bob";
        
        System.out.println("   ✓ Test de chiffrement/déchiffrement de texte réussi!");
    }
    
    /**
     * Teste le chiffrement et déchiffrement de petits fichiers texte
     */
    private static void testSmallFileEncryption(IdentityBasedEncryption ibe, Map<String, User> users) throws Exception {
        // Créer un petit fichier texte pour le test
        String smallFileContent = "Contenu d'un petit fichier texte pour le test de chiffrement IBE.\n"
            + "Ce fichier sera chiffré par Alice et envoyé à Bob.\n"
            + "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
        
        File smallTextFile = FileUtils.createTempFileWithContent(smallFileContent, "small_text", ".txt");
        System.out.println("   - Fichier créé: " + smallTextFile.getAbsolutePath());
        System.out.println("   - Taille du fichier: " + smallTextFile.length() + " octets");
        
        // Alice chiffre le fichier pour Bob
        System.out.println("   - Alice chiffre le fichier pour Bob...");
        byte[] fileContent = FileUtils.readFileAsBytes(smallTextFile);
        IBEcipher encryptedFile = ibe.IBEencryption(fileContent, BOB_EMAIL);
        
        // Bob déchiffre le fichier
        System.out.println("   - Bob déchiffre le fichier...");
        Element bobPrivateKey = users.get(BOB_EMAIL).getKeyPair().getSk();
        byte[] decryptedFileContent = ibe.IBEdecryption(bobPrivateKey, encryptedFile);
        
        // Vérification du contenu déchiffré
        String decryptedText = new String(decryptedFileContent, StandardCharsets.UTF_8);
        assert smallFileContent.equals(decryptedText) : "Le contenu du fichier déchiffré ne correspond pas à l'original";
        
        System.out.println("   - Contenu du fichier déchiffré (extrait): " + 
                          decryptedText.substring(0, Math.min(50, decryptedText.length())) + "...");
        System.out.println("   ✓ Test de chiffrement/déchiffrement de petit fichier réussi!");
    }
    
    /**
     * Teste le chiffrement et déchiffrement de fichiers plus grands
     */
    private static void testLargeFileEncryption(IdentityBasedEncryption ibe, Map<String, User> users) throws Exception {
        // Créer un fichier plus volumineux pour le test
        StringBuilder largeContentBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContentBuilder.append("Ligne ").append(i).append(" du fichier de test volumineux. ");
            largeContentBuilder.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ");
            largeContentBuilder.append("Sed non risus. Suspendisse lectus tortor, dignissim sit amet. ");
            largeContentBuilder.append("\n");
        }
        String largeFileContent = largeContentBuilder.toString();
        
        File largeTextFile = FileUtils.createTempFileWithContent(largeFileContent, "large_text", ".txt");
        System.out.println("   - Fichier volumineux créé: " + largeTextFile.getAbsolutePath());
        System.out.println("   - Taille du fichier: " + largeTextFile.length() + " octets");
        
        // Charlie chiffre le fichier pour Alice
        System.out.println("   - Charlie chiffre le fichier volumineux pour Alice...");
        byte[] fileContent = FileUtils.readFileAsBytes(largeTextFile);
        IBEcipher encryptedFile = ibe.IBEencryption(fileContent, ALICE_EMAIL);
        
        // Alice déchiffre le fichier
        System.out.println("   - Alice déchiffre le fichier volumineux...");
        Element alicePrivateKey = users.get(ALICE_EMAIL).getKeyPair().getSk();
        byte[] decryptedFileContent = ibe.IBEdecryption(alicePrivateKey, encryptedFile);
        
        // Vérification du contenu déchiffré
        String decryptedText = new String(decryptedFileContent, StandardCharsets.UTF_8);
        boolean contentMatches = largeFileContent.equals(decryptedText);
        
        System.out.println("   - Contenu du fichier déchiffré (extrait): " + 
                          decryptedText.substring(0, Math.min(50, decryptedText.length())) + "...");
        System.out.println("   - Taille du fichier déchiffré: " + decryptedText.length() + " caractères");
        
        if (contentMatches) {
            System.out.println("   ✓ Test de chiffrement/déchiffrement de fichier volumineux réussi!");
        } else {
            System.err.println("   ✗ Le contenu du fichier volumineux déchiffré ne correspond pas à l'original");
        }
    }
    
    /**
     * Teste le chiffrement croisé entre utilisateurs (Alice → Bob → Charlie → Alice)
     */
    private static void testCrossUserEncryption(IdentityBasedEncryption ibe, Map<String, User> users) throws Exception {
        System.out.println("   Simulation d'un échange de messages chiffrés en chaîne:");
        
        // Étape 1: Alice envoie un fichier à Bob
        String messageContent = "Message initial d'Alice: Bonjour à tous, voici un message qui va être transmis en chaîne.";
        File messageFile = FileUtils.createTempFileWithContent(messageContent, "message_chain", ".txt");
        System.out.println("   - Alice crée un message: \"" + messageContent + "\"");
        
        byte[] originalContent = FileUtils.readFileAsBytes(messageFile);
        IBEcipher aliceToBob = ibe.IBEencryption(originalContent, BOB_EMAIL);
        System.out.println("   - Alice chiffre le message pour Bob");
        
        // Étape 2: Bob déchiffre, ajoute du contenu et envoie à Charlie
        Element bobPrivateKey = users.get(BOB_EMAIL).getKeyPair().getSk();
        byte[] bobDecrypted = ibe.IBEdecryption(bobPrivateKey, aliceToBob);
        String bobMessage = new String(bobDecrypted, StandardCharsets.UTF_8) + 
                          "\nAjout de Bob: J'ai bien reçu le message d'Alice et je le transmets.";
        System.out.println("   - Bob déchiffre, ajoute son message: \"" + 
                          bobMessage.substring(messageContent.length()) + "\"");
        
        IBEcipher bobToCharlie = ibe.IBEencryption(bobMessage.getBytes(StandardCharsets.UTF_8), CHARLIE_EMAIL);
        System.out.println("   - Bob chiffre le message étendu pour Charlie");
        
        // Étape 3: Charlie déchiffre, ajoute du contenu et renvoie à Alice
        Element charliePrivateKey = users.get(CHARLIE_EMAIL).getKeyPair().getSk();
        byte[] charlieDecrypted = ibe.IBEdecryption(charliePrivateKey, bobToCharlie);
        String charlieMessage = new String(charlieDecrypted, StandardCharsets.UTF_8) + 
                              "\nAjout de Charlie: Message bien reçu, je le renvoie complété à Alice.";
        System.out.println("   - Charlie déchiffre, ajoute son message: \"" + 
                          charlieMessage.substring(bobMessage.length()) + "\"");
        
        IBEcipher charlieToAlice = ibe.IBEencryption(charlieMessage.getBytes(StandardCharsets.UTF_8), ALICE_EMAIL);
        System.out.println("   - Charlie chiffre le message complet pour Alice");
        
        // Étape 4: Alice déchiffre le message final
        Element alicePrivateKey = users.get(ALICE_EMAIL).getKeyPair().getSk();
        byte[] aliceFinalDecrypted = ibe.IBEdecryption(alicePrivateKey, charlieToAlice);
        String finalMessage = new String(aliceFinalDecrypted, StandardCharsets.UTF_8);
        
        System.out.println("   - Alice déchiffre le message final");
        System.out.println("   - Message final reçu par Alice:");
        System.out.println("\n------ DÉBUT DU MESSAGE ------");
        System.out.println(finalMessage);
        System.out.println("------ FIN DU MESSAGE ------\n");
        
        // Vérification que le message final contient bien tous les ajouts
        boolean containsAll = finalMessage.contains(messageContent) && 
                             finalMessage.contains("Ajout de Bob") && 
                             finalMessage.contains("Ajout de Charlie");
        
        if (containsAll) {
            System.out.println("   ✓ Test de chiffrement croisé entre utilisateurs réussi!");
        } else {
            System.err.println("   ✗ Le message final ne contient pas tous les ajouts attendus");
        }
    }
}
