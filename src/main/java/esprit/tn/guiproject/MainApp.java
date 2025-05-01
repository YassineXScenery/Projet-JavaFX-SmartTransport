package esprit.tn.guiproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/esprit/tn/guiproject/views/MainView.fxml"));
            if (fxmlLoader.getLocation() == null) {
                System.err.println("Error: Cannot find MainView.fxml at /esprit/tn/guiproject/views/MainView.fxml");
                return;
            }
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            primaryStage.setTitle("Cartography and Route Management");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("IOException while loading FXML: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}