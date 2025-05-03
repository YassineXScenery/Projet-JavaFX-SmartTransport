package esprit.tn.guiproject.controllers;

import esprit.tn.guiproject.models.PointInteret;
import esprit.tn.guiproject.services.PointInteretService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import java.sql.Time;
import java.util.concurrent.atomic.AtomicInteger;

public class MapController {
    @FXML
    private WebView mapWebView;
    private WebEngine webEngine;
    private PointInteretService poiService;
    private boolean isSelectingRoute = false;
    private PointInteret startPoint = null;
    private PointInteret endPoint = null;
    private MapBridge bridgeInstance;
    private PoiController poiController;
    private TrajetController trajetController;
    private boolean routeInProgress = false;

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
        System.out.println("PoiController set in MapController to: " + (controller != null ? "not null" : "null"));
    }

    public void setTrajetController(TrajetController controller) {
        this.trajetController = controller;
        System.out.println("TrajetController set in MapController.");
    }

    private void setupMapAndBridge() {
        boolean isReady = false;
        int attempts = 0;
        final int maxAttempts = 50;
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
                        bridgeInstance = new MapBridge();
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
        var pois = poiService.afficher();
        System.out.println("Loading " + pois.size() + " existing POIs.");
        for (var poi : pois) {
            addMarkerToMap(poi);
        }
    }

    public void addMarkerToMap(PointInteret poi) {
        System.out.println("Adding marker for POI: id=" + poi.getId() + ", nom=" + poi.getNom() +
                ", lat=" + poi.getLatitude() + ", lng=" + poi.getLongitude() + ", type=" + poi.getType());
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
            e.printStackTrace();
        }
    }

    public void displayRoute(double lat1, double lng1, double lat2, double lng2, double distance) {
        System.out.println("displayRoute called: lat1=" + lat1 + ", lng1=" + lng1 +
                ", lat2=" + lat2 + ", lng2=" + lng2 + ", distance=" + distance);
        try {
            clearMap();
            String script = String.format(
                    "drawRoute(%f, %f, %f, %f, %f);",
                    lat1, lng1, lat2, lng2, distance
            );
            webEngine.executeScript(script);
            System.out.println("Displayed route on map, distance: " + distance + " km");
        } catch (Exception e) {
            System.out.println("Error displaying route: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void clearMap() {
        try {
            webEngine.executeScript("clearMap();");
            System.out.println("Map cleared of routes and markers.");
            routeInProgress = false;
        } catch (Exception e) {
            System.out.println("Error clearing map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        System.out.println("calculateDistance called: lat1=" + lat1 + ", lon1=" + lon1 +
                ", lat2=" + lat2 + ", lon2=" + lon2);
        return bridgeInstance.calculateDistance(lat1, lon1, lat2, lon2);
    }

    public void startRouteSelection() {
        System.out.println("startRouteSelection called.");
        isSelectingRoute = true;
        startPoint = null;
        endPoint = null;
        clearMap();
        try {
            webEngine.executeScript("startRouteSelection();");
            System.out.println("JavaScript startRouteSelection invoked.");
        } catch (Exception e) {
            System.out.println("Error invoking startRouteSelection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public class MapBridge {
        private final MapController controller;
        private final AtomicInteger tempIdCounter = new AtomicInteger(-1);

        public MapBridge() {
            this.controller = MapController.this;
        }

        public void onMapClick(double lat, double lng, Integer id, String pointType) {
            System.out.println("onMapClick triggered: lat=" + lat + ", lng=" + lng +
                    ", id=" + id + ", pointType=" + pointType +
                    ", isSelectingRoute=" + isSelectingRoute +
                    ", poiController=" + (controller.poiController != null));
            try {
                if (lat == 0.0 || lng == 0.0) {
                    System.out.println("Invalid coordinates: lat=" + lat + ", lng=" + lng);
                    return;
                }

                if (isSelectingRoute) {
                    System.out.println("Handling route point selection...");
                    handleRoutePointSelection(lat, lng, id, pointType);
                } else if (controller.poiController != null) {
                    System.out.println("Updating POI fields with coordinates: lat=" + lat + ", lng=" + lng);
                    Platform.runLater(() -> {
                        if (controller.poiController != null) {
                            controller.poiController.updateCoordinates(lat, lng);
                            System.out.println("POI fields updated successfully.");
                        } else {
                            System.out.println("poiController is null during update attempt.");
                        }
                    });
                } else {
                    System.out.println("PoiController is not set, creating new POI...");
                    handlePOICreation(lat, lng);
                }
            } catch (Exception e) {
                System.out.println("Error in onMapClick: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handleRoutePointSelection(double lat, double lng, Integer id, String pointType) {
            try {
                PointInteret point = id != null && id > 0 ? poiService.getById(id) : null;
                if (pointType != null && pointType.equals("start") && startPoint == null) {
                    startPoint = point != null ? point : new PointInteret(lat, lng, "Start Point", "Route");
                    if (point == null) {
                        startPoint.setId(tempIdCounter.decrementAndGet());
                    }
                    System.out.println("Set start point: id=" + startPoint.getId() +
                            ", lat=" + startPoint.getLatitude() +
                            ", lng=" + startPoint.getLongitude() +
                            ", nom=" + startPoint.getNom());
                    controller.addMarkerToMap(startPoint);
                } else if (pointType != null && pointType.equals("end") && endPoint == null && startPoint != null) {
                    endPoint = point != null ? point : new PointInteret(lat, lng, "End Point", "Route");
                    if (point == null) {
                        endPoint.setId(tempIdCounter.decrementAndGet());
                    }
                    System.out.println("Set end point: id=" + endPoint.getId() +
                            ", lat=" + endPoint.getLatitude() +
                            ", lng=" + endPoint.getLongitude() +
                            ", nom=" + endPoint.getNom());
                    controller.addMarkerToMap(endPoint);
                    calculateRoute();
                    isSelectingRoute = false;
                } else {
                    System.out.println("Invalid pointType or state: pointType=" + pointType +
                            ", startPoint=" + (startPoint != null) +
                            ", endPoint=" + (endPoint != null));
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
                System.out.println("Created new POI: id=" + id + ", lat=" + lat + ", lng=" + lng);
                controller.addMarkerToMap(poi);
            } catch (Exception e) {
                System.out.println("Error in handlePOICreation: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void calculateRoute() {
            if (routeInProgress) {
                System.out.println("Route calculation already in progress, skipping...");
                return;
            }

            try {
                routeInProgress = true;
                if (startPoint != null && endPoint != null) {
                    System.out.println("Calculating route: startPoint id=" + startPoint.getId() +
                            ", lat=" + startPoint.getLatitude() + ", lng=" + startPoint.getLongitude() +
                            ", endPoint id=" + endPoint.getId() +
                            ", lat=" + endPoint.getLatitude() + ", lng=" + endPoint.getLongitude());
                    double distance = calculateDistance(
                            startPoint.getLatitude(), startPoint.getLongitude(),
                            endPoint.getLatitude(), endPoint.getLongitude()
                    );
                    Time estimatedTime = new Time(10 * 60 * 1000); // 10 minutes
                    String script = String.format(
                            "drawRoute(%f, %f, %f, %f, %f);",
                            startPoint.getLatitude(), startPoint.getLongitude(),
                            endPoint.getLatitude(), endPoint.getLongitude(),
                            distance
                    );
                    webEngine.executeScript(script);
                    System.out.println("Route drawn via JavaScript: " + script);

                    // Call TrajetController directly
                    if (trajetController != null) {
                        System.out.println("Calling trajetController.createTrajetFromRoute");
                        Platform.runLater(() -> {
                            trajetController.createTrajetFromRoute(startPoint, endPoint, distance, estimatedTime);
                            routeInProgress = false;
                        });
                    } else {
                        System.out.println("TrajetController is null, cannot create Trajet");
                        routeInProgress = false;
                    }
                } else {
                    System.out.println("Cannot calculate route: startPoint=" + (startPoint != null) +
                            ", endPoint=" + (endPoint != null));
                    routeInProgress = false;
                }
            } catch (Exception e) {
                System.out.println("Error in calculateRoute: " + e.getMessage());
                e.printStackTrace();
                routeInProgress = false;
            }
        }

        public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
            System.out.println("calculateDistance: lat1=" + lat1 + ", lon1=" + lon1 +
                    ", lat2=" + lat2 + ", lon2=" + lon2);
            final int R = 6371; // Earth's radius in km
            double latDistance = Math.toRadians(lat2 - lat1);
            double lonDistance = Math.toRadians(lon2 - lon1);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = R * c;
            System.out.println("Calculated distance: " + distance + " km");
            return distance;
        }

        @SuppressWarnings("unused")
        public void createTrajetFromRoute(double lat1, double lng1, double lat2, double lng2,
                                          double distance, double timeInMinutes, Integer startId, Integer endId) {
            System.out.println("createTrajetFromRoute called: lat1=" + lat1 + ", lng1=" + lng1 +
                    ", lat2=" + lat2 + ", lng2=" + lng2 + ", distance=" + distance +
                    ", timeInMinutes=" + timeInMinutes + ", startId=" + startId +
                    ", endId=" + endId);
            System.out.println("routesfound event data: distance=" + distance + " km, time=" + timeInMinutes + " minutes");
            try {
                if (lat1 == 0.0 || lng1 == 0.0 || lat2 == 0.0 || lng2 == 0.0) {
                    System.out.println("Invalid coordinates in createTrajetFromRoute");
                    routeInProgress = false;
                    return;
                }

                // Use existing startPoint/endPoint if available, otherwise fetch or create
                if (startId != null && startId > 0) {
                    startPoint = poiService.getById(startId);
                    System.out.println("Fetched startPoint from DB: id=" + startId +
                            ", lat=" + (startPoint != null ? startPoint.getLatitude() : "null") +
                            ", lng=" + (startPoint != null ? startPoint.getLongitude() : "null"));
                }
                if (startPoint == null) {
                    startPoint = new PointInteret(lat1, lng1, "Start Point", "Route");
                    startPoint.setId(tempIdCounter.decrementAndGet());
                    System.out.println("Created new startPoint: id=" + startPoint.getId() +
                            ", lat=" + lat1 + ", lng=" + lng1);
                }

                if (endId != null && endId > 0) {
                    endPoint = poiService.getById(endId);
                    System.out.println("Fetched endPoint from DB: id=" + endId +
                            ", lat=" + (endPoint != null ? endPoint.getLatitude() : "null") +
                            ", lng=" + (endPoint != null ? endPoint.getLongitude() : "null"));
                }
                if (endPoint == null) {
                    endPoint = new PointInteret(lat2, lng2, "End Point", "Route");
                    endPoint.setId(tempIdCounter.decrementAndGet());
                    System.out.println("Created new endPoint: id=" + endPoint.getId() +
                            ", lat=" + lat2 + ", lng=" + lng2);
                }

                long seconds = (long) (timeInMinutes * 60);
                Time estimatedTime = new Time(seconds * 1000);

                controller.addMarkerToMap(startPoint);
                controller.addMarkerToMap(endPoint);

                if (trajetController != null) {
                    System.out.println("Calling trajetController.createTrajetFromRoute from Java bridge");
                    Platform.runLater(() -> {
                        trajetController.createTrajetFromRoute(startPoint, endPoint, distance, estimatedTime);
                        routeInProgress = false;
                    });
                } else {
                    System.out.println("TrajetController is null, cannot create Trajet.");
                    routeInProgress = false;
                }
            } catch (Exception e) {
                System.out.println("Error in createTrajetFromRoute: " + e.getMessage());
                e.printStackTrace();
                routeInProgress = false;
            }
        }
    }
}