package fr.insa.crypto.ui;

import fr.insa.crypto.MainUI;
import fr.insa.crypto.utils.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class responsible for managing views and scene transitions in the
 * application.
 * It loads FXML files, caches scenes, and provides methods for view navigation.
 */
public class ViewManager {
    // Constants for view paths
    public static final String VIEW_PORTAL = "/fr/insa/crypto/portal.fxml";
    public static final String VIEW_AUTH = "/fr/insa/crypto/auth.fxml";
    public static final String VIEW_RECEPT = "/fr/insa/crypto/recept.fxml";
    public static final String VIEW_SEND = "/fr/insa/crypto/send.fxml";
    public static final String VIEW_EMAIL = "/fr/insa/crypto/email.fxml";

    // Window dimensions
    private static final double WINDOW_WIDTH = 1000;
    private static final double WINDOW_HEIGHT = 700;

    // Main stage reference
    private final Stage primaryStage;

    // Cache for loaded controllers
    private final Map<String, Object> controllers = new HashMap<>();

    /**
     * Constructor for ViewManager
     *
     * @param primaryStage    The primary stage of the application
     * @param mainApplication Reference to the main application
     */
    public ViewManager(@SuppressWarnings("exports") Stage primaryStage, MainUI mainApplication) {
        this.primaryStage = primaryStage;
        // Configure the primary stage
        primaryStage.setTitle("Messenger Secure");
        primaryStage.setMinWidth(WINDOW_WIDTH);
        primaryStage.setMinHeight(WINDOW_HEIGHT);
        primaryStage.setMaxWidth(1920);
        primaryStage.setMaxHeight(1080);

        // Default dimensions
        primaryStage.setWidth(WINDOW_WIDTH);
        primaryStage.setHeight(WINDOW_HEIGHT);

        // Center the window
        primaryStage.centerOnScreen();
    }

    /**
     * Shows a specific view
     *
     * @param viewPath The path to the FXML file
     * @return The controller of the loaded view
     */
    public <T> T showView(String viewPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            // Store the controller for later access
            T controller = loader.getController();
            controllers.put(viewPath, controller);

            // Apply the scene to the primary stage
            setSceneAndShow(scene);

            return controller;
        } catch (IOException e) {
            Logger.error("Error loading view: " + viewPath + " - " + e.getMessage());
            showErrorAlert("Error loading view", "Could not load the requested view: " + e.getMessage());
            throw new RuntimeException("Failed to load view: " + viewPath, e);
        }
    }

    /**
     * Gets the controller for the specified view path
     *
     * @param viewPath The path to the FXML file
     * @return The controller associated with the view
     */
    @SuppressWarnings("unchecked")
    public <T> T getController(String viewPath) {
        return (T) controllers.get(viewPath);
    }

    /**
     * Sets the scene to the primary stage and shows it
     *
     * @param scene The scene to show
     */
    private void setSceneAndShow(Scene scene) {
        // Store current dimensions to maintain window size
        double currentWidth = primaryStage.getWidth();
        double currentHeight = primaryStage.getHeight();

        primaryStage.setScene(scene);

        // Maintain window size
        if (primaryStage.isMaximized()) {
            primaryStage.setMaximized(false);
        }

        // Maintain dimensions
        if (primaryStage.isShowing()) {
            primaryStage.setWidth(currentWidth);
            primaryStage.setHeight(currentHeight);
        } else {
            // Default dimensions for first show
            primaryStage.setWidth(WINDOW_WIDTH);
            primaryStage.setHeight(WINDOW_HEIGHT);
            primaryStage.centerOnScreen();
        }

        primaryStage.show();
    }

    /**
     * Shows an error alert
     *
     * @param title   The title of the alert
     * @param message The message to display
     */
    public void showErrorAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an information alert
     *
     * @param title   The title of the alert
     * @param message The message to display
     */
    public void showInfoAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
