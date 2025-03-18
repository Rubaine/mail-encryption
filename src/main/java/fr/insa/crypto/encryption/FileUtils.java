package fr.insa.crypto.encryption;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Utilitaires pour la manipulation des fichiers
 */
public class FileUtils {

    /**
     * Crée un fichier temporaire avec le contenu spécifié
     * @param content Le contenu à écrire dans le fichier
     * @param prefix Le préfixe du nom du fichier
     * @param suffix Le suffixe du nom du fichier
     * @return Le fichier créé
     * @throws IOException Si une erreur survient lors de la création du fichier
     */
    public static File createTempFileWithContent(String content, String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        Files.write(tempFile.toPath(), content.getBytes());
        tempFile.deleteOnExit();
        return tempFile;
    }
    
    /**
     * Crée un fichier temporaire avec les données binaires spécifiées
     * @param data Les données à écrire dans le fichier
     * @param prefix Le préfixe du nom du fichier
     * @param suffix Le suffixe du nom du fichier
     * @return Le fichier créé
     * @throws IOException Si une erreur survient lors de la création du fichier
     */
    public static File createTempFileWithData(byte[] data, String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        Files.write(tempFile.toPath(), data);
        tempFile.deleteOnExit();
        return tempFile;
    }
    
    /**
     * Crée un fichier temporaire vide
     * @param prefix Le préfixe du nom du fichier
     * @param suffix Le suffixe du nom du fichier
     * @return Le fichier créé
     * @throws IOException Si une erreur survient lors de la création du fichier
     */
    public static File createEmptyTempFile(String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();
        return tempFile;
    }
    
    /**
     * Lit le contenu d'un fichier en tant que chaîne de caractères
     * @param file Le fichier à lire
     * @return Le contenu du fichier
     * @throws IOException Si une erreur survient lors de la lecture du fichier
     */
    public static String readFileAsString(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
    
    /**
     * Lit le contenu d'un fichier en tant que tableau d'octets
     * @param file Le fichier à lire
     * @return Le contenu du fichier
     * @throws IOException Si une erreur survient lors de la lecture du fichier
     */
    public static byte[] readFileAsBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }
    
    /**
     * Génère un chemin de fichier unique dans le répertoire temporaire du système
     * @param prefix Le préfixe du nom du fichier
     * @param suffix Le suffixe du nom du fichier
     * @return Le chemin du fichier
     */
    public static Path generateTempFilePath(String prefix, String suffix) {
        String tempDir = System.getProperty("java.io.tmpdir");
        String uniqueFileName = prefix + UUID.randomUUID().toString() + suffix;
        return Paths.get(tempDir, uniqueFileName);
    }
}
