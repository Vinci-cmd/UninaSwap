package gui;

import Controller.Controller;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            Controller controller = new Controller(conn);


            LoginView loginView = new LoginView(primaryStage, controller);
            Scene scene = new Scene(loginView.getRoot(), 560, 450);
            primaryStage.setScene(scene);
            primaryStage.setTitle("UninaSwap - Login");
            primaryStage.setMaximized(true);
            
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