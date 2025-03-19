package fr.insa.crypto;

import fr.insa.crypto.trustAuthority.AccountStatus;
import fr.insa.crypto.trustAuthority.TrustAuthorityClient;
import fr.insa.crypto.utils.Logger;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Contrôleur pour l'écran d'authentification et d'enregistrement 2FA
 */
public class AuthController {
    // Référence à l'application principale
    private MainUI mainApp;
    
    // Client pour communiquer avec l'autorité de confiance
    private TrustAuthorityClient trustClient;
    
    // État courant
    private String currentEmail;
    private String totpSecret;
    
    // Composants UI
    @FXML private VBox mainPanel;
    @FXML private VBox registerPanel;
    @FXML private VBox otpPanel;
    @FXML private VBox totpSetupPanel;
    @FXML private StackPane loadingOverlay;
    
    @FXML private Label emailDisplay;
    @FXML private TextField totpCodeField;
    @FXML private Button verifyTotpButton;
    @FXML private Button registerButton;
    @FXML private Text mainInfoText;
    
    @FXML private TextField registerEmailField;
    @FXML private Button sendOtpButton;
    @FXML private Button backToLoginButton;
    
    @FXML private Text otpInfoText;
    @FXML private TextField otpField;
    @FXML private Button verifyOtpButton;
    @FXML private Button resendOtpButton;
    
    @FXML private ImageView qrCodeImageView;
    @FXML private Text manualCodeText;
    @FXML private TextField setupTotpField;
    @FXML private Button confirmSetupButton;
    
    @FXML private Label loadingText;
    
    /**
     * Initialise le contrôleur après que FXML est chargé
     */
    @FXML
    private void initialize() {
        // Boutons du panel principal
        verifyTotpButton.setOnAction(event -> verifyTotpCode());
        registerButton.setOnAction(event -> showRegisterPanel());
        
        // Boutons du panel d'enregistrement
        sendOtpButton.setOnAction(event -> sendOtp());
        backToLoginButton.setOnAction(event -> showMainPanel());
        
        // Boutons du panel OTP
        verifyOtpButton.setOnAction(event -> verifyOtp());
        resendOtpButton.setOnAction(event -> sendOtp());
        
        // Boutons du panel de configuration TOTP
        confirmSetupButton.setOnAction(event -> confirmTotpSetup());
    }
    
    /**
     * Définit l'application principale et configure le contrôleur
     * @param mainApp Référence à l'application principale
     * @param trustClient Client d'autorité de confiance
     */
    public void setMainApp(MainUI mainApp, TrustAuthorityClient trustClient) {
        this.mainApp = mainApp;
        this.trustClient = trustClient;
    }
    
    /**
     * Lance le processus d'authentification pour un utilisateur
     * @param email Email de l'utilisateur
     */
    public void startAuthProcess(String email) {
        currentEmail = email;
        
        // Vérifier si l'utilisateur a un compte
        showLoading("Vérification du compte...");
        
        Task<AccountStatus> checkTask = new Task<AccountStatus>() {
            @Override
            protected AccountStatus call() throws Exception {
                return trustClient.checkAccountStatus(email);
            }
        };
        
        checkTask.setOnSucceeded(e -> {
            hideLoading();
            AccountStatus status = checkTask.getValue();
            
            if (status.exists() && status.isVerified()) {
                // L'utilisateur a un compte vérifié, afficher l'écran d'authentification TOTP
                showTotpVerificationPanel(email);
            } else {
                // L'utilisateur n'a pas de compte ou n'est pas vérifié, afficher l'écran d'enregistrement
                showRegisterPanel();
            }
        });
        
        checkTask.setOnFailed(e -> {
            hideLoading();
            Logger.error("Erreur lors de la vérification du compte: " + checkTask.getException().getMessage());
            mainApp.showErrorAlert("Erreur lors de la vérification du compte", 
                    "Impossible de vérifier le statut du compte: " + checkTask.getException().getMessage());
            showRegisterPanel(); // Par défaut, proposer la création de compte
        });
        
        new Thread(checkTask).start();
    }
    
