package fr.insa.crypto;

import fr.insa.crypto.encryption.IdentityBasedEncryption;
import fr.insa.crypto.mail.Authentication;
import fr.insa.crypto.mail.MailReceiver;
import fr.insa.crypto.mail.MailSender;
import fr.insa.crypto.mail.SecureAttachmentHandler;
import fr.insa.crypto.trustAuthority.KeyPair;
import fr.insa.crypto.utils.Logger;
import it.unisa.dia.gas.jpbc.Element;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    // Ajout de champs pour la cryptographie
    private IdentityBasedEncryption ibeEngine;
    private KeyPair userKeyPair;

    // Dimensions constantes pour assurer l'uniformité
    private static final double WINDOW_WIDTH = 1000;
    private static final double WINDOW_HEIGHT = 700;

    @Override
    public void start(@SuppressWarnings("exports") Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Messenger Secure");
        primaryStage.setMinWidth(WINDOW_WIDTH);
        primaryStage.setMinHeight(WINDOW_HEIGHT);
        primaryStage.setMaxWidth(1920);
        primaryStage.setMaxHeight(1080);

        // Applique les dimensions par défaut
        primaryStage.setWidth(WINDOW_WIDTH);
        primaryStage.setHeight(WINDOW_HEIGHT);

        // Centrer la fenêtre
        primaryStage.centerOnScreen();

        showPortal();
    }

    private void showPortal() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("portal.fxml"));
        Scene scene = new Scene(root);
        setSceneAndShow(scene, false);

        TextField emailField = (TextField) scene.lookup("#emailField");
        TextField passwordField = (TextField) scene.lookup("#passwordField");
        Button loginButton = (Button) scene.lookup("#loginButton");
        ProgressIndicator loginProgress = (ProgressIndicator) scene.lookup("#loginProgress");
        StackPane loadingOverlay = (StackPane) scene.lookup("#loadingOverlay");
        HBox progressContainer = (HBox) scene.lookup("#progressContainer");

        if (loginProgress == null) {
            Logger.error("Indicateur de progression de connexion non trouvé");
        }

        if (loadingOverlay == null) {
            Logger.error("Overlay de chargement non trouvé");
        }

        loginButton.setOnAction(event -> {
            String email = emailField.getText();
            String password = passwordField.getText();

            if (email.isEmpty() || password.isEmpty()) {
                showErrorAlert("Veuillez saisir votre email et votre mot de passe");
                return;
            }

            // Afficher l'indicateur de chargement si disponible
            loginButton.setDisable(true);
            if (progressContainer != null) {
                progressContainer.setVisible(true);
            }

            // Tâche en arrière-plan pour l'authentification
            Task<Boolean> loginTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    try {
                        auth = new Authentication(email, password);
                        Session session = auth.getAuthenticatedSession();

                        // Récupérer les éléments cryptographiques
                        ibeEngine = auth.getIbeEngine();
                        userKeyPair = auth.getUserKeyPair();

                        // Tester la connexion en créant un récepteur d'emails
                        mailReceiver = new MailReceiver();
                        mailReceiver.connect(email, password);

                        currentEmail = email;
                        currentAppKey = password;
                        return true;
                    } catch (Exception e) {
                        Logger.error("Erreur d'authentification: " + e.getMessage());
                        return false;
                    }
                }
            };

            loginTask.setOnSucceeded(e -> {
                loginButton.setDisable(false);
                if (loginProgress != null) {
                    loginProgress.setVisible(false);
                }

                if (loginTask.getValue()) {
                    // Connexion réussie
                    try {
                        showRecept(email);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showErrorAlert("Erreur lors de l'affichage de la boîte de réception: " + ex.getMessage());
                    }
                } else {
                    // Échec de connexion
                    showErrorAlert("Identifiants incorrects ou problème de connexion au serveur");
                }
            });

            loginTask.setOnFailed(e -> {
                loginButton.setDisable(false);
                if (loginProgress != null) {
                    loginProgress.setVisible(false);
                }
                showErrorAlert("Erreur d'authentification: " + loginTask.getException().getMessage());
            });

            new Thread(loginTask).start();
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
        setSceneAndShow(scene, true);

        Button newMessageButton = (Button) scene.lookup("#newMessageButton");
        Button logoutButton = (Button) scene.lookup("#logoutButton");
        Button refreshButton = (Button) scene.lookup("#refreshButton");
        ProgressIndicator refreshProgress = (ProgressIndicator) scene.lookup("#refreshProgress");
        StackPane loadingOverlay = (StackPane) scene.lookup("#loadingOverlay");
        VBox emailListView = (VBox) scene.lookup("#emailListView");
        VBox emptyInboxView = (VBox) scene.lookup("#emptyInboxView");
        Label emailCountLabel = (Label) scene.lookup("#emailCountLabel");
        Button emptyRefreshButton = (Button) scene.lookup("#emptyRefreshButton");

        // Configurer les actions des boutons
        newMessageButton.setOnAction(event -> {
            try {
                showSend();
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Erreur lors de l'ouverture de la fenêtre d'envoi: " + e.getMessage());
            }
        });

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
                showErrorAlert("Erreur lors de la déconnexion: " + e.getMessage());
            }
        });

        // Fonction pour actualiser les messages
        Runnable refreshAction = () -> {
            // Afficher l'indicateur de chargement
            refreshButton.setDisable(true);
            refreshProgress.setVisible(true);

            // Tâche en arrière-plan pour récupérer les messages
            Task<Message[]> refreshTask = new Task<Message[]>() {
                @Override
                protected Message[] call() throws Exception {
                    try {
                        if (mailReceiver == null) {
                            mailReceiver = new MailReceiver();
                            mailReceiver.connect(currentEmail, currentAppKey);
                        }

                        mailReceiver.openFolder("INBOX", true);
                        return mailReceiver.getMessages();
                    } catch (Exception e) {
                        Logger.error("Erreur lors de la récupération des messages: " + e.getMessage());
                        throw e;
                    }
                }
            };

            refreshTask.setOnSucceeded(e -> {
                refreshButton.setDisable(false);
                refreshProgress.setVisible(false);

                messages = refreshTask.getValue();

                // Mettre à jour le compteur d'emails
                int messageCount = messages != null ? messages.length : 0;
                emailCountLabel.setText(String.valueOf(messageCount));

                // Afficher la vue appropriée en fonction du nombre de messages
                if (messageCount > 0) {
                    emailListView.setVisible(true);
                    emptyInboxView.setVisible(false);
                    updateEmailButtons(scene, messages);
                } else {
                    emailListView.setVisible(false);
                    emptyInboxView.setVisible(true);
                }

                Logger.info("Messages rafraîchis: " + messageCount + " messages trouvés");
            });

            refreshTask.setOnFailed(e -> {
                refreshButton.setDisable(false);
                refreshProgress.setVisible(false);
                showErrorAlert("Erreur lors de la récupération des messages: " + refreshTask.getException().getMessage());
            });

            new Thread(refreshTask).start();
        };

        refreshButton.setOnAction(event -> refreshAction.run());
        emptyRefreshButton.setOnAction(event -> refreshAction.run());

        // Déclencher un rafraîchissement initial des messages
        refreshAction.run();

        // Afficher l'email connecté
        Text connectedText = (Text) scene.lookup("#connectedText");
        connectedText.setText("Connecté à : " + email);
    }

    private void updateEmailButtons(Scene scene, Message[] messages) {
        // Mettre à jour les boutons d'email avec les messages réels
        for (int i = 0; i < 7; i++) {
            Button emailButton = (Button) scene.lookup("#emailButton" + i);
            if (emailButton != null) {
                // Réinitialiser la visibilité et le texte
                emailButton.setVisible(true);
                emailButton.setManaged(true);

                if (messages != null && i < messages.length) {
                    try {
                        // Configurer le bouton avec les informations du message
                        Message msg = messages[i];
                        String subject = msg.getSubject() != null ? msg.getSubject() : "(Sans objet)";
                        String from = msg.getFrom()[0].toString();

                        // Extraire le nom de l'expéditeur pour une meilleure lisibilité
                        String displayFrom = from;
                        if (from.contains("<")) {
                            displayFrom = from.substring(0, from.indexOf("<")).trim();
                            if (displayFrom.isEmpty()) {
                                displayFrom = from.substring(from.indexOf("<") + 1, from.indexOf(">"));
                            }
                        }

                        // Formater le texte du bouton pour une meilleure lisibilité
                        emailButton.setText(displayFrom + " - " + subject);

                        // Ajouter des classes CSS pour le style
                        emailButton.getStyleClass().removeAll("email-item-unread");
                        if (!msg.isSet(Flags.Flag.SEEN)) {
                            emailButton.getStyleClass().add("email-item-unread");
                        }

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
                    // Cacher les boutons sans message plutôt que d'afficher "Pas de message"
                    emailButton.setVisible(false);
                    emailButton.setManaged(false);
                }
            }
        }
    }

    private void showSend() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("send.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        setSceneAndShow(scene, true);

        // Récupérer les références des champs
        TextField toField = (TextField) scene.lookup("#toField");
        TextField subjectField = (TextField) scene.lookup("#subjectField");
        TextArea messageArea = (TextArea) scene.lookup("#messageArea");
        Text attachmentStatus = (Text) scene.lookup("#attachmentStatus");
        Button sendButton = (Button) scene.lookup("#sendButton");
        Button attachButton = (Button) scene.lookup("#attachButton");
        ProgressIndicator sendProgress = (ProgressIndicator) scene.lookup("#sendProgress");
        ProgressIndicator attachProgress = (ProgressIndicator) scene.lookup("#attachProgress");
        StackPane loadingOverlay = (StackPane) scene.lookup("#loadingOverlay");

        if (toField == null || subjectField == null || messageArea == null || attachmentStatus == null) {
            Logger.error("Certains champs sont introuvables dans l'interface");
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

        sendButton.setOnAction(event -> {
            String to = toField.getText();
            String subject = subjectField.getText();
            String body = messageArea.getText();

            // Validation renforcée
            if (to == null || to.trim().isEmpty()) {
                showErrorAlert("L'adresse email du destinataire ne peut pas être vide");
                return;
            }

            // Vérifier le format de l'adresse email
            if (!fr.insa.crypto.utils.Config.isValidEmail(to)) {
                showErrorAlert("Format d'adresse email invalide: " + to);
                return;
            }

            if (subject.isEmpty() || body.isEmpty()) {
                showErrorAlert("Veuillez remplir tous les champs obligatoires");
                return;
            }

            // Afficher l'indicateur de chargement
            sendButton.setDisable(true);
            sendProgress.setVisible(true);

            // Tâche en arrière-plan pour envoyer l'email
            Task<Boolean> sendTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    try {
                        Session session = auth.getAuthenticatedSession();

                        if (attachments.isEmpty()) {
                            // Envoyer un email simple
                            MailSender.sendEmail(session, to, subject, body);
                        } else {
                            // Création du gestionnaire de pièces jointes sécurisé avec chiffrement
                            SecureAttachmentHandler secureHandler = new SecureAttachmentHandler(ibeEngine, userKeyPair);

                            // Ajouter chaque pièce jointe avec chiffrement automatique
                            for (File file : attachments) {
                                try {
                                    // Chiffrer le fichier pour le destinataire
                                    secureHandler.addEncryptedAttachment(file.getAbsolutePath(), to);
                                    Logger.info("Pièce jointe chiffrée: " + file.getName());
                                } catch (Exception e) {
                                    Logger.error("Erreur lors du chiffrement de la pièce jointe " + file.getName() + ": " + e.getMessage());
                                    throw new Exception("Erreur lors du chiffrement de la pièce jointe " + file.getName() + ": " + e.getMessage());
                                }
                            }

                            // Envoyer l'email avec les pièces jointes chiffrées
                            MailSender.sendEmailWithAttachments(session, to, subject,
                                    body + "\n\nCet email contient des pièces jointes chiffrées.", secureHandler);
                        }
                        return true;
                    } catch (Exception e) {
                        Logger.error("Erreur lors de l'envoi du message: " + e.getMessage());
                        throw e;
                    }
                }
            };

            sendTask.setOnSucceeded(e -> {
                sendButton.setDisable(false);
                sendProgress.setVisible(false);

                if (sendTask.getValue()) {
                    showInfoAlert("Message envoyé", "Votre message a été envoyé avec succès.");
                    try {
                        showRecept(currentEmail);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            sendTask.setOnFailed(e -> {
                sendButton.setDisable(false);
                sendProgress.setVisible(false);
                showErrorAlert("Erreur lors de l'envoi du message: " + sendTask.getException().getMessage());
            });

            new Thread(sendTask).start();
        });

        attachButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner une pièce jointe");
            File file = fileChooser.showOpenDialog(primaryStage);

            if (file != null) {
                // Afficher l'indicateur de chargement
                attachButton.setDisable(true);
                attachProgress.setVisible(true);

                // Tâche en arrière-plan pour traiter la pièce jointe
                Task<Void> attachTask = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        // Simuler un traitement pour les gros fichiers
                        if (file.length() > 1000000) { // 1MB
                            Thread.sleep(1000); // Attendre 1 seconde pour les gros fichiers
                        }
                        return null;
                    }
                };

                attachTask.setOnSucceeded(e -> {
                    attachButton.setDisable(false);
                    attachProgress.setVisible(false);

                    // Ajouter le fichier à la liste des pièces jointes
                    attachments.add(file);
                    updateAttachmentStatus(attachmentStatus);
                });

                attachTask.setOnFailed(e -> {
                    attachButton.setDisable(false);
                    attachProgress.setVisible(false);
                    showErrorAlert("Erreur lors de l'ajout de la pièce jointe: " + attachTask.getException().getMessage());
                });

                new Thread(attachTask).start();
            }
        });
    }

    private void showEmail() throws Exception {
        // Use FXMLLoader to get access to the controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("email.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        setSceneAndShow(scene, true);

        // Get the controller from the FXMLLoader
        EmailController controller = loader.getController();

        // Log if controller isn't available
        if (controller == null) {
            Logger.error("EmailController n'a pas pu être chargé");
            showErrorAlert("Erreur d'interface utilisateur. Veuillez contacter le développeur.");
            return;
        }

        // Access components through the controller
        Button quitButton = controller.getQuitButton();
        TextArea messageArea = controller.getMessageArea();
        TextArea fromArea = controller.getFromArea();
        TextArea subjectArea = controller.getSubjectArea();
        Text attachmentStatus = controller.getAttachmentStatus();
        Button attachmentButton = controller.getAttachmentButton();
        ProgressIndicator downloadProgress = controller.getDownloadProgress();

        // Check if any component is missing
        boolean missingComponents = (quitButton == null || messageArea == null || fromArea == null ||
                subjectArea == null || attachmentStatus == null || attachmentButton == null);

        if (missingComponents) {
            Logger.error("Composants UI manquants dans la vue d'email (via controller)");
            showErrorAlert("Erreur d'interface utilisateur. Veuillez contacter le développeur.");
            return;
        }

        // Rest of the method remains unchanged
        quitButton.setOnAction(event -> {
            try {
                showRecept(currentEmail);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Rendre les zones de texte non-éditables
        messageArea.setEditable(false);
        fromArea.setEditable(false);
        subjectArea.setEditable(false);

        // Afficher les informations du message courant
        if (currentMessage != null) {
            try {
                fromArea.setText(currentMessage.getFrom()[0].toString());
                subjectArea.setText(currentMessage.getSubject() != null ? currentMessage.getSubject() : "(Sans objet)");

                // Récupérer le contenu du message
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
                            // C'est probablement le contenu du message
                            Object partContent = bodyPart.getContent();
                            if (partContent instanceof String) {
                                messageContent = (String) partContent;
                            }
                        } else {
                            // C'est une pièce jointe
                            hasAttachment = true;
                            if (bodyPart instanceof MimeBodyPart) {
                                attachmentParts.add((MimeBodyPart) bodyPart);
                            }
                        }
                    }
                }

                messageArea.setText(messageContent);

                // Gérer les pièces jointes
                if (hasAttachment) {
                    attachmentStatus.setText("Ce message contient des pièces jointes (chiffrées)");
                    attachmentButton.setDisable(false);
                    attachmentButton.setText("Télécharger & Déchiffrer");

                    attachmentButton.setOnAction(event -> {
                        try {
                            // Désactiver le bouton et afficher l'indicateur de chargement
                            attachmentButton.setDisable(true);
                            if (downloadProgress != null) {
                                downloadProgress.setVisible(true);
                            }

                            DirectoryChooser directoryChooser = new DirectoryChooser();
                            directoryChooser.setTitle("Choisir le dossier de téléchargement");
                            File directory = directoryChooser.showDialog(primaryStage);

                            if (directory != null) {
                                Task<Integer> downloadTask = new Task<Integer>() {
                                    @Override
                                    protected Integer call() throws Exception {
                                        // Récupérer et sauvegarder les pièces jointes
                                        Multipart multipart = (Multipart) currentMessage.getContent();
                                        int attachmentsSaved = 0;

                                        for (int i = 0; i < multipart.getCount(); i++) {
                                            BodyPart bodyPart = multipart.getBodyPart(i);
                                            if (bodyPart.getDisposition() != null) {
                                                // Récupérer le nom du fichier
                                                String fileName = bodyPart.getFileName();
                                                File tempFile = File.createTempFile("temp_attachment_", fileName);

                                                // Sauvegarder la pièce jointe dans un fichier temporaire
                                                ((MimeBodyPart) bodyPart).saveFile(tempFile);
                                                attachmentsSaved++;

                                                // Vérifier si c'est un fichier IBE chiffré
                                                if (SecureAttachmentHandler.isIBEEncryptedFile(tempFile)) {
                                                    // C'est un fichier chiffré, le déchiffrer
                                                    Element privateKey = userKeyPair.getSk();
                                                    File decryptedFile = SecureAttachmentHandler.decryptFile(
                                                            tempFile, directory.getAbsolutePath(), privateKey, ibeEngine);

                                                    Logger.info("Pièce jointe déchiffrée: " + decryptedFile.getName());

                                                    // Supprimer le fichier temporaire
                                                    tempFile.delete();
                                                } else {
                                                    // C'est un fichier non chiffré, le copier dans le répertoire cible
                                                    File targetFile = new File(directory, fileName);
                                                    java.nio.file.Files.copy(
                                                            tempFile.toPath(), targetFile.toPath(),
                                                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                                                    // Supprimer le fichier temporaire
                                                    tempFile.delete();
                                                }
                                            }
                                        }
                                        return attachmentsSaved;
                                    }
                                };

                                downloadTask.setOnSucceeded(e -> {
                                    attachmentButton.setDisable(false);
                                    if (downloadProgress != null) {
                                        downloadProgress.setVisible(false);
                                    }

                                    int attachmentsSaved = downloadTask.getValue();
                                    showInfoAlert("Pièces jointes téléchargées",
                                            attachmentsSaved + " pièce(s) jointe(s) téléchargée(s) dans " +
                                                    directory.getAbsolutePath());
                                });

                                downloadTask.setOnFailed(e -> {
                                    attachmentButton.setDisable(false);
                                    if (downloadProgress != null) {
                                        downloadProgress.setVisible(false);
                                    }
                                    showErrorAlert("Erreur lors du téléchargement des pièces jointes: " +
                                            downloadTask.getException().getMessage());
                                });

                                new Thread(downloadTask).start();
                            } else {
                                attachmentButton.setDisable(false);
                                if (downloadProgress != null) {
                                    downloadProgress.setVisible(false);
                                }
                            }
                        } catch (Exception e) {
                            Logger.error("Erreur lors du téléchargement des pièces jointes: " + e.getMessage());
                            showErrorAlert("Erreur lors du téléchargement des pièces jointes: " + e.getMessage());
                            attachmentButton.setDisable(false);
                            if (downloadProgress != null) {
                                downloadProgress.setVisible(false);
                            }
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

    // Méthode centralisée pour gérer l'affichage de la scène
    private void setSceneAndShow(Scene scene, boolean maximized) {
        primaryStage.setScene(scene);

        // Conserve les mêmes dimensions à moins que maximized soit vrai
        if (maximized && !primaryStage.isMaximized()) {
            // Sauvegarder les dimensions actuelles avant de maximiser
            primaryStage.setUserData(new double[]{primaryStage.getWidth(), primaryStage.getHeight()});
            primaryStage.setMaximized(true);
        } else if (!maximized && primaryStage.isMaximized()) {
            // Restaurer les dimensions précédentes si disponibles
            primaryStage.setMaximized(false);
            if (primaryStage.getUserData() instanceof double[]) {
                double[] dims = (double[]) primaryStage.getUserData();
                primaryStage.setWidth(dims[0]);
                primaryStage.setHeight(dims[1]);
            } else {
                // Utilisez les dimensions par défaut
                primaryStage.setWidth(WINDOW_WIDTH);
                primaryStage.setHeight(WINDOW_HEIGHT);
            }
            primaryStage.centerOnScreen();
        }

        primaryStage.show();
    }

    // Remplacer la méthode adaptWindowSize existante par celle-ci
    private void adaptWindowSize(boolean maximize) {
        if (maximize && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        } else if (!maximize) {
            primaryStage.setMaximized(false);
            primaryStage.setWidth(WINDOW_WIDTH);
            primaryStage.setHeight(WINDOW_HEIGHT);
            primaryStage.centerOnScreen();
        }
    }

    private void updateAttachmentStatus(Text attachmentStatus) {
        attachmentStatus.setText("Nombre de pièces jointes: " + attachments.size() + " (seront chiffrées automatiquement)");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
