package esprit.tn.guiproject.models;

import java.sql.Time;

public class Trajet {
    private int id;
    private Integer pointDepart;
    private Integer pointArrivee;
    private double distance;
    private Time tempsEstime;
    private Double startLatitude;
    private Double startLongitude;
    private String startNom;
    private String startType;
    private Double endLatitude;
    private Double endLongitude;
    private String endNom;
    private String endType;

    // Default constructor
    public Trajet() {}

    // Constructor for map-selected routes
    public Trajet(PointInteret start, PointInteret end, double distance, Time estimatedTime) {
        this.distance = distance;
        this.tempsEstime = estimatedTime;
        if (start.getId() > 0) {
            this.pointDepart = start.getId();
        } else {
            this.startLatitude = start.getLatitude();
            this.startLongitude = start.getLongitude();
            this.startNom = start.getNom();
            this.startType = start.getType();
        }
        if (end.getId() > 0) {
            this.pointArrivee = end.getId();
        } else {
            this.endLatitude = end.getLatitude();
            this.endLongitude = end.getLongitude();
            this.endNom = end.getNom();
            this.endType = end.getType();
        }
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getPointDepart() { return pointDepart; }
    public void setPointDepart(Integer pointDepart) { this.pointDepart = pointDepart; }

    public Integer getPointArrivee() { return pointArrivee; }
    public void setPointArrivee(Integer pointArrivee) { this.pointArrivee = pointArrivee; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public Time getTempsEstime() { return tempsEstime; }
    public void setTempsEstime(Time tempsEstime) { this.tempsEstime = tempsEstime; }

    public Double getStartLatitude() { return startLatitude; }
    public void setStartLatitude(Double startLatitude) { this.startLatitude = startLatitude; }

    public Double getStartLongitude() { return startLongitude; }
    public void setStartLongitude(Double startLongitude) { this.startLongitude = startLongitude; }

    public String getStartNom() { return startNom; }
    public void setStartNom(String startNom) { this.startNom = startNom; }

    public String getStartType() { return startType; }
    public void setStartType(String startType) { this.startType = startType; }

    public Double getEndLatitude() { return endLatitude; }
    public void setEndLatitude(Double endLatitude) { this.endLatitude = endLatitude; }

    public Double getEndLongitude() { return endLongitude; }
    public void setEndLongitude(Double endLongitude) { this.endLongitude = endLongitude; }

    public String getEndNom() { return endNom; }
    public void setEndNom(String endNom) { this.endNom = endNom; }

    public String getEndType() { return endType; }
    public void setEndType(String endType) { this.endType = endType; }
}