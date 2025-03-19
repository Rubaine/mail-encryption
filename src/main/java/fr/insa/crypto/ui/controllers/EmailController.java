package fr.insa.crypto.ui.controllers;

import fr.insa.crypto.MainUI;
import fr.insa.crypto.encryption.IdentityBasedEncryption;
import fr.insa.crypto.mail.SecureAttachmentHandler;
import fr.insa.crypto.trustAuthority.KeyPair;
import fr.insa.crypto.ui.ViewManager;
import fr.insa.crypto.utils.Logger;
import it.unisa.dia.gas.jpbc.Element;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the email viewing screen
 */
public class EmailController {
    // UI components
    @FXML
    private Button quitButton;
    @FXML
    private TextArea messageArea;
    @FXML
    private TextArea fromArea;
    @FXML
    private TextArea subjectArea;
    @FXML
    private Text attachmentStatus;
    @FXML
    private Button attachmentButton;
    @FXML
    private ProgressIndicator downloadProgress;
    @FXML
    private StackPane loadingOverlay;

    private ViewManager viewManager;

    // Current message being displayed
    private Message currentMessage;

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
    public void setup(MainUI mainApp, ViewManager viewManager, @SuppressWarnings("exports") Message message,
            KeyPair userKeyPair,
            IdentityBasedEncryption ibeEngine) {
        this.viewManager = viewManager;
        this.currentMessage = message;

        // Set up the quit button action
        quitButton.setOnAction(event -> mainApp.showReceptView());

        // Make text areas non-editable
        messageArea.setEditable(false);
        fromArea.setEditable(false);
        subjectArea.setEditable(false);

        // Display message content
        displayMessageContent(userKeyPair, ibeEngine);
    }

    /**
     * Displays the content of the current message
     */
    private void displayMessageContent(KeyPair userKeyPair, IdentityBasedEncryption ibeEngine) {
        if (currentMessage == null) {
            fromArea.setText("No sender");
            subjectArea.setText("No subject");
            messageArea.setText("No message selected");
            attachmentStatus.setText("No attachments");
            attachmentButton.setDisable(true);
            return;
        }

        try {
            // Display sender and subject
            fromArea.setText(currentMessage.getFrom()[0].toString());
            subjectArea.setText(currentMessage.getSubject() != null ? currentMessage.getSubject() : "(No subject)");

            // Get message content
            Object content = currentMessage.getContent();
            String messageContent = "";
            boolean hasAttachment = false;
            List<MimeBodyPart> attachmentParts = new ArrayList<>();

            if (content instanceof String) {
                messageContent = (String) content;
            } else if (content instanceof Multipart) {
                Multipart multipart = (Multipart) content;
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (bodyPart.getDisposition() == null) {
                        // This is likely the message content
                        Object partContent = bodyPart.getContent();
                        if (partContent instanceof String) {
                            messageContent = (String) partContent;
                        }
                    } else {
                        // This is an attachment
                        hasAttachment = true;
                        if (bodyPart instanceof MimeBodyPart) {
                            attachmentParts.add((MimeBodyPart) bodyPart);
                        }
                    }
                }
            }

            // Display message content
            messageArea.setText(messageContent);

            // Handle attachments
            if (hasAttachment) {
                attachmentStatus.setText("This message contains attachments (encrypted)");
                attachmentButton.setDisable(false);
                attachmentButton.setText("Download & Decrypt");

                // Set up attachment button action
                setupAttachmentButton(attachmentParts, userKeyPair, ibeEngine);
            } else {
                attachmentStatus.setText("No attachments in this message");
                attachmentButton.setDisable(true);
            }

        } catch (Exception e) {
            Logger.error("Error displaying message: " + e.getMessage());
            messageArea.setText("Error displaying message: " + e.getMessage());
            viewManager.showErrorAlert("Display Error", "Error displaying message: " + e.getMessage());
        }
    }

    /**
     * Sets up the attachment button action
     */
    private void setupAttachmentButton(List<MimeBodyPart> attachmentParts, KeyPair userKeyPair,
            IdentityBasedEncryption ibeEngine) {
        attachmentButton.setOnAction(event -> {
            try {
                // Disable button and show progress indicator
                attachmentButton.setDisable(true);
                downloadProgress.setVisible(true);

                // Choose download directory
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Choose download folder");
                File directory = directoryChooser.showDialog(attachmentButton.getScene().getWindow());

                if (directory != null) {
                    // Download task
                    Task<Integer> downloadTask = new Task<Integer>() {
                        @Override
                        protected Integer call() throws Exception {
                            // Get and save attachments
                            int attachmentsSaved = 0;

                            for (MimeBodyPart bodyPart : attachmentParts) {
                                // Get filename
                                String fileName = bodyPart.getFileName();
                                File tempFile = File.createTempFile("temp_attachment_", fileName);

                                // Save attachment to temp file
                                bodyPart.saveFile(tempFile);
                                attachmentsSaved++;

                                // Check if it's an IBE encrypted file
                                if (SecureAttachmentHandler.isIBEEncryptedFile(tempFile)) {
                                    // It's an encrypted file, decrypt it
                                    Element privateKey = userKeyPair.getSk();
                                    File decryptedFile = SecureAttachmentHandler.decryptFile(
                                            tempFile, directory.getAbsolutePath(), privateKey,
                                            ibeEngine);

                                    Logger.info("Attachment decrypted: " + decryptedFile.getName());

                                    // Delete temp file
                                    tempFile.delete();
                                } else {
                                    // It's a non-encrypted file, copy to target directory
                                    File targetFile = new File(directory, fileName);
                                    java.nio.file.Files.copy(
                                            tempFile.toPath(), targetFile.toPath(),
                                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                                    // Delete temp file
                                    tempFile.delete();
                                }
                            }
                            return attachmentsSaved;
                        }
                    };

                    downloadTask.setOnSucceeded(e -> {
                        attachmentButton.setDisable(false);
                        downloadProgress.setVisible(false);

                        int attachmentsSaved = downloadTask.getValue();
                        viewManager.showInfoAlert("Attachments Downloaded",
                                attachmentsSaved + " attachment(s) downloaded to " + directory.getAbsolutePath());
                    });

                    downloadTask.setOnFailed(e -> {
                        attachmentButton.setDisable(false);
                        downloadProgress.setVisible(false);
                        viewManager.showErrorAlert("Download Error",
                                "Error downloading attachments: " + downloadTask.getException().getMessage());
                    });

                    new Thread(downloadTask).start();
                } else {
                    // User canceled directory selection
                    attachmentButton.setDisable(false);
                    downloadProgress.setVisible(false);
                }
            } catch (Exception e) {
                Logger.error("Error downloading attachments: " + e.getMessage());
                viewManager.showErrorAlert("Download Error",
                        "Error downloading attachments: " + e.getMessage());
                attachmentButton.setDisable(false);
                downloadProgress.setVisible(false);
            }
        });
    }
}
