package esprit.tn.guiproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Logger;

public class MainApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/MainView.fxml"));
            if (fxmlLoader.getLocation() == null) {
                LOGGER.severe("Error: Cannot find MainView.fxml at /esprit/tn/guiproject/views/MainView.fxml");
                return;
            }
            Scene scene = new Scene(fxmlLoader.load(), 1000, 700); // Reduced to 1000x700
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