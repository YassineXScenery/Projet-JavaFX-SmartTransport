package esprit.tn.guiproject.services;

import esprit.tn.guiproject.connection.DatabaseConnection;
import esprit.tn.guiproject.models.PointInteret;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PointInteretService {

    private static final Logger LOGGER = Logger.getLogger(PointInteretService.class.getName());

    // Create
    public int ajouter(PointInteret pi) {
        // Check if a record with the same nom, latitude, and longitude already exists
        String checkSql = "SELECT id FROM pointinteret WHERE nom = ? AND latitude = ? AND longitude = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, pi.getNom());
            checkPs.setDouble(2, pi.getLatitude());
            checkPs.setDouble(3, pi.getLongitude());
            ResultSet rs = checkPs.executeQuery();
            if (rs.next()) {
                System.out.println("Point d'intérêt déjà existant : " + pi.getNom());
                return rs.getInt("id"); // Return the existing ID
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking for existing PointInteret", e);
        }

        // Insert the new record
        String sql = "INSERT INTO pointinteret (latitude, longitude, nom, type) VALUES (?, ?, ?, ?)";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setDouble(1, pi.getLatitude());
            ps.setDouble(2, pi.getLongitude());
            ps.setString(3, pi.getNom());
            ps.setString(4, pi.getType());
            ps.executeUpdate();
            System.out.println("Point d'intérêt ajouté !");
            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1); // Return the new ID
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding PointInteret", e);
        }
        return -1; // Indicate failure
    }

    // Read
    public List<PointInteret> afficher() {
        List<PointInteret> list = new ArrayList<>();
        String sql = "SELECT * FROM pointinteret";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PointInteret pi = new PointInteret();
                pi.setId(rs.getInt("id"));
                pi.setLatitude(rs.getDouble("latitude"));
                pi.setLongitude(rs.getDouble("longitude"));
                pi.setNom(rs.getString("nom"));
                pi.setType(rs.getString("type"));
                list.add(pi);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving PointInteret list", e);
        }
        return list;
    }

    // Update
    public void modifier(PointInteret pi) {
        String sql = "UPDATE pointinteret SET latitude = ?, longitude = ?, nom = ?, type = ? WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDouble(1, pi.getLatitude());
            ps.setDouble(2, pi.getLongitude());
            ps.setString(3, pi.getNom());
            ps.setString(4, pi.getType());
            ps.setInt(5, pi.getId());
            ps.executeUpdate();
            System.out.println("Point d'intérêt modifié !");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating PointInteret", e);
        }
    }

    // Delete
    public void supprimer(int id) {
        String sql = "DELETE FROM pointinteret WHERE id = ?";
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Point d'intérêt supprimé !");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting PointInteret", e);
        }
    }
}