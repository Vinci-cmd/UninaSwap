// LoginView.java
package gui;

import Controller.Controller;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class LoginView {
    private VBox root;
    private TextField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Label messageLabel;
    private Controller controller;
    private Consumer<Boolean> onLoginSuccess;

    public LoginView(Controller controller, Consumer<Boolean> onLoginSuccess) {
        this.controller = controller;
        this.onLoginSuccess = onLoginSuccess;
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
            onLoginSuccess.accept(true);
        } else {
            messageLabel.setText("Email o password errati");
        }
    }

    public VBox getRoot() {
        return root;
    }
}