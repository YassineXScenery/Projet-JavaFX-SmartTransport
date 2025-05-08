package esprit.tn.guiproject.controllers;

import esprit.tn.guiproject.models.PointInteret;
import esprit.tn.guiproject.models.Trajet;
import esprit.tn.guiproject.services.TrajetService;
import esprit.tn.guiproject.services.PointInteretService;
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

    private final TrajetService trajetService = new TrajetService();
    private final PointInteretService poiService = new PointInteretService();
    private final ObservableList<Trajet> routeList = FXCollections.observableArrayList();
    private MapController mapController;

    @FXML
    public void initialize() {
        System.out.println("TrajetController initialize called");
        System.out.println("routeTable: " + (routeTable != null ? "not null" : "null"));
        System.out.println("routeStartPointField: " + (routeStartPointField != null ? "not null" : "null"));
        System.out.println("routeEndPointField: " + (routeEndPointField != null ? "not null" : "null"));
        System.out.println("addRouteButton: " + (addRouteButton != null ? "not null" : "null"));
        System.out.println("updateRouteButton: " + (updateRouteButton != null ? "not null" : "null"));
        System.out.println("deleteRouteButton: " + (deleteRouteButton != null ? "not null" : "null"));
        System.out.println("clearRouteButton: " + (clearRouteButton != null ? "not null" : "null"));
        System.out.println("removeAllButton: " + (removeAllButton != null ? "not null" : "null"));

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
                // Display the selected route on the map
                if (mapController != null) {
                    displayRouteOnMap(newSelection);
                    System.out.println("Displayed route for Trajet ID: " + newSelection.getId());
                } else {
                    System.out.println("MapController is null, cannot display route.");
                }
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

        if (addRouteButton != null) {
            addRouteButton.setOnAction(event -> addRoute());
            System.out.println("Manually set onAction for addRouteButton");
        } else {
            System.out.println("addRouteButton is null in initialize()");
        }
        if (updateRouteButton != null) {
            updateRouteButton.setOnAction(event -> updateRoute());
            System.out.println("Manually set onAction for updateRouteButton");
        } else {
            System.out.println("updateRouteButton is null in initialize()");
        }
        if (deleteRouteButton != null) {
            deleteRouteButton.setOnAction(event -> deleteRoute());
            System.out.println("Manually set onAction for deleteRouteButton");
        } else {
            System.out.println("deleteRouteButton is null in initialize()");
        }
        if (clearRouteButton != null) {
            clearRouteButton.setOnAction(event -> clearRouteFields());
            System.out.println("Manually set onAction for clearRouteButton");
        } else {
            System.out.println("clearRouteButton is null in initialize()");
        }
        if (removeAllButton != null) {
            removeAllButton.setOnAction(event -> removeAll());
            System.out.println("Manually set onAction for removeAllButton");
        } else {
            System.out.println("removeAllButton is null in initialize()");
        }
    }

    public void setMapController(MapController controller) {
        this.mapController = controller;
        System.out.println("MapController set in TrajetController: " + (controller != null ? "not null" : "null"));
    }

    public Trajet getSelectedTrajet() {
        return routeTable.getSelectionModel().getSelectedItem();
    }

    private void loadRouteData() {
        System.out.println("Loading route data...");
        routeList.clear();
        routeList.addAll(trajetService.afficher());
        System.out.println("Route data loaded: " + routeList.size() + " routes");
    }

    @FXML
    private void addRoute() {
        System.out.println("addRoute() method invoked");
        try {
            String startInput = routeStartPointField.getText().trim();
            String endInput = routeEndPointField.getText().trim();
            System.out.println("Start ID input: '" + startInput + "', End ID input: '" + endInput + "'");

            Integer startId = startInput.isEmpty() ? null : Integer.parseInt(startInput);
            Integer endId = endInput.isEmpty() ? null : Integer.parseInt(endInput);
            if (startId == null || endId == null) {
                System.out.println("Invalid input: startId or endId is null");
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid Start and End Point IDs.");
                return;
            }
            System.out.println("Parsed IDs: startId=" + startId + ", endId=" + endId);

            PointInteret startPoint = poiService.getById(startId);
            PointInteret endPoint = poiService.getById(endId);
            System.out.println("Start Point: " + (startPoint != null ? startPoint.getNom() + " (ID: " + startId + ")" : "null"));
            System.out.println("End Point: " + (endPoint != null ? endPoint.getNom() + " (ID: " + endId + ")" : "null"));
            if (startPoint == null || endPoint == null) {
                System.out.println("One or both Point IDs do not exist in the database");
                showAlert(Alert.AlertType.ERROR, "Invalid Points", "One or both Point IDs do not exist in the database.");
                return;
            }

            if (mapController == null) {
                System.out.println("MapController is not initialized");
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
            trajet.setTempsEstime(new Time(10 * 60 * 1000));
            System.out.println("Created Trajet: point_depart=" + startId + ", point_arrivee=" + endId + ", distance=" + distance);

            System.out.println("Attempting to add Trajet to database...");
            int addedId = trajetService.ajouter(trajet);
            System.out.println("trajetService.ajouter returned: " + addedId);
            if (addedId == -1) {
                System.out.println("Failed to add Trajet to database");
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add the route to the database.");
                return;
            }

            loadRouteData();
            clearRouteFields();

            // Refresh map to show new POIs and route
            if (mapController != null) {
                mapController.refreshMap();
                displayRouteOnMap(trajet);
                System.out.println("Map refreshed and route displayed after adding Trajet ID: " + addedId);
            } else {
                System.out.println("MapController is null, cannot refresh map.");
            }

            System.out.println("Route added successfully");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Route added successfully! Distance: " + String.format("%.2f", distance) + " km");
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for point IDs.");
            LOGGER.log(Level.WARNING, "Invalid input for point IDs", e);
        } catch (Exception e) {
            System.out.println("Unexpected error in addRoute: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while adding the Route: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error adding Route", e);
        }
    }

    @FXML
    private void updateRoute() {
        System.out.println("updateRoute() method invoked");
        Trajet selected = routeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No route selected for update");
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a Route to update.");
            return;
        }
        System.out.println("Selected Trajet ID: " + selected.getId());
        try {
            String startInput = routeStartPointField.getText().trim();
            String endInput = routeEndPointField.getText().trim();
            System.out.println("Start ID input: '" + startInput + "', End ID input: '" + endInput + "'");

            Integer startId = startInput.isEmpty() ? null : Integer.parseInt(startInput);
            Integer endId = endInput.isEmpty() ? null : Integer.parseInt(endInput);
            if (startId == null || endId == null) {
                System.out.println("Invalid input: startId or endId is null");
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid Start and End Point IDs.");
                return;
            }
            System.out.println("Parsed IDs: startId=" + startId + ", endId=" + endId);

            PointInteret startPoint = poiService.getById(startId);
            PointInteret endPoint = poiService.getById(endId);
            System.out.println("Start Point: " + (startPoint != null ? startPoint.getNom() + " (ID: " + startId + ")" : "null"));
            System.out.println("End Point: " + (endPoint != null ? endPoint.getNom() + " (ID: " + endId + ")" : "null"));
            if (startPoint == null || endPoint == null) {
                System.out.println("One or both Point IDs do not exist in the database");
                showAlert(Alert.AlertType.ERROR, "Invalid Points", "One or both Point IDs do not exist in the database.");
                return;
            }

            if (mapController == null) {
                System.out.println("MapController is not initialized");
                showAlert(Alert.AlertType.ERROR, "Error", "MapController is not initialized.");
                return;
            }

            double distance = mapController.calculateDistance(
                    startPoint.getLatitude(), startPoint.getLongitude(),
                    endPoint.getLatitude(), endPoint.getLongitude()
            );
            System.out.println("Calculated distance: " + distance + " km");

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
            if (selected.getTempsEstime() == null) {
                selected.setTempsEstime(new Time(10 * 60 * 1000));
            }
            System.out.println("Updating Trajet ID: " + selected.getId() + " with point_depart=" + startId + ", point_arrivee=" + endId + ", distance=" + distance);
            trajetService.modifier(selected);
            System.out.println("TrajetService.modifier called for Trajet ID: " + selected.getId());

            loadRouteData();
            clearRouteFields();

            // Display updated route on map
            if (mapController != null) {
                displayRouteOnMap(selected);
                System.out.println("Displayed route after updating Trajet ID: " + selected.getId());
            } else {
                System.out.println("MapController is null, cannot display route.");
            }

            System.out.println("Route updated successfully");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Route updated successfully! Distance: " + String.format("%.2f", distance) + " km");
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for point IDs.");
            LOGGER.log(Level.WARNING, "Invalid input for point IDs", e);
        } catch (Exception e) {
            System.out.println("Unexpected error in updateRoute: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while updating the Route: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error updating Route", e);
        }
    }

    @FXML
    private void deleteRoute() {
        System.out.println("deleteRoute() method invoked");
        Trajet selected = routeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No route selected for deletion");
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a Route to delete.");
            return;
        }
        System.out.println("Selected Trajet ID: " + selected.getId());
        try {
            System.out.println("Calling trajetService.supprimer for Trajet ID: " + selected.getId());
            trajetService.supprimer(selected.getId());
            routeList.remove(selected);
            clearRouteFields();
            // Refresh map to clear routes and reload POIs
            if (mapController != null) {
                mapController.refreshMap();
                System.out.println("Map refreshed after deleting Trajet ID: " + selected.getId());
            } else {
                System.out.println("MapController is null, cannot refresh map.");
            }
            System.out.println("Route deleted successfully");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Route deleted successfully!");
        } catch (Exception e) {
            System.out.println("Unexpected error in deleteRoute: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while deleting the Route: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error deleting Route", e);
        }
    }

    @FXML
    private void clearRouteFields() {
        System.out.println("clearRouteFields() method invoked");
        try {
            routeStartPointField.clear();
            routeEndPointField.clear();
            routeTable.getSelectionModel().clearSelection();
            System.out.println("Route fields cleared");
        } catch (Exception e) {
            System.out.println("Unexpected error in clearRouteFields: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while clearing fields: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error clearing route fields", e);
        }
    }

    private void populateRouteFields(Trajet trajet) {
        System.out.println("Populating route fields for Trajet ID: " + trajet.getId());
        routeStartPointField.setText(trajet.getPointDepart() != null ? String.valueOf(trajet.getPointDepart()) : "");
        routeEndPointField.setText(trajet.getPointArrivee() != null ? String.valueOf(trajet.getPointArrivee()) : "");
    }

    public void createTrajetFromRoute(PointInteret start, PointInteret end, double distance, Time estimatedTime) {
        System.out.println("createTrajetFromRoute called: start ID=" + (start != null ? start.getId() : "null") +
                ", end ID=" + (end != null ? end.getId() : "null"));
        try {
            if (start == null || end == null) {
                System.out.println("Invalid input: start or end PointInteret is null");
                showAlert(Alert.AlertType.ERROR, "Error", "Start or end point is null.");
                return;
            }

            System.out.println("Start PointInteret: id=" + start.getId() +
                    ", latitude=" + start.getLatitude() +
                    ", longitude=" + start.getLongitude() +
                    ", nom=" + start.getNom() +
                    ", type=" + start.getType());
            System.out.println("End PointInteret: id=" + end.getId() +
                    ", latitude=" + end.getLatitude() +
                    ", longitude=" + end.getLongitude() +
                    ", nom=" + end.getNom() +
                    ", type=" + end.getType());

            // Validate coordinates
            if (start.getLatitude() == 0.0 || start.getLongitude() == 0.0 ||
                    end.getLatitude() == 0.0 || end.getLongitude() == 0.0) {
                System.out.println("Invalid coordinates: One or more coordinates are zero");
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid coordinates: Points cannot have zero latitude or longitude.");
                return;
            }

            Trajet trajet = new Trajet();
            trajet.setDistance(distance);
            trajet.setTempsEstime(estimatedTime);

            // Use PointInteret IDs if available, otherwise store coordinates
            if (start.getId() > 0) {
                trajet.setPointDepart(start.getId());
            } else {
                trajet.setStartLatitude(start.getLatitude());
                trajet.setStartLongitude(start.getLongitude());
                trajet.setStartNom(start.getNom());
                trajet.setStartType(start.getType());
            }
            if (end.getId() > 0) {
                trajet.setPointArrivee(end.getId());
            } else {
                trajet.setEndLatitude(end.getLatitude());
                trajet.setEndLongitude(end.getLongitude());
                trajet.setEndNom(end.getNom());
                trajet.setEndType(end.getType());
            }

            System.out.println("Trajet created: point_depart=" + trajet.getPointDepart() +
                    ", point_arrivee=" + trajet.getPointArrivee() +
                    ", start_latitude=" + trajet.getStartLatitude() +
                    ", start_longitude=" + trajet.getStartLongitude() +
                    ", end_latitude=" + trajet.getEndLatitude() +
                    ", end_longitude=" + trajet.getEndLongitude());

            int addedId = trajetService.ajouter(trajet);
            if (addedId == -1) {
                System.out.println("Failed to add Trajet from map selection");
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add the route to the database.");
                return;
            }

            loadRouteData();
            Platform.runLater(() -> {
                routeStartPointField.setText(trajet.getPointDepart() != null ? String.valueOf(trajet.getPointDepart()) : "");
                routeEndPointField.setText(trajet.getPointArrivee() != null ? String.valueOf(trajet.getPointArrivee()) : "");
            });

            // Refresh map to show new route and all POIs
            if (mapController != null) {
                mapController.refreshMap();
                displayRouteOnMap(trajet);
                System.out.println("Map refreshed and route displayed after creating Trajet ID: " + addedId);
            } else {
                System.out.println("MapController is null, cannot refresh map.");
            }

            System.out.println("Route added from map selection with ID: " + addedId);
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Route added from map selection! Distance: " + String.format("%.2f", distance) + " km, Estimated Time: " + estimatedTime);
        } catch (Exception e) {
            System.out.println("Unexpected error in createTrajetFromRoute: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while creating the Trajet: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error creating Trajet from route", e);
        }
    }

    @FXML
    private void removeAll() {
        System.out.println("removeAll() method invoked");
        try {
            trajetService.removeAll();
            routeList.clear();
            clearRouteFields();
            // Refresh map to clear routes and reload POIs
            if (mapController != null) {
                mapController.refreshMap();
                System.out.println("Map refreshed after removing all Trajets");
            } else {
                System.out.println("MapController is null, cannot refresh map.");
            }
            System.out.println("All routes removed successfully");
            showAlert(Alert.AlertType.INFORMATION, "Success", "All routes removed successfully!");
        } catch (Exception e) {
            System.out.println("Unexpected error in removeAll: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while removing all routes: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error removing all routes", e);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        System.out.println("Showing alert: " + type + " - " + title + ": " + content);
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void displayRouteOnMap(Trajet trajet) {
        System.out.println("displayRouteOnMap called for Trajet ID: " + trajet.getId());
        System.out.println("Trajet details: point_depart=" + trajet.getPointDepart() +
                ", point_arrivee=" + trajet.getPointArrivee() +
                ", start_latitude=" + trajet.getStartLatitude() +
                ", start_longitude=" + trajet.getStartLongitude() +
                ", end_latitude=" + trajet.getEndLatitude() +
                ", end_longitude=" + trajet.getEndLongitude());
        try {
            if (mapController == null) {
                System.out.println("MapController is null");
                showAlert(Alert.AlertType.ERROR, "Error", "MapController is not initialized.");
                return;
            }

            // Clear the map to remove any existing routes and markers
            mapController.clearMap();

            Double startLat = null, startLng = null, endLat = null, endLng = null;

            // Try to get coordinates from PointInteret if point_depart/point_arrivee exist
            if (trajet.getPointDepart() != null) {
                PointInteret startPoint = poiService.getById(trajet.getPointDepart());
                if (startPoint != null) {
                    startLat = startPoint.getLatitude();
                    startLng = startPoint.getLongitude();
                    System.out.println("Using PointInteret for start: ID=" + trajet.getPointDepart() +
                            ", lat=" + startLat + ", lng=" + startLng);
                } else {
                    System.out.println("PointInteret not found for point_depart ID: " + trajet.getPointDepart());
                }
            }
            if (trajet.getPointArrivee() != null) {
                PointInteret endPoint = poiService.getById(trajet.getPointArrivee());
                if (endPoint != null) {
                    endLat = endPoint.getLatitude();
                    endLng = endPoint.getLongitude();
                    System.out.println("Using PointInteret for end: ID=" + trajet.getPointArrivee() +
                            ", lat=" + endLat + ", lng=" + endLng);
                } else {
                    System.out.println("PointInteret not found for point_arrivee ID: " + trajet.getPointArrivee());
                }
            }

            // Fall back to start_latitude/start_longitude and end_latitude/end_longitude if available
            if (startLat == null || startLng == null) {
                startLat = trajet.getStartLatitude();
                startLng = trajet.getStartLongitude();
                System.out.println("Using stored coordinates for start: lat=" + startLat + ", lng=" + startLng);
            }
            if (endLat == null || endLng == null) {
                endLat = trajet.getEndLatitude();
                endLng = trajet.getEndLongitude();
                System.out.println("Using stored coordinates for end: lat=" + endLat + ", lng=" + endLng);
            }

            // Validate coordinates before calling displayRoute
            if (startLat != null && startLng != null && endLat != null && endLng != null) {
                System.out.println("Calling mapController.displayRoute: startLat=" + startLat +
                        ", startLng=" + startLng + ", endLat=" + endLat +
                        ", endLng=" + endLng + ", distance=" + trajet.getDistance());
                mapController.displayRoute(startLat, startLng, endLat, endLng, trajet.getDistance());
                System.out.println("Displayed route on map for Trajet ID: " + trajet.getId());
            } else {
                System.out.println("Cannot display route: Missing coordinates for Trajet ID: " + trajet.getId());
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Cannot display route: Missing start or end coordinates.");
            }
        } catch (Exception e) {
            System.out.println("Error displaying route: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error",
                    "An error occurred while displaying the route: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error displaying route on map", e);
        }
    }
}