package esprit.tn.guiproject.controllers;

import esprit.tn.guiproject.models.PointInteret;
import esprit.tn.guiproject.services.PointInteretService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PoiController {

    private static final Logger LOGGER = Logger.getLogger(PoiController.class.getName());

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

    private PointInteretService poiService = new PointInteretService();
    private ObservableList<PointInteret> poiList = FXCollections.observableArrayList();
    private MapController mapController;
    private TrajetController trajetController;  // Add this field

    public void setMapController(MapController mapController) {
        this.mapController = mapController;
        System.out.println("MapController set in PoiController to: " + (mapController != null ? "not null" : "null"));
    }

    public void setTrajetController(TrajetController controller) {
        this.trajetController = controller;
    }

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

        // Table row click handler
        poiTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populatePoiFields(newSelection);
                // Refresh map to show all POIs and center on selected POI
                if (mapController != null) {
                    mapController.refreshMap();
                    mapController.centerMap(newSelection.getLatitude(), newSelection.getLongitude());
                    System.out.println("Map refreshed and centered on POI: lat=" + newSelection.getLatitude() + ", lng=" + newSelection.getLongitude());
                } else {
                    System.out.println("MapController is null, cannot refresh or center map.");
                }
            }
        });

        // Log to confirm fields are initialized
        System.out.println("PoiController initialized. poiLatitudeField = " + (poiLatitudeField != null ? "not null" : "null") + ", poiLongitudeField = " + (poiLongitudeField != null ? "not null" : "null"));
    }

    public void updateCoordinates(double latitude, double longitude) {
        System.out.println("updateCoordinates called with: Latitude = " + latitude + ", Longitude = " + longitude);
        if (poiLatitudeField != null && poiLongitudeField != null) {
            poiLatitudeField.setText(String.valueOf(latitude));
            poiLongitudeField.setText(String.valueOf(longitude));
            System.out.println("Updated POI fields: Latitude = " + poiLatitudeField.getText() + ", Longitude = " + poiLongitudeField.getText());
        } else {
            System.out.println("Error: Latitude or Longitude fields are null. poiLatitudeField = " + (poiLatitudeField != null ? "not null" : "null") + ", poiLongitudeField = " + (poiLongitudeField != null ? "not null" : "null"));
        }
    }

    private void loadPoiData() {
        poiList.clear();
        poiList.addAll(poiService.afficher());
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
                
                // Notify TrajetController of the new POI
                if (trajetController != null) {
                    trajetController.onPointInteretAdded();
                }
                
                // Refresh map to show new POI
                if (mapController != null) {
                    mapController.refreshMap();
                    System.out.println("Map refreshed after adding POI ID: " + id);
                } else {
                    System.out.println("MapController is null, cannot refresh map.");
                }
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
            
            // Notify TrajetController of the POI update
            if (trajetController != null) {
                trajetController.onPointInteretAdded(); // Reuse the same method as it does the same refresh
            }
            
            // Refresh map to reflect updated POI
            if (mapController != null) {
                mapController.refreshMap();
                System.out.println("Map refreshed after updating POI ID: " + selected.getId());
            } else {
                System.out.println("MapController is null, cannot refresh map.");
            }
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
            
            // Notify TrajetController of the POI deletion
            if (trajetController != null) {
                trajetController.onPointInteretAdded(); // Reuse the same method as it does the same refresh
            }
            
            // Refresh map to reflect deleted POI
            if (mapController != null) {
                mapController.refreshMap();
                System.out.println("Map refreshed after deleting POI ID: " + selected.getId());
            } else {
                System.out.println("MapController is null, cannot refresh map.");
            }
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

    private void populatePoiFields(PointInteret poi) {
        poiNameField.setText(poi.getNom());
        poiLatitudeField.setText(String.valueOf(poi.getLatitude()));
        poiLongitudeField.setText(String.valueOf(poi.getLongitude()));
        poiTypeField.setText(poi.getType());
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}