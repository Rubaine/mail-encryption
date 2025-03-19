package fr.insa.crypto.ui.controllers;

import fr.insa.crypto.MainUI;
import fr.insa.crypto.mail.Authentication;
import fr.insa.crypto.mail.MailReceiver;
import fr.insa.crypto.ui.ViewManager;
import fr.insa.crypto.utils.Logger;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * Controller for the login/portal screen
 */
public class PortalController {
    // UI components
    @FXML
    private TextField emailField;
    @FXML
    private TextField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private ProgressIndicator loginProgress;
    @FXML
    private HBox progressContainer;
    @FXML
    private StackPane loadingOverlay;

    private MainUI mainApp;
    private ViewManager viewManager;

    /**
     * Initializes the controller after FXML is loaded
     */
    @FXML
    private void initialize() {
        // No action yet, setup is needed first
    }

    /**
     * Sets up the controller with necessary references
     */
    public void setup(MainUI mainApp, ViewManager viewManager) {
        this.mainApp = mainApp;
        this.viewManager = viewManager;

        // Set up login button action
        loginButton.setOnAction(event -> handleLogin());
    }

    /**
     * Handles login button click
     */
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            viewManager.showErrorAlert("Missing Information", "Please enter your email and password");
            return;
        }

        // Show loading indicator
        loginButton.setDisable(true);
        if (progressContainer != null) {
            progressContainer.setVisible(true);
        }

        // Background task for authentication
        Task<Boolean> loginTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    // Step 1: Traditional SMTP/IMAP authentication
                    Authentication auth = new Authentication(email, password);
                    auth.getAuthenticatedSession();

                    // Test connection by creating mail receiver
                    MailReceiver mailReceiver = new MailReceiver();
                    mailReceiver.connect(email, password);

                    // Success, store credentials in main app
                    // mainApp.setCredentials(email, password);

                    return true;
                } catch (Exception e) {
                    Logger.error("Authentication error: " + e.getMessage());
                    return false;
                }
            }
        };

        loginTask.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            if (progressContainer != null) {
                progressContainer.setVisible(false);
            }

            if (loginTask.getValue()) {
                // SMTP/IMAP authentication successful
                // Continue with 2FA via Google Authenticator
                mainApp.showTotpVerification(email);
            } else {
                // Login failed
                viewManager.showErrorAlert("Authentication Failed",
                        "Incorrect credentials or server connection problem");
            }
        });

        loginTask.setOnFailed(e -> {
            loginButton.setDisable(false);
            if (progressContainer != null) {
                progressContainer.setVisible(false);
            }
            viewManager.showErrorAlert("Authentication Error",
                    "Authentication failed: " + loginTask.getException().getMessage());
        });

        new Thread(loginTask).start();
    }
}
