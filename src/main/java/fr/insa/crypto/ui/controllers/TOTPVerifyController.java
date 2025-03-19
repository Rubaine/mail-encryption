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
import javafx.scene.text.Text;

/**
 * Contrôleur pour la vérification du code TOTP (Google Authenticator)
 */
public class TOTPVerifyController {
    @FXML
    private Text emailLabel;
    @FXML
    private TextField totpCodeField;
    @FXML
    private Button verifyButton;
    @FXML
    private Button registerButton;
    @FXML
    private StackPane loadingOverlay;
    @FXML
    private Label loadingText;

    private MainUI mainApp;
    private ViewManager viewManager;
    private TrustAuthorityClient trustClient;
    private String email;

    /**
     * Initialise le contrôleur après le chargement du FXML
     */
    @FXML
    private void initialize() {
        // Configuration des actions sur les boutons
        verifyButton.setOnAction(event -> verifyTotpCode());
        registerButton.setOnAction(event -> showRegistrationScreen());
    }

    /**
     * Configure le contrôleur avec les références nécessaires
     */
    public void setup(MainUI mainApp, ViewManager viewManager, TrustAuthorityClient trustClient, String email) {
        this.mainApp = mainApp;
        this.viewManager = viewManager;
        this.trustClient = trustClient;
        this.email = email;
        
        if (emailLabel != null) {
            emailLabel.setText(email);
        }
    }

    /**
     * Vérifie le code TOTP saisi
     */
    private void verifyTotpCode() {
        String totpCode = totpCodeField.getText().trim();

        if (totpCode.isEmpty()) {
            viewManager.showErrorAlert("Code manquant", 
                    "Veuillez saisir le code généré par Google Authenticator.");
            return;
        }

        showLoading("Vérification du code...");

        Task<Boolean> verifyTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return trustClient.verifyTOTP(email, totpCode);
            }
        };

        verifyTask.setOnSucceeded(e -> {
            hideLoading();
            boolean isValid = verifyTask.getValue();

            if (isValid) {
                // Code valide, poursuivre l'authentification
                mainApp.completeAuthentication(email, totpCode);
            } else {
                viewManager.showErrorAlert("Code incorrect", 
                        "Le code saisi est incorrect. Veuillez réessayer.");
                totpCodeField.clear();
                totpCodeField.requestFocus();
            }
        });

        verifyTask.setOnFailed(e -> {
            hideLoading();
            Logger.error("Erreur de vérification: " + verifyTask.getException().getMessage());
            viewManager.showErrorAlert("Erreur de vérification",
                    "Impossible de vérifier le code: " + verifyTask.getException().getMessage());
        });

        new Thread(verifyTask).start();
    }

    /**
     * Affiche l'écran d'enregistrement
     */
    private void showRegistrationScreen() {
        mainApp.showRegistration(email);
    }
    
    /**
     * Affiche l'overlay de chargement
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
