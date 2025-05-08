package esprit.tn.guiproject.controllers;

import esprit.tn.guiproject.models.PointInteret;
import esprit.tn.guiproject.models.Trajet;
import esprit.tn.guiproject.services.PointInteretService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;
import java.sql.Time;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

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

    // Weather display fields
    @FXML
    private Label temperatureLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label humidityLabel;

    private static final Logger LOGGER = Logger.getLogger(MapController.class.getName());

    @FXML
    public void initialize() {
        System.out.println("MapController initialize called.");
        if (mapWebView == null) {
            System.out.println("Error: mapWebView is null. Check MapView.fxml for fx:id='mapWebView'.");
            return;
        }
        System.out.println("mapWebView successfully injected.");

        // Verify weather labels initialization
        if (temperatureLabel == null || descriptionLabel == null || humidityLabel == null) {
            System.out.println("Warning: One or more weather labels are null. Check FXML injection.");
            System.out.println("Temperature Label: " + (temperatureLabel != null ? "initialized" : "null"));
            System.out.println("Description Label: " + (descriptionLabel != null ? "initialized" : "null"));
            System.out.println("Humidity Label: " + (humidityLabel != null ? "initialized" : "null"));
        } else {
            System.out.println("All weather labels initialized successfully.");
            // Set initial values
            temperatureLabel.setText("Temperature: N/A");
            descriptionLabel.setText("Description: N/A");
            humidityLabel.setText("Humidity: N/A");
        }

        poiService = new PointInteretService();
        webEngine = mapWebView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setOnAlert(event -> System.out.println("JavaScript Alert: " + event.getData()));
        webEngine.setOnError(event -> LOGGER.severe("WebView Error: " + event.getMessage()));

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
                LOGGER.severe("Failed to load map.html: " + webEngine.getLoadWorker().getException());
            }
        });
    }

    public void setPoiController(PoiController controller) {
        this.poiController = controller;
        System.out.println("PoiController set in MapController to: " + (controller != null ? "not null" : "null"));
        if (controller != null) {
            controller.setMapController(this);
        }
    }

    public void setTrajetController(TrajetController controller) {
        this.trajetController = controller;
        System.out.println("TrajetController set in MapController.");
    }

    public void centerMap(double latitude, double longitude) {
        System.out.println("centerMap called: lat=" + latitude + ", lng=" + longitude);
        try {
            String script = String.format("map.setView([%f, %f], 20);", latitude, longitude);
            webEngine.executeScript(script);
            System.out.println("Map centered and zoomed at: lat=" + latitude + ", lng=" + longitude);
        } catch (Exception e) {
            LOGGER.severe("Error centering map: " + e.getMessage());
        }
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
                        LOGGER.severe("Error setting up Java bridge: " + e.getMessage());
                    }
                } else {
                    Thread.sleep(100);
                    attempts++;
                }
            } catch (Exception e) {
                LOGGER.severe("Error checking map readiness: " + e.getMessage());
                break;
            }
        }
        if (!isReady) {
            LOGGER.severe("Failed to initialize map after " + maxAttempts + " attempts.");
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
            LOGGER.severe("Error adding marker: " + e.getMessage());
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
            LOGGER.severe("Error displaying route: " + e.getMessage());
        }
    }

    public void clearMap() {
        try {
            webEngine.executeScript("clearMap();");
            System.out.println("Map cleared of routes and markers.");
            routeInProgress = false;
            isSelectingRoute = false;
            startPoint = null;
            endPoint = null;
        } catch (Exception e) {
            LOGGER.severe("Error clearing map: " + e.getMessage());
        }
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        System.out.println("calculateDistance called: lat1=" + lat1 + ", lon1=" + lon1 +
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
            LOGGER.severe("Error invoking startRouteSelection: " + e.getMessage());
        }
    }

    public void refreshMap() {
        System.out.println("refreshMap called.");
        try {
            // Clear existing markers and routes
            clearMap();
            // Reload all PointInteret markers
            loadExistingPOIs();
            System.out.println("Map refreshed with POIs only.");
        } catch (Exception e) {
            LOGGER.severe("Error refreshing map: " + e.getMessage());
        }
    }

    public class MapBridge {
        private final MapController controller;
        private final AtomicInteger tempIdCounter = new AtomicInteger(-1);

        public MapBridge() {
            this.controller = MapController.this;
            System.out.println("MapBridge initialized with controller: " + (controller != null ? "not null" : "null"));
        }

        public void onMapClick(double lat, double lng, Integer id, String pointType) {
            System.out.println("onMapClick triggered: lat=" + lat + ", lng=" + lng +
                    ", id=" + id + ", pointType=" + pointType +
                    ", isSelectingRoute=" + isSelectingRoute +
                    ", poiController=" + (controller.poiController != null));

            // Log weather label status at click time
            System.out.println("Weather labels status at click - Temperature: " + (controller.temperatureLabel != null ? "available" : "null") +
                    ", Description: " + (controller.descriptionLabel != null ? "available" : "null") +
                    ", Humidity: " + (controller.humidityLabel != null ? "available" : "null"));

            try {
                if (lat == 0.0 || lng == 0.0) {
                    System.out.println("Invalid coordinates: lat=" + lat + ", lng=" + lng);
                    return;
                }

                // Always fetch weather data on map click
                System.out.println("Initiating weather fetch for coordinates: lat=" + lat + ", lng=" + lng);
                fetchWeather(lat, lng);

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
                LOGGER.severe("Error in onMapClick: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handleRoutePointSelection(double lat, double lng, Integer id, String pointType) {
            try {
                if (pointType == null || pointType.isEmpty()) {
                    if (startPoint == null) {
                        pointType = "start";
                        System.out.println("Inferred pointType=start for null input");
                    } else if (endPoint == null && startPoint != null) {
                        pointType = "end";
                        System.out.println("Inferred pointType=end for null input");
                    } else {
                        System.out.println("Invalid state for null pointType: startPoint=" + (startPoint != null) +
                                ", endPoint=" + (endPoint != null));
                        return;
                    }
                }

                PointInteret point = id != null && id > 0 ? poiService.getById(id) : null;
                if (pointType.equals("start") && startPoint == null) {
                    startPoint = point != null ? point : new PointInteret(lat, lng, "Start Point", "Route");
                    if (point == null) {
                        startPoint.setId(tempIdCounter.decrementAndGet());
                    }
                    System.out.println("Set start point: id=" + startPoint.getId() +
                            ", lat=" + startPoint.getLatitude() +
                            ", lng=" + startPoint.getLongitude() +
                            ", nom=" + startPoint.getNom());
                    String script = String.format(
                            "addRouteMarker(%f, %f, '%s', %s, 'start');",
                            startPoint.getLatitude(),
                            startPoint.getLongitude(),
                            startPoint.getNom(),
                            startPoint.getId() > 0 ? startPoint.getId() : "null"
                    );
                    webEngine.executeScript(script);
                    System.out.println("Added start marker to routeLayer via addRouteMarker");
                } else if (pointType.equals("end") && endPoint == null && startPoint != null) {
                    endPoint = point != null ? point : new PointInteret(lat, lng, "End Point", "Route");
                    if (point == null) {
                        endPoint.setId(tempIdCounter.decrementAndGet());
                    }
                    System.out.println("Set end point: id=" + endPoint.getId() +
                            ", lat=" + endPoint.getLatitude() +
                            ", lng=" + endPoint.getLongitude() +
                            ", nom=" + endPoint.getNom());
                    String script = String.format(
                            "addRouteMarker(%f, %f, '%s', %s, 'end');",
                            endPoint.getLatitude(),
                            endPoint.getLongitude(),
                            endPoint.getNom(),
                            endPoint.getId() > 0 ? endPoint.getId() : "null"
                    );
                    webEngine.executeScript(script);
                    System.out.println("Added end marker to routeLayer via addRouteMarker");
                    calculateRoute();
                    isSelectingRoute = false;
                } else {
                    System.out.println("Invalid pointType or state: pointType=" + pointType +
                            ", startPoint=" + (startPoint != null) +
                            ", endPoint=" + (endPoint != null));
                }
            } catch (Exception e) {
                LOGGER.severe("Error in handleRoutePointSelection: " + e.getMessage());
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
                LOGGER.severe("Error in handlePOICreation: " + e.getMessage());
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
                LOGGER.severe("Error in calculateRoute: " + e.getMessage());
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

                String startScript = String.format(
                        "addRouteMarker(%f, %f, '%s', %s, 'start');",
                        startPoint.getLatitude(),
                        startPoint.getLongitude(),
                        startPoint.getNom(),
                        startPoint.getId() > 0 ? startPoint.getId() : "null"
                );
                webEngine.executeScript(startScript);
                System.out.println("Added start marker to routeLayer via addRouteMarker");

                String endScript = String.format(
                        "addRouteMarker(%f, %f, '%s', %s, 'end');",
                        endPoint.getLatitude(),
                        endPoint.getLongitude(),
                        endPoint.getNom(),
                        endPoint.getId() > 0 ? endPoint.getId() : "null"
                );
                webEngine.executeScript(endScript);
                System.out.println("Added end marker to routeLayer via addRouteMarker");

                long seconds = (long) (timeInMinutes * 60);
                Time estimatedTime = new Time(seconds * 1000);

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
                LOGGER.severe("Error in createTrajetFromRoute: " + e.getMessage());
                routeInProgress = false;
            }
        }

        private void fetchWeather(double lat, double lng) {
            if (temperatureLabel == null || descriptionLabel == null || humidityLabel == null) {
                System.out.println("Error: Weather labels not initialized. Cannot update weather information.");
                return;
            }

            String apiKey = "b8b223d150fcf2eeafba803b1be226c7";
            String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric", lat, lng, apiKey);
            System.out.println("Fetching weather from URL: " + url);

            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .build();

                System.out.println("Sending HTTP request to weather API...");
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Received response with status: " + response.statusCode());

                if (response.statusCode() != 200) {
                    System.out.println("Weather API request failed with status: " + response.statusCode());
                    Platform.runLater(() -> {
                        temperatureLabel.setText("Temperature: Error");
                        descriptionLabel.setText("Description: Error");
                        humidityLabel.setText("Humidity: Error");
                    });
                    return;
                }

                String responseBody = response.body();
                System.out.println("Weather API Response Body: " + responseBody);

                JSONObject json = new JSONObject(responseBody);
                if (!json.has("main") || !json.has("weather")) {
                    System.out.println("Weather API response missing required fields");
                    Platform.runLater(() -> {
                        temperatureLabel.setText("Temperature: Error");
                        descriptionLabel.setText("Description: Error");
                        humidityLabel.setText("Humidity: Error");
                    });
                    return;
                }

                double temp = json.getJSONObject("main").getDouble("temp");
                JSONArray weatherArray = json.getJSONArray("weather");
                String description = weatherArray.getJSONObject(0).getString("description");
                int humidity = json.getJSONObject("main").getInt("humidity");

                System.out.println("Parsed weather data - Temp: " + temp + "°C, Description: " + description + ", Humidity: " + humidity + "%");

                Platform.runLater(() -> {
                    try {
                        temperatureLabel.setText(String.format("Temperature: %.1f°C", temp));
                        descriptionLabel.setText("Description: " + description);
                        humidityLabel.setText("Humidity: " + humidity + "%");
                        System.out.println("Weather labels updated successfully");
                    } catch (Exception e) {
                        System.out.println("Error updating weather labels: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.out.println("Error fetching weather data: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    temperatureLabel.setText("Temperature: Error");
                    descriptionLabel.setText("Description: Error");
                    humidityLabel.setText("Humidity: Error");
                });
            }
        }
    }

    // Getter methods for weather labels
    public Label getTemperatureLabel() {
        return temperatureLabel;
    }

    public Label getDescriptionLabel() {
        return descriptionLabel;
    }

    public Label getHumidityLabel() {
        return humidityLabel;
    }

    // Setter methods for weather labels (added to allow injection)
    public void setTemperatureLabel(Label temperatureLabel) {
        this.temperatureLabel = temperatureLabel;
    }

    public void setDescriptionLabel(Label descriptionLabel) {
        this.descriptionLabel = descriptionLabel;
    }

    public void setHumidityLabel(Label humidityLabel) {
        this.humidityLabel = humidityLabel;
    }
}