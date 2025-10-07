package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class SideMenuView {
    private VBox menuBox;
    private ToggleButton toggleButton;
    private boolean isVisible = true;
    private Consumer<String> onMenuSelection;

    // Sezione Annunci
    private Button btnAnnunciHeader;
    private VBox annunciSubmenu;
    private boolean annunciExpanded = false;
    
    // Nuovo: pulsante home
    private Button btnHome;

    public SideMenuView() {
        createUI();
    }

    private void createUI() {
        menuBox = new VBox(10);
        menuBox.setPadding(new Insets(10));
        menuBox.setStyle("-fx-background-color: #2c3e50;");
        menuBox.setPrefWidth(200);
        menuBox.setAlignment(Pos.TOP_LEFT);

        toggleButton = new ToggleButton("☰");
        toggleButton.setStyle("-fx-font-size: 18; -fx-background-color: #34495e; -fx-text-fill: white;");
        toggleButton.setMaxWidth(Double.MAX_VALUE);
        toggleButton.setOnAction(e -> toggleMenu());

        // Aggiunta tasto Home con simbolo ⌂
        btnHome = new Button("⌂ Home");
        styleMenuButton(btnHome);
        btnHome.setOnAction(e -> notifyMenuSelection("home"));

        // Header "Annunci" con freccia
        btnAnnunciHeader = new Button("Annunci ▶");
        styleMenuButton(btnAnnunciHeader);
        btnAnnunciHeader.setOnAction(e -> toggleAnnunciSubmenu());

        // Submenu annunci
        Button btnGestisciAnnunci = new Button("Gestisci Annunci");
        Button btnListaAnnunci = new Button("Lista Annunci");
        styleSubmenuButton(btnGestisciAnnunci);
        styleSubmenuButton(btnListaAnnunci);

        btnGestisciAnnunci.setOnAction(e -> notifyMenuSelection("annunci_gestisci"));
        btnListaAnnunci.setOnAction(e -> notifyMenuSelection("annunci_lista"));

        annunciSubmenu = new VBox(6, btnGestisciAnnunci, btnListaAnnunci);
        annunciSubmenu.setPadding(new Insets(0, 0, 0, 16));
        annunciSubmenu.setVisible(false);
        annunciSubmenu.setManaged(false);

        // Altre voci esempio
        Button btnGestioneOfferte = new Button("Gestione Offerte");
        Button btnStatistiche = new Button("Statistiche");
        styleMenuButton(btnGestioneOfferte);
        styleMenuButton(btnStatistiche);
        btnGestioneOfferte.setOnAction(e -> notifyMenuSelection("offerte"));
        btnStatistiche.setOnAction(e -> notifyMenuSelection("statistiche"));

        // Inserimento del nuovo pulsante home DOPO il toggleButton
        menuBox.getChildren().addAll(toggleButton, btnHome, btnAnnunciHeader, annunciSubmenu, btnGestioneOfferte, btnStatistiche);
    }

    private void styleMenuButton(Button btn) {
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-alignment: CENTER_LEFT;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #3d566e; -fx-text-fill: white; -fx-alignment: CENTER_LEFT;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-alignment: CENTER_LEFT;"));
    }

    private void styleSubmenuButton(Button btn) {
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #223041; -fx-text-fill: white; -fx-alignment: CENTER_LEFT;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2a3a4e; -fx-text-fill: white; -fx-alignment: CENTER_LEFT;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #223041; -fx-text-fill: white; -fx-alignment: CENTER_LEFT;"));
    }

    private void toggleAnnunciSubmenu() {
        annunciExpanded = !annunciExpanded;
        annunciSubmenu.setVisible(annunciExpanded);
        annunciSubmenu.setManaged(annunciExpanded);
        btnAnnunciHeader.setText(annunciExpanded ? "Annunci ▼" : "Annunci ▶");
    }

    private void toggleMenu() {
        if (isVisible) {
            menuBox.setPrefWidth(40);
            menuBox.getChildren().filtered(node -> node != toggleButton).forEach(node -> node.setVisible(false));
            isVisible = false;
            toggleButton.setText("✕");
        } else {
            menuBox.setPrefWidth(200);
            menuBox.getChildren().forEach(node -> node.setVisible(true));
            isVisible = true;
            toggleButton.setText("☰");
        }
    }

    private void notifyMenuSelection(String key) {
        if (onMenuSelection != null) onMenuSelection.accept(key);
    }

    public void setOnMenuSelection(Consumer<String> onMenuSelection) {
        this.onMenuSelection = onMenuSelection;
    }

    public VBox getRoot() {
        return menuBox;
    }
}
