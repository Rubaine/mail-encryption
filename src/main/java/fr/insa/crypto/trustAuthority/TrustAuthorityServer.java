package fr.insa.crypto.trustAuthority;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Serveur HTTP pour l'autorité de confiance
 */
public class TrustAuthorityServer {
    private final TrustAuthority trustAuthority;
    private final int port;
    private HttpServer server;
    
    public TrustAuthorityServer(TrustAuthority trustAuthority, int port) {
        this.trustAuthority = trustAuthority;
        this.port = port;
    }
    
    /**
     * Démarrage du serveur HTTP
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Endpoint pour récupérer une clé privée
        server.createContext("/get-private-key", new PrivateKeyHandler());
        
        // Endpoint pour obtenir les paramètres publics du système
        server.createContext("/public-parameters", new PublicParametersHandler());
        
        server.setExecutor(null); // Utilise l'exécuteur par défaut
        server.start();
        
        System.out.println("Trust Authority Server started on port " + port);
    }
    
    /**
     * Arrêt du serveur
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Trust Authority Server stopped");
        }
    }
    
    /**
     * Handler pour la distribution des clés privées
     */
    private class PrivateKeyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            try {
                // Lecture de l'email dans le corps de la requête
                String email = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
                
                // Distribution de la clé privée
                KeyPair privateKey = trustAuthority.getKeyDistributor().distributePrivateKey(email);
                
                // Sérialisation de la clé privée pour la transmission
                String response = String.format("{\"identity\":\"%s\",\"privateKey\":\"%s\"}",
                    privateKey.getPk(),
                    Base64.getEncoder().encodeToString(privateKey.getSk().toBytes())
                );
                
                sendResponse(exchange, 200, response);
                
            } catch (IllegalArgumentException e) {
                sendResponse(exchange, 400, "Invalid email format: " + e.getMessage());
            } catch (Exception e) {
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handler pour l'obtention des paramètres publics
     */
    private class PublicParametersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            try {
                // Sérialisation complète des paramètres publics en JSON
                String publicParams = String.format(
                    "{\"publicKey\":\"%s\",\"generator\":\"%s\",\"pairingParams\":\"params/curves/a.properties\"}",
                    Base64.getEncoder().encodeToString(trustAuthority.getParameters().getPublicKey().toBytes()),
                    Base64.getEncoder().encodeToString(trustAuthority.getParameters().getGenerator().toBytes())
                );
                sendResponse(exchange, 200, publicParams);
                
            } catch (Exception e) {
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Envoie une réponse au client
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    /**
     * Méthode principale pour démarrer le serveur
     */
    public static void main(String[] args) {
        try {
            TrustAuthority trustAuthority = new TrustAuthority();
            TrustAuthorityServer server = new TrustAuthorityServer(trustAuthority, 8080);
            server.start();
            
            System.out.println("Trust Authority Server is running. Press Enter to stop.");
            System.in.read();
            server.stop();
            
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
