package fr.insa.crypto.ui.controllers;

import fr.insa.crypto.MainUI;
import fr.insa.crypto.encryption.IdentityBasedEncryption;
import fr.insa.crypto.mail.SecureAttachmentHandler;
import fr.insa.crypto.trustAuthority.KeyPair;
import fr.insa.crypto.ui.ViewManager;
import fr.insa.crypto.utils.Logger;
import it.unisa.dia.gas.jpbc.Element;
import javafx.application.Platform;
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
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private TextArea subjectArea;
    @FXML
    private Text fromText;
    @FXML
    private Text attachmentStatus;
    @FXML
    private Button attachmentButton;
    @FXML
    private ProgressIndicator downloadProgress;
    @FXML
    private StackPane loadingOverlay;
    @FXML
    private Text dateText;

    private ViewManager viewManager;
    private MainUI mainApp;

    // Current message being displayed
    private Message currentMessage;

    // Date formatter
    private final SimpleDateFormat fullDateFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm");

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
        this.mainApp = mainApp;
        this.currentMessage = message;

        // Set up the quit button action to return to inbox
        quitButton.setOnAction(event -> mainApp.showReceptView());

        // Display message content
        displayMessageContent(userKeyPair, ibeEngine);
    }

    /**
     * Displays the content of the current message
     */
    private void displayMessageContent(KeyPair userKeyPair, IdentityBasedEncryption ibeEngine) {
        if (currentMessage == null) {
            fromText.setText("No sender");
            subjectArea.setText("No subject");
            messageArea.setText("No message selected");
            attachmentStatus.setText("No attachments");
            attachmentButton.setDisable(true);
            dateText.setText("");
            return;
        }

        try {
            // Afficher l'état de chargement
            loadingOverlay.setVisible(true);
            
            // Tâche asynchrone pour charger le contenu de l'email
            Task<Void> loadContentTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        // On utilise Platform.runLater pour mettre à jour l'interface
                        Platform.runLater(() -> {
                            try {
                                // Afficher expéditeur et sujet
                                fromText.setText(currentMessage.getFrom()[0].toString());
                                subjectArea.setText(currentMessage.getSubject() != null ? currentMessage.getSubject() : "(Sans objet)");
                                
                                // Amélioration de l'affichage de la date
                                Date messageDate = null;
                                
                                // Essayer d'obtenir la date de réception en priorité
                                if (currentMessage.getReceivedDate() != null) {
                                    messageDate = currentMessage.getReceivedDate();
                                    Logger.debug("Utilisation de la date de réception de l'email");
                                } 
                                // Si pas de date de réception, utiliser la date d'envoi
                                else if (currentMessage.getSentDate() != null) {
                                    messageDate = currentMessage.getSentDate();
                                    Logger.debug("Utilisation de la date d'envoi de l'email car pas de date de réception");
                                }
                                
                                if (messageDate != null) {
                                    dateText.setText(fullDateFormatter.format(messageDate));
                                } else {
                                    dateText.setText("Date inconnue");
                                    Logger.warning("Aucune date disponible pour ce message");
                                }
                            } catch (Exception e) {
                                Logger.error("Erreur lors de l'affichage des métadonnées: " + e.getMessage());
                            }
                        });

                        // Récupérer le contenu du message
                        Object content = currentMessage.getContent();
                        final String messageContent;
                        final boolean hasAttachment;
                        final List<MimeBodyPart> attachmentParts = new ArrayList<>();

                        if (content instanceof String) {
                            messageContent = (String) content;
                            hasAttachment = false;
                        } else if (content instanceof Multipart) {
                            StringBuilder textContent = new StringBuilder();
                            Multipart multipart = (Multipart) content;
                            boolean foundAttachments = false;
                            
                            for (int i = 0; i < multipart.getCount(); i++) {
                                BodyPart bodyPart = multipart.getBodyPart(i);
                                if (bodyPart.getDisposition() == null || 
                                        bodyPart.getDisposition().equalsIgnoreCase(Part.INLINE)) {
                                    // Contenu du message
                                    Object partContent = bodyPart.getContent();
                                    if (partContent instanceof String) {
                                        textContent.append(partContent).append("\n");
                                    }
                                } else {
                                    // Pièce jointe
                                    foundAttachments = true;
                                    if (bodyPart instanceof MimeBodyPart) {
                                        attachmentParts.add((MimeBodyPart) bodyPart);
                                    }
                                }
                            }
                            
                            messageContent = textContent.toString();
                            hasAttachment = foundAttachments;
                        } else {
                            messageContent = "Contenu non pris en charge: " + content.getClass().getName();
                            hasAttachment = false;
                        }

                        // Mise à jour de l'interface
                        String finalMessageContent = messageContent;
                        boolean finalHasAttachment = hasAttachment;
                        Platform.runLater(() -> {
                            // Afficher le contenu
                            messageArea.setText(finalMessageContent);
                            
                            // Gérer les pièces jointes
                            if (finalHasAttachment) {
                                attachmentStatus.setText("Ce message contient " + 
                                        attachmentParts.size() + " pièce(s) jointe(s)");
                                attachmentButton.setDisable(false);
                                
                                // Configurer le bouton de téléchargement
                                setupAttachmentButton(attachmentParts, userKeyPair, ibeEngine);
                            } else {
                                attachmentStatus.setText("Pas de pièce jointe dans ce message");
                                attachmentButton.setDisable(true);
                            }
                            
                            // Masquer l'overlay de chargement
                            loadingOverlay.setVisible(false);
                        });
                    } catch (Exception e) {
                        // Gérer l'erreur
                        Platform.runLater(() -> {
                            Logger.error("Erreur lors de l'affichage du message: " + e.getMessage());
                            messageArea.setText("Erreur lors de l'affichage du message: " + e.getMessage());
                            loadingOverlay.setVisible(false);
                        });
                    }
                    
                    return null;
                }
            };
            
            // Démarrer la tâche de chargement
            new Thread(loadContentTask).start();
            
        } catch (Exception e) {
            loadingOverlay.setVisible(false);
            Logger.error("Erreur lors de l'affichage du message: " + e.getMessage());
            messageArea.setText("Erreur lors de l'affichage du message: " + e.getMessage());
            viewManager.showErrorAlert("Erreur d'affichage", 
                    "Erreur lors de l'affichage du message: " + e.getMessage());
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
