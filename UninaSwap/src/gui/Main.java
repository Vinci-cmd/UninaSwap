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
    @Override
    public void start(Stage primaryStage) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            Service service = new Service(conn);
            Controller controller = new Controller(service);

            // Partenza dalla schermata di login!
            LoginView loginView = new LoginView(primaryStage, controller);
            Scene scene = new Scene(loginView.getRoot(), 560, 450);
            primaryStage.setScene(scene);
            primaryStage.setTitle("UninaSwap - Login");
            primaryStage.show();

            // Sarà la LoginView (dopo il login) a creare l'HomePageView col suo costruttore:
            // new HomePageView(primaryStage, controller);

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Errore connessione al database.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
