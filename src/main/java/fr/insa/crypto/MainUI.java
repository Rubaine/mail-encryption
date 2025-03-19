package fr.insa.crypto;

import fr.insa.crypto.encryption.IdentityBasedEncryption;
import fr.insa.crypto.mail.Authentication;
import fr.insa.crypto.mail.MailReceiver;
import fr.insa.crypto.trustAuthority.KeyPair;
import fr.insa.crypto.trustAuthority.TrustAuthorityClient;
import fr.insa.crypto.ui.ViewManager;
import fr.insa.crypto.ui.controllers.AuthController;
import fr.insa.crypto.ui.controllers.EmailController;
import fr.insa.crypto.ui.controllers.PortalController;
import fr.insa.crypto.ui.controllers.ReceptController;
import fr.insa.crypto.ui.controllers.SendController;
import fr.insa.crypto.utils.Config;
import fr.insa.crypto.utils.Logger;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.stage.Stage;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;

/**
 * Main UI class for the Messenger Secure application.
 * Controls the flow between different screens and handles business logic.
 */
public class MainUI extends Application {

    // Core components
    private ViewManager viewManager;
    private TrustAuthorityClient trustClient;

    // Authentication state
    private String currentEmail;
    private String currentPassword;
    // Mail components
    private Authentication auth;
    private Session mailSession;
    private MailReceiver mailReceiver;

    // Cryptography components
    private IdentityBasedEncryption ibeEngine;
    private KeyPair userKeyPair;

    @Override
    public void start(@SuppressWarnings("exports") Stage primaryStage) throws IOException {
        // Initialize ViewManager with primary stage
        viewManager = new ViewManager(primaryStage, this);

        // Initialize trust authority client
        trustClient = new TrustAuthorityClient(Config.TRUST_AUTHORITY_URL);

        // Show login portal
        showLoginPortal();
    }

    /**
     * Shows the login portal screen
     */
    public void showLoginPortal() {
        PortalController controller = viewManager.showView(ViewManager.VIEW_PORTAL);
        controller.setup(this, viewManager);
    }

    /**
     * Sets user credentials after initial authentication
     */
    public void setCredentials(String email, String password) {
        this.currentEmail = email;
        this.currentPassword = password;
    }

    /**
     * Proceeds to 2FA authentication
     */
    public void proceedTo2FAAuthentication(String email) {
        AuthController controller = viewManager.showView(ViewManager.VIEW_AUTH);
        controller.setup(this, viewManager, trustClient);
        controller.startAuthProcess(email);
    }

    /**
     * Called when 2FA authentication is complete
     */
    public void completeAuthentication(String email, String totpCode) {
        Logger.info("Starting cryptographic key retrieval for " + email);

        // Task for retrieving cryptographic keys
        Task<Boolean> keyTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    Logger.debug("Requesting private key via TrustAuthorityClient for " + email);

                    // Retrieve cryptographic elements with 2FA
                    userKeyPair = trustClient.requestPrivateKey(email, totpCode);

                    if (userKeyPair == null) {
                        Logger.error("Retrieved key pair is null");
                        throw new Exception("Retrieved key pair is null");
                    }

                    // Initialize IBE engine with retrieved parameters
                    ibeEngine = new IdentityBasedEncryption(trustClient.getParameters());

                    // Initialize mail session
                    auth = new Authentication(currentEmail, currentPassword);
                    mailSession = auth.getAuthenticatedSession();

                    // Initialize mail receiver
                    mailReceiver = new MailReceiver();
                    mailReceiver.connect(currentEmail, currentPassword);

                    return true;
                } catch (Exception e) {
                    Logger.error("Exception retrieving keys: " + e);
                    e.printStackTrace();
                    throw e;
                }
            }
        };

        keyTask.setOnSucceeded(e -> {
            Logger.info("Key retrieval task completed successfully");

            try {
                // Authentication complete, show inbox
                showReceptView();
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.error("Exception showing reception screen: " + ex);
                viewManager.showErrorAlert("Error", "Error showing inbox: " + ex.getMessage());
            }
        });

        keyTask.setOnFailed(e -> {
            Throwable exception = keyTask.getException();
            Logger.error("Key retrieval task failed: " + exception);

            if (exception != null) {
                exception.printStackTrace();
                viewManager.showErrorAlert("Authentication Error",
                        "Failed to retrieve cryptographic keys: " + exception.getMessage());
            } else {
                viewManager.showErrorAlert("Authentication Error",
                        "An unknown error occurred during key retrieval.");
            }

            // Return to login screen
            showLoginPortal();
        });

        // Start key retrieval thread
        Logger.debug("Starting key retrieval thread");
        Thread keyThread = new Thread(keyTask);
        keyThread.setName("KeyRetrievalThread");
        keyThread.setDaemon(true);
        keyThread.start();
    }

    /**
     * Shows the reception/inbox view
     */
    public void showReceptView() {
        ReceptController controller = viewManager.showView(ViewManager.VIEW_RECEPT);
        controller.setup(this, viewManager, currentEmail, mailReceiver);
    }

    /**
     * Shows the email composition view
     */
    public void showSendView() {
        SendController controller = viewManager.showView(ViewManager.VIEW_SEND);
        controller.setup(this, viewManager, mailSession, userKeyPair, ibeEngine);
    }

    /**
     * Shows the email detail view
     */
    public void showEmailView(@SuppressWarnings("exports") Message message) {
        EmailController controller = viewManager.showView(ViewManager.VIEW_EMAIL);
        controller.setup(this, viewManager, message, userKeyPair, ibeEngine);
    }

    /**
     * Logs out the current user
     */
    public void logout() {
        // Close mail connections
        if (mailReceiver != null) {
            try {
                mailReceiver.close();
            } catch (MessagingException e) {
                Logger.error("Error during logout (closing mail receiver): " + e.getMessage());
            }
        }

        if (auth != null) {
            auth.logout();
        }

        // Clear state
        currentEmail = null;
        currentPassword = null;
        auth = null;
        mailSession = null;
        mailReceiver = null;
        userKeyPair = null;
        ibeEngine = null;

        // Show login portal
        showLoginPortal();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
