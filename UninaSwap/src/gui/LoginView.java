package gui;

import Controller.Controller;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;

public class LoginView {
    private VBox root;
    private TextField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Label messageLabel;
    private Controller controller;

    public LoginView(Controller controller) {
        this.controller = controller;
        createUI();
    }

    private void createUI() {
        root = new VBox(10);
        root.setPadding(new Insets(20));

        emailField = new TextField();
        emailField.setPromptText("Email");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        loginButton = new Button("Login");
        messageLabel = new Label();

        loginButton.setOnAction(e -> handleLogin());

        root.getChildren().addAll(new Label("Benvenuto in UninaSwap"), emailField, passwordField, loginButton, messageLabel);
    }

    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        boolean success = controller.login(email, password);
        if (success) {
            messageLabel.setText("");
            HomePageView homepage = new HomePageView(controller);
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(new Scene(homepage.getRoot(), 900, 600));
            stage.setTitle("UninaSwap - Dashboard");
        } else {
            messageLabel.setText("Email o password errati");
        }
    }


    public VBox getRoot() {
        return root;
    }
}
