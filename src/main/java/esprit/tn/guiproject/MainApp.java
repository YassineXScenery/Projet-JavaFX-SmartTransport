package esprit.tn.guiproject;

import esprit.tn.guiproject.controllers.MapController;
import esprit.tn.guiproject.controllers.PoiController;
import esprit.tn.guiproject.controllers.TrajetController;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

public class MainApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    @FXML private VBox mainVBox;
    @FXML private BorderPane mapPane;
    @FXML private HBox crudHBox;
    @FXML private HBox tablesHBox;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load MainView.fxml
            FXMLLoader mainLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/MainView.fxml"));
            if (mainLoader.getLocation() == null) {
                LOGGER.severe("Error: Cannot find MainView.fxml at /esprit/tn/guiproject/views/MainView.fxml");
                return;
            }
            mainLoader.setController(this); // Set MainApp as the controller for MainView.fxml
            Scene scene = new Scene(mainLoader.load(), 1000, 700);

            // Load MapView.fxml
            FXMLLoader mapLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/MapView.fxml"));
            if (mapLoader.getLocation() == null) {
                LOGGER.severe("Error: Cannot find MapView.fxml at /esprit/tn/guiproject/views/MapView.fxml");
                return;
            }
            mapPane.setCenter(mapLoader.load());

            // Load PoiView.fxml
            FXMLLoader poiLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/PoiView.fxml"));
            if (poiLoader.getLocation() == null) {
                LOGGER.severe("Error: Cannot find PoiView.fxml at /esprit/tn/guiproject/views/PoiView.fxml");
                return;
            }
            VBox poiView = poiLoader.load();
            crudHBox.getChildren().add(poiView);

            // Load TrajetView.fxml
            FXMLLoader trajetLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/TrajetView.fxml"));
            if (trajetLoader.getLocation() == null) {
                LOGGER.severe("Error: Cannot find TrajetView.fxml at /esprit/tn/guiproject/views/TrajetView.fxml");
                return;
            }
            VBox trajetView = trajetLoader.load();
            crudHBox.getChildren().add(trajetView);

            // Wire MapController and PoiController
            MapController mapController = mapLoader.getController();
            PoiController poiController = poiLoader.getController();
            if (mapController != null && poiController != null) {
                mapController.setPoiController(poiController);
                System.out.println("Controllers wired successfully in MainApp.");
            } else {
                LOGGER.severe("Error: One or more controllers not found.");
            }

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