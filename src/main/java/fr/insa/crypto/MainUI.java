package fr.insa.crypto;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import javax.mail.Session;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.BodyPart;
import javax.mail.internet.MimeBodyPart;

// Import des classes de gestion d'emails
import fr.insa.crypto.mail.Authentication;
import fr.insa.crypto.mail.MailSender;
import fr.insa.crypto.mail.MailReceiver;
import fr.insa.crypto.mail.AttachmentHandler;
import fr.insa.crypto.utils.Logger;

public class MainUI extends Application {

    private Stage primaryStage;
    private String currentEmail;
    private String currentAppKey;
    private Authentication auth;
    private MailReceiver mailReceiver;
    private Message[] messages;
    private Message currentMessage;
    private File currentAttachment;
    private List<File> attachments = new ArrayList<>();

    @Override
    public void start(@SuppressWarnings("exports") Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        showPortal();
    }

    private void showPortal() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("portal.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        TextField emailField = (TextField) scene.lookup("#emailField");
        TextField passwordField = (TextField) scene.lookup("#passwordField");
        Button loginButton = (Button) scene.lookup("#loginButton");

        loginButton.setOnAction(event -> {
            String email = emailField.getText();
            String password = passwordField.getText();
            
            // Utiliser l'authentification réelle
            try {
                auth = new Authentication(email, password);
                Session session = auth.getAuthenticatedSession();
                
                // Tester la connexion en créant un récepteur d'emails
                mailReceiver = new MailReceiver();
                mailReceiver.connect(email, password);
                
                currentEmail = email;
                currentAppKey = password;
                
                // Connexion réussie
                showRecept(email);
                
            } catch (Exception e) {
                Logger.error("Erreur d'authentification: " + e.getMessage());
                showErrorAlert("Erreur d'authentification: " + e.getMessage());
            }
        });

        adaptWindowSize(false);
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showRecept(String email) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("recept.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        Button newMessageButton = (Button) scene.lookup("#newMessageButton");
        newMessageButton.setOnAction(event -> {
            try {
                showSend();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Button logoutButton = (Button) scene.lookup("#logoutButton");
        logoutButton.setOnAction(event -> {
            try {
                // Déconnexion
                if (mailReceiver != null) {
                    try {
                        mailReceiver.close();
                    } catch (MessagingException e) {
                        Logger.error("Erreur lors de la déconnexion: " + e.getMessage());
                    }
                }
                if (auth != null) {
                    auth.logout();
                }
                showPortal();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Button refreshButton = (Button) scene.lookup("#refreshButton");
        refreshButton.setOnAction(event -> {
            // Rafraîchir les messages avec MailReceiver
            try {
                if (mailReceiver == null) {
                    mailReceiver = new MailReceiver();
                    mailReceiver.connect(currentEmail, currentAppKey);
                }
                
                mailReceiver.openFolder("INBOX", true);
                messages = mailReceiver.getMessages();
                
                // Afficher les messages dans l'interface
                updateEmailButtons(scene, messages);
                
                Logger.info("Messages rafraîchis: " + (messages != null ? messages.length : 0) + " messages trouvés");
            } catch (Exception e) {
                Logger.error("Erreur lors de la récupération des messages: " + e.getMessage());
                showErrorAlert("Erreur lors de la récupération des messages: " + e.getMessage());
            }
        });

        // Déclencher un rafraîchissement initial des messages
        refreshButton.fire();

        // Afficher l'email connecté
        Text connectedText = (Text) scene.lookup("#connectedText");
        connectedText.setText("Connecté à : " + email);

        adaptWindowSize(true);
    }

    private void updateEmailButtons(Scene scene, Message[] messages) {
        // Mettre à jour les boutons d'email avec les messages réels
        for (int i = 0; i < 7; i++) {
            Button emailButton = (Button) scene.lookup("#emailButton" + i);
            if (emailButton != null) {
                if (messages != null && i < messages.length) {
                    try {
                        // Configurer le bouton avec les informations du message
                        Message msg = messages[i];
                        String subject = msg.getSubject();
                        String from = msg.getFrom()[0].toString();
                        
                        emailButton.setText(subject + " - De: " + from);
                        emailButton.setDisable(false);
                        
                        // Configurer l'action du bouton pour afficher ce message spécifique
                        final int messageIndex = i;
                        emailButton.setOnAction(event -> {
                            try {
                                currentMessage = messages[messageIndex];
                                showEmail();
                            } catch (Exception e) {
                                Logger.error("Erreur lors de l'affichage du message: " + e.getMessage());
                                showErrorAlert("Erreur lors de l'affichage du message: " + e.getMessage());
                            }
                        });
                    } catch (MessagingException e) {
                        Logger.error("Erreur lors de la lecture du message: " + e.getMessage());
                        emailButton.setText("Erreur de lecture");
                        emailButton.setDisable(true);
                    }
                } else {
                    // Pas de message à cet index
                    emailButton.setText("Pas de message");
                    emailButton.setDisable(true);
                }
            }
        }
    }

    private void showSend() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("send.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Récupérer les références des champs
        TextField toField = (TextField) scene.lookup("#toField");
        TextField subjectField = (TextField) scene.lookup("#subjectField");
        TextArea messageArea = (TextArea) scene.lookup("#messageArea");
        Text attachmentStatus = (Text) scene.lookup("#attachmentStatus");

        if (toField == null || subjectField == null || messageArea == null || attachmentStatus == null) {
            Logger.error("Certains champs sont introuvables dans l'interface : " +
                        "toField=" + (toField != null) + ", " +
                        "subjectField=" + (subjectField != null) + ", " +
                        "messageArea=" + (messageArea != null) + ", " +
                        "attachmentStatus=" + (attachmentStatus != null));
            showErrorAlert("Erreur d'interface utilisateur. Veuillez contacter le développeur.");
            return;
        }
        
        // Réinitialiser la liste des pièces jointes
        attachments.clear();
        updateAttachmentStatus(attachmentStatus);

        Button quitButton = (Button) scene.lookup("#quitButton");
        quitButton.setOnAction(event -> {
            try {
                showRecept(currentEmail);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Button sendButton = (Button) scene.lookup("#sendButton");
        sendButton.setOnAction(event -> {
            String to = toField.getText();
            String subject = subjectField.getText();
            String body = messageArea.getText();
            
            if (to.isEmpty() || subject.isEmpty() || body.isEmpty()) {
                showErrorAlert("Veuillez remplir tous les champs obligatoires");
                return;
            }
            
            try {
                Session session = auth.getAuthenticatedSession();
                
                if (attachments.isEmpty()) {
                    // Envoyer un email simple
                    MailSender.sendEmail(session, to, subject, body);
                } else {
                    // Envoyer un email avec pièces jointes
                    AttachmentHandler attachmentHandler = new AttachmentHandler();
                    
                    for (File file : attachments) {
                        attachmentHandler.addAttachment(file.getAbsolutePath());
                    }
                    
                    MailSender.sendEmailWithAttachments(session, to, subject, body, attachmentHandler);
                }
                
                showInfoAlert("Message envoyé", "Votre message a été envoyé avec succès.");
                showRecept(currentEmail);
                
            } catch (Exception e) {
                Logger.error("Erreur lors de l'envoi du message: " + e.getMessage());
                showErrorAlert("Erreur lors de l'envoi du message: " + e.getMessage());
            }
        });

        Button attachButton = (Button) scene.lookup("#attachButton");
        attachButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner une pièce jointe");
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                attachments.add(file);
                updateAttachmentStatus(attachmentStatus);
            }
        });

        adaptWindowSize(true);
    }

    private void showEmail() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("email.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        Button quitButton = (Button) scene.lookup("#quitButton");
        quitButton.setOnAction(event -> {
            try {
                showRecept(currentEmail);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Récupérer les références des zones de texte
        TextArea messageArea = (TextArea) scene.lookup("#messageArea");
        TextArea fromArea = (TextArea) scene.lookup("#fromArea");
        TextArea subjectArea = (TextArea) scene.lookup("#subjectArea");
        Text attachmentStatus = (Text) scene.lookup("#attachmentStatus");
        Button attachmentButton = (Button) scene.lookup("#attachmentButton");
        
        // Rendre les zones de texte non-éditables
        messageArea.setEditable(false);
        fromArea.setEditable(false);
        subjectArea.setEditable(false);

        // Afficher les informations du message courant
        if (currentMessage != null) {
            try {
                fromArea.setText(currentMessage.getFrom()[0].toString());
                subjectArea.setText(currentMessage.getSubject());
                
                // Récupérer le contenu du message
                Object content = currentMessage.getContent();
                String messageContent = "";
                boolean hasAttachment = false;
                
                if (content instanceof String) {
                    messageContent = (String) content;
                } else if (content instanceof Multipart) {
                    Multipart multipart = (Multipart) content;
                    for (int i = 0; i < multipart.getCount(); i++) {
                        BodyPart bodyPart = multipart.getBodyPart(i);
                        if (bodyPart.getDisposition() == null) {
                            // C'est probablement le contenu du message
                            messageContent = bodyPart.getContent().toString();
                        } else {
                            // C'est une pièce jointe
                            hasAttachment = true;
                        }
                    }
                }
                
                messageArea.setText(messageContent);
                
                // Gérer les pièces jointes
                if (hasAttachment) {
                    attachmentStatus.setText("Ce message contient des pièces jointes");
                    attachmentButton.setDisable(false);
                    attachmentButton.setText("Télécharger");
                    
                    attachmentButton.setOnAction(event -> {
                        try {
                            DirectoryChooser directoryChooser = new DirectoryChooser();
                            directoryChooser.setTitle("Choisir le dossier de téléchargement");
                            File directory = directoryChooser.showDialog(primaryStage);
                            
                            if (directory != null) {
                                // Récupérer et sauvegarder les pièces jointes
                                Multipart multipart = (Multipart) currentMessage.getContent();
                                int attachmentsSaved = 0;
                                
                                for (int i = 0; i < multipart.getCount(); i++) {
                                    BodyPart bodyPart = multipart.getBodyPart(i);
                                    if (bodyPart.getDisposition() != null) {
                                        // C'est une pièce jointe
                                        String fileName = bodyPart.getFileName();
                                        String filePath = directory.getAbsolutePath() + File.separator + fileName;
                                        
                                        if (bodyPart instanceof MimeBodyPart) {
                                            MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;
                                            mimeBodyPart.saveFile(filePath);
                                            attachmentsSaved++;
                                        }
                                    }
                                }
                                
                                showInfoAlert("Pièces jointes téléchargées", 
                                              attachmentsSaved + " pièce(s) jointe(s) téléchargée(s) dans " + 
                                              directory.getAbsolutePath());
                            }
                        } catch (Exception e) {
                            Logger.error("Erreur lors du téléchargement des pièces jointes: " + e.getMessage());
                            showErrorAlert("Erreur lors du téléchargement des pièces jointes: " + e.getMessage());
                        }
                    });
                } else {
                    attachmentStatus.setText("Pas de pièce jointe dans ce message");
                    attachmentButton.setDisable(true);
                }
            } catch (Exception e) {
                Logger.error("Erreur lors de l'affichage du message: " + e.getMessage());
                showErrorAlert("Erreur lors de l'affichage du message: " + e.getMessage());
            }
        } else {
            fromArea.setText("Aucun expéditeur");
            subjectArea.setText("Aucun sujet");
            messageArea.setText("Aucun message sélectionné");
            attachmentStatus.setText("Pas de pièce jointe");
            attachmentButton.setDisable(true);
        }

        adaptWindowSize(true);
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void adaptWindowSize(boolean maximize) {
        if (maximize) {
            primaryStage.setMaximized(true);
        } else {
            primaryStage.setWidth(640);
            primaryStage.setHeight(400);
            primaryStage.centerOnScreen();
        }
    }
    
    private void updateAttachmentStatus(Text attachmentStatus) {
        attachmentStatus.setText("Nombre de pièces jointes: " + attachments.size());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
