package esprit.tn.guiproject.services;

import esprit.tn.guiproject.connection.DatabaseConnection;
import esprit.tn.guiproject.models.Trajet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TrajetService {

    private static final Logger LOGGER = Logger.getLogger(TrajetService.class.getName());

    // Create
    public void ajouter(Trajet trajet) {
        String sql = "INSERT INTO trajet (distance, point_depart, point_arrivee, temps_estime) VALUES (?, ?, ?, ?)";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDouble(1, trajet.getDistance());
            if (trajet.getPointDepart() != null) {
                ps.setInt(2, trajet.getPointDepart());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            if (trajet.getPointArrivee() != null) {
                ps.setInt(3, trajet.getPointArrivee());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            if (trajet.getTempsEstime() != null) {
                ps.setTime(4, trajet.getTempsEstime());
            } else {
                ps.setNull(4, java.sql.Types.TIME);
            }
            ps.executeUpdate();
            System.out.println("Trajet ajouté !");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding Trajet", e);
        }
    }

    // Read
    public List<Trajet> afficher() {
        List<Trajet> list = new ArrayList<>();
        String sql = "SELECT * FROM trajet";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Trajet trajet = new Trajet();
                trajet.setId(rs.getInt("id"));
                trajet.setDistance(rs.getDouble("distance"));
                trajet.setPointDepart(rs.getObject("point_depart", Integer.class));
                trajet.setPointArrivee(rs.getObject("point_arrivee", Integer.class));
                trajet.setTempsEstime(rs.getTime("temps_estime"));
                list.add(trajet);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving Trajet list", e);
        }
        return list;
    }

    // Update
    public void modifier(Trajet trajet) {
        String sql = "UPDATE trajet SET distance = ?, point_depart = ?, point_arrivee = ?, temps_estime = ? WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDouble(1, trajet.getDistance());
            if (trajet.getPointDepart() != null) {
                ps.setInt(2, trajet.getPointDepart());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            if (trajet.getPointArrivee() != null) {
                ps.setInt(3, trajet.getPointArrivee());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            if (trajet.getTempsEstime() != null) {
                ps.setTime(4, trajet.getTempsEstime());
            } else {
                ps.setNull(4, java.sql.Types.TIME);
            }
            ps.setInt(5, trajet.getId());
            ps.executeUpdate();
            System.out.println("Trajet modifié !");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating Trajet", e);
        }
    }

    // Delete
    public void supprimer(int id) {
        String sql = "DELETE FROM trajet WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Trajet supprimé !");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting Trajet", e);
        }
    }
}