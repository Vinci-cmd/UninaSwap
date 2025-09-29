package gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.Service;
import utils.DatabaseConnection;
import model.Utente;

import java.sql.Connection;
import java.sql.SQLException;

public class Main extends Application {

    private Service service;

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Apri connessione al database
            Connection conn = DatabaseConnection.getConnection();
            service = new Service(conn);

            // 2. Crea LoginView e passagli il Service + callback per Dashboard
            LoginView loginView = new LoginView(service, utente -> {
                DashboardView dashboardView = new DashboardView(utente, service);
                primaryStage.getScene().setRoot(dashboardView.getRoot());
            });

            // 3. Imposta la scena con LoginView
            Scene scene = new Scene(loginView.getRoot(), 400, 300);
            primaryStage.setScene(scene);
            primaryStage.setTitle("UninaSwap");
            primaryStage.show();

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Errore connessione al database. Controlla credenziali e URL.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
