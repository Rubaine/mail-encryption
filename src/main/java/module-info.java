module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.httpserver;
    requires jpbc.api;
    requires jpbc.plaf;
    requires mail;
    requires org.json;


    opens fr.insa.crypto to javafx.fxml;
    exports fr.insa.crypto;
}