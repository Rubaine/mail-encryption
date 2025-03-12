module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;


    opens fr.insa.crypto to javafx.fxml;
    exports fr.insa.crypto;
}