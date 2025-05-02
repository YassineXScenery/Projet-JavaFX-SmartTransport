package esprit.tn.guiproject.controllers;

import esprit.tn.guiproject.models.PointInteret;
import esprit.tn.guiproject.services.PointInteretService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;
import java.util.List;

public class MapController {
    @FXML
    private WebView mapWebView;
    private WebEngine webEngine;
    private PointInteretService poiService;
    private boolean isSelectingRoute = false;
    private PointInteret startPoint = null;
    private PointInteret endPoint = null;
    private MapBridge bridgeInstance; // Strong reference to MapBridge
    private PoiController poiController; // Reference to PoiController

    @FXML
    public void initialize() {
        System.out.println("MapController initialize called.");
        if (mapWebView == null) {
            System.out.println("Error: mapWebView is null. Check MapView.fxml for fx:id='mapWebView'.");
            return;
        }
        System.out.println("mapWebView successfully injected.");

        poiService = new PointInteretService();
        webEngine = mapWebView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setOnAlert(event -> System.out.println("JavaScript Alert: " + event.getData()));

        String mapHtml = getClass().getResource("/esprit/tn/guiproject/views/map.html").toExternalForm();
        if (mapHtml == null) {
            System.out.println("Error: map.html resource not found.");
            return;
        }
        System.out.println("Loading map.html: " + mapHtml);
        webEngine.load(mapHtml);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == javafx.concurrent.Worker.State.SUCCEEDED) {
                System.out.println("Map HTML loaded successfully.");
                Platform.runLater(() -> setupMapAndBridge());
            } else if (newVal == javafx.concurrent.Worker.State.FAILED) {
                System.out.println("Failed to load map.html.");
            }
        });

        webEngine.setOnError(event -> System.out.println("WebView Error: " + event.getMessage()));
    }

    public void setPoiController(PoiController controller) {
        this.poiController = controller;
        System.out.println("PoiController set in MapController.");
    }

    private void setupMapAndBridge() {
        boolean isReady = false;
        int attempts = 0;
        final int maxAttempts = 50; // Approx 5 seconds with 100ms delay
        while (!isReady && attempts < maxAttempts) {
            try {
                Object result = webEngine.executeScript("typeof addMarker !== 'undefined' && typeof map !== 'undefined' && typeof setBridge === 'function'");
                if (Boolean.TRUE.equals(result)) {
                    isReady = true;
                    System.out.println("Map is ready for interaction.");
                    loadExistingPOIs();
                    webEngine.executeScript("setBridge();");
                    try {
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        bridgeInstance = new MapBridge(); // Create and store instance
                        window.setMember("java", bridgeInstance);
                        System.out.println("Java bridge registered with window object.");
                        webEngine.executeScript("if (typeof java !== 'undefined') { alert('Java bridge is accessible'); } else { alert('Java bridge not accessible'); }");
                    } catch (Exception e) {
                        System.out.println("Error setting up Java bridge: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    Thread.sleep(100);
                    attempts++;
                }
            } catch (Exception e) {
                System.out.println("Error checking map readiness: " + e.getMessage());
                break;
            }
        }
        if (!isReady) {
            System.out.println("Failed to initialize map after " + maxAttempts + " attempts.");
        }
    }

    private void loadExistingPOIs() {
        List<PointInteret> pois = poiService.afficher();
        System.out.println("Loading " + pois.size() + " existing POIs.");
        for (PointInteret poi : pois) {
            addMarkerToMap(poi);
        }
    }

    public void addMarkerToMap(PointInteret poi) {
        String script = String.format(
                "addMarker(%f, %f, '%s', '%s', %d);",
                poi.getLatitude(),
                poi.getLongitude(),
                poi.getNom(),
                poi.getType(),
                poi.getId()
        );
        try {
            webEngine.executeScript(script);
            System.out.println("Added marker for POI: " + poi.getNom());
        } catch (Exception e) {
            System.out.println("Error adding marker: " + e.getMessage());
        }
    }

    public void startRouteSelection() {
        isSelectingRoute = true;
        startPoint = null;
        endPoint = null;
        webEngine.executeScript("startRouteSelection();");
        System.out.println("Started route selection.");
    }

    public class MapBridge {
        private final MapController controller;

        public MapBridge() {
            this.controller = MapController.this;
        }

        public void onMapClick(double lat, double lng) {
            System.out.println("onMapClick called in MapBridge: Latitude = " + lat + ", Longitude = " + lng);
            try {
                if (isSelectingRoute) {
                    System.out.println("Handling route point selection...");
                    handleRoutePointSelection(lat, lng);
                } else if (controller.poiController != null) {
                    System.out.println("Updating POI fields with coordinates...");
                    Platform.runLater(() -> {
                        System.out.println("Inside Platform.runLater: Attempting to update coordinates...");
                        controller.poiController.updateCoordinates(lat, lng);
                    });
                } else {
                    System.out.println("PoiController is not set, proceeding with default POI creation...");
                    handlePOICreation(lat, lng);
                }
            } catch (Exception e) {
                System.out.println("Error in onMapClick: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handleRoutePointSelection(double lat, double lng) {
            try {
                if (startPoint == null) {
                    startPoint = new PointInteret();
                    startPoint.setLatitude(lat);
                    startPoint.setLongitude(lng);
                    startPoint.setNom("Start Point");
                    startPoint.setType("Route");
                    int id = poiService.ajouter(startPoint);
                    startPoint.setId(id);
                    controller.addMarkerToMap(startPoint);
                    System.out.println("Set start point: " + lat + ", " + lng);
                } else if (endPoint == null) {
                    endPoint = new PointInteret();
                    endPoint.setLatitude(lat);
                    endPoint.setLongitude(lng);
                    endPoint.setNom("End Point");
                    endPoint.setType("Route");
                    int id = poiService.ajouter(endPoint);
                    endPoint.setId(id);
                    controller.addMarkerToMap(endPoint);
                    calculateRoute();
                    isSelectingRoute = false;
                    System.out.println("Set end point: " + lat + ", " + lng);
                }
            } catch (Exception e) {
                System.out.println("Error in handleRoutePointSelection: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handlePOICreation(double lat, double lng) {
            try {
                PointInteret poi = new PointInteret();
                poi.setLatitude(lat);
                poi.setLongitude(lng);
                poi.setNom("New POI");
                poi.setType("Custom");
                int id = poiService.ajouter(poi);
                poi.setId(id);
                controller.addMarkerToMap(poi);
                System.out.println("Created new POI at: " + lat + ", " + lng);
            } catch (Exception e) {
                System.out.println("Error in handlePOICreation: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void calculateRoute() {
            try {
                if (startPoint != null && endPoint != null) {
                    double distance = calculateDistance(
                            startPoint.getLatitude(), startPoint.getLongitude(),
                            endPoint.getLatitude(), endPoint.getLongitude()
                    );
                    String script = String.format(
                            "drawRoute(%f, %f, %f, %f, %f);",
                            startPoint.getLatitude(), startPoint.getLongitude(),
                            endPoint.getLatitude(), endPoint.getLongitude(),
                            distance
                    );
                    webEngine.executeScript(script);
                    System.out.println("Route calculated, distance: " + distance + " km");
                }
            } catch (Exception e) {
                System.out.println("Error in calculateRoute: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
            final int R = 6371;
            double latDistance = Math.toRadians(lat2 - lat1);
            double lonDistance = Math.toRadians(lon2 - lon1);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        }
    }
}