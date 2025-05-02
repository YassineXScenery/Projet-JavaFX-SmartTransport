package esprit.tn.guiproject;

import esprit.tn.guiproject.controllers.MapController;
import esprit.tn.guiproject.controllers.PoiController;
import esprit.tn.guiproject.controllers.TrajetController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

public class MainApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load MainView.fxml as the root
            FXMLLoader mainLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/MainView.fxml"));
            if (mainLoader.getLocation() == null) {
                LOGGER.severe("Error: Cannot find MainView.fxml at /esprit/tn/guiproject/views/MainView.fxml");
                return;
            }
            ScrollPane root = mainLoader.load();
            Scene scene = new Scene(root, 1000, 700);

            // Access the mainVBox and its children
            VBox mainVBox = (VBox) root.getContent();
            BorderPane mapPane = (BorderPane) mainVBox.lookup("#mapPane");
            HBox crudHBox = (HBox) mainVBox.lookup("#crudHBox");
            HBox tablesHBox = (HBox) mainVBox.lookup("#tablesHBox");

            if (mapPane == null || crudHBox == null || tablesHBox == null) {
                LOGGER.severe("Error: One or more fx:id elements (mapPane, crudHBox, tablesHBox) not found in MainView.fxml");
                return;
            }

            // Load MapView.fxml into mapPane
            FXMLLoader mapLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/MapView.fxml"));
            if (mapLoader.getLocation() == null) {
                LOGGER.severe("Error: Cannot find MapView.fxml at /esprit/tn/guiproject/views/MapView.fxml");
                return;
            }
            mapPane.setCenter(mapLoader.load());

            // Load PoiView.fxml into crudHBox
            FXMLLoader poiLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/PoiView.fxml"));
            if (poiLoader.getLocation() == null) {
                LOGGER.severe("Error: Cannot find PoiView.fxml at /esprit/tn/guiproject/views/PoiView.fxml");
                return;
            }
            VBox poiView = poiLoader.load();
            crudHBox.getChildren().add(poiView);

            // Load TrajetView.fxml into crudHBox
            FXMLLoader trajetLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/TrajetView.fxml"));
            if (trajetLoader.getLocation() == null) {
                LOGGER.severe("Error: Cannot find TrajetView.fxml at /esprit/tn/guiproject/views/TrajetView.fxml");
                return;
            }
            VBox trajetView = trajetLoader.load();
            crudHBox.getChildren().add(trajetView);

            // Wire controllers
            MapController mapController = mapLoader.getController();
            PoiController poiController = poiLoader.getController();
            TrajetController trajetController = trajetLoader.getController();
            if (mapController != null && poiController != null && trajetController != null) {
                mapController.setPoiController(poiController);
                mapController.setTrajetController(trajetController);
                trajetController.setMapController(mapController);
                System.out.println("Controllers wired successfully in MainApp.");
            } else {
                LOGGER.severe("Error: One or more controllers not found.");
                return;
            }

            // Ensure the ScrollPane starts at the top
            root.setVvalue(0.0);

            primaryStage.setTitle("Cartography and Route Management");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            LOGGER.severe("IOException while loading FXML: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            LOGGER.severe("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}