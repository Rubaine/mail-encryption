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
 * Contrôleur pour la vérification du code OTP envoyé par email
 */
public class OTPVerifyController {
    @FXML
    private Text infoText;
    @FXML
    private TextField otpField;
    @FXML
    private Button verifyButton;
    @FXML
    private Button resendButton;
    @FXML
    private Button backButton;
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
        verifyButton.setOnAction(event -> verifyOtp());
        resendButton.setOnAction(event -> resendOtp());
        backButton.setOnAction(event -> goBack());
    }

    /**
     * Configure le contrôleur avec les références nécessaires
     */
    public void setup(MainUI mainApp, ViewManager viewManager, TrustAuthorityClient trustClient, String email) {
        this.mainApp = mainApp;
        this.viewManager = viewManager;
        this.trustClient = trustClient;
        this.email = email;
        
        // Mise à jour du texte d'info
        infoText.setText("Veuillez saisir le code de vérification envoyé à " + email);
    }

    /**
     * Vérifie le code OTP entré par l'utilisateur
     */
    private void verifyOtp() {
        String otp = otpField.getText().trim();
        
        if (otp.isEmpty()) {
            viewManager.showErrorAlert("Code manquant", "Veuillez entrer le code de vérification reçu par email.");
            return;
        }
        
        showLoading("Vérification du code...");
        
        Task<String> verifyTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return trustClient.verifyOtpAndSetupTOTP(email, otp);
            }
        };
        
        verifyTask.setOnSucceeded(e -> {
            hideLoading();
            String qrCodeUri = verifyTask.getValue();
            
            if (qrCodeUri != null) {
                // Succès, passer à la configuration TOTP
                mainApp.showTotpSetup(email, qrCodeUri);
            } else {
                viewManager.showErrorAlert("Code incorrect", 
                        "Le code de vérification est incorrect ou expiré. Veuillez réessayer.");
                otpField.clear();
                otpField.requestFocus();
            }
        });
        
        verifyTask.setOnFailed(e -> {
            hideLoading();
            Logger.error("Erreur lors de la vérification OTP: " + verifyTask.getException().getMessage());
            viewManager.showErrorAlert("Erreur de vérification", 
                    "Impossible de vérifier le code: " + verifyTask.getException().getMessage());
        });
        
        new Thread(verifyTask).start();
    }
    
    /**
     * Renvoie le code OTP
     */
    private void resendOtp() {
        showLoading("Envoi d'un nouveau code...");
        
        Task<Boolean> resendTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return trustClient.requestRegistration(email);
            }
        };
        
        resendTask.setOnSucceeded(e -> {
            hideLoading();
            boolean success = resendTask.getValue();
            
            if (success) {
                viewManager.showInfoAlert("Code envoyé", 
                        "Un nouveau code de vérification a été envoyé à " + email);
                otpField.clear();
                otpField.requestFocus();
            } else {
                viewManager.showErrorAlert("Échec de l'envoi", 
                        "Impossible d'envoyer un nouveau code. Veuillez réessayer plus tard.");
            }
        });
        
        resendTask.setOnFailed(e -> {
            hideLoading();
            Logger.error("Erreur lors du renvoi de l'OTP: " + resendTask.getException().getMessage());
            viewManager.showErrorAlert("Erreur d'envoi", 
                    "Impossible d'envoyer un nouveau code: " + resendTask.getException().getMessage());
        });
        
        new Thread(resendTask).start();
    }
    
    /**
     * Retourne à l'écran précédent
     */
    private void goBack() {
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
