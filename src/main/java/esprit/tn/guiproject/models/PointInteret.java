package esprit.tn.guiproject.models;

public class PointInteret {
    private int id;
    private double latitude;
    private double longitude;
    private String nom;
    private String type;

    // Constructors
    public PointInteret() {}

    public PointInteret(int id, double latitude, double longitude, String nom, String type) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nom = nom;
        this.type = type;
    }

    // New constructor to match MapController usage
    public PointInteret(double latitude, double longitude, String nom, String type) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.nom = nom;
        this.type = type;
    }

    // Getters and setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getNom() {
        return nom;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
}