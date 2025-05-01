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

    @FXML
    public void initialize() {
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
                    // Load existing POIs
                    loadExistingPOIs();
                    // Set the bridge using the JavaScript callback
                    webEngine.executeScript("setBridge();");
                    try {
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.setMember("java", new MapBridge());
                        System.out.println("Java bridge registered with window object.");
                        webEngine.executeScript("if (typeof java !== 'undefined') { alert('Java bridge is accessible'); } else { alert('Java bridge not accessible'); }");
                    } catch (Exception e) {
                        System.out.println("Error setting up Java bridge: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    Thread.sleep(100); // Wait 100ms before retrying
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

    private void addMarkerToMap(PointInteret poi) {
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
        public void onMapClick(double lat, double lng) {
            System.out.println("Map clicked at: Latitude = " + lat + ", Longitude = " + lng);
            if (isSelectingRoute) {
                handleRoutePointSelection(lat, lng);
            } else {
                handlePOICreation(lat, lng);
            }
        }

        private void handleRoutePointSelection(double lat, double lng) {
            if (startPoint == null) {
                startPoint = new PointInteret();
                startPoint.setLatitude(lat);
                startPoint.setLongitude(lng);
                startPoint.setNom("Start Point");
                startPoint.setType("Route");
                int id = poiService.ajouter(startPoint);
                startPoint.setId(id);
                addMarkerToMap(startPoint);
                System.out.println("Set start point: " + lat + ", " + lng);
            } else if (endPoint == null) {
                endPoint = new PointInteret();
                endPoint.setLatitude(lat);
                endPoint.setLongitude(lng);
                endPoint.setNom("End Point");
                endPoint.setType("Route");
                int id = poiService.ajouter(endPoint);
                endPoint.setId(id);
                addMarkerToMap(endPoint);
                calculateRoute();
                isSelectingRoute = false;
                System.out.println("Set end point: " + lat + ", " + lng);
            }
        }

        private void handlePOICreation(double lat, double lng) {
            PointInteret poi = new PointInteret();
            poi.setLatitude(lat);
            poi.setLongitude(lng);
            poi.setNom("New POI");
            poi.setType("Custom");
            int id = poiService.ajouter(poi);
            poi.setId(id);
            addMarkerToMap(poi);
            System.out.println("Created new POI at: " + lat + ", " + lng);
        }

        private void calculateRoute() {
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