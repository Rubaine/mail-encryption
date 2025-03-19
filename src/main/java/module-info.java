module fr.insa.crypto {
    // Required JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;

    // Other dependencies
    requires mail;
    requires jpbc.api;
    requires jpbc.plaf;
    requires org.json;
    requires activation;
    requires owasp.encoder;
    requires totp;

    // Export packages to JavaFX FXML
    exports fr.insa.crypto to javafx.graphics;
    exports fr.insa.crypto.ui.controllers to javafx.fxml;

    // Open packages for reflection
    opens fr.insa.crypto.ui.controllers to javafx.fxml;

    // Export other packages as needed
    exports fr.insa.crypto.ui;
    exports fr.insa.crypto.utils;
    exports fr.insa.crypto.mail;
    exports fr.insa.crypto.encryption;
    exports fr.insa.crypto.trustAuthority;
}