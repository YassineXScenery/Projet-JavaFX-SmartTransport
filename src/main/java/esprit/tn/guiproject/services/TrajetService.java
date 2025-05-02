package esprit.tn.guiproject.services;

import esprit.tn.guiproject.connection.DatabaseConnection;
import esprit.tn.guiproject.models.Trajet;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrajetService {
    private final Connection connection;

    public TrajetService() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        if (connection == null) {
            System.err.println("Erreur: Connexion à la base de données non établie.");
        } else {
            System.out.println("Connexion récupérée avec succès via DatabaseConnection.");
        }
    }

    public int ajouter(Trajet trajet) {
        String query = "INSERT INTO trajet (point_depart, point_arrivee, distance, temps_estime, start_latitude, start_longitude, start_nom, start_type, end_latitude, end_longitude, end_nom, end_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, trajet.getPointDepart() != null ? trajet.getPointDepart() : null);
            stmt.setObject(2, trajet.getPointArrivee() != null ? trajet.getPointArrivee() : null);
            stmt.setDouble(3, trajet.getDistance());
            stmt.setTime(4, trajet.getTempsEstime());
            stmt.setObject(5, trajet.getStartLatitude());
            stmt.setObject(6, trajet.getStartLongitude());
            stmt.setObject(7, trajet.getStartNom());
            stmt.setObject(8, trajet.getStartType());
            stmt.setObject(9, trajet.getEndLatitude());
            stmt.setObject(10, trajet.getEndLongitude());
            stmt.setObject(11, trajet.getEndNom());
            stmt.setObject(12, trajet.getEndType());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                trajet.setId(id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du trajet: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public List<Trajet> afficher() {
        List<Trajet> trajets = new ArrayList<>();
        String query = "SELECT * FROM trajet";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Trajet trajet = new Trajet();
                trajet.setId(rs.getInt("id"));
                trajet.setPointDepart(rs.getInt("point_depart"));
                if (rs.wasNull()) trajet.setPointDepart(null);
                trajet.setPointArrivee(rs.getInt("point_arrivee"));
                if (rs.wasNull()) trajet.setPointArrivee(null);
                trajet.setDistance(rs.getDouble("distance"));
                trajet.setTempsEstime(rs.getTime("temps_estime"));
                trajet.setStartLatitude(rs.getDouble("start_latitude"));
                if (rs.wasNull()) trajet.setStartLatitude(null);
                trajet.setStartLongitude(rs.getDouble("start_longitude"));
                if (rs.wasNull()) trajet.setStartLongitude(null);
                trajet.setStartNom(rs.getString("start_nom"));
                trajet.setStartType(rs.getString("start_type"));
                trajet.setEndLatitude(rs.getDouble("end_latitude"));
                if (rs.wasNull()) trajet.setEndLatitude(null);
                trajet.setEndLongitude(rs.getDouble("end_longitude"));
                if (rs.wasNull()) trajet.setEndLongitude(null);
                trajet.setEndNom(rs.getString("end_nom"));
                trajet.setEndType(rs.getString("end_type"));
                trajets.add(trajet);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des trajets: " + e.getMessage());
            e.printStackTrace();
        }
        return trajets;
    }

    public void modifier(Trajet trajet) {
        String query = "UPDATE trajet SET point_depart = ?, point_arrivee = ?, distance = ?, temps_estime = ?, start_latitude = ?, start_longitude = ?, start_nom = ?, start_type = ?, end_latitude = ?, end_longitude = ?, end_nom = ?, end_type = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setObject(1, trajet.getPointDepart());
            stmt.setObject(2, trajet.getPointArrivee());
            stmt.setDouble(3, trajet.getDistance());
            stmt.setTime(4, trajet.getTempsEstime());
            stmt.setObject(5, trajet.getStartLatitude());
            stmt.setObject(6, trajet.getStartLongitude());
            stmt.setObject(7, trajet.getStartNom());
            stmt.setObject(8, trajet.getStartType());
            stmt.setObject(9, trajet.getEndLatitude());
            stmt.setObject(10, trajet.getEndLongitude());
            stmt.setObject(11, trajet.getEndNom());
            stmt.setObject(12, trajet.getEndType());
            stmt.setInt(13, trajet.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification du trajet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void supprimer(int id) {
        String query = "DELETE FROM trajet WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du trajet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeAll() {
        String query = "DELETE FROM trajet";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(query);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de tous les trajets: " + e.getMessage());
            e.printStackTrace();
        }
    }
}