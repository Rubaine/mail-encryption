package fr.insa.crypto;

import fr.insa.crypto.encryption.IdentityBasedEncryption;
import fr.insa.crypto.mail.Authentication;
import fr.insa.crypto.mail.MailReceiver;
import fr.insa.crypto.trustAuthority.KeyPair;
import fr.insa.crypto.trustAuthority.TrustAuthorityClient;
import fr.insa.crypto.ui.ViewManager;
import fr.insa.crypto.ui.controllers.*;
import fr.insa.crypto.utils.Config;
import fr.insa.crypto.utils.Logger;
import fr.insa.crypto.utils.SessionManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import javax.mail.Message;

/**
 * Main application entry point
 */
public class MainUI extends Application {
    private ViewManager viewManager;
    private TrustAuthorityClient trustClient;
    
    // Current session state
    private Authentication currentAuth;
    private MailReceiver currentMailReceiver;
    private KeyPair userKeyPair;
    private IdentityBasedEncryption ibeEngine;
    private Message currentMessage;
    
    // Gestionnaire de session
    private final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize UI view manager
            viewManager = new ViewManager(primaryStage, this);
            
            // Connect to trust authority
            connectToTrustAuthority();
            
            // Show login screen
            showLoginScreen();
        } catch (Exception e) {
            Logger.error("Application initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize connection to the trust authority
     */
    private void connectToTrustAuthority() {
        try {
            trustClient = new TrustAuthorityClient(Config.TRUST_AUTHORITY_URL);
            
            // Établir proactivement le canal sécurisé
            try {
                boolean success = trustClient.establishSecureChannel();
                if (success) {
                    Logger.info("Canal sécurisé établi avec l'autorité de confiance");
                } else {
                    Logger.warning("Impossible d'établir un canal sécurisé, les communications ne seront pas chiffrées");
                }
            } catch (Exception e) {
                Logger.warning("Échec de l'établissement du canal sécurisé: " + e.getMessage());
            }
            
            Logger.info("Connected to trust authority at " + Config.TRUST_AUTHORITY_URL);
        } catch (Exception e) {
            Logger.error("Failed to connect to trust authority: " + e.getMessage());
            viewManager.showErrorAlert("Connection Error", 
                "Unable to connect to trust authority: " + e.getMessage() + 
                "\nThis may cause issues with encryption features.");
        }
    }

    /**
     * Shows the login screen
     */
    public void showLoginScreen() {
        // Show portal view
        PortalController controller = viewManager.showView(ViewManager.VIEW_PORTAL);
        controller.setup(this, viewManager);
    }

    /**
     * Login method - initiates the authentication process
     */
    public void login(String email, String password) {
        try {
            // Store credentials in SessionManager
            sessionManager.storeCredentials(email, password);
            Logger.info("Identifiants stockés dans le SessionManager pour " + email);
            
            // First step: authenticate with email and password
            currentAuth = new Authentication(email, password);
            
            // Store auth session in SessionManager
            sessionManager.storeAuthSession(currentAuth);
            Logger.info("Session d'authentification stockée dans le SessionManager");
            
            // Initialize mail receiver
            currentMailReceiver = new MailReceiver();
            currentMailReceiver.connect(email, password);
            Logger.info("MailReceiver connecté pour " + email);
            
            // Start 2FA process - Ensure this happens on the JavaFX thread
            Platform.runLater(() -> {
                startAuthProcess(email);
            });
        } catch (Exception e) {
            Logger.error("Login failed: " + e.getMessage());
            Platform.runLater(() -> {
                viewManager.showErrorAlert("Login Failed", "Authentication error: " + e.getMessage());
            });
        }
    }

    /**
     * Starts the 2FA authentication process
     */
    public void startAuthProcess(String email) {
        // Show the TOTP verification screen directly
        showTotpVerification(email);
    }

    /**
     * Shows the TOTP verification screen
     */
    public void showTotpVerification(String email) {
        TOTPVerifyController controller = viewManager.showView(ViewManager.VIEW_TOTP_VERIFY);
        controller.setup(this, viewManager, trustClient, email);
    }

    /**
     * Shows the registration screen
     */
    public void showRegistration(String email) {
        RegisterController controller = viewManager.showView(ViewManager.VIEW_REGISTER);
        controller.setup(this, viewManager, trustClient, email);
    }

    /**
     * Shows the OTP verification screen
     */
    public void showOtpVerification(String email) {
        OTPVerifyController controller = viewManager.showView(ViewManager.VIEW_OTP_VERIFY);
        controller.setup(this, viewManager, trustClient, email);
    }

    /**
     * Shows the TOTP setup screen
     */
    public void showTotpSetup(String email, String qrCodeUri) {
        TOTPSetupController controller = viewManager.showView(ViewManager.VIEW_TOTP_SETUP);
        controller.setup(this, viewManager, trustClient, email, qrCodeUri);
    }

    /**
     * Complete authentication after successful 2FA
     */
    public void completeAuthentication(String email, String totpCode) {
        try {
            // Log authentication state
            Logger.info("Tentative de complétion de l'authentification pour: " + email);
            
            // Check if we have an auth session, otherwise try to recover from SessionManager
            if (currentAuth == null) {
                currentAuth = sessionManager.getAuthSession();
                Logger.info("État currentAuth depuis SessionManager: " + (currentAuth != null ? "récupéré" : "null"));
            }
            
            // If still null, try to recreate using stored credentials
            if (currentAuth == null) {
                Logger.warning("Session expirée, tentative de reconnexion automatique via SessionManager...");
                
                if (sessionManager.hasCredentialsFor(email)) {
                    try {
                        // Recreate authentication
                        String storedPassword = sessionManager.getPassword();
                        currentAuth = new Authentication(email, storedPassword);
                        Logger.info("Réauthentification réussie pour " + email);
                        
                        // Reconnect mail receiver if needed
                        if (currentMailReceiver == null) {
                            currentMailReceiver = new MailReceiver();
                            currentMailReceiver.connect(email, storedPassword);
                            Logger.info("MailReceiver reconnecté pour " + email);
                        }
                    } catch (Exception e) {
                        Logger.error("Échec de reconnexion automatique: " + e.getMessage());
                        viewManager.showErrorAlert("Session expirée", 
                            "Impossible de rétablir votre session. Veuillez vous reconnecter.");
                        showLoginScreen();
                        return;
                    }
                } else {
                    Logger.error("Identifiants non disponibles dans SessionManager pour la reconnexion");
                    viewManager.showInfoAlert("Authentification incomplète", 
                        "La session d'authentification a expiré. Veuillez vous reconnecter.");
                    showLoginScreen();
                    return;
                }
            }
            
            // Finalize authentication with 2FA code
            Logger.info("Appel de completeAuthentication sur currentAuth pour " + email);
            currentAuth.completeAuthentication(totpCode);
            
            // Get encryption keys and engine
            Logger.info("Demande de clé privée au serveur d'autorité...");
            userKeyPair = trustClient.requestPrivateKey(email, totpCode);
            Logger.info("Clé privée obtenue pour " + email);
            
            ibeEngine = new IdentityBasedEncryption(trustClient.getParameters());
            Logger.info("Moteur de chiffrement IBE initialisé");
            
            // Show main inbox screen
            showReceptView();
        } catch (Exception e) {
            Logger.error("Error completing authentication: " + e.getMessage());
            viewManager.showErrorAlert("Authentication Error", 
                    "Failed to complete authentication: " + e.getMessage());
        }
    }

    /**
     * Shows the inbox/reception screen
     */
    public void showReceptView() {
        if (currentAuth == null || currentMailReceiver == null) {
            viewManager.showErrorAlert("Not Logged In", "You need to log in first");
            showLoginScreen();
            return;
        }
        
        ReceptController controller = viewManager.showView(ViewManager.VIEW_RECEPT);
        controller.setup(this, viewManager, currentAuth.getEmail(), currentMailReceiver);
    }

    /**
     * Shows the send email screen
     */
    public void showSendView() {
        if (currentAuth == null) {
            viewManager.showErrorAlert("Not Logged In", "You need to log in first");
            showLoginScreen();
            return;
        }
        
        SendController controller = viewManager.showView(ViewManager.VIEW_SEND);
        controller.setup(this, viewManager, currentAuth.getAuthenticatedSession(), userKeyPair, ibeEngine);
    }
    
    /**
     * Shows the email viewing screen
     */
    public void showEmailView(Message message) {
        if (currentAuth == null) {
            viewManager.showErrorAlert("Not Logged In", "You need to log in first");
            showLoginScreen();
            return;
        }
        
        currentMessage = message;
        EmailController controller = viewManager.showView(ViewManager.VIEW_EMAIL);
        controller.setup(this, viewManager, message, userKeyPair, ibeEngine);
    }
    
    /**
     * Logs out the current user
     */
    public void logout() {
        // Clean up resources
        if (currentAuth != null) {
            currentAuth.logout();
            currentAuth = null;
        }
        
        if (currentMailReceiver != null) {
            try {
                currentMailReceiver.close();
            } catch (Exception e) {
                Logger.error("Error closing mail connection: " + e.getMessage());
            }
            currentMailReceiver = null;
        }
        
        // Reset state
        userKeyPair = null;
        ibeEngine = null;
        currentMessage = null;
        
        // Clear session manager
        sessionManager.clearSession();
        
        // Show login screen
        showLoginScreen();
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        launch(args);
    }
}
