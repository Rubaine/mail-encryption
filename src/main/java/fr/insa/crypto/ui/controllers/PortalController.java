package fr.insa.crypto.ui.controllers;

import fr.insa.crypto.MainUI;
import fr.insa.crypto.ui.ViewManager;
import fr.insa.crypto.utils.Logger;
import fr.insa.crypto.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * Controller for the login/portal screen
 */
public class PortalController {
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private HBox progressContainer;
    @FXML
    private ProgressIndicator loginProgress;
    @FXML
    private StackPane loadingOverlay;

    private MainUI mainApp;
    private ViewManager viewManager;
    private final SessionManager sessionManager = SessionManager.getInstance();

    /**
     * Initializes the controller
     */
    @FXML
    private void initialize() {
        // Set up login button action
        loginButton.setOnAction(event -> handleLogin());
    }

    /**
     * Set up with main application reference
     */
    public void setup(MainUI mainApp, ViewManager viewManager) {
        this.mainApp = mainApp;
        this.viewManager = viewManager;
        
        // Check if we have stored credentials to auto-fill
        if (sessionManager.getEmail() != null) {
            emailField.setText(sessionManager.getEmail());
            Logger.info("Email pré-rempli depuis SessionManager: " + sessionManager.getEmail());
        }
    }

    /**
     * Handles the login button action
     */
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validate inputs
        if (email.isEmpty()) {
            viewManager.showErrorAlert("Email Missing", "Please enter your email address.");
            return;
        }

        if (password.isEmpty()) {
            viewManager.showErrorAlert("Password Missing", "Please enter your password.");
            return;
        }

        // Show loading indicators
        loginButton.setDisable(true);
        progressContainer.setVisible(true);
        loadingOverlay.setVisible(true);

        // Use a JavaFX Service to handle background tasks properly
        javafx.concurrent.Service<Void> loginService = new javafx.concurrent.Service<Void>() {
            @Override
            protected javafx.concurrent.Task<Void> createTask() {
                return new javafx.concurrent.Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        try {
                            // Try to login (this now properly handles UI updates)
                            mainApp.login(email, password);
                        } catch (Exception e) {
                            throw e; // Propagate exception to be handled in onFailed
                        }
                        return null;
                    }
                };
            }
        };
        
        // Handle completion
        loginService.setOnSucceeded(event -> {
            loginButton.setDisable(false);
            progressContainer.setVisible(false);
            passwordField.clear();
            loadingOverlay.setVisible(false);
        });
        
        // Handle errors
        loginService.setOnFailed(event -> {
            loginButton.setDisable(false);
            progressContainer.setVisible(false);
            passwordField.clear();
            loadingOverlay.setVisible(false);
            
            Throwable exception = loginService.getException();
            viewManager.showErrorAlert("Login Failed", 
                    "Authentication error: " + (exception != null ? exception.getMessage() : "Unknown error"));
        });
        
        // Start the service
        loginService.start();
    }
}
