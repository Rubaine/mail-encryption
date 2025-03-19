package fr.insa.crypto.ui.controllers;

import fr.insa.crypto.MainUI;
import fr.insa.crypto.mail.MailReceiver;
import fr.insa.crypto.ui.ViewManager;
import fr.insa.crypto.utils.Logger;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Controller for the inbox/reception screen
 */
public class ReceptController {
    // UI components
    @FXML
    private Button newMessageButton;
    @FXML
    private Button logoutButton;
    @FXML
    private Button refreshButton;
    @FXML
    private ProgressIndicator refreshProgress;
    @FXML
    private ListView<String> emailListView;
    @FXML
    private Text connectedText;
    @FXML
    private VBox emptyInboxView;
    @FXML
    private Label emailCountLabel;
    @FXML
    private Button emptyRefreshButton;
    @FXML
    private StackPane loadingOverlay;

    // References to main application and view manager
    private MainUI mainApp;
    private ViewManager viewManager;

    // State
    private Message[] messages;

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
    public void setup(MainUI mainApp, ViewManager viewManager, String userEmail, MailReceiver mailReceiver) {
        this.mainApp = mainApp;
        this.viewManager = viewManager;
        // Set up button actions
        newMessageButton.setOnAction(event -> mainApp.showSendView());
        logoutButton.setOnAction(event -> handleLogout(mailReceiver));
        refreshButton.setOnAction(event -> refreshMessages(mailReceiver));

        if (emptyRefreshButton != null) {
            emptyRefreshButton.setOnAction(event -> refreshMessages(mailReceiver));
        } else {
            Logger.warning("emptyRefreshButton is null - can't set action");
        }

        // Display connected user
        connectedText.setText("Connected as: " + userEmail);

        // Set up email list selection handler
        setupEmailListSelection();

        // Initial refresh
        refreshMessages(mailReceiver);
    }

    /**
     * Sets up the email list selection handling
     */
    private void setupEmailListSelection() {
        emailListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        emailListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.intValue() >= 0 && messages != null && newVal.intValue() < messages.length) {
                try {
                    Message selectedMessage = messages[newVal.intValue()];
                    mainApp.showEmailView(selectedMessage);
                } catch (Exception e) {
                    Logger.error("Error displaying message: " + e.getMessage());
                    viewManager.showErrorAlert("Display Error",
                            "Error displaying message: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Refreshes the message list
     */
    private void refreshMessages(MailReceiver mailReceiver) {
        // Show loading indicator
        refreshButton.setDisable(true);
        refreshProgress.setVisible(true);

        // Background task to fetch messages
        Task<Message[]> refreshTask = new Task<Message[]>() {
            @Override
            protected Message[] call() throws Exception {
                try {
                    mailReceiver.openFolder("INBOX", true);
                    return mailReceiver.getMessages();
                } catch (Exception e) {
                    Logger.error("Error retrieving messages: " + e.getMessage());
                    throw e;
                }
            }
        };

        refreshTask.setOnSucceeded(e -> {
            refreshButton.setDisable(false);
            refreshProgress.setVisible(false);

            messages = refreshTask.getValue();

            // Update email counter
            int messageCount = messages != null ? messages.length : 0;
            emailCountLabel.setText(String.valueOf(messageCount));

            // Show appropriate view based on message count
            if (messageCount > 0) {
                emailListView.setVisible(true);
                emptyInboxView.setVisible(false);
                updateEmailListView();
            } else {
                emailListView.setVisible(false);
                emptyInboxView.setVisible(true);
            }

            Logger.info("Messages refreshed: " + messageCount + " messages found");
        });

        refreshTask.setOnFailed(e -> {
            refreshButton.setDisable(false);
            refreshProgress.setVisible(false);
            viewManager.showErrorAlert("Refresh Error",
                    "Error retrieving messages: " + refreshTask.getException().getMessage());
        });

        new Thread(refreshTask).start();
    }

    /**
     * Updates the email list view with messages
     */
    private void updateEmailListView() {
        emailListView.getItems().clear();

        if (messages == null || messages.length == 0) {
            return;
        }

        // Fill list with all available messages
        for (int i = 0; i < messages.length; i++) {
            try {
                Message msg = messages[i];
                String subject = msg.getSubject() != null ? msg.getSubject() : "(No subject)";
                String from = msg.getFrom()[0].toString();

                // Extract sender name for better readability
                String displayFrom = from;
                if (from.contains("<")) {
                    displayFrom = from.substring(0, from.indexOf("<")).trim();
                    if (displayFrom.isEmpty()) {
                        displayFrom = from.substring(from.indexOf("<") + 1, from.indexOf(">"));
                    }
                }

                // Format list item for better readability
                String itemText = displayFrom + " - " + subject;

                // Add item to list
                emailListView.getItems().add(itemText);

                // Mark unread items with a different style
                int finalI = i;
                if (!msg.isSet(Flags.Flag.SEEN)) {
                    emailListView.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setText(null);
                            } else {
                                setText(item);
                                if (getIndex() == finalI) {
                                    getStyleClass().add("email-item-unread");
                                }
                            }
                        }
                    });
                }
            } catch (MessagingException e) {
                Logger.error("Error reading message: " + e.getMessage());
                emailListView.getItems().add("Read error");
            }
        }
    }

    /**
     * Handles logout button click
     */
    private void handleLogout(MailReceiver mailReceiver) {
        try {
            // Logout
            if (mailReceiver != null) {
                try {
                    mailReceiver.close();
                } catch (MessagingException e) {
                    Logger.error("Error during logout: " + e.getMessage());
                }
            }

            mainApp.logout();
        } catch (Exception e) {
            Logger.error("Error during logout: " + e.getMessage());
            viewManager.showErrorAlert("Logout Error", "Error during logout: " + e.getMessage());
        }
    }
}
