module fr.insa.crypto {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.httpserver;
    requires jpbc.api;
    requires jpbc.plaf;
    requires mail;
    requires org.json;

    requires activation;
    requires owasp.encoder;
    opens fr.insa.crypto to javafx.fxml;
    exports fr.insa.crypto;
}