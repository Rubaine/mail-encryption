package fr.insa.crypto.ui.controllers;

import fr.insa.crypto.MainUI;
import fr.insa.crypto.trustAuthority.TrustAuthorityClient;
import fr.insa.crypto.ui.ViewManager;
import fr.insa.crypto.utils.Logger;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

/**
 * Contrôleur pour la page d'enregistrement d'un nouveau compte
 */
public class RegisterController {
    @FXML
    private TextField emailField;
    @FXML
    private Button sendOtpButton;
    @FXML
    private Button backToLoginButton;
    @FXML
    private StackPane loadingOverlay;
    @FXML
    private Label loadingText;

    private MainUI mainApp;
    private ViewManager viewManager;
    private TrustAuthorityClient trustClient;

    /**
     * Initialise le contrôleur après le chargement du FXML
     */
    @FXML
    private void initialize() {
        // Configuration des actions sur les boutons
        sendOtpButton.setOnAction(event -> sendOtp());
        backToLoginButton.setOnAction(event -> goBack());
    }

    /**
     * Configurer le contrôleur avec les références nécessaires
     */
    public void setup(MainUI mainApp, ViewManager viewManager, TrustAuthorityClient trustClient, String prefilledEmail) {
        this.mainApp = mainApp;
        this.viewManager = viewManager;
        this.trustClient = trustClient;
        
        // Pré-remplir l'email s'il est fourni
        if (prefilledEmail != null && !prefilledEmail.isEmpty()) {
            emailField.setText(prefilledEmail);
        }
    }

    /**
     * Envoyer un code OTP à l'adresse email pour l'enregistrement
     */
    private void sendOtp() {
        String email = emailField.getText().trim();
        
        // Validation de l'email
        if (email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            viewManager.showErrorAlert("Email invalide", "Veuillez entrer une adresse email valide.");
            return;
        }

        // Afficher l'overlay de chargement
        showLoading("Envoi du code de vérification...");

        Task<Boolean> otpTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return trustClient.requestRegistration(email);
            }
        };

        otpTask.setOnSucceeded(e -> {
            hideLoading();
            boolean success = otpTask.getValue();

            if (success) {
                // Rediriger vers l'écran de vérification OTP
                mainApp.showOtpVerification(email);
            } else {
                viewManager.showErrorAlert("Erreur d'envoi", 
                        "Impossible d'envoyer le code de vérification. Veuillez réessayer.");
            }
        });

        otpTask.setOnFailed(e -> {
            hideLoading();
            Logger.error("Erreur lors de l'envoi de l'OTP: " + otpTask.getException().getMessage());
            viewManager.showErrorAlert("Erreur d'envoi",
                    "Impossible d'envoyer le code de vérification: " + otpTask.getException().getMessage());
        });

        new Thread(otpTask).start();
    }

    /**
     * Retour à l'écran précédent
     */
    private void goBack() {
        mainApp.showTotpVerification(null);
    }

    /**
     * Affiche l'overlay de chargement avec un message
     */
    private void showLoading(String message) {
        if (loadingText != null) {
            loadingText.setText(message);
        }
        loadingOverlay.setVisible(true);
    }

    /**
     * Cache l'overlay de chargement
     */
    private void hideLoading() {
        loadingOverlay.setVisible(false);
    }
}
