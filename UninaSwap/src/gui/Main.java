// Main.java
package gui;

import Controller.Controller;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.Service;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class Main extends Application {
    private Service service;
    private Controller controller;

    @Override
    public void start(Stage primaryStage) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            service = new Service(conn);
            controller = new Controller(service);

            LoginView loginView = new LoginView(controller, success -> {
                if (success) {
                    DashboardView dashboardView = new DashboardView(controller);
                    primaryStage.getScene().setRoot(dashboardView.getRoot());
                }
            });

            Scene scene = new Scene(loginView.getRoot(), 400, 300);
            primaryStage.setScene(scene);
            primaryStage.setTitle("UninaSwap - Login");
            primaryStage.show();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Errore connessione al database.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}