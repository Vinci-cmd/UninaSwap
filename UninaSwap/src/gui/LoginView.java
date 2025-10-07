package gui;

import Controller.Controller;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginView {
    private VBox root;

    // --- LOGIN ---
    private TextField emailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Label loginMessageLabel;
    private Hyperlink goToRegisterLink;

    // --- REGISTRAZIONE ---
    private TextField nomeField;
    private TextField cognomeField;
    private TextField matricolaField;
    private TextField regEmailField;
    private PasswordField regPasswordField;
    private PasswordField regPasswordConfirmField;
    private TextField universitaField;
    private Button registerButton;
    private Label registerMessageLabel;
    private Hyperlink backToLoginLink;

    private final Controller controller;

    public LoginView(Controller controller) {
        this.controller = controller;
        createShell();
        showLoginView(); // di default mostra login
    }

    private void createShell() {
        root = new VBox(12);
        root.setPadding(new Insets(20));
    }

    // =============== LOGIN VIEW ===============
    private void showLoginView() {
        root.getChildren().clear();

        Label title = new Label("Benvenuto in UninaSwap");

        emailField = new TextField();
        emailField.setPromptText("Email");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        loginButton = new Button("Login");
        loginMessageLabel = new Label();

        goToRegisterLink = new Hyperlink("Crea un nuovo account");
        goToRegisterLink.setOnAction(e -> showRegisterView());

        loginButton.setOnAction(e -> handleLogin());

        VBox box = new VBox(10);
        box.getChildren().addAll(
                new Label("Accedi"),
                emailField,
                passwordField,
                loginButton,
                loginMessageLabel,
                new Separator(),
                goToRegisterLink
        );

        root.getChildren().addAll(title, box);
    }

    // =============== REGISTER VIEW ===============
    private void showRegisterView() {
        root.getChildren().clear();

        Label title = new Label("Crea un nuovo account");

        nomeField = new TextField();
        nomeField.setPromptText("Nome");

        cognomeField = new TextField();
        cognomeField.setPromptText("Cognome");

        matricolaField = new TextField();
        matricolaField.setPromptText("Matricola");

        regEmailField = new TextField();
        regEmailField.setPromptText("Email");

        regPasswordField = new PasswordField();
        regPasswordField.setPromptText("Password (min 8)");

        regPasswordConfirmField = new PasswordField();
        regPasswordConfirmField.setPromptText("Conferma password");

        universitaField = new TextField();
        universitaField.setPromptText("Università");
        universitaField.setText("Università degli Studi di Napoli Federico II"); // default utile

        registerButton = new Button("Crea account");
        registerMessageLabel = new Label();

        backToLoginLink = new Hyperlink("Hai già un account? Accedi");
        backToLoginLink.setOnAction(e -> showLoginView());

        registerButton.setOnAction(e -> handleRegister());

        VBox box = new VBox(10);
        box.getChildren().addAll(
                nomeField,
                cognomeField,
                matricolaField,
                regEmailField,
                regPasswordField,
                regPasswordConfirmField,
                universitaField,
                registerButton,
                registerMessageLabel,
                new Separator(),
                backToLoginLink
        );

        root.getChildren().addAll(title, box);
    }

    // =============== HANDLERS ===============
    private void handleLogin() {
        String email = safeTrim(emailField.getText());
        String password = safeTrim(passwordField.getText());

        if (email.isEmpty() || password.isEmpty()) {
            loginMessageLabel.setText("Inserisci email e password.");
            return;
        }

        boolean success = controller.login(email, password);
        if (success) {
            loginMessageLabel.setText("");
            goHome();
        } else {
            loginMessageLabel.setText("Email o password errati");
        }
    }

    private void handleRegister() {
        String nome = safeTrim(nomeField.getText());
        String cognome = safeTrim(cognomeField.getText());
        String matricola = safeTrim(matricolaField.getText());
        String email = safeTrim(regEmailField.getText());
        String pass = safeTrim(regPasswordField.getText());
        String pass2 = safeTrim(regPasswordConfirmField.getText());
        String universita = safeTrim(universitaField.getText());

        if (nome.isEmpty() || cognome.isEmpty() || matricola.isEmpty()
                || email.isEmpty() || pass.isEmpty() || pass2.isEmpty() || universita.isEmpty()) {
            registerMessageLabel.setText("Compila tutti i campi.");
            return;
        }
        if (!isLikelyEmail(email)) {
            registerMessageLabel.setText("Email non valida.");
            return;
        }
        if (pass.length() < 8) {
            registerMessageLabel.setText("La password deve avere almeno 8 caratteri.");
            return;
        }
        if (!pass.equals(pass2)) {
            registerMessageLabel.setText("Le password non coincidono.");
            return;
        }

        // aggiorna momentaneamente la register(...) per includere università
        boolean ok = controller.register(nome, cognome, matricola, email, pass, universita);
        if (ok) {
            // login automatico
            boolean logged = controller.login(email, pass);
            if (logged) {
                registerMessageLabel.setText("");
                goHome();
            } else {
                registerMessageLabel.setText("Registrazione ok. Effettua il login.");
                showLoginView();
            }
        } else {
            registerMessageLabel.setText("Registrazione fallita. Email o matricola già in uso?");
        }
    }

    private void goHome() {
        HomePageView homepage = new HomePageView(controller);
        Stage stage = (Stage) root.getScene().getWindow();
        stage.setScene(new Scene(homepage.getRoot(), 900, 600));
        stage.setTitle("UninaSwap - Dashboard");
    }

    // =============== UTILS ===============
    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isLikelyEmail(String s) {
        return s.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    public VBox getRoot() {
        return root;
    }
}
