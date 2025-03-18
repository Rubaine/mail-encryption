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
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainUI extends Application {

    private Stage primaryStage;
    private Map<String, String> validCredentials;
    private String currentEmail;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        initializeValidCredentials();
        showPortal();
    }

    private void initializeValidCredentials() {
        validCredentials = new HashMap<>();
        validCredentials.put("user1@example.com", "password123");
        validCredentials.put("user2@example.com", "password456");
        validCredentials.put("user3@example.com", "password789");
        validCredentials.put("root", "root"); // Add root credentials
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
            if (isValidCredentials(email, password)) {
                try {
                    currentEmail = email;
                    showRecept(email);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                showErrorAlert("Invalid email or password.");
            }
        });

        adaptWindowSize(false);
    }

    private boolean isValidCredentials(String email, String password) {
        return validCredentials.containsKey(email) && validCredentials.get(email).equals(password);
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Login Error");
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
                showPortal();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Button refreshButton = (Button) scene.lookup("#refreshButton");
        refreshButton.setOnAction(event -> {
            // Add functionality to refresh the messages
            System.out.println("Messages refreshed");
        });

        // Display the connected email
        Text connectedText = (Text) scene.lookup("#connectedText");
        connectedText.setText("Connecté à : " + email);

        // Handle email buttons
        for (int i = 0; i < 7; i++) {
            Button emailButton = (Button) scene.lookup("#emailButton" + i);
            if (emailButton != null) {
                emailButton.setOnAction(event -> {
                    try {
                        showEmail();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        adaptWindowSize(true);
    }

    private void showSend() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("send.fxml"));
        Parent root = loader.load();
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

        Button sendButton = (Button) scene.lookup("#sendButton");
        sendButton.setOnAction(event -> {
            showInfoAlert("Message envoyé", "Votre message a été envoyé avec succès.");
            try {
                showRecept(currentEmail);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Button attachButton = (Button) scene.lookup("#attachButton");
        Text attachmentStatus = (Text) scene.lookup("#attachmentStatus");

        attachButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Attachment");
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                System.out.println("Selected file: " + file.getAbsolutePath());
                showInfoAlert("Attachment Selected", "Selected file: " + file.getName());
                attachmentStatus.setText("Attachment: " + file.getName());
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

        // Make text areas non-editable
        TextArea messageArea = (TextArea) scene.lookup("#messageArea");
        TextArea fromArea = (TextArea) scene.lookup("#fromArea");
        TextArea subjectArea = (TextArea) scene.lookup("#subjectArea");
        messageArea.setEditable(false);
        fromArea.setEditable(false);
        subjectArea.setEditable(false);

        // Handle attachments
        Button attachmentButton = (Button) scene.lookup("#attachmentButton");
        Text attachmentStatus = (Text) scene.lookup("#attachmentStatus");
        attachmentButton.setText("Télécharger");
        if (attachmentStatus.getText().equals("Pas de pièce jointe dans ce message")) {
            attachmentButton.setDisable(true);
        } else {
            attachmentButton.setDisable(false);
            attachmentButton.setOnAction(event -> {
                // Add functionality to download the attachments
                System.out.println("Downloading attachments...");
            });
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

    public static void main(String[] args) {
        launch(args);
    }
}
