package esprit.tn.guiproject;

import esprit.tn.guiproject.controllers.MapController;
import esprit.tn.guiproject.controllers.PoiController;
import esprit.tn.guiproject.controllers.TrajetController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
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
                LOGGER.severe("Error: Cannot find MainView.fxml");
                return;
            }
            BorderPane root = mainLoader.load();
            Scene scene = new Scene(root, 1000, 700);

            // Access containers in MainView.fxml using namespace
            BorderPane mapPane = (BorderPane) mainLoader.getNamespace().get("mapPane");
            VBox trajetViewContainer = (VBox) mainLoader.getNamespace().get("trajetView");
            VBox poiViewContainer = (VBox) mainLoader.getNamespace().get("poiView");

            if (mapPane == null || trajetViewContainer == null || poiViewContainer == null) {
                LOGGER.severe("Error: Missing fx:id elements (mapPane, trajetView, or poiView) in MainView.fxml");
                return;
            }

            // Load MapView.fxml into mapPane
            FXMLLoader mapLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/MapView.fxml"));
            mapPane.setCenter(mapLoader.load());
            MapController mapController = mapLoader.getController();

            // Load TrajetView.fxml into trajetViewContainer
            FXMLLoader trajetLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/TrajetView.fxml"));
            trajetViewContainer.getChildren().add(trajetLoader.load());
            TrajetController trajetController = trajetLoader.getController();

            // Load PoiView.fxml into poiViewContainer
            FXMLLoader poiLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/PoiView.fxml"));
            poiViewContainer.getChildren().add(poiLoader.load());
            PoiController poiController = poiLoader.getController();

            // Wire controllers together
            if (mapController != null && poiController != null && trajetController != null) {
                mapController.setPoiController(poiController);
                mapController.setTrajetController(trajetController);
                trajetController.setMapController(mapController);
                poiController.setMapController(mapController);

                // Inject weather labels into MapController
                mapController.setTemperatureLabel((javafx.scene.control.Label) mainLoader.getNamespace().get("temperatureLabel"));
                mapController.setDescriptionLabel((javafx.scene.control.Label) mainLoader.getNamespace().get("descriptionLabel"));
                mapController.setHumidityLabel((javafx.scene.control.Label) mainLoader.getNamespace().get("humidityLabel"));

                LOGGER.info("Controllers wired successfully.");
            } else {
                LOGGER.severe("Error: One or more controllers are null.");
                return;
            }

            primaryStage.setTitle("Cartography and Route Management");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            LOGGER.severe("IOException while loading FXML: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Unexpected error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}