package fr.insa.crypto.trustAuthority;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.samstevens.totp.exceptions.QrGenerationException;
import fr.insa.crypto.trustAuthority.auth.OTPManager;
import fr.insa.crypto.trustAuthority.auth.TOTPManager;
import fr.insa.crypto.trustAuthority.user.UserAccount;
import fr.insa.crypto.trustAuthority.user.UserManager;
import fr.insa.crypto.utils.Config;
import fr.insa.crypto.utils.Logger;
import org.json.JSONObject;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Serveur HTTP pour l'autorité de confiance
 */
public class TrustAuthorityServer {
    private final TrustAuthority trustAuthority;
    private final int port;
    private HttpServer server;

    // Gestionnaires pour l'authentification 2FA
    private final UserManager userManager = new UserManager();
    private final OTPManager otpManager = new OTPManager();
    private final TOTPManager totpManager = new TOTPManager();

    // Session email pour l'envoi des OTP
    private Session emailSession;
    private String senderEmail;

    public TrustAuthorityServer(TrustAuthority trustAuthority, int port) {
        this.trustAuthority = trustAuthority;
        this.port = port;

        // Initialiser la session email pour l'envoi d'OTP sans utiliser la classe Authentication
        try {
            // Récupérer les identifiants email depuis les variables d'environnement
            senderEmail = System.getenv("EMAIL_USERNAME");
            String password = System.getenv("EMAIL_PASSWORD");

            if (senderEmail == null || password == null) {
                Logger.warning("Variables d'environnement EMAIL_USERNAME ou EMAIL_PASSWORD non définies. L'envoi d'OTP sera désactivé.");
            } else {
                // Créer une session email directe pour l'envoi des OTP
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", System.getenv("SMTP_HOST") != null ? System.getenv("SMTP_HOST") : "smtp.gmail.com");
                props.put("mail.smtp.port", System.getenv("SMTP_PORT") != null ? System.getenv("SMTP_PORT") : "587");
                props.put("mail.smtp.ssl.trust", "*");
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");
                props.put("mail.smtp.connectiontimeout", "30000");
                props.put("mail.smtp.timeout", "30000");
                props.put("mail.smtp.writetimeout", "30000");

                final String finalPassword = password;

                emailSession = Session.getInstance(props, new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(senderEmail, finalPassword);
                    }
                });

                Logger.info("Session email initialisée pour l'envoi d'OTP avec " + senderEmail);
            }
        } catch (Exception e) {
            Logger.error("Erreur lors de l'initialisation de la session email: " + e.getMessage());
        }
    }

    /**
     * Démarrage du serveur HTTP
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Endpoints existants
        server.createContext("/get-private-key", new SecurePrivateKeyHandler());
        server.createContext("/public-parameters", new PublicParametersHandler());

        // Nouveaux endpoints pour l'authentification 2FA
        server.createContext("/auth/register", new RegistrationRequestHandler());
        server.createContext("/auth/verify-otp", new VerifyOtpHandler());
        server.createContext("/auth/verify-totp", new VerifyTotpHandler());
        server.createContext("/auth/check-account", new CheckAccountHandler());

        server.setExecutor(null); // Utilise l'exécuteur par défaut
        server.start();

        Logger.info("Trust Authority Server started on port " + port);
    }

    /**
     * Arrêt du serveur
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            Logger.info("Trust Authority Server stopped");
        }
    }

    /**
     * Vérifie si un utilisateur est authentifié avant de distribuer une clé privée
     *
     * @param email    Adresse email de l'utilisateur
     * @param totpCode Code TOTP (Google Authenticator)
     * @return true si l'utilisateur est authentifié
     */
    private boolean isUserAuthenticated(String email, String totpCode) {
        if (email == null || totpCode == null) {
            return false;
        }

        UserAccount account = userManager.getUser(email);
        if (account == null || !account.isVerified()) {
            return false;
        }

        return totpManager.verifyCode(totpCode, account.getTotpSecret());
    }

    /**
     * Envoie un email contenant un code OTP
     *
     * @param email Adresse email destinataire
     * @param otp   Code OTP
     * @return true si l'envoi a réussi
     */
    private boolean sendOtpEmail(String email, String otp) {
        try {
            if (emailSession != null && senderEmail != null) {
                // Créer le message directement sans utiliser MailSender
                Message message = new MimeMessage(emailSession);
                message.setFrom(new InternetAddress(senderEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                message.setSubject("Votre code de vérification Messenger Secure");

                // Corps du message
                String body =
                        "<html><body style='font-family: Arial, sans-serif;'>" +
                                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0;'>" +
                                "<h2 style='color: #4285f4;'>Messenger Secure</h2>" +
                                "<p>Voici votre code de vérification pour créer votre compte Messenger Secure :</p>" +
                                "<div style='background-color: #f5f5f5; padding: 15px; text-align: center; margin: 20px 0;'>" +
                                "<h1 style='color: #4285f4; letter-spacing: 5px;'>" + otp + "</h1>" +
                                "</div>" +
                                "<p>Ce code est valable pendant 5 minutes.</p>" +
                                "<p>Si vous n'avez pas demandé ce code, veuillez ignorer cet email.</p>" +
                                "<p>Cordialement,<br>L'équipe Messenger Secure</p>" +
                                "</div></body></html>";

                message.setContent(body, "text/html; charset=utf-8");

                // Envoyer le message
                Transport.send(message);
                Logger.info("Email OTP envoyé avec succès à " + email);
                return true;
            } else {
                Logger.error("Impossible d'envoyer l'email OTP: session email non initialisée");
                return false;
            }
        } catch (Exception e) {
            Logger.error("Erreur lors de l'envoi de l'email OTP à " + email + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Handler sécurisé pour la distribution des clés privées (avec authentification 2FA)
     */
    private class SecurePrivateKeyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                // Lire les données de la requête
                String requestBody = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                // Déterminer si c'est une demande JSON (nouvelle méthode) ou texte brut (ancienne méthode)
                String email;
                boolean isSecureRequest = false;

                try {
                    JSONObject jsonRequest = new JSONObject(requestBody);
                    email = jsonRequest.getString("email");
                    String totpCode = jsonRequest.getString("totpCode");
                    isSecureRequest = true;

                    // Vérifier l'authentification
                    if (!isUserAuthenticated(email, totpCode)) {
                        sendResponse(exchange, 401, "Unauthorized: Invalid authentication");
                        return;
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 400, "Bad Request: Invalid JSON format or missing fields");
                    return;
                }


                // Distribution de la clé privée
                KeyPair privateKey = trustAuthority.getKeyDistributor().distributePrivateKey(email);

                // Sérialisation de la clé privée pour la transmission
                String response = String.format("{\"identity\":\"%s\",\"privateKey\":\"%s\"}",
                        privateKey.getPk(),
                        Base64.getEncoder().encodeToString(privateKey.getSk().toBytes())
                );

                sendResponse(exchange, 200, response);

            } catch (Exception e) {
                Logger.error("Erreur lors de la distribution de clé privée: " + e.getMessage());
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
    }

    /**
     * Handler pour l'enregistrement d'un compte (demande initiale)
     */
    private class RegistrationRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                // Lire l'email dans le corps de la requête
                String email = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                // Vérifier le format de l'email
                if (!Config.isValidEmail(email)) {
                    sendResponse(exchange, 400, "Invalid email format");
                    return;
                }

                // Vérifier si l'utilisateur existe déjà et est vérifié
                if (userManager.isUserVerified(email)) {
                    sendResponse(exchange, 400, "User already registered and verified");
                    return;
                }

                // Créer ou récupérer le compte utilisateur
                UserAccount account = userManager.createOrGetUser(email);

                // Générer un OTP
                String otp = otpManager.generateOtp();
                account.storePendingOtp(otp, otpManager.getDefaultExpirationSeconds());

                // Envoyer l'OTP par email
                boolean emailSent = sendOtpEmail(email, otp);

                if (emailSent) {
                    sendResponse(exchange, 200, "OTP sent successfully");
                } else {
                    sendResponse(exchange, 500, "Failed to send OTP email");
                }

            } catch (Exception e) {
                Logger.error("Erreur lors de la demande d'enregistrement: " + e.getMessage());
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
    }

    /**
     * Handler pour la vérification de l'OTP et la configuration du TOTP
     */
    private class VerifyOtpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                // Lire les données de la requête
                String requestBody = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                // Analyser les données JSON
                JSONObject jsonRequest = new JSONObject(requestBody);
                String email = jsonRequest.getString("email");
                String otp = jsonRequest.getString("otp");

                // Récupérer le compte utilisateur
                UserAccount account = userManager.getUser(email);
                if (account == null) {
                    sendResponse(exchange, 404, "User not found");
                    return;
                }

                // Vérifier l'OTP
                if (!account.validateOtp(otp)) {
                    sendResponse(exchange, 401, "Invalid or expired OTP");
                    return;
                }

                // Générer un secret TOTP pour Google Authenticator
                String totpSecret = totpManager.generateSecret();

                // Générer un QR code pour configurer Google Authenticator
                String qrCodeUri = totpManager.generateQrCodeUri(email, totpSecret);

                // Enregistrer le secret TOTP dans le compte utilisateur
                userManager.verifyUserAndSetTotpSecret(email, totpSecret);

                // Préparer la réponse JSON
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("status", "success");
                jsonResponse.put("message", "OTP verified successfully");
                jsonResponse.put("totpSecret", totpSecret);
                jsonResponse.put("qrCodeUri", qrCodeUri);

                sendResponse(exchange, 200, jsonResponse.toString());

            } catch (QrGenerationException e) {
                Logger.error("Erreur lors de la génération du QR code: " + e.getMessage());
                sendResponse(exchange, 500, "Failed to generate QR code");
            } catch (Exception e) {
                Logger.error("Erreur lors de la vérification OTP: " + e.getMessage());
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
    }

    /**
     * Handler pour la vérification du code TOTP
     */
    private class VerifyTotpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                // Lire les données de la requête
                String requestBody = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                // Analyser les données JSON
                JSONObject jsonRequest = new JSONObject(requestBody);
                String email = jsonRequest.getString("email");
                String totpCode = jsonRequest.getString("totpCode");

                // Vérifier l'authentification
                boolean isAuthenticated = isUserAuthenticated(email, totpCode);

                // Préparer la réponse JSON
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("authenticated", isAuthenticated);

                if (isAuthenticated) {
                    sendResponse(exchange, 200, jsonResponse.toString());
                } else {
                    sendResponse(exchange, 401, jsonResponse.toString());
                }

            } catch (Exception e) {
                Logger.error("Erreur lors de la vérification TOTP: " + e.getMessage());
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
    }

    /**
     * Handler pour vérifier l'existence d'un compte
     */
    private class CheckAccountHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                // Lire l'email dans le corps de la requête
                String email = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                // Vérifier si l'utilisateur existe et est vérifié
                boolean exists = userManager.isUserRegistered(email);
                boolean verified = userManager.isUserVerified(email);

                // Préparer la réponse JSON
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("exists", exists);
                jsonResponse.put("verified", verified);

                sendResponse(exchange, 200, jsonResponse.toString());

            } catch (Exception e) {
                Logger.error("Erreur lors de la vérification du compte: " + e.getMessage());
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
    }

    /**
     * Handler pour l'obtention des paramètres publics (inchangé)
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
                Logger.error("Erreur lors de la récupération des paramètres publics: " + e.getMessage());
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
    }

    /**
     * Envoie une réponse au client
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
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
            // Récupérer le port depuis les variables d'environnement ou utiliser 8080 par défaut
            int port = fr.insa.crypto.utils.Config.TRUST_AUTHORITY_PORT;

            Logger.info("Démarrage du serveur d'autorité de confiance sur le port " + port);
            TrustAuthority trustAuthority = new TrustAuthority();
            TrustAuthorityServer server = new TrustAuthorityServer(trustAuthority, port);
            server.start();

            // En mode conteneurisé, nous voulons que le serveur continue à s'exécuter
            Logger.info("Serveur démarré avec succès. Appuyez sur Ctrl+C pour arrêter.");

            // Ajouter un hook d'arrêt pour une fermeture propre
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Logger.info("Arrêt du serveur...");
                server.stop();
                Logger.info("Serveur arrêté.");
            }));

            // Garder le thread principal en vie
            Thread.currentThread().join();

        } catch (Exception e) {
            Logger.error("Erreur lors du démarrage du serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
