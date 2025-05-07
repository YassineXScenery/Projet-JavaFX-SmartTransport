module esprit.tn.guiproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires jdk.jsobject;
    requires javafx.base;
    requires java.net.http;
    requires org.json;

    // Open packages for FXML and JavaFX reflection
    opens esprit.tn.guiproject to javafx.fxml;
    opens esprit.tn.guiproject.controllers to javafx.fxml, javafx.web;
    opens esprit.tn.guiproject.views to javafx.fxml;
    opens esprit.tn.guiproject.models to javafx.base;

    // Export packages
    exports esprit.tn.guiproject;
    exports esprit.tn.guiproject.controllers;
}