    /**
     * Vérifie le code TOTP saisi par l'utilisateur
     */
    private void verifyTotpCode() {
        String totpCode = totpCodeField.getText().trim();
        
        if (totpCode.isEmpty()) {
            mainApp.showErrorAlert("Code manquant", "Veuillez saisir le code généré par Google Authenticator.");
            return;
        }
        
        showLoading("Vérification du code...");
        
        Task<Boolean> verifyTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return trustClient.verifyTOTP(currentEmail, totpCode);
            }
        };
        
        verifyTask.setOnSucceeded(e -> {
            hideLoading();
            boolean isValid = verifyTask.getValue();
            
            if (isValid) {
                // Code valide, continuer avec le processus d'authentification principal
                mainApp.completeAuthentication(currentEmail, totpCode);
            } else {
                mainApp.showErrorAlert("Code incorrect", "Le code saisi est incorrect. Veuillez réessayer.");
                totpCodeField.clear();
                totpCodeField.requestFocus();
            }
        });
        
        verifyTask.setOnFailed(e -> {
            hideLoading();
            Logger.error("Erreur lors de la vérification du code: " + verifyTask.getException().getMessage());
            mainApp.showErrorAlert("Erreur de vérification", 
                    "Impossible de vérifier le code: " + verifyTask.getException().getMessage());
        });
        
        new Thread(verifyTask).start();
    }
    
    /**
     * Envoie un code OTP à l'adresse email pour l'enregistrement
     */
    private void sendOtp() {
        String email = currentEmail;
        
        // Si nous sommes dans le panel d'enregistrement, utiliser l'email saisi
        if (registerPanel.isVisible()) {
            email = registerEmailField.getText().trim();
            if (email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                mainApp.showErrorAlert("Email invalide", "Veuillez saisir une adresse email valide.");
                return;
            }
            currentEmail = email;
        }
        
        final String finalEmail = email;
        showLoading("Envoi du code de vérification...");
        
        Task<Boolean> otpTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return trustClient.requestRegistration(finalEmail);
            }
        };
        
        otpTask.setOnSucceeded(e -> {
            hideLoading();
            boolean success = otpTask.getValue();
            
            if (success) {
                // Afficher le panel de saisie du code OTP
                showOtpVerificationPanel(finalEmail);
            } else {
                mainApp.showErrorAlert("Erreur d'envoi", 
                        "Impossible d'envoyer le code de vérification. Veuillez réessayer.");
            }
        });
        
        otpTask.setOnFailed(e -> {
            hideLoading();
            Logger.error("Erreur lors de l'envoi du code OTP: " + otpTask.getException().getMessage());
            mainApp.showErrorAlert("Erreur d'envoi", 
                    "Impossible d'envoyer le code de vérification: " + otpTask.getException().getMessage());
        });
        
        new Thread(otpTask).start();
    }
    
    /**
     * Vérifie le code OTP saisi par l'utilisateur
     */
    private void verifyOtp() {
        String otp = otpField.getText().trim();
        
        if (otp.isEmpty()) {
            mainApp.showErrorAlert("Code manquant", "Veuillez saisir le code de vérification reçu par email.");
            return;
        }
        
        showLoading("Vérification du code...");
        
        Task<String> verifyTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return trustClient.verifyOtpAndSetupTOTP(currentEmail, otp);
            }
        };
        
        verifyTask.setOnSucceeded(e -> {
            hideLoading();
            String qrCodeUri = verifyTask.getValue();
            
            if (qrCodeUri != null) {
                // Afficher le QR code pour Google Authenticator
                showTotpSetupPanel(qrCodeUri);
            } else {
                mainApp.showErrorAlert("Code incorrect", 
                        "Le code de vérification est incorrect ou expiré. Veuillez réessayer.");
                otpField.clear();
                otpField.requestFocus();
            }
        });
        
        verifyTask.setOnFailed(e -> {
            hideLoading();
            Logger.error("Erreur lors de la vérification du code OTP: " + verifyTask.getException().getMessage());
            mainApp.showErrorAlert("Erreur de vérification", 
                    "Impossible de vérifier le code: " + verifyTask.getException().getMessage());
        });
        
        new Thread(verifyTask).start();
    }
    
    /**
     * Confirme la configuration du code TOTP (Google Authenticator)
     */
    private void confirmTotpSetup() {
        String code = setupTotpField.getText().trim();
        
        if (code.isEmpty()) {
            mainApp.showErrorAlert("Code manquant", "Veuillez saisir le code généré par Google Authenticator.");
            return;
        }
        
        showLoading("Vérification du code...");
        
        Task<Boolean> verifyTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return trustClient.verifyTOTP(currentEmail, code);
            }
        };
        
        verifyTask.setOnSucceeded(e -> {
            hideLoading();
            boolean isValid = verifyTask.getValue();
            
            if (isValid) {
                mainApp.showInfoAlert("Configuration réussie", 
                        "Votre compte a été configuré avec succès. Vous pouvez maintenant vous connecter.");
                // Revenir à l'écran d'authentification TOTP
                showTotpVerificationPanel(currentEmail);
            } else {
                mainApp.showErrorAlert("Code incorrect", "Le code saisi est incorrect. Veuillez réessayer.");
                setupTotpField.clear();
                setupTotpField.requestFocus();
            }
        });
        
        verifyTask.setOnFailed(e -> {
            hideLoading();
            Logger.error("Erreur lors de la vérification du code TOTP: " + verifyTask.getException().getMessage());
            mainApp.showErrorAlert("Erreur de vérification", 
                    "Impossible de vérifier le code: " + verifyTask.getException().getMessage());
        });
        
        new Thread(verifyTask).start();
    }
    
    /**
     * Affiche le panel principal avec la vérification TOTP
     */
    private void showTotpVerificationPanel(String email) {
        mainPanel.setVisible(true);
        registerPanel.setVisible(false);
        otpPanel.setVisible(false);
        totpSetupPanel.setVisible(false);
        
        emailDisplay.setText(email);
        mainInfoText.setText("Veuillez saisir le code à 6 chiffres généré par Google Authenticator");
        totpCodeField.clear();
        totpCodeField.requestFocus();
    }
    
    /**
     * Affiche le panel d'enregistrement (étape 1)
     */
    private void showRegisterPanel() {
        mainPanel.setVisible(false);
        registerPanel.setVisible(true);
        otpPanel.setVisible(false);
        totpSetupPanel.setVisible(false);
        
        registerEmailField.setText(currentEmail);
        registerEmailField.requestFocus();
    }
    
    /**
     * Affiche le panel de vérification OTP (étape 2)
     */
    private void showOtpVerificationPanel(String email) {
        mainPanel.setVisible(false);
        registerPanel.setVisible(false);
        otpPanel.setVisible(true);
        totpSetupPanel.setVisible(false);
        
        otpInfoText.setText("Veuillez saisir le code de vérification envoyé à " + email);
        otpField.clear();
        otpField.requestFocus();
    }
    
    /**
     * Affiche le panel de configuration TOTP (étape 3)
     */
    private void showTotpSetupPanel(String qrCodeUri) {
        mainPanel.setVisible(false);
        registerPanel.setVisible(false);
        otpPanel.setVisible(false);
        totpSetupPanel.setVisible(true);
        
        // Extraire le secret TOTP depuis l'URI (au format data:image/png;base64,...)
        if (qrCodeUri.startsWith("data:image/png;base64,")) {
            String base64Image = qrCodeUri.substring("data:image/png;base64,".length());
            try {
                byte[] imageData = Base64.getDecoder().decode(base64Image);
                Image qrCodeImage = new Image(new ByteArrayInputStream(imageData));
                qrCodeImageView.setImage(qrCodeImage);
                
                // Récupérer le secret TOTP depuis le client
                this.totpSecret = trustClient.getTotpSecret();
                if (this.totpSecret != null) {
                    manualCodeText.setText(this.totpSecret);
                } else {
                    manualCodeText.setText("(Secret non disponible)");
                }
            } catch (Exception ex) {
                Logger.error("Erreur lors du décodage du QR code: " + ex.getMessage());
                qrCodeImageView.setImage(null);
                manualCodeText.setText("(Erreur de décodage)");
            }
        } else {
            Logger.error("Format de QR code non supporté: " + qrCodeUri);
        }
        
        setupTotpField.clear();
        setupTotpField.requestFocus();
    }
    
    /**
     * Affiche le panel principal
     */
    private void showMainPanel() {
        mainPanel.setVisible(true);
        registerPanel.setVisible(false);
        otpPanel.setVisible(false);
        totpSetupPanel.setVisible(false);
    }
    
    /**
     * Affiche l'overlay de chargement avec un message
     */
    private void showLoading(String message) {
        loadingText.setText(message);
        loadingOverlay.setVisible(true);
    }
    
    /**
     * Masque l'overlay de chargement
     */
    private void hideLoading() {
        loadingOverlay.setVisible(false);
    }
}
