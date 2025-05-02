package esprit.tn.guiproject.controllers;

import esprit.tn.guiproject.models.PointInteret;
import esprit.tn.guiproject.models.Trajet;
import esprit.tn.guiproject.services.TrajetService;
import esprit.tn.guiproject.services.PointInteretService;
import esprit.tn.guiproject.controllers.MapController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.Time;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrajetController {

    private static final Logger LOGGER = Logger.getLogger(TrajetController.class.getName());

    @FXML private TableView<Trajet> routeTable;
    @FXML private TableColumn<Trajet, Integer> routeIdColumn;
    @FXML private TableColumn<Trajet, Double> routeDistanceColumn;
    @FXML private TableColumn<Trajet, Integer> routeStartPointColumn;
    @FXML private TableColumn<Trajet, Integer> routeEndPointColumn;
    @FXML private TableColumn<Trajet, Time> routeTimeColumn;

    @FXML private TextField routeStartPointField;
    @FXML private TextField routeEndPointField;
    @FXML private Button addRouteButton;
    @FXML private Button updateRouteButton;
    @FXML private Button deleteRouteButton;
    @FXML private Button clearRouteButton;
    @FXML private Button selectRouteButton;
    @FXML private Button removeAllButton;

    private TrajetService trajetService = new TrajetService();
    private PointInteretService poiService = new PointInteretService();
    private ObservableList<Trajet> routeList = FXCollections.observableArrayList();
    private MapController mapController;

    @FXML
    public void initialize() {
        routeIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        routeDistanceColumn.setCellValueFactory(new PropertyValueFactory<>("distance"));
        routeStartPointColumn.setCellValueFactory(new PropertyValueFactory<>("pointDepart"));
        routeEndPointColumn.setCellValueFactory(new PropertyValueFactory<>("pointArrivee"));
        routeTimeColumn.setCellValueFactory(new PropertyValueFactory<>("tempsEstime"));
        routeTable.setItems(routeList);
        loadRouteData();

        routeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateRouteFields(newSelection);
                displayRouteOnMap(newSelection);
            }
        });

        selectRouteButton.setOnAction(event -> {
            if (mapController != null) {
                mapController.startRouteSelection();
                showAlert(Alert.AlertType.INFORMATION, "Route Selection", "Click two points on the map or existing POIs to select start and end points.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Map controller not initialized.");
            }
        });
    }

    public void setMapController(MapController controller) {
        this.mapController = controller;
        System.out.println("MapController set in TrajetController.");
    }

    private void loadRouteData() {
        routeList.clear();
        routeList.addAll(trajetService.afficher());
    }

    @FXML
    private void addRoute() {
        try {
            Integer startId = routeStartPointField.getText().isEmpty() ? null : Integer.parseInt(routeStartPointField.getText());
            Integer endId = routeEndPointField.getText().isEmpty() ? null : Integer.parseInt(routeEndPointField.getText());
            if (startId == null || endId == null) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid Start and End Point IDs.");
                return;
            }

            PointInteret startPoint = poiService.getById(startId);
            PointInteret endPoint = poiService.getById(endId);
            if (startPoint == null || endPoint == null) {
                showAlert(Alert.AlertType.ERROR, "Invalid Points", "One or both Point IDs do not exist in the database.");
                return;
            }

            if (mapController == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "MapController is not initialized.");
                return;
            }

            double distance = mapController.calculateDistance(
                    startPoint.getLatitude(), startPoint.getLongitude(),
                    endPoint.getLatitude(), endPoint.getLongitude()
            );
            System.out.println("Calculated distance: " + distance + " km");

            Trajet trajet = new Trajet();
            trajet.setDistance(distance);
            trajet.setPointDepart(startId);
            trajet.setPointArrivee(endId);
            // Set a default estimated time (e.g., 10 minutes) since it's not provided in the form
            trajet.setTempsEstime(new Time(10 * 60 * 1000)); // 10 minutes in milliseconds

            int addedId = trajetService.ajouter(trajet);
            if (addedId == -1) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add the route to the database.");
                return;
            }

            loadRouteData();
            clearRouteFields();

            mapController.displayRoute(
                    startPoint.getLatitude(), startPoint.getLongitude(),
                    endPoint.getLatitude(), endPoint.getLongitude(),
                    distance
            );

            showAlert(Alert.AlertType.INFORMATION, "Success", "Route added successfully! Distance: " + String.format("%.2f", distance) + " km");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for point IDs.");
            LOGGER.log(Level.WARNING, "Invalid input for point IDs", e);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while adding the Route: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error adding Route", e);
        }
    }

    @FXML
    private void updateRoute() {
        Trajet selected = routeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a Route to update.");
            return;
        }
        try {
            Integer startId = routeStartPointField.getText().isEmpty() ? null : Integer.parseInt(routeStartPointField.getText());
            Integer endId = routeEndPointField.getText().isEmpty() ? null : Integer.parseInt(routeEndPointField.getText());
            if (startId == null || endId == null) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid Start and End Point IDs.");
                return;
            }

            PointInteret startPoint = poiService.getById(startId);
            PointInteret endPoint = poiService.getById(endId);
            if (startPoint == null || endPoint == null) {
                showAlert(Alert.AlertType.ERROR, "Invalid Points", "One or both Point IDs do not exist in the database.");
                return;
            }

            if (mapController == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "MapController is not initialized.");
                return;
            }

            double distance = mapController.calculateDistance(
                    startPoint.getLatitude(), startPoint.getLongitude(),
                    endPoint.getLatitude(), endPoint.getLongitude()
            );

            selected.setDistance(distance);
            selected.setPointDepart(startId);
            selected.setPointArrivee(endId);
            selected.setStartLatitude(null);
            selected.setStartLongitude(null);
            selected.setStartNom(null);
            selected.setStartType(null);
            selected.setEndLatitude(null);
            selected.setEndLongitude(null);
            selected.setEndNom(null);
            selected.setEndType(null);
            // Ensure tempsEstime is not null
            if (selected.getTempsEstime() == null) {
                selected.setTempsEstime(new Time(10 * 60 * 1000)); // Default to 10 minutes
            }
            trajetService.modifier(selected);
            loadRouteData();
            clearRouteFields();

            mapController.displayRoute(
                    startPoint.getLatitude(), startPoint.getLongitude(),
                    endPoint.getLatitude(), endPoint.getLongitude(),
                    distance
            );

            showAlert(Alert.AlertType.INFORMATION, "Success", "Route updated successfully! Distance: " + String.format("%.2f", distance) + " km");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for point IDs.");
            LOGGER.log(Level.WARNING, "Invalid input for point IDs", e);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while updating the Route: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error updating Route", e);
        }
    }

    @FXML
    private void deleteRoute() {
        Trajet selected = routeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a Route to delete.");
            return;
        }
        try {
            trajetService.supprimer(selected.getId());
            routeList.remove(selected);
            clearRouteFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Route deleted successfully!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while deleting the Route: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error deleting Route", e);
        }
    }

    @FXML
    private void clearRouteFields() {
        routeStartPointField.clear();
        routeEndPointField.clear();
        routeTable.getSelectionModel().clearSelection();
    }

    private void populateRouteFields(Trajet trajet) {
        routeStartPointField.setText(trajet.getPointDepart() != null ? String.valueOf(trajet.getPointDepart()) : "");
        routeEndPointField.setText(trajet.getPointArrivee() != null ? String.valueOf(trajet.getPointArrivee()) : "");
    }

    public void createTrajetFromRoute(PointInteret start, PointInteret end, double distance, Time estimatedTime) {
        try {
            Trajet trajet = new Trajet(start, end, distance, estimatedTime);
            int addedId = trajetService.ajouter(trajet);
            if (addedId == -1) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add the route to the database.");
                return;
            }
            loadRouteData();
            Platform.runLater(() -> {
                routeStartPointField.setText(start.getId() > 0 ? String.valueOf(start.getId()) : "");
                routeEndPointField.setText(end.getId() > 0 ? String.valueOf(end.getId()) : "");
            });
            showAlert(Alert.AlertType.INFORMATION, "Success", "Route added from map selection! Distance: " + String.format("%.2f", distance) + " km, Estimated Time: " + estimatedTime);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while creating the Trajet: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error creating Trajet from route", e);
        }
    }

    @FXML
    private void removeAll() {
        try {
            trajetService.removeAll();
            routeList.clear();
            clearRouteFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "All routes removed successfully!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while removing all routes: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error removing all routes", e);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void displayRouteOnMap(Trajet trajet) {
        try {
            if (mapController != null) {
                Double startLat, startLng, endLat, endLng;
                if (trajet.getPointDepart() != null) {
                    PointInteret startPoint = poiService.getById(trajet.getPointDepart());
                    startLat = startPoint.getLatitude();
                    startLng = startPoint.getLongitude();
                } else {
                    startLat = trajet.getStartLatitude();
                    startLng = trajet.getStartLongitude();
                }
                if (trajet.getPointArrivee() != null) {
                    PointInteret endPoint = poiService.getById(trajet.getPointArrivee());
                    endLat = endPoint.getLatitude();
                    endLng = endPoint.getLongitude();
                } else {
                    endLat = trajet.getEndLatitude();
                    endLng = trajet.getEndLongitude();
                }
                if (startLat != null && startLng != null && endLat != null && endLng != null) {
                    mapController.displayRoute(startLat, startLng, endLat, endLng, trajet.getDistance());
                    System.out.println("Displayed route on map: Start ID " + trajet.getPointDepart() + ", End ID " + trajet.getPointArrivee());
                } else {
                    System.out.println("Error: Start or End Point coordinates not found for IDs " + trajet.getPointDepart() + ", " + trajet.getPointArrivee());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error displaying route on map", e);
            System.out.println("Error displaying route: " + e.getMessage());
        }
    }
}