package fr.insa.crypto.trustAuthority;

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

    /**
     * Constructeur qui récupère les paramètres publics du serveur
     * @param serverUrl L'URL du serveur de l'autorité de confiance
     */
    public TrustAuthorityClient(String serverUrl) throws IOException {
        this.serverUrl = serverUrl;
        this.parameters = fetchParameters();
    }

    /**
     * Récupère les paramètres publics depuis le serveur
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
        
        // Dans un cas réel, nous devrions désérialiser complètement les paramètres
        // Pour l'instant, on utilise une implémentation simplifiée
        return new SettingParametersClient(response);
    }

    /**
     * Demande une clé privée au serveur pour l'identité spécifiée
     */
    public KeyPair requestPrivateKey(String identity) throws IOException {
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
        if (responseCode != 200) {
            throw new IOException("Failed to get private key: HTTP error code " + responseCode);
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
     * Lit la réponse HTTP
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
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
