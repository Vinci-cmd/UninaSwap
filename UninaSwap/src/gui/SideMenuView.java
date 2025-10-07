package gui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
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

    // Sezione Offerte
    private Button btnOfferteHeader;
    private VBox offerteSubmenu;
    private boolean offerteExpanded = false;

    // Pulsanti principali
    private Button btnHome;
    private Button btnOggetti;
    private Button btnStatistiche;

    // tracking per stato selezionato
    private final List<Button> allClickableButtons = new ArrayList<>();
    private Button selectedBtn = null;

    public SideMenuView() {
        createUI();
    }

    private void createUI() {
        menuBox = new VBox(8);
        menuBox.setPadding(new Insets(12));
        menuBox.setPrefWidth(220);
        menuBox.setAlignment(Pos.TOP_LEFT);
        menuBox.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-width: 1;"
        );
        menuBox.setEffect(new DropShadow(24, Color.color(0,0,0,0.45)));

        toggleButton = new ToggleButton("☰");
        styleToggle(toggleButton);
        toggleButton.setOnAction(e -> toggleMenu());

        // HOME
        btnHome = mainItem("⌂  Home", "home");
        rememberTexts(btnHome, "⌂  Home");
        btnHome.setOnAction(e -> { select(btnHome); notifyMenuSelection("home"); });

        // ANNUNCI
        btnAnnunciHeader = mainItem("📄  Annunci ▶", null);
        rememberTexts(btnAnnunciHeader, "📄  Annunci ▶");
        btnAnnunciHeader.setOnAction(e -> toggleAnnunciSubmenu());

        Button btnGestisciAnnunci = subItem("I miei Annunci", "annunci_gestisci");
        rememberTexts(btnGestisciAnnunci, "I miei Annunci");
        btnGestisciAnnunci.setOnAction(e -> { select(btnGestisciAnnunci); notifyMenuSelection("annunci_gestisci"); });

        Button btnListaAnnunci = subItem("Lista Annunci", "annunci_lista");
        rememberTexts(btnListaAnnunci, "Lista Annunci");
        btnListaAnnunci.setOnAction(e -> { select(btnListaAnnunci); notifyMenuSelection("annunci_lista"); });

        annunciSubmenu = new VBox(6, btnGestisciAnnunci, btnListaAnnunci);
        annunciSubmenu.setPadding(new Insets(0, 0, 0, 16));
        annunciSubmenu.setVisible(false);
        annunciSubmenu.setManaged(false);

        // OFFERTE
        btnOfferteHeader = mainItem("💬  Offerte ▶", null);
        rememberTexts(btnOfferteHeader, "💬  Offerte ▶");
        btnOfferteHeader.setOnAction(e -> toggleOfferteSubmenu());

        Button btnOfferteInviate = subItem("Offerte Inviate", "offerte_inviate");
        rememberTexts(btnOfferteInviate, "Offerte Inviate");
        btnOfferteInviate.setOnAction(e -> { select(btnOfferteInviate); notifyMenuSelection("offerte_inviate"); });

        Button btnOfferteRicevute = subItem("Offerte Ricevute", "offerte_ricevute");
        rememberTexts(btnOfferteRicevute, "Offerte Ricevute");
        btnOfferteRicevute.setOnAction(e -> { select(btnOfferteRicevute); notifyMenuSelection("offerte_ricevute"); });

        offerteSubmenu = new VBox(6, btnOfferteInviate, btnOfferteRicevute);
        offerteSubmenu.setPadding(new Insets(0, 0, 0, 16));
        offerteSubmenu.setVisible(false);
        offerteSubmenu.setManaged(false);

        // OGGETTI / STATISTICHE
        btnOggetti = mainItem("📦  Oggetti personali", "oggetti");
        rememberTexts(btnOggetti, "📦  Oggetti personali");
        btnOggetti.setOnAction(e -> { select(btnOggetti); notifyMenuSelection("oggetti"); });

        btnStatistiche = mainItem("📊  Statistiche", "statistiche");
        rememberTexts(btnStatistiche, "📊  Statistiche");
        btnStatistiche.setOnAction(e -> { select(btnStatistiche); notifyMenuSelection("statistiche"); });

        menuBox.getChildren().addAll(
            toggleButton,
            spacer(4),
            btnHome,
            spacer(2),
            btnAnnunciHeader, annunciSubmenu,
            btnOfferteHeader, offerteSubmenu,
            spacer(2),
            btnOggetti,
            btnStatistiche
        );

        // di default: seleziona Home
        select(btnHome);
    }

    // ---------- Helpers testi full vs icon ----------
    private void rememberTexts(Button b, String fullText) {
        b.getProperties().put("fullText", fullText);
        b.getProperties().put("iconText", extractIcon(fullText));
    }
    private String extractIcon(String fullText) {
        int i = fullText.indexOf("  "); // due spazi dopo l'emoji
        return (i > 0) ? fullText.substring(0, i) : fullText;
    }
    private String fullTextOf(Button b) {
        Object o = b.getProperties().get("fullText");
        return (o instanceof String) ? (String) o : b.getText();
    }
    private String iconTextOf(Button b) {
        Object o = b.getProperties().get("iconText");
        return (o instanceof String) ? (String) o : extractIcon(b.getText());
    }

    // ---------- Styling ----------
    private void styleToggle(ToggleButton t) {
        t.setMaxWidth(Double.MAX_VALUE);
        t.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: 800;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-width: 1;" +
            "-fx-padding: 8 10;"
        );
        t.setOnMouseEntered(e -> t.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-font-size: 16px; -fx-font-weight: 800;" +
            "-fx-background-radius: 12; -fx-border-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.18); -fx-border-width: 1;" +
            "-fx-padding: 8 10;"
        ));
        t.setOnMouseExited(e -> t.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-font-size: 16px; -fx-font-weight: 800;" +
            "-fx-background-radius: 12; -fx-border-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.18); -fx-border-width: 1;" +
            "-fx-padding: 8 10;"
        ));
    }

    private Button mainItem(String text, String key) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setUserData(key); // può essere null per gli header espandibili
        btn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 12;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 12;"
        ));
        btn.setOnMouseExited(e -> {
            if (btn != selectedBtn)
                btn.setStyle(
                    "-fx-background-color: transparent;" +
                    "-fx-text-fill: #EAF0FF;" +
                    "-fx-font-size: 13px;" +
                    "-fx-background-radius: 12;" +
                    "-fx-padding: 10 12;"
                );
        });
        allClickableButtons.add(btn);
        return btn;
    }

    private Button subItem(String text, String key) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setUserData(key);
        btn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.04);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-font-size: 12px;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 8 12;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-font-size: 12px;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 8 12;"
        ));
        btn.setOnMouseExited(e -> {
            if (btn != selectedBtn)
                btn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.04);" +
                    "-fx-text-fill: #EAF0FF;" +
                    "-fx-font-size: 12px;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 8 12;"
                );
        });
        allClickableButtons.add(btn);
        return btn;
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    // ---------- Selettore visuale ----------
    private void select(Button btn) {
        // reset stile degli altri
        for (Button b : allClickableButtons) {
            if (b == btn) continue;
            if (b.getParent() == annunciSubmenu || b.getParent() == offerteSubmenu) {
                b.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.04);" +
                    "-fx-text-fill: #EAF0FF;" +
                    "-fx-font-size: 12px;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 8 12;"
                );
            } else {
                b.setStyle(
                    "-fx-background-color: transparent;" +
                    "-fx-text-fill: #EAF0FF;" +
                    "-fx-font-size: 13px;" +
                    "-fx-background-radius: 12;" +
                    "-fx-padding: 10 12;"
                );
            }
        }
        // stile selezionato
        selectedBtn = btn;
        btn.setStyle(
            "-fx-background-color: #4f8cff;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 700;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 12;"
        );
        // auto-espandi il relativo submenu
        if (btn.getParent() == annunciSubmenu && !annunciExpanded) toggleAnnunciSubmenu();
        if (btn.getParent() == offerteSubmenu && !offerteExpanded) toggleOfferteSubmenu();
    }

    // ---------- Toggle submenu con animazione ----------
    private void toggleAnnunciSubmenu() {
        annunciExpanded = !annunciExpanded;
        animateSubmenu(annunciSubmenu, annunciExpanded);
        btnAnnunciHeader.setText(annunciExpanded ? "📄  Annunci ▼" : "📄  Annunci ▶");
        rememberTexts(btnAnnunciHeader, btnAnnunciHeader.getText()); // aggiorna freccia nel "fullText"
    }

    private void toggleOfferteSubmenu() {
        offerteExpanded = !offerteExpanded;
        animateSubmenu(offerteSubmenu, offerteExpanded);
        btnOfferteHeader.setText(offerteExpanded ? "💬  Offerte ▼" : "💬  Offerte ▶");
        rememberTexts(btnOfferteHeader, btnOfferteHeader.getText()); // aggiorna freccia nel "fullText"
    }

    private void animateSubmenu(VBox submenu, boolean expand) {
        submenu.setManaged(true);
        submenu.setVisible(true);

        double from = expand ? 0 : submenu.getHeight();
        double to   = expand ? computeVBoxHeight(submenu) : 0;

        Timeline heightTl = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(submenu.prefHeightProperty(), from)),
            new KeyFrame(Duration.millis(220), new KeyValue(submenu.prefHeightProperty(), to, Interpolator.EASE_BOTH))
        );

        FadeTransition fade = new FadeTransition(Duration.millis(200), submenu);
        fade.setFromValue(expand ? 0 : 1);
        fade.setToValue(expand ? 1 : 0);

        fade.setOnFinished(ev -> {
            if (!expand) {
                submenu.setVisible(false);
                submenu.setManaged(false);
                submenu.setPrefHeight(Region.USE_COMPUTED_SIZE);
            } else {
                submenu.setPrefHeight(Region.USE_COMPUTED_SIZE);
            }
        });

        new ParallelTransition(heightTl, fade).play();
    }

    private double computeVBoxHeight(VBox v) {
        double h = v.getInsets().getTop() + v.getInsets().getBottom();
        for (Node n : v.getChildren()) {
            h += n.prefHeight(-1) + v.getSpacing();
        }
        return h;
    }

    // ---------- Toggle menu con animazione larghezza ----------
    private void toggleMenu() {
        double start = menuBox.getPrefWidth();
        double end = isVisible ? 60 : 220; // 60px: solo icone
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(menuBox.prefWidthProperty(), start)),
            new KeyFrame(Duration.millis(240), new KeyValue(menuBox.prefWidthProperty(), end, Interpolator.EASE_BOTH))
        );
        tl.play();

        isVisible = !isVisible;
        toggleButton.setText(isVisible ? "☰" : "✕");

        // Applica stato collassato/espanso DOPO aver aggiornato isVisible
        applyCollapsedState(!isVisible);
    }

    private void applyCollapsedState(boolean collapsed) {
        for (Node n : menuBox.getChildren()) {
            if (n == toggleButton) continue;

            if (n instanceof Button b) {
                String full = fullTextOf(b);
                String icon = iconTextOf(b);

                if (collapsed) {
                    // mostra solo l'icona, tooltip col label
                    String label = full.startsWith(icon + "  ") ? full.substring((icon + "  ").length()) : full;
                    b.setText(icon);
                    b.setAlignment(Pos.CENTER);
                    b.setTooltip(new Tooltip(label));
                } else {
                    // ripristina testo completo
                    b.setText(full);
                    b.setAlignment(Pos.CENTER_LEFT);
                    b.setTooltip(null);
                }
            } else if (n instanceof VBox v) {
                // submenu visibili solo se espanso e flag expanded true
                if (v == annunciSubmenu) {
                    boolean show = !collapsed && annunciExpanded;
                    v.setVisible(show);
                    v.setManaged(show);
                } else if (v == offerteSubmenu) {
                    boolean show = !collapsed && offerteExpanded;
                    v.setVisible(show);
                    v.setManaged(show);
                } else {
                    v.setVisible(!collapsed);
                    v.setManaged(!collapsed);
                }
            }
        }

        // ripristina testi header con freccia corretta quando espanso
        if (!collapsed) {
            btnAnnunciHeader.setText(annunciExpanded ? "📄  Annunci ▼" : "📄  Annunci ▶");
            rememberTexts(btnAnnunciHeader, btnAnnunciHeader.getText());
            btnOfferteHeader.setText(offerteExpanded ? "💬  Offerte ▼" : "💬  Offerte ▶");
            rememberTexts(btnOfferteHeader, btnOfferteHeader.getText());
        }
    }

    // ---------- API esistente ----------
    private void notifyMenuSelection(String key) {
        if (onMenuSelection != null) onMenuSelection.accept(key);
    }

    public void setOnMenuSelection(Consumer<String> onMenuSelection) {
        this.onMenuSelection = onMenuSelection;
    }

    public VBox getRoot() {
        return menuBox;
    }

    // ---------- (Opzionale) selezione dall'esterno ----------
    public void select(String key) {
        for (Button b : allClickableButtons) {
            if (key != null && key.equals(b.getUserData())) {
                select(b);
                return;
            }
        }
    }
}
