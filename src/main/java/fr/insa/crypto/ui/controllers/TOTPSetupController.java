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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * Contrôleur pour la configuration de l'authentification TOTP (Google Authenticator)
 */
public class TOTPSetupController {
    @FXML
    private ImageView qrCodeImageView;
    @FXML
    private Text manualCodeText;
    @FXML
    private TextField totpField;
    @FXML
    private Button confirmButton;
    @FXML
    private StackPane loadingOverlay;
    @FXML
    private Label loadingText;

    private MainUI mainApp;
    private ViewManager viewManager;
    private TrustAuthorityClient trustClient;
    private String email;
    private String qrCodeUri;
    private String totpSecret;

    /**
     * Initialise le contrôleur après le chargement du FXML
     */
    @FXML
    private void initialize() {
        // Configuration des actions sur les boutons
        confirmButton.setOnAction(event -> verifyTotpCode());
    }

    /**
     * Configure le contrôleur avec les références nécessaires
     */
    public void setup(MainUI mainApp, ViewManager viewManager, TrustAuthorityClient trustClient, 
                    String email, String qrCodeUri) {
        this.mainApp = mainApp;
        this.viewManager = viewManager;
        this.trustClient = trustClient;
        this.email = email;
        this.qrCodeUri = qrCodeUri;
        
        // Afficher le QR code
        loadQrCode();
        
        // Récupérer et afficher le secret TOTP
        this.totpSecret = trustClient.getTotpSecret();
        if (this.totpSecret != null) {
            manualCodeText.setText(this.totpSecret);
        } else {
            manualCodeText.setText("(Secret non disponible)");
        }
    }

    /**
     * Charge et affiche le QR code
     */
    private void loadQrCode() {
        try {
            if (qrCodeUri != null && qrCodeUri.startsWith("data:image/png;base64,")) {
                String base64Image = qrCodeUri.substring("data:image/png;base64,".length());
                byte[] imageData = Base64.getDecoder().decode(base64Image);
                Image qrCodeImage = new Image(new ByteArrayInputStream(imageData));
                qrCodeImageView.setImage(qrCodeImage);
            }
        } catch (Exception e) {
            Logger.error("Erreur lors du chargement du QR code: " + e.getMessage());
            qrCodeImageView.setImage(null);
        }
    }

    /**
     * Vérifie le code TOTP entré
     */
    private void verifyTotpCode() {
        String code = totpField.getText().trim();
        
        if (code.isEmpty()) {
            viewManager.showErrorAlert("Code manquant", "Veuillez entrer le code généré par Google Authenticator.");
            return;
        }
        
        showLoading("Vérification du code...");
        
        Task<Boolean> verifyTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return trustClient.verifyTOTP(email, code);
            }
        };
        
        verifyTask.setOnSucceeded(e -> {
            hideLoading();
            boolean isValid = verifyTask.getValue();
            
            if (isValid) {
                viewManager.showInfoAlert("Configuration réussie", 
                        "Votre compte est maintenant protégé par l'authentification à deux facteurs.");
                mainApp.completeAuthentication(email, code);
            } else {
                viewManager.showErrorAlert("Code incorrect", 
                        "Le code entré est incorrect. Veuillez vérifier l'heure de votre téléphone et réessayer.");
                totpField.clear();
                totpField.requestFocus();
            }
        });
        
        verifyTask.setOnFailed(e -> {
            hideLoading();
            Logger.error("Erreur lors de la vérification TOTP: " + verifyTask.getException().getMessage());
            viewManager.showErrorAlert("Erreur de vérification", 
                    "Impossible de vérifier le code: " + verifyTask.getException().getMessage());
        });
        
        new Thread(verifyTask).start();
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
