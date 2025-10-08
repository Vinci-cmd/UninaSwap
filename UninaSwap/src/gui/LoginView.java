package gui;

import Controller.Controller;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class LoginView {
    private VBox root;
    private final Controller controller;
    private final Stage stage; // Stage sempre presente!

    // --- LOGIN ---
    private TextField emailField;
    private PasswordField passwordField;
    private TextField passwordMirror; // per "Mostra password"
    private CheckBox showPass;
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

    public LoginView(Stage stage, Controller controller) {
        this.controller = controller;
        this.stage = stage; // Stage salvato!
        createShell();
        showLoginView();
    }

    private void createShell() {
        root = new VBox();
        root.setPadding(new Insets(24));
        root.setSpacing(0);
        root.setFillWidth(true);
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);" +
            "-fx-font-family: 'Segoe UI', 'Roboto', 'Arial';"
        );
    }

    // =============== LOGIN VIEW ===============
    private void showLoginView() {
        root.getChildren().clear();

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22));
        card.setMaxWidth(460);
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-width: 1;"
        );
        card.setEffect(new DropShadow(24, Color.color(0,0,0,0.45)));

        Label title = new Label("Accedi a UninaSwap");
        title.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 22px; -fx-font-weight: 900;");
        Label subtitle = new Label("Gestisci annunci e scambi universitari in modo semplice.");
        subtitle.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 12px;");

        Label emailLbl = smallLabel("Email");
        emailField = styledTextField("es. nome.cognome@unina.it");

        Label passLbl = smallLabel("Password");
        passwordField = styledPasswordField("Inserisci la tua password");
        passwordMirror = styledTextField("Inserisci la tua password");
        passwordMirror.setManaged(false);
        passwordMirror.setVisible(false);

        showPass = new CheckBox("Mostra password");
        showPass.setStyle("-fx-text-fill: #EAF0FF;");
        showPass.selectedProperty().addListener((obs, oldV, show) -> {
            if (show) {
                passwordMirror.setText(passwordField.getText());
                togglePasswordFields(true);
            } else {
                passwordField.setText(passwordMirror.getText());
                togglePasswordFields(false);
            }
        });

        loginMessageLabel = new Label();
        loginMessageLabel.setManaged(false);
        loginMessageLabel.setVisible(false);
        loginMessageLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        loginButton = new Button("Accedi");
        stylePrimary(loginButton);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> tryLogin());

        Button registerBtn = new Button("Registrati");
        styleGhost(registerBtn);
        registerBtn.setOnAction(e -> showRegisterView());

        actions.getChildren().addAll(loginButton, registerBtn);

        emailField.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) tryLogin(); });
        passwordField.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) tryLogin(); });
        passwordMirror.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) tryLogin(); });

        goToRegisterLink = new Hyperlink("Non hai un account? Crea un nuovo account");
        goToRegisterLink.setOnAction(e -> showRegisterView());
        goToRegisterLink.setStyle("-fx-text-fill: #7af7c3; -fx-underline: true; -fx-font-size: 12px;");

        card.getChildren().addAll(
            title,
            subtitle,
            gap(6),
            emailLbl, emailField,
            passLbl, passwordField, passwordMirror, showPass,
            loginMessageLabel,
            gap(4),
            actions,
            new Separator(),
            goToRegisterLink
        );

        StackPane centerWrap = new StackPane(card);
        StackPane.setAlignment(card, Pos.CENTER);
        centerWrap.setPadding(new Insets(16));

        Label brand = new Label("UninaSwap");
        brand.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 18px; -fx-font-weight: 800;");
        HBox header = new HBox(brand);
        header.setPadding(new Insets(4, 6, 16, 6));
        header.setAlignment(Pos.TOP_LEFT);

        root.getChildren().addAll(header, centerWrap);
        VBox.setVgrow(centerWrap, Priority.ALWAYS);
    }

    // =============== REGISTER VIEW ===============
    private void showRegisterView() {
        root.getChildren().clear();

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22));
        card.setMaxWidth(520);
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-width: 1;"
        );
        card.setEffect(new DropShadow(24, Color.color(0,0,0,0.45)));

        Label title = new Label("Crea un nuovo account");
        title.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 22px; -fx-font-weight: 900;");
        Label subtitle = new Label("Compila i dati per iniziare a usare UninaSwap.");
        subtitle.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 12px;");

        nomeField = styledTextField("Nome");
        cognomeField = styledTextField("Cognome");
        matricolaField = styledTextField("Matricola");
        regEmailField = styledTextField("Email");
        regPasswordField = styledPasswordField("Password (min 8)");
        regPasswordConfirmField = styledPasswordField("Conferma password");
        universitaField = styledTextField("Università");
        universitaField.setText("Università degli Studi di Napoli Federico II");

        registerMessageLabel = new Label();
        registerMessageLabel.setManaged(false);
        registerMessageLabel.setVisible(false);
        registerMessageLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");

        registerButton = new Button("Crea account");
        stylePrimary(registerButton);
        registerButton.setOnAction(e -> handleRegister());

        backToLoginLink = new Hyperlink("Hai già un account? Accedi");
        backToLoginLink.setStyle("-fx-text-fill: #7af7c3; -fx-underline: true; -fx-font-size: 12px;");
        backToLoginLink.setOnAction(e -> showLoginView());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(rowLabel("Nome"), 0, 0);    form.add(nomeField, 1, 0);
        form.add(rowLabel("Cognome"), 0, 1); form.add(cognomeField, 1, 1);
        form.add(rowLabel("Matricola"), 0, 2); form.add(matricolaField, 1, 2);
        form.add(rowLabel("Email"), 0, 3);   form.add(regEmailField, 1, 3);
        form.add(rowLabel("Password"), 0, 4); form.add(regPasswordField, 1, 4);
        form.add(rowLabel("Conferma"), 0, 5); form.add(regPasswordConfirmField, 1, 5);
        form.add(rowLabel("Università"), 0, 6); form.add(universitaField, 1, 6);
        ColumnConstraints c0 = new ColumnConstraints(); c0.setPercentWidth(35);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(65);
        form.getColumnConstraints().addAll(c0, c1);

        card.getChildren().addAll(
            title, subtitle, gap(6),
            form,
            registerMessageLabel,
            gap(6),
            registerButton,
            new Separator(),
            backToLoginLink
        );

        StackPane centerWrap = new StackPane(card);
        StackPane.setAlignment(card, Pos.CENTER);
        centerWrap.setPadding(new Insets(16));

        Label brand = new Label("UninaSwap");
        brand.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 18px; -fx-font-weight: 800;");
        HBox header = new HBox(brand);
        header.setPadding(new Insets(4, 6, 16, 6));
        header.setAlignment(Pos.TOP_LEFT);

        root.getChildren().addAll(header, centerWrap);
        VBox.setVgrow(centerWrap, Priority.ALWAYS);
    }

    // =============== HANDLERS ===============
    private void tryLogin() {
        String email = safeTrim(emailField.getText());
        String password = showPass.isSelected()
                ? safeTrim(passwordMirror.getText())
                : safeTrim(passwordField.getText());

        if (email.isEmpty() || password.isEmpty()) {
            showLoginError("Inserisci email e password.");
            return;
        }

        boolean success = controller.login(email, password);
        if (success) {
            hideLoginError();
            goHome();
        } else {
            showLoginError("Email o password errati.");
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
            showRegisterError("Compila tutti i campi.");
            return;
        }
        if (!isLikelyEmail(email)) {
            showRegisterError("Email non valida.");
            return;
        }
        if (pass.length() < 8) {
            showRegisterError("La password deve avere almeno 8 caratteri.");
            return;
        }
        if (!pass.equals(pass2)) {
            showRegisterError("Le password non coincidono.");
            return;
        }

        boolean ok = controller.register(nome, cognome, matricola, email, pass, universita);
        if (ok) {
            boolean logged = controller.login(email, pass);
            if (logged) {
                hideRegisterError();
                goHome();
            } else {
                hideRegisterError();
                showLoginView();
            }
        } else {
            showRegisterError("Registrazione fallita. Email o matricola già in uso?");
        }
    }

    private void goHome() {
        HomePageView homepage = new HomePageView(stage,controller);
        // Stage sempre presente!
        stage.setScene(new Scene(homepage.getRoot(), 1000, 700));
        stage.setTitle("UninaSwap - Dashboard");
    }

    private Label smallLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 12px; -fx-font-weight: 700;");
        return l;
    }

    private Label rowLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 12px; -fx-font-weight: 700;");
        return l;
    }

    private TextField styledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 12;" +
            "-fx-prompt-text-fill: rgba(234,240,255,0.45);" +
            "-fx-border-color: transparent;"
        );
        return tf;
    }

    private PasswordField styledPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 12;" +
            "-fx-prompt-text-fill: rgba(234,240,255,0.45);" +
            "-fx-border-color: transparent;"
        );
        return pf;
    }

    private void stylePrimary(Button b) {
        b.setStyle(
            "-fx-background-color: #4f8cff;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 16;" +
            "-fx-font-weight: 700;"
        );
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color: #3b6fe0; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 10 16; -fx-font-weight: 700;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color: #4f8cff; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 10 16; -fx-font-weight: 700;"
        ));
    }

    private void styleGhost(Button b) {
        b.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 16;" +
            "-fx-font-weight: 700;"
        );
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 16;" +
            "-fx-font-weight: 700;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 16;" +
            "-fx-font-weight: 700;"
        ));
    }

    private void togglePasswordFields(boolean showPlain) {
        passwordField.setManaged(!showPlain);
        passwordField.setVisible(!showPlain);
        passwordMirror.setManaged(showPlain);
        passwordMirror.setVisible(showPlain);
    }

    private Region gap(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    // =============== UTILS ===============
    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isLikelyEmail(String s) {
        return s.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private void showLoginError(String msg) {
        loginMessageLabel.setText(msg);
        loginMessageLabel.setManaged(true);
        loginMessageLabel.setVisible(true);
    }

    private void hideLoginError() {
        loginMessageLabel.setManaged(false);
        loginMessageLabel.setVisible(false);
        loginMessageLabel.setText("");
    }

    private void showRegisterError(String msg) {
        registerMessageLabel.setText(msg);
        registerMessageLabel.setManaged(true);
        registerMessageLabel.setVisible(true);
    }

    private void hideRegisterError() {
        registerMessageLabel.setManaged(false);
        registerMessageLabel.setVisible(false);
        registerMessageLabel.setText("");
    }

    public VBox getRoot() {
        return root;
    }
}
