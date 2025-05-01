package esprit.tn.guiproject;

import esprit.tn.guiproject.connection.DatabaseConnection;

public class Main {
    public static void main(String[] args) {
        System.out.println("Test de la connexion à la base de données...");

        if (DatabaseConnection.getInstance().getConnection() != null) {
            System.out.println("✅ Connexion réussie !");
        } else {
            System.out.println("❌ Échec de la connexion.");
        }


    }
}
