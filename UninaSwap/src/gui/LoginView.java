package gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import model.Utente;
import service.Service;

import java.sql.SQLException;
import java.util.function.Consumer;

public class LoginView {

    private VBox root;
    private TextField txtEmail;
    private PasswordField txtPassword;
    private Button btnLogin;
    private Label lblMessage;

    public LoginView(Service service, Consumer<Utente> onLoginSuccess) {
        root = new VBox(10);
        root.setPadding(new Insets(20));

        Label lblTitle = new Label("Login");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        txtEmail = new TextField();
        txtEmail.setPromptText("Email");

        txtPassword = new PasswordField();
        txtPassword.setPromptText("Password");

        lblMessage = new Label();
        lblMessage.setStyle("-fx-text-fill: red;");

        btnLogin = new Button("Login");
        btnLogin.setOnAction(e -> {
            try {
                Utente utente = service.login(txtEmail.getText(), txtPassword.getText());
                if (utente != null) {
                    lblMessage.setText("");
                    onLoginSuccess.accept(utente); // callback per aprire dashboard
                } else {
                    lblMessage.setText("Email o password errati");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                lblMessage.setText("Errore nel login");
            }
        });

        root.getChildren().addAll(lblTitle, txtEmail, txtPassword, btnLogin, lblMessage);
    }

    public VBox getRoot() {
        return root;
    }
}
