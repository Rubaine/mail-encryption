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
import javafx.application.Application;
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
            // First step: authenticate with email and password
            currentAuth = new Authentication(email, password);
            
            // Initialize mail receiver
            currentMailReceiver = new MailReceiver();
            currentMailReceiver.connect(email, password);
            
            // Start 2FA process
            startAuthProcess(email);
        } catch (Exception e) {
            Logger.error("Login failed: " + e.getMessage());
            viewManager.showErrorAlert("Login Failed", "Authentication error: " + e.getMessage());
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
            // Finalize authentication with 2FA code
            if (currentAuth != null) {
                currentAuth.completeAuthentication(totpCode);
            } else {
                throw new Exception("Authentication session expired");
            }
            
            // Get encryption keys and engine
            userKeyPair = trustClient.requestPrivateKey(email, totpCode);
            ibeEngine = new IdentityBasedEncryption(trustClient.getParameters());
            
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
