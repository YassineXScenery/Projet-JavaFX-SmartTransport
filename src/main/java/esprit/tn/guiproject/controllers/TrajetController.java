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
import javafx.util.Callback;
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

    @FXML private ComboBox<PointInteret> routeStartPointComboBox;
    @FXML private ComboBox<PointInteret> routeEndPointComboBox;
    @FXML private Button addRouteButton;
    @FXML private Button updateRouteButton;
    @FXML private Button deleteRouteButton;
    @FXML private Button clearRouteButton;
    @FXML private Button selectRouteButton;
    @FXML private Button removeAllButton;

    private final TrajetService trajetService = new TrajetService();
    private final PointInteretService poiService = new PointInteretService();
    private final ObservableList<Trajet> routeList = FXCollections.observableArrayList();
    private final ObservableList<PointInteret> pointInteretList = FXCollections.observableArrayList();
    private FilteredList<PointInteret> filteredStartPoints;
    private FilteredList<PointInteret> filteredEndPoints;
    private MapController mapController;

    @FXML
    public void initialize() {
        System.out.println("TrajetController initialize called");

        // Initialize TableView columns
        routeIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        routeDistanceColumn.setCellValueFactory(new PropertyValueFactory<>("distance"));
        routeStartPointColumn.setCellValueFactory(new PropertyValueFactory<>("pointDepart"));
        routeEndPointColumn.setCellValueFactory(new PropertyValueFactory<>("pointArrivee"));
        routeTimeColumn.setCellValueFactory(new PropertyValueFactory<>("tempsEstime"));
        routeTable.setItems(routeList);
        loadRouteData();

        // Populate and configure ComboBoxes with improved setup
        loadPointInteretData();
        setupComboBoxes();  // Use the new method instead of the old code

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
        }
        if (updateRouteButton != null) {
            updateRouteButton.setOnAction(event -> updateRoute());
        }
        if (deleteRouteButton != null) {
            deleteRouteButton.setOnAction(event -> deleteRoute());
        }
        if (clearRouteButton != null) {
            clearRouteButton.setOnAction(event -> clearRouteFields());
        }
        if (removeAllButton != null) {
            removeAllButton.setOnAction(event -> removeAll());
        }
    }

    // Setup ComboBoxes with proper cell factories and event handling
    private void setupComboBoxes() {
        // Create filtered lists with a stable predicate (initially show all)
        filteredStartPoints = new FilteredList<>(pointInteretList, p -> true);
        filteredEndPoints = new FilteredList<>(pointInteretList, p -> true);

        routeStartPointComboBox.setItems(filteredStartPoints);
        routeEndPointComboBox.setItems(filteredEndPoints);

        // Make comboboxes editable
        routeStartPointComboBox.setEditable(true);
        routeEndPointComboBox.setEditable(true);

        // Create a StringConverter for both ComboBoxes
        javafx.util.StringConverter<PointInteret> converter = new javafx.util.StringConverter<PointInteret>() {
            @Override
            public String toString(PointInteret point) {
                return point != null ? "ID: " + point.getId() + " - Name: " + point.getNom() : "";
            }

            @Override
            public PointInteret fromString(String string) {
                if (string == null || string.isEmpty()) return null;
                return pointInteretList.stream()
                        .filter(pi -> ("ID: " + pi.getId() + " - Name: " + pi.getNom()).equals(string))
                        .findFirst()
                        .orElse(null);
            }
        };

        routeStartPointComboBox.setConverter(converter);
        routeEndPointComboBox.setConverter(converter);

        // Create custom cell factories to display PointInteret objects properly
        Callback<ListView<PointInteret>, ListCell<PointInteret>> cellFactory = p -> new ListCell<PointInteret>() {
            @Override
            protected void updateItem(PointInteret item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("ID: " + item.getId() + " - Name: " + item.getNom());
                }
            }

            // Add safe mouse handling to prevent exceptions during direct clicks
            {
                setOnMouseClicked(event -> {
                    if (getItem() != null) {
                        // Get the parent ComboBox
                        @SuppressWarnings("unchecked")
                        ComboBox<PointInteret> comboBox = (ComboBox<PointInteret>) getListView().getParent().getParent().getParent();

                        // Safely set the value and update UI
                        Platform.runLater(() -> {
                            PointInteret selectedItem = getItem();
                            comboBox.setValue(selectedItem);
                            comboBox.getEditor().setText("ID: " + selectedItem.getId() + " - Name: " + selectedItem.getNom());
                            comboBox.hide();
                        });

                        event.consume();
                    }
                });
            }
        };

        routeStartPointComboBox.setCellFactory(cellFactory);
        routeEndPointComboBox.setCellFactory(cellFactory);

        // Set up safe filtering for start point ComboBox
        routeStartPointComboBox.getEditor().focusedProperty().addListener((obs, oldVal, newVal) -> {
            // When gaining focus, ensure all items are visible
            if (newVal) {
                Platform.runLater(() -> {
                    filteredStartPoints.setPredicate(p -> true);
                });
            }
        });

        routeStartPointComboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            // Avoid filtering during selection events
            Platform.runLater(() -> {
                if (!routeStartPointComboBox.isShowing() && newVal != null) {
                    filteredStartPoints.setPredicate(point -> {
                        if (newVal.isEmpty()) return true;
                        String lowerCaseFilter = newVal.toLowerCase();
                        return point.getNom().toLowerCase().contains(lowerCaseFilter) ||
                                String.valueOf(point.getId()).contains(lowerCaseFilter);
                    });

                    if (!filteredStartPoints.isEmpty()) {
                        routeStartPointComboBox.show();
                    }
                }
            });
        });

        // Custom showing/hiding behavior to prevent empty list exceptions
        routeStartPointComboBox.setOnShowing(event -> {
            Platform.runLater(() -> {
                // Ensure we have items to show
                if (filteredStartPoints.isEmpty()) {
                    filteredStartPoints.setPredicate(p -> true);
                }
            });
        });

        // Safe selection handling
        routeStartPointComboBox.setOnAction(event -> {
            Platform.runLater(() -> {
                PointInteret selected = routeStartPointComboBox.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    // Update editor text without triggering the filter
                    String displayText = "ID: " + selected.getId() + " - Name: " + selected.getNom();
                    if (!routeStartPointComboBox.getEditor().getText().equals(displayText)) {
                        routeStartPointComboBox.getEditor().setText(displayText);
                    }
                }
            });
        });

        // Repeat the same setup for end point ComboBox
        routeEndPointComboBox.getEditor().focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(() -> {
                    filteredEndPoints.setPredicate(p -> true);
                });
            }
        });

        routeEndPointComboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                if (!routeEndPointComboBox.isShowing() && newVal != null) {
                    filteredEndPoints.setPredicate(point -> {
                        if (newVal.isEmpty()) return true;
                        String lowerCaseFilter = newVal.toLowerCase();
                        return point.getNom().toLowerCase().contains(lowerCaseFilter) ||
                                String.valueOf(point.getId()).contains(lowerCaseFilter);
                    });

                    if (!filteredEndPoints.isEmpty()) {
                        routeEndPointComboBox.show();
                    }
                }
            });
        });

        routeEndPointComboBox.setOnShowing(event -> {
            Platform.runLater(() -> {
                if (filteredEndPoints.isEmpty()) {
                    filteredEndPoints.setPredicate(p -> true);
                }
            });
        });

        routeEndPointComboBox.setOnAction(event -> {
            Platform.runLater(() -> {
                PointInteret selected = routeEndPointComboBox.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    String displayText = "ID: " + selected.getId() + " - Name: " + selected.getNom();
                    if (!routeEndPointComboBox.getEditor().getText().equals(displayText)) {
                        routeEndPointComboBox.getEditor().setText(displayText);
                    }
                }
            });
        });
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
            PointInteret startPoint = routeStartPointComboBox.getValue();
            PointInteret endPoint = routeEndPointComboBox.getValue();
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
            PointInteret startPoint = routeStartPointComboBox.getValue();
            PointInteret endPoint = routeEndPointComboBox.getValue();
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

    /**
     * Safely clears the route fields
     */
    @FXML
    private void clearRouteFields() {
        System.out.println("clearRouteFields() method invoked");
        try {
            Platform.runLater(() -> {
                // First reset the filtering so we have all items
                filteredStartPoints.setPredicate(p -> true);
                filteredEndPoints.setPredicate(p -> true);

                // Then clear values safely
                routeStartPointComboBox.setValue(null);
                routeEndPointComboBox.setValue(null);
                routeStartPointComboBox.getEditor().setText("");
                routeEndPointComboBox.getEditor().setText("");
                routeTable.getSelectionModel().clearSelection();
            });
            System.out.println("Route fields cleared");
        } catch (Exception e) {
            System.out.println("Unexpected error in clearRouteFields: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while clearing fields: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error clearing route fields", e);
        }
    }

    /**
     * Safe method to populate route fields that avoids IndexOutOfBoundsException
     */
    private void populateRouteFields(Trajet trajet) {
        System.out.println("Populating route fields for Trajet ID: " + trajet.getId());

        Platform.runLater(() -> {
            // First, reset filtering to show all items
            filteredStartPoints.setPredicate(p -> true);
            filteredEndPoints.setPredicate(p -> true);

            // Then populate start point
            if (trajet.getPointDepart() != null) {
                final Integer startId = trajet.getPointDepart();
                PointInteret startPoint = pointInteretList.stream()
                        .filter(pi -> pi.getId() == startId)
                        .findFirst()
                        .orElse(null);

                if (startPoint != null) {
                    // Set value first
                    routeStartPointComboBox.setValue(startPoint);
                    // Then update the editor text
                    routeStartPointComboBox.getEditor().setText("ID: " + startPoint.getId() + " - Name: " + startPoint.getNom());
                } else {
                    routeStartPointComboBox.setValue(null);
                    routeStartPointComboBox.getEditor().setText("");
                }
            } else {
                routeStartPointComboBox.setValue(null);
                routeStartPointComboBox.getEditor().setText("");
            }

            // Populate end point with the same safe approach
            if (trajet.getPointArrivee() != null) {
                final Integer endId = trajet.getPointArrivee();
                PointInteret endPoint = pointInteretList.stream()
                        .filter(pi -> pi.getId() == endId)
                        .findFirst()
                        .orElse(null);

                if (endPoint != null) {
                    routeEndPointComboBox.setValue(endPoint);
                    routeEndPointComboBox.getEditor().setText("ID: " + endPoint.getId() + " - Name: " + endPoint.getNom());
                } else {
                    routeEndPointComboBox.setValue(null);
                    routeEndPointComboBox.getEditor().setText("");
                }
            } else {
                routeEndPointComboBox.setValue(null);
                routeEndPointComboBox.getEditor().setText("");
            }
        });
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

            int addedId = trajetService.ajouter(trajet);
            if (addedId == -1) {
                System.out.println("Failed to add Trajet from map selection");
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add the route to the database.");
                return;
            }

            loadRouteData();
            Platform.runLater(() -> {
                routeStartPointComboBox.setValue(start);
                routeEndPointComboBox.setValue(end);
                routeStartPointComboBox.getEditor().setText("ID: " + start.getId() + " - Name: " + start.getNom());
                routeEndPointComboBox.getEditor().setText("ID: " + end.getId() + " - Name: " + end.getNom());
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
                }
            }
            if (trajet.getPointArrivee() != null) {
                PointInteret endPoint = poiService.getById(trajet.getPointArrivee());
                if (endPoint != null) {
                    endLat = endPoint.getLatitude();
                    endLng = endPoint.getLongitude();
                }
            }

            if (startLat == null || startLng == null) {
                startLat = trajet.getStartLatitude();
                startLng = trajet.getStartLongitude();
            }
            if (endLat == null || endLng == null) {
                endLat = trajet.getEndLatitude();
                endLng = trajet.getEndLongitude();
            }

            if (startLat != null && startLng != null && endLat != null && endLng != null) {
                mapController.displayRoute(startLat, startLng, endLat, endLng, trajet.getDistance());
                System.out.println("Displayed route on map for Trajet ID: " + trajet.getId());
            } else {
                System.out.println("Cannot display route: Missing coordinates for Trajet ID: " + trajet.getId());
                showAlert(Alert.AlertType.ERROR, "Error", "Cannot display route: Missing start or end coordinates.");
            }
        } catch (Exception e) {
            System.out.println("Error displaying route: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while displaying the route: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error displaying route on map", e);
        }
    }
}