package esprit.tn.guiproject.controllers;

import esprit.tn.guiproject.models.PointInteret;
import esprit.tn.guiproject.models.Trajet;
import esprit.tn.guiproject.services.TrajetService;
import esprit.tn.guiproject.services.PointInteretService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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

    @FXML private ComboBox<PointInteret> routeStartPointComboBox; // Editable ComboBox
    @FXML private ComboBox<PointInteret> routeEndPointComboBox;   // Editable ComboBox
    @FXML private Button addRouteButton;
    @FXML private Button updateRouteButton;
    @FXML private Button deleteRouteButton;
    @FXML private Button clearRouteButton;
    @FXML private Button selectRouteButton;
    @FXML private Button removeAllButton;

    private final TrajetService trajetService = new TrajetService();
    private final PointInteretService poiService = new PointInteretService();
    private final ObservableList<Trajet> routeList = FXCollections.observableArrayList();
    private final ObservableList<PointInteret> pointInteretList = FXCollections.observableArrayList(); // For ComboBoxes
    private FilteredList<PointInteret> filteredStartPoints; // For filtering
    private FilteredList<PointInteret> filteredEndPoints;   // For filtering
    private MapController mapController;

    @FXML
    public void initialize() {
        System.out.println("TrajetController initialize called");
        System.out.println("routeTable: " + (routeTable != null ? "not null" : "null"));
        System.out.println("routeStartPointComboBox: " + (routeStartPointComboBox != null ? "not null" : "null"));
        System.out.println("routeEndPointComboBox: " + (routeEndPointComboBox != null ? "not null" : "null"));
        System.out.println("addRouteButton: " + (addRouteButton != null ? "not null" : "null"));
        System.out.println("updateRouteButton: " + (updateRouteButton != null ? "not null" : "null"));
        System.out.println("deleteRouteButton: " + (deleteRouteButton != null ? "not null" : "null"));
        System.out.println("clearRouteButton: " + (clearRouteButton != null ? "not null" : "null"));
        System.out.println("removeAllButton: " + (removeAllButton != null ? "not null" : "null"));

        // Initialize TableView columns
        routeIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        routeDistanceColumn.setCellValueFactory(new PropertyValueFactory<>("distance"));
        routeStartPointColumn.setCellValueFactory(new PropertyValueFactory<>("pointDepart"));
        routeEndPointColumn.setCellValueFactory(new PropertyValueFactory<>("pointArrivee"));
        routeTimeColumn.setCellValueFactory(new PropertyValueFactory<>("tempsEstime"));
        routeTable.setItems(routeList);
        loadRouteData();

        // Populate and configure ComboBoxes with filtering
        loadPointInteretData();
        routeStartPointComboBox.setEditable(true); // Enable editing for filtering
        routeEndPointComboBox.setEditable(true);   // Enable editing for filtering

        // Set the initial number of visible rows for the dropdown
        routeStartPointComboBox.setVisibleRowCount(10); // Set to a reasonable maximum
        routeEndPointComboBox.setVisibleRowCount(10);   // Set to a reasonable maximum

        // Set up filtered lists
        filteredStartPoints = new FilteredList<>(pointInteretList, p -> true);
        filteredEndPoints = new FilteredList<>(pointInteretList, p -> true);

        // Bind filtered lists to ComboBoxes
        routeStartPointComboBox.setItems(filteredStartPoints);
        routeEndPointComboBox.setItems(filteredEndPoints);

        // Add listener to filter based on typed text for Start Point
        routeStartPointComboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            filteredStartPoints.setPredicate(point -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true; // Show all if no text
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return point.getNom().toLowerCase().contains(lowerCaseFilter);
            });
            // Ensure the dropdown is visible when filtered
            if (!filteredStartPoints.isEmpty()) {
                routeStartPointComboBox.show();
            } else {
                routeStartPointComboBox.hide();
            }
            // When text is cleared, force the dropdown to resize by hiding and re-showing
            if (newValue == null || newValue.isEmpty()) {
                routeStartPointComboBox.hide();
                Platform.runLater(() -> {
                    if (routeStartPointComboBox.getEditor().isFocused()) {
                        routeStartPointComboBox.show();
                    }
                });
            }
        });

        // Add listener to filter based on typed text for End Point
        routeEndPointComboBox.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            filteredEndPoints.setPredicate(point -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true; // Show all if no text
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return point.getNom().toLowerCase().contains(lowerCaseFilter);
            });
            // Ensure the dropdown is visible when filtered
            if (!filteredEndPoints.isEmpty()) {
                routeEndPointComboBox.show();
            } else {
                routeEndPointComboBox.hide();
            }
            // When text is cleared, force the dropdown to resize by hiding and re-showing
            if (newValue == null || newValue.isEmpty()) {
                routeEndPointComboBox.hide();
                Platform.runLater(() -> {
                    if (routeEndPointComboBox.getEditor().isFocused()) {
                        routeEndPointComboBox.show();
                    }
                });
            }
        });

        // Handle TableView selection
        routeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateRouteFields(newSelection);
                if (mapController != null) {
                    displayRouteOnMap(newSelection);
                    System.out.println("Displayed route for Trajet ID: " + newSelection.getId());
                } else {
                    System.out.println("MapController is null, cannot display route.");
                }
            }
        });

        // Set action for Select on Map button
        selectRouteButton.setOnAction(event -> {
            if (mapController != null) {
                mapController.startRouteSelection();
                showAlert(Alert.AlertType.INFORMATION, "Route Selection", "Click two points on the map or existing POIs to select start and end points.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Map controller not initialized.");
            }
        });

        // Set button actions
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

    private void loadPointInteretData() {
        System.out.println("Loading PointInteret data...");
        pointInteretList.clear();
        pointInteretList.addAll(poiService.afficher());
        System.out.println("PointInteret data loaded: " + pointInteretList.size() + " points");
    }

    @FXML
    private void addRoute() {
        System.out.println("addRoute() method invoked");
        try {
            PointInteret startPoint = routeStartPointComboBox.getSelectionModel().getSelectedItem();
            PointInteret endPoint = routeEndPointComboBox.getSelectionModel().getSelectedItem();
            System.out.println("Selected Start Point: " + (startPoint != null ? startPoint.toString() : "null"));
            System.out.println("Selected End Point: " + (endPoint != null ? endPoint.toString() : "null"));

            if (startPoint == null || endPoint == null) {
                System.out.println("Invalid selection: Start or End Point is not selected");
                showAlert(Alert.AlertType.ERROR, "Invalid Selection", "Please select both Start and End Points.");
                return;
            }

            Integer startId = startPoint.getId();
            Integer endId = endPoint.getId();
            System.out.println("Extracted IDs: startId=" + startId + ", endId=" + endId);

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
            trajet.setPointDepart(startId);  // Use only the ID
            trajet.setPointArrivee(endId);   // Use only the ID
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

            if (mapController != null) {
                mapController.refreshMap();
                displayRouteOnMap(trajet);
                System.out.println("Map refreshed and route displayed after adding Trajet ID: " + addedId);
            } else {
                System.out.println("MapController is null, cannot refresh map.");
            }

            System.out.println("Route added successfully");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Route added successfully! Distance: " + String.format("%.2f", distance) + " km");
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
            PointInteret startPoint = routeStartPointComboBox.getSelectionModel().getSelectedItem();
            PointInteret endPoint = routeEndPointComboBox.getSelectionModel().getSelectedItem();
            System.out.println("Selected Start Point: " + (startPoint != null ? startPoint.toString() : "null"));
            System.out.println("Selected End Point: " + (endPoint != null ? endPoint.toString() : "null"));

            if (startPoint == null || endPoint == null) {
                System.out.println("Invalid selection: Start or End Point is not selected");
                showAlert(Alert.AlertType.ERROR, "Invalid Selection", "Please select both Start and End Points.");
                return;
            }

            Integer startId = startPoint.getId();
            Integer endId = endPoint.getId();
            System.out.println("Extracted IDs: startId=" + startId + ", endId=" + endId);

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
            selected.setPointDepart(startId);  // Use only the ID
            selected.setPointArrivee(endId);   // Use only the ID
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

            if (mapController != null) {
                displayRouteOnMap(selected);
                System.out.println("Displayed route after updating Trajet ID: " + selected.getId());
            } else {
                System.out.println("MapController is null, cannot display route.");
            }

            System.out.println("Route updated successfully");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Route updated successfully! Distance: " + String.format("%.2f", distance) + " km");
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
            routeStartPointComboBox.getEditor().clear(); // Clear the editor text
            routeStartPointComboBox.getSelectionModel().clearSelection();
            routeEndPointComboBox.getEditor().clear();   // Clear the editor text
            routeEndPointComboBox.getSelectionModel().clearSelection();
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
        // Find the corresponding PointInteret objects for the IDs
        if (trajet.getPointDepart() != null) {
            PointInteret startPoint = pointInteretList.stream()
                    .filter(pi -> pi.getId() == trajet.getPointDepart())
                    .findFirst()
                    .orElse(null);
            if (startPoint != null) {
                routeStartPointComboBox.getSelectionModel().select(startPoint);
                routeStartPointComboBox.getEditor().setText(startPoint.toString()); // Set editor text
            } else {
                routeStartPointComboBox.getSelectionModel().clearSelection();
                routeStartPointComboBox.getEditor().clear();
            }
        } else {
            routeStartPointComboBox.getSelectionModel().clearSelection();
            routeStartPointComboBox.getEditor().clear();
        }

        if (trajet.getPointArrivee() != null) {
            PointInteret endPoint = pointInteretList.stream()
                    .filter(pi -> pi.getId() == trajet.getPointArrivee())
                    .findFirst()
                    .orElse(null);
            if (endPoint != null) {
                routeEndPointComboBox.getSelectionModel().select(endPoint);
                routeEndPointComboBox.getEditor().setText(endPoint.toString()); // Set editor text
            } else {
                routeEndPointComboBox.getSelectionModel().clearSelection();
                routeEndPointComboBox.getEditor().clear();
            }
        } else {
            routeEndPointComboBox.getSelectionModel().clearSelection();
            routeEndPointComboBox.getEditor().clear();
        }
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

            if (start.getLatitude() == 0.0 || start.getLongitude() == 0.0 ||
                    end.getLatitude() == 0.0 || end.getLongitude() == 0.0) {
                System.out.println("Invalid coordinates: One or more coordinates are zero");
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid coordinates: Points cannot have zero latitude or longitude.");
                return;
            }

            Trajet trajet = new Trajet();
            trajet.setDistance(distance);
            trajet.setTempsEstime(estimatedTime);

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
                // Find and select the corresponding PointInteret objects in the ComboBoxes
                PointInteret startPoint = pointInteretList.stream()
                        .filter(pi -> pi.getId() == trajet.getPointDepart())
                        .findFirst()
                        .orElse(null);
                PointInteret endPoint = pointInteretList.stream()
                        .filter(pi -> pi.getId() == trajet.getPointArrivee())
                        .findFirst()
                        .orElse(null);
                if (startPoint != null) {
                    routeStartPointComboBox.getSelectionModel().select(startPoint);
                    routeStartPointComboBox.getEditor().setText(startPoint.toString());
                }
                if (endPoint != null) {
                    routeEndPointComboBox.getSelectionModel().select(endPoint);
                    routeEndPointComboBox.getEditor().setText(endPoint.toString());
                }
            });

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

            mapController.clearMap();

            Double startLat = null, startLng = null, endLat = null, endLng = null;

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