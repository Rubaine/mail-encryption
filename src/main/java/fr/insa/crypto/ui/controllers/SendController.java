package fr.insa.crypto.ui.controllers;

import fr.insa.crypto.MainUI;
import fr.insa.crypto.encryption.IdentityBasedEncryption;
import fr.insa.crypto.mail.MailSender;
import fr.insa.crypto.mail.SecureAttachmentHandler;
import fr.insa.crypto.trustAuthority.KeyPair;
import fr.insa.crypto.ui.ViewManager;
import fr.insa.crypto.utils.Config;
import fr.insa.crypto.utils.Logger;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import javax.mail.Session;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the message composition screen
 */
public class SendController {
    // UI components
    @FXML
    private Button quitButton;
    @FXML
    private TextField toField;
    @FXML
    private TextField subjectField;
    @FXML
    private TextArea messageArea;
    @FXML
    private Text attachmentStatus;
    @FXML
    private Button sendButton;
    @FXML
    private Button attachButton;
    @FXML
    private ProgressIndicator sendProgress;
    @FXML
    private ProgressIndicator attachProgress;
    @FXML
    private StackPane loadingOverlay;

    // References to main application and view manager
    private MainUI mainApp;
    private ViewManager viewManager;

    // State
    private List<File> attachments = new ArrayList<>();
    private Session mailSession;
    private KeyPair userKeyPair;
    private IdentityBasedEncryption ibeEngine;

    /**
     * Initializes the controller after FXML is loaded
     */
    @FXML
    private void initialize() {
        // Will be set up in setup method
    }

    /**
     * Sets up the controller with necessary references and data
     */
    public void setup(MainUI mainApp, ViewManager viewManager, @SuppressWarnings("exports") Session mailSession,
            KeyPair userKeyPair, IdentityBasedEncryption ibeEngine) {
        this.mainApp = mainApp;
        this.viewManager = viewManager;
        this.mailSession = mailSession;
        this.userKeyPair = userKeyPair;
        this.ibeEngine = ibeEngine;

        // Clear attachments list
        attachments.clear();
        updateAttachmentStatus();

        // Set up button actions
        quitButton.setOnAction(event -> mainApp.showReceptView());
        sendButton.setOnAction(event -> handleSend());
        attachButton.setOnAction(event -> handleAttachment());
    }

    /**
     * Handles the send button click
     */
    private void handleSend() {
        String to = toField.getText();
        String subject = subjectField.getText();
        String body = messageArea.getText();

        // Input validation
        if (to == null || to.trim().isEmpty()) {
            viewManager.showErrorAlert("Missing Information", "Recipient email address cannot be empty");
            return;
        }

        // Check email format
        if (!Config.isValidEmail(to)) {
            viewManager.showErrorAlert("Invalid Email", "Invalid email address format: " + to);
            return;
        }

        if (subject.isEmpty() || body.isEmpty()) {
            viewManager.showErrorAlert("Missing Information", "Please fill all required fields");
            return;
        }

        // Show loading indicator
        sendButton.setDisable(true);
        sendProgress.setVisible(true);

        // Background task for sending email
        Task<Boolean> sendTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    if (attachments.isEmpty()) {
                        // Send simple email
                        MailSender.sendEmail(mailSession, to, subject, body);
                    } else {
                        // Create secure attachment handler with encryption
                        SecureAttachmentHandler secureHandler = new SecureAttachmentHandler(ibeEngine, userKeyPair);

                        // Add each attachment with automatic encryption
                        for (File file : attachments) {
                            try {
                                // Encrypt file for recipient
                                secureHandler.addEncryptedAttachment(file.getAbsolutePath(), to);
                                Logger.info("Attachment encrypted: " + file.getName());
                            } catch (Exception e) {
                                Logger.error("Error encrypting attachment " + file.getName() + ": " + e.getMessage());
                                throw new Exception(
                                        "Error encrypting attachment " + file.getName() + ": " + e.getMessage());
                            }
                        }

                        // Send email with encrypted attachments
                        MailSender.sendEmailWithAttachments(mailSession, to, subject,
                                body + "\n\nThis email contains encrypted attachments.", secureHandler);
                    }
                    return true;
                } catch (Exception e) {
                    Logger.error("Error sending message: " + e.getMessage());
                    throw e;
                }
            }
        };

        sendTask.setOnSucceeded(e -> {
            sendButton.setDisable(false);
            sendProgress.setVisible(false);

            if (sendTask.getValue()) {
                viewManager.showInfoAlert("Message Sent", "Your message has been sent successfully.");
                mainApp.showReceptView();
            }
        });

        sendTask.setOnFailed(e -> {
            sendButton.setDisable(false);
            sendProgress.setVisible(false);
            viewManager.showErrorAlert("Send Error", "Error sending message: " + sendTask.getException().getMessage());
        });

        new Thread(sendTask).start();
    }

    /**
     * Handles attachment button click
     */
    private void handleAttachment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Attachment");
        File file = fileChooser.showOpenDialog(attachButton.getScene().getWindow());

        if (file != null) {
            // Show loading indicator
            attachButton.setDisable(true);
            attachProgress.setVisible(true);

            // Background task for processing attachment
            Task<Void> attachTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    // Simulate processing for large files
                    if (file.length() > 1000000) { // 1MB
                        Thread.sleep(1000); // Wait 1 second for large files
                    }
                    return null;
                }
            };

            attachTask.setOnSucceeded(e -> {
                attachButton.setDisable(false);
                attachProgress.setVisible(false);

                // Add file to attachments list
                attachments.add(file);
                updateAttachmentStatus();
            });

            attachTask.setOnFailed(e -> {
                attachButton.setDisable(false);
                attachProgress.setVisible(false);
                viewManager.showErrorAlert("Attachment Error",
                        "Error adding attachment: " + attachTask.getException().getMessage());
            });

            new Thread(attachTask).start();
        }
    }

    /**
     * Updates the attachment status display
     */
    private void updateAttachmentStatus() {
        attachmentStatus.setText("Number of attachments: " + attachments.size() + " (will be encrypted automatically)");
    }
}
