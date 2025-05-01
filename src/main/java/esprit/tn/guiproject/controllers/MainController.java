package esprit.tn.guiproject.controllers;

import esprit.tn.guiproject.models.PointInteret;
import esprit.tn.guiproject.models.Trajet;
import esprit.tn.guiproject.services.PointInteretService;
import esprit.tn.guiproject.services.TrajetService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Time;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    // POI Table
    @FXML private TableView<PointInteret> poiTable;
    @FXML private TableColumn<PointInteret, Integer> poiIdColumn;
    @FXML private TableColumn<PointInteret, String> poiNameColumn;
    @FXML private TableColumn<PointInteret, Double> poiLatitudeColumn;
    @FXML private TableColumn<PointInteret, Double> poiLongitudeColumn;
    @FXML private TableColumn<PointInteret, String> poiTypeColumn;

    // POI Form
    @FXML private TextField poiNameField;
    @FXML private TextField poiLatitudeField;
    @FXML private TextField poiLongitudeField;
    @FXML private TextField poiTypeField;
    @FXML private Button addPoiButton;
    @FXML private Button updatePoiButton;
    @FXML private Button deletePoiButton;
    @FXML private Button clearPoiButton;

    // Route Table
    @FXML private TableView<Trajet> routeTable;
    @FXML private TableColumn<Trajet, Integer> routeIdColumn;
    @FXML private TableColumn<Trajet, Double> routeDistanceColumn;
    @FXML private TableColumn<Trajet, Integer> routeStartPointColumn;
    @FXML private TableColumn<Trajet, Integer> routeEndPointColumn;
    @FXML private TableColumn<Trajet, Time> routeTimeColumn;

    // Route Form
    @FXML private TextField routeDistanceField;
    @FXML private TextField routeStartPointField;
    @FXML private TextField routeEndPointField;
    @FXML private TextField routeTimeField;
    @FXML private Button addRouteButton;
    @FXML private Button updateRouteButton;
    @FXML private Button deleteRouteButton;
    @FXML private Button clearRouteButton;

    private PointInteretService poiService = new PointInteretService();
    private TrajetService trajetService = new TrajetService();
    private ObservableList<PointInteret> poiList = FXCollections.observableArrayList();
    private ObservableList<Trajet> routeList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize POI Table
        poiIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        poiNameColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        poiLatitudeColumn.setCellValueFactory(new PropertyValueFactory<>("latitude"));
        poiLongitudeColumn.setCellValueFactory(new PropertyValueFactory<>("longitude"));
        poiTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        poiTable.setItems(poiList);
        loadPoiData();

        // Initialize Route Table
        routeIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        routeDistanceColumn.setCellValueFactory(new PropertyValueFactory<>("distance"));
        routeStartPointColumn.setCellValueFactory(new PropertyValueFactory<>("pointDepart"));
        routeEndPointColumn.setCellValueFactory(new PropertyValueFactory<>("pointArrivee"));
        routeTimeColumn.setCellValueFactory(new PropertyValueFactory<>("tempsEstime"));
        routeTable.setItems(routeList);
        loadRouteData();

        // Table row click handlers
        poiTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populatePoiFields(newSelection);
            }
        });

        routeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateRouteFields(newSelection);
            }
        });
    }

    private void loadPoiData() {
        poiList.clear();
        poiList.addAll(poiService.afficher());
    }

    private void loadRouteData() {
        routeList.clear();
        routeList.addAll(trajetService.afficher());
    }

    @FXML
    private void addPoi() {
        try {
            PointInteret poi = new PointInteret();
            poi.setNom(poiNameField.getText());
            poi.setLatitude(Double.parseDouble(poiLatitudeField.getText()));
            poi.setLongitude(Double.parseDouble(poiLongitudeField.getText()));
            poi.setType(poiTypeField.getText());
            int id = poiService.ajouter(poi);
            if (id != -1) {
                poi.setId(id);
                poiList.add(poi);
                clearPoiFields();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Point of Interest added successfully!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add Point of Interest.");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for latitude and longitude.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding POI", e);
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while adding the Point of Interest.");
        }
    }

    @FXML
    private void updatePoi() {
        PointInteret selected = poiTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a Point of Interest to update.");
            return;
        }
        try {
            selected.setNom(poiNameField.getText());
            selected.setLatitude(Double.parseDouble(poiLatitudeField.getText()));
            selected.setLongitude(Double.parseDouble(poiLongitudeField.getText()));
            selected.setType(poiTypeField.getText());
            poiService.modifier(selected);
            loadPoiData();
            clearPoiFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Point of Interest updated successfully!");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for latitude and longitude.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating POI", e);
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while updating the Point of Interest.");
        }
    }

    @FXML
    private void deletePoi() {
        PointInteret selected = poiTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a Point of Interest to delete.");
            return;
        }
        try {
            poiService.supprimer(selected.getId());
            poiList.remove(selected);
            clearPoiFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Point of Interest deleted successfully!");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting POI", e);
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while deleting the Point of Interest.");
        }
    }

    @FXML
    private void clearPoiFields() {
        poiNameField.clear();
        poiLatitudeField.clear();
        poiLongitudeField.clear();
        poiTypeField.clear();
        poiTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void addRoute() {
        try {
            Trajet trajet = new Trajet();
            trajet.setDistance(Double.parseDouble(routeDistanceField.getText()));
            trajet.setPointDepart(routeStartPointField.getText().isEmpty() ? null : Integer.parseInt(routeStartPointField.getText()));
            trajet.setPointArrivee(routeEndPointField.getText().isEmpty() ? null : Integer.parseInt(routeEndPointField.getText()));
            trajet.setTempsEstime(Time.valueOf(routeTimeField.getText()));
            trajetService.ajouter(trajet);
            loadRouteData();
            clearRouteFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Route added successfully!");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for distance and point IDs.");
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Time Format", "Please enter time in HH:mm:ss format.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding Route", e);
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while adding the Route.");
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
            selected.setDistance(Double.parseDouble(routeDistanceField.getText()));
            selected.setPointDepart(routeStartPointField.getText().isEmpty() ? null : Integer.parseInt(routeStartPointField.getText()));
            selected.setPointArrivee(routeEndPointField.getText().isEmpty() ? null : Integer.parseInt(routeEndPointField.getText()));
            selected.setTempsEstime(Time.valueOf(routeTimeField.getText()));
            trajetService.modifier(selected);
            loadRouteData();
            clearRouteFields();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Route updated successfully!");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numbers for distance and point IDs.");
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Time Format", "Please enter time in HH:mm:ss format.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating Route", e);
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while updating the Route.");
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
            LOGGER.log(Level.SEVERE, "Error deleting Route", e);
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while deleting the Route.");
        }
    }

    @FXML
    private void clearRouteFields() {
        routeDistanceField.clear();
        routeStartPointField.clear();
        routeEndPointField.clear();
        routeTimeField.clear();
        routeTable.getSelectionModel().clearSelection();
    }

    private void populatePoiFields(PointInteret poi) {
        poiNameField.setText(poi.getNom());
        poiLatitudeField.setText(String.valueOf(poi.getLatitude()));
        poiLongitudeField.setText(String.valueOf(poi.getLongitude()));
        poiTypeField.setText(poi.getType());
    }

    private void populateRouteFields(Trajet trajet) {
        routeDistanceField.setText(String.valueOf(trajet.getDistance()));
        routeStartPointField.setText(trajet.getPointDepart() != null ? String.valueOf(trajet.getPointDepart()) : "");
        routeEndPointField.setText(trajet.getPointArrivee() != null ? String.valueOf(trajet.getPointArrivee()) : "");
        routeTimeField.setText(trajet.getTempsEstime() != null ? trajet.getTempsEstime().toString() : "");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}