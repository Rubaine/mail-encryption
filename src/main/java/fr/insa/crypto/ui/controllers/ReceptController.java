package fr.insa.crypto.ui.controllers;

import fr.insa.crypto.MainUI;
import fr.insa.crypto.mail.MailReceiver;
import fr.insa.crypto.ui.ViewManager;
import fr.insa.crypto.ui.models.EmailItem;
import fr.insa.crypto.utils.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    private ListView<EmailItem> emailListView;
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
    
    // Composants pour la recherche
    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;
    @FXML
    private ProgressIndicator searchProgress;
    
    // Composants pour la pagination
    @FXML
    private Button prevPageButton;
    @FXML
    private Button nextPageButton;
    @FXML
    private Label pageInfoLabel;
    @FXML
    private Label resultsInfoLabel;

    // References to main application and view manager
    private MainUI mainApp;
    private ViewManager viewManager;
    
    // Pool d'exécuteurs pour les opérations asynchrones
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    // State
    private Message[] messages;
    private List<EmailItem> allEmailItems = new ArrayList<>();
    private List<EmailItem> filteredEmailItems = new ArrayList<>();
    private int currentPage = 1;
    private int itemsPerPage = 20;
    private String currentSearchTerm = "";
    private MailReceiver mailReceiver;
    private final SimpleDateFormat hourFormatter = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM");
    private final SimpleDateFormat fullDateFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm");
    private final Date today = new Date();

    /**
     * Initializes the controller after FXML is loaded
     */
    @FXML
    private void initialize() {
        // Configure email list cell factory
        emailListView.setCellFactory(listView -> new EmailListCell());
        
        // Configure search field with submit on Enter
        searchField.setOnAction(event -> performSearch());
    }

    /**
     * Sets up the controller with necessary references and data
     */
    public void setup(MainUI mainApp, ViewManager viewManager, String userEmail, MailReceiver mailReceiver) {
        this.mainApp = mainApp;
        this.viewManager = viewManager;
        this.mailReceiver = mailReceiver;
        
        // Set up button actions
        newMessageButton.setOnAction(event -> mainApp.showSendView());
        logoutButton.setOnAction(event -> handleLogout(mailReceiver));
        refreshButton.setOnAction(event -> refreshMessages(mailReceiver));
        
        if (emptyRefreshButton != null) {
            emptyRefreshButton.setOnAction(event -> refreshMessages(mailReceiver));
        }
        
        // Set up search functionality
        searchButton.setOnAction(event -> performSearch());
        
        // Set up pagination controls
        nextPageButton.setOnAction(event -> goToNextPage());
        prevPageButton.setOnAction(event -> goToPrevPage());

        // Display connected user
        connectedText.setText("Connecté en tant que : " + userEmail);

        // Set up email list selection handler
        setupEmailListSelection();

        // Initial refresh
        refreshMessages(mailReceiver);
    }

    /**
     * Performs a search based on the current search term
     */
    private void performSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        
        // Si le terme de recherche est identique, ne rien faire
        if (searchTerm.equals(currentSearchTerm)) {
            return;
        }
        
        // Désactiver les contrôles et afficher l'indicateur de progression
        searchButton.setDisable(true);
        searchProgress.setVisible(true);
        
        // Réinitialiser la pagination
        currentPage = 1;
        
        // Stocker le terme de recherche actuel
        currentSearchTerm = searchTerm;
        
        Task<List<EmailItem>> searchTask = new Task<List<EmailItem>>() {
            @Override
            protected List<EmailItem> call() {
                // Si la recherche est vide, retourner tous les items
                if (searchTerm.isEmpty()) {
                    return new ArrayList<>(allEmailItems);
                }
                
                // Filtrer les items par recherche
                return allEmailItems.stream()
                        .filter(item -> 
                            item.getSender().toLowerCase().contains(searchTerm) ||
                            item.getSubject().toLowerCase().contains(searchTerm) ||
                            item.getPreview().toLowerCase().contains(searchTerm))
                        .collect(Collectors.toList());
            }
        };
        
        searchTask.setOnSucceeded(e -> {
            filteredEmailItems = searchTask.getValue();
            updatePaginationControls();
            displayCurrentPage();
            searchButton.setDisable(false);
            searchProgress.setVisible(false);
        });
        
        searchTask.setOnFailed(e -> {
            Logger.error("Erreur lors de la recherche: " + searchTask.getException().getMessage());
            searchButton.setDisable(false);
            searchProgress.setVisible(false);
        });
        
        executorService.submit(searchTask);
    }
    
    /**
     * Sets up the email list selection handling
     */
    private void setupEmailListSelection() {
        emailListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        emailListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                try {
                    mainApp.showEmailView(newVal.getOriginalMessage());
                } catch (Exception e) {
                    Logger.error("Error displaying message: " + e.getMessage());
                    viewManager.showErrorAlert("Display Error",
                            "Error displaying message: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Navigate to the next page of emails
     */
    private void goToNextPage() {
        int totalPages = calculateTotalPages();
        if (currentPage < totalPages) {
            currentPage++;
            updatePaginationControls();
            displayCurrentPage();
        }
    }
    
    /**
     * Navigate to the previous page of emails
     */
    private void goToPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePaginationControls();
            displayCurrentPage();
        }
    }
    
    /**
     * Updates the pagination controls based on the current state
     */
    private void updatePaginationControls() {
        int totalPages = calculateTotalPages();
        int totalItems = filteredEmailItems.size();
        
        // Mettre à jour les infos de page
        pageInfoLabel.setText("Page " + currentPage + "/" + Math.max(1, totalPages));
        
        // Calculer les indices de début et de fin pour cette page
        int startIndex = (currentPage - 1) * itemsPerPage + 1;
        int endIndex = Math.min(currentPage * itemsPerPage, totalItems);
        
        if (totalItems == 0) {
            resultsInfoLabel.setText("Aucun résultat");
        } else {
            resultsInfoLabel.setText("Affichage " + startIndex + "-" + endIndex + " sur " + totalItems);
        }
        
        // Activer/désactiver les boutons de navigation
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
    }
    
    /**
     * Calculate the total number of pages based on the filtered items
     */
    private int calculateTotalPages() {
        return (int) Math.ceil((double) filteredEmailItems.size() / itemsPerPage);
    }
    
    /**
     * Display the current page of emails in the ListView
     */
    private void displayCurrentPage() {
        emailListView.getItems().clear();
        
        if (filteredEmailItems.isEmpty()) {
            emptyInboxView.setVisible(true);
            emailListView.setVisible(false);
            return;
        }
        
        // Calculer les indices de début et de fin pour cette page
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredEmailItems.size());
        
        // Extraire les éléments pour cette page
        List<EmailItem> pageItems = filteredEmailItems.subList(startIndex, endIndex);
        
        // Ajouter les éléments à la liste
        emailListView.getItems().addAll(pageItems);
        
        // Afficher la vue appropriée
        emptyInboxView.setVisible(false);
        emailListView.setVisible(true);
    }

    /**
     * Refreshes the message list
     */
    private void refreshMessages(MailReceiver mailReceiver) {
        // Show loading indicators
        refreshButton.setDisable(true);
        refreshProgress.setVisible(true);
        
        // Reset the search
        searchField.setText("");
        currentSearchTerm = "";
        
        // Reset pagination
        currentPage = 1;

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
            messages = refreshTask.getValue();
            processMessages(messages);
        });

        refreshTask.setOnFailed(e -> {
            refreshButton.setDisable(false);
            refreshProgress.setVisible(false);
            viewManager.showErrorAlert("Refresh Error",
                    "Error retrieving messages: " + refreshTask.getException().getMessage());
        });

        executorService.submit(refreshTask);
    }
    
    /**
     * Process the retrieved messages and convert them to EmailItems
     */
    private void processMessages(Message[] messages) {
        Task<List<EmailItem>> processTask = new Task<List<EmailItem>>() {
            @Override
            protected List<EmailItem> call() throws Exception {
                List<EmailItem> items = new ArrayList<>();
                
                if (messages == null || messages.length == 0) {
                    return items;
                }
                
                for (Message message : messages) {
                    try {
                        // Create EmailItem from Message
                        EmailItem item = createEmailItemFromMessage(message);
                        items.add(item);
                    } catch (Exception ex) {
                        Logger.error("Error processing message: " + ex.getMessage());
                    }
                }
                
                // Sort messages by date (newest first)
                Collections.sort(items, Comparator.comparing(EmailItem::getDate).reversed());
                
                return items;
            }
        };
        
        processTask.setOnSucceeded(e -> {
            allEmailItems = processTask.getValue();
            filteredEmailItems = new ArrayList<>(allEmailItems);
            
            // Update UI
            updateEmailListView();
            refreshButton.setDisable(false);
            refreshProgress.setVisible(false);
            
            Logger.info("Messages refreshed: " + (messages != null ? messages.length : 0) + " messages found");
        });
        
        processTask.setOnFailed(e -> {
            Logger.error("Error processing messages: " + processTask.getException().getMessage());
            refreshButton.setDisable(false);
            refreshProgress.setVisible(false);
        });
        
        executorService.submit(processTask);
    }
    
    /**
     * Creates an EmailItem from a Message object
     */
    private EmailItem createEmailItemFromMessage(Message message) throws MessagingException {
        String subject = message.getSubject() != null ? message.getSubject() : "(Sans objet)";
        String sender = message.getFrom()[0].toString();
        boolean isUnread = !message.isSet(Flags.Flag.SEEN);
        Date date = message.getReceivedDate() != null ? message.getReceivedDate() : new Date();
        
        // Extract sender name for better readability
        if (sender.contains("<")) {
            String displayName = sender.substring(0, sender.indexOf("<")).trim();
            if (displayName.isEmpty()) {
                sender = sender.substring(sender.indexOf("<") + 1, sender.indexOf(">"));
            } else {
                sender = displayName;
            }
        }
        
        // Format time string
        String timeText = formatDate(date);
        
        // Try to get a preview of the message content
        String preview = "";
        try {
            Object content = message.getContent();
            if (content instanceof String) {
                // Get first 100 chars
                preview = ((String) content).replaceAll("\\s+", " ").trim();
                if (preview.length() > 100) {
                    preview = preview.substring(0, 97) + "...";
                }
            }
        } catch (Exception e) {
            Logger.debug("Couldn't get message preview: " + e.getMessage());
            preview = "...";
        }
        
        return new EmailItem(message, sender, subject, preview, date, timeText, isUnread);
    }
    
    /**
     * Format the date to a user-friendly string (today: hour only, this year: day+month, older: full date)
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        
        // Check if it's today
        if (isSameDay(date, today)) {
            return hourFormatter.format(date);
        }
        
        // Check if it's this year
        if (isSameYear(date, today)) {
            return dateFormatter.format(date);
        }
        
        // Otherwise, return full date
        return fullDateFormatter.format(date);
    }
    
    /**
     * Check if two dates are on the same day
     */
    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    /**
     * Check if two dates are in the same year
     */
    private boolean isSameYear(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
    }

    /**
     * Updates the email list view with messages
     */
    private void updateEmailListView() {
        // Update email counter
        int messageCount = allEmailItems.size();
        emailCountLabel.setText(String.valueOf(messageCount));

        // Update pagination controls
        updatePaginationControls();
        
        // Display the first page
        displayCurrentPage();
    }

    /**
     * Handles logout button click
     */
    private void handleLogout(MailReceiver mailReceiver) {
        try {
            // Shutdown the executor service
            executorService.shutdownNow();
            
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
    
    /**
     * Custom cell class for email items
     */
    private class EmailListCell extends ListCell<EmailItem> {
        private HBox container;
        private VBox contentBox;
        private Label senderLabel;
        private Label subjectLabel;
        private Label previewLabel;
        private Label timeLabel;
        
        public EmailListCell() {
            // Create UI elements
            container = new HBox();
            container.setSpacing(10);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPrefHeight(70);
            container.getStyleClass().add("email-cell");
            
            contentBox = new VBox();
            contentBox.setSpacing(2);
            HBox.setHgrow(contentBox, Priority.ALWAYS);
            
            senderLabel = new Label();
            senderLabel.getStyleClass().add("email-sender");
            
            subjectLabel = new Label();
            subjectLabel.getStyleClass().add("email-subject");
            
            previewLabel = new Label();
            previewLabel.getStyleClass().add("email-preview");
            
            timeLabel = new Label();
            timeLabel.getStyleClass().add("email-time");
            
            // Add components to their containers
            contentBox.getChildren().addAll(senderLabel, subjectLabel, previewLabel);
            container.getChildren().addAll(contentBox, timeLabel);
        }
        
        @Override
        protected void updateItem(EmailItem item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                // Set content
                senderLabel.setText(item.getSender());
                subjectLabel.setText(item.getSubject());
                previewLabel.setText(item.getPreview());
                timeLabel.setText(item.getTimeText());
                
                // Apply unread styling if needed
                if (item.isUnread()) {
                    senderLabel.setStyle("-fx-font-weight: bold;");
                    subjectLabel.setStyle("-fx-font-weight: bold;");
                    container.getStyleClass().add("email-cell-unread");
                } else {
                    senderLabel.setStyle("");
                    subjectLabel.setStyle("");
                    container.getStyleClass().remove("email-cell-unread");
                }
                
                setText(null);
                setGraphic(container);
            }
        }
    }
}
