module esprit.tn.guiproject.esprit {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.web;
    requires jdk.jsobject;

    // Required for JavaFX reflection
    requires javafx.base;

    // Export and open packages for FXML and controllers
    exports esprit.tn.guiproject.controllers to javafx.fxml;
    opens esprit.tn.guiproject.controllers to javafx.fxml, javafx.web; // Added javafx.web
    opens esprit.tn.guiproject.views to javafx.fxml;
    opens esprit.tn.guiproject to javafx.fxml;

    // Export main package
    exports esprit.tn.guiproject;

    // Open the models package to javafx.base for PropertyValueFactory reflection
    opens esprit.tn.guiproject.models to javafx.base;
}