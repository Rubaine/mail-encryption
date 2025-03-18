module fr.insa.crypto {
    requires javafx.controls;
    requires javafx.fxml;
            
                        
    opens fr.insa.crypto to javafx.fxml;
    exports fr.insa.crypto;
}