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
        System.out.println("TrajetService connection: " + (connection != null ? "valid" : "null"));
        if (connection == null) {
            System.err.println("Erreur: Connexion à la base de données non établie.");
        }
    }

    public int ajouter(Trajet trajet) {
        System.out.println("TrajetService.ajouter called: point_depart=" + trajet.getPointDepart() +
                ", point_arrivee=" + trajet.getPointArrivee() +
                ", start_latitude=" + trajet.getStartLatitude() +
                ", start_longitude=" + trajet.getStartLongitude() +
                ", end_latitude=" + trajet.getEndLatitude() +
                ", end_longitude=" + trajet.getEndLongitude() +
                ", distance=" + trajet.getDistance());

        // Validate coordinates for map-selected routes
        if (trajet.getPointDepart() == null && trajet.getPointArrivee() == null) {
            if (trajet.getStartLatitude() == null || trajet.getStartLongitude() == null ||
                    trajet.getEndLatitude() == null || trajet.getEndLongitude() == null) {
                System.out.println("Invalid Trajet: Missing coordinates for map-selected route");
                return -1;
            }
        }

        String query = "INSERT INTO trajet (point_depart, point_arrivee, distance, temps_estime, start_latitude, start_longitude, start_nom, start_type, end_latitude, end_longitude, end_nom, end_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            System.out.println("Preparing query: " + query);
            stmt.setObject(1, trajet.getPointDepart(), Types.INTEGER);
            stmt.setObject(2, trajet.getPointArrivee(), Types.INTEGER);
            stmt.setDouble(3, trajet.getDistance());
            stmt.setTime(4, trajet.getTempsEstime());
            stmt.setObject(5, trajet.getStartLatitude(), Types.DOUBLE);
            stmt.setObject(6, trajet.getStartLongitude(), Types.DOUBLE);
            stmt.setObject(7, trajet.getStartNom(), Types.VARCHAR);
            stmt.setObject(8, trajet.getStartType(), Types.VARCHAR);
            stmt.setObject(9, trajet.getEndLatitude(), Types.DOUBLE);
            stmt.setObject(10, trajet.getEndLongitude(), Types.DOUBLE);
            stmt.setObject(11, trajet.getEndNom(), Types.VARCHAR);
            stmt.setObject(12, trajet.getEndType(), Types.VARCHAR);
            System.out.println("Executing INSERT for Trajet: point_depart=" + trajet.getPointDepart() +
                    ", point_arrivee=" + trajet.getPointArrivee() +
                    ", start_latitude=" + trajet.getStartLatitude() +
                    ", start_longitude=" + trajet.getStartLongitude() +
                    ", end_latitude=" + trajet.getEndLatitude() +
                    ", end_longitude=" + trajet.getEndLongitude());
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Rows affected by insert: " + rowsAffected);
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                trajet.setId(id);
                System.out.println("Trajet added with ID: " + id);
                return id;
            } else {
                System.out.println("No generated keys returned");
            }
        } catch (SQLException e) {
            System.err.println("SQLException in ajouter: " + e.getMessage() + ", SQLState: " + e.getSQLState() + ", ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
        }
        System.out.println("Failed to add Trajet, returning -1");
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
            System.err.println("SQLException in afficher: " + e.getMessage());
            e.printStackTrace();
        }
        return trajets;
    }

    public void modifier(Trajet trajet) {
        System.out.println("TrajetService.modifier called: Trajet ID=" + trajet.getId());
        String query = "UPDATE trajet SET point_depart = ?, point_arrivee = ?, distance = ?, temps_estime = ?, start_latitude = ?, start_longitude = ?, start_nom = ?, start_type = ?, end_latitude = ?, end_longitude = ?, end_nom = ?, end_type = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            System.out.println("Preparing query: " + query);
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
            System.out.println("Executing UPDATE for Trajet ID: " + trajet.getId());
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Rows affected by update: " + rowsAffected);
        } catch (SQLException e) {
            System.err.println("SQLException in modifier: " + e.getMessage() + ", SQLState: " + e.getSQLState() + ", ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
        }
    }

    public void supprimer(int id) {
        System.out.println("TrajetService.supprimer called: Trajet ID=" + id);
        String query = "DELETE FROM trajet WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            System.out.println("Preparing query: " + query);
            stmt.setInt(1, id);
            System.out.println("Executing DELETE for Trajet ID: " + id);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Rows affected by delete: " + rowsAffected);
        } catch (SQLException e) {
            System.err.println("SQLException in supprimer: " + e.getMessage() + ", SQLState: " + e.getSQLState() + ", ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
        }
    }

    public void removeAll() {
        System.out.println("TrajetService.removeAll called");
        String query = "DELETE FROM trajet";
        try (Statement stmt = connection.createStatement()) {
            System.out.println("Preparing query: " + query);
            int rowsAffected = stmt.executeUpdate(query);
            System.out.println("Rows affected by removeAll: " + rowsAffected);
        } catch (SQLException e) {
            System.err.println("SQLException in removeAll: " + e.getMessage() + ", SQLState: " + e.getSQLState() + ", ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
        }
    }
}