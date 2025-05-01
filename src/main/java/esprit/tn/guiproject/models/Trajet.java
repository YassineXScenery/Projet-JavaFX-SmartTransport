package esprit.tn.guiproject.models;

import java.sql.Time;

public class Trajet {
    private int id;
    private double distance;
    private Integer pointDepart; // Using Integer to allow null values
    private Integer pointArrivee; // Using Integer to allow null values
    private Time tempsEstime;

    // Constructors
    public Trajet() {}

    public Trajet(int id, double distance, Integer pointDepart, Integer pointArrivee, Time tempsEstime) {
        this.id = id;
        this.distance = distance;
        this.pointDepart = pointDepart;
        this.pointArrivee = pointArrivee;
        this.tempsEstime = tempsEstime;
    }

    // Getters and setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public double getDistance() {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }

    public Integer getPointDepart() {
        return pointDepart;
    }
    public void setPointDepart(Integer pointDepart) {
        this.pointDepart = pointDepart;
    }

    public Integer getPointArrivee() {
        return pointArrivee;
    }
    public void setPointArrivee(Integer pointArrivee) {
        this.pointArrivee = pointArrivee;
    }

    public Time getTempsEstime() {
        return tempsEstime;
    }
    public void setTempsEstime(Time tempsEstime) {
        this.tempsEstime = tempsEstime;
    }
}