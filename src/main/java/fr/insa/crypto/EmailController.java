package fr.insa.crypto;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

/**
 * Controller for the email viewing screen
 */
public class EmailController {
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

    // Getters for all components
    public Button getQuitButton() {
        return quitButton;
    }

    public TextArea getMessageArea() {
        return messageArea;
    }

    public TextArea getFromArea() {
        return fromArea;
    }

    public TextArea getSubjectArea() {
        return subjectArea;
    }

    public Text getAttachmentStatus() {
        return attachmentStatus;
    }

    public Button getAttachmentButton() {
        return attachmentButton;
    }

    public ProgressIndicator getDownloadProgress() {
        return downloadProgress;
    }
}
