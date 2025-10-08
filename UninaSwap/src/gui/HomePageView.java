package gui;

import Controller.Controller;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.*;
import java.sql.SQLException;

public class HomePageView {
    private HBox root;
    private final Controller controller;
    private final Stage stage; // aggiunto per il logout

    private SideMenuView sideMenu;

    public HomePageView(Stage stage, Controller controller) {
        this.controller = controller;
        this.stage = stage;
        createUI();
    }

    private void createUI() {
        root = new HBox(16);
        root.setPadding(new Insets(16));
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);" +
            "-fx-font-family: 'Segoe UI','Roboto','Arial';"
        );

        sideMenu = new SideMenuView();

        Node contentArea = createHomeContentArea();

        root.getChildren().addAll(sideMenu.getRoot(), contentArea);
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        sideMenu.setOnMenuSelection(key -> {
            switch (key) {
                case "home": {
                    Node newContent = createHomeContentArea();
                    root.getChildren().set(1, newContent);
                    HBox.setHgrow(newContent, Priority.ALWAYS);
                    break;
                }
                case "annunci_gestisci": {
                    AnnunciView annunciView = new AnnunciView(controller);
                    Node newContent = annunciView.getRoot();
                    root.getChildren().set(1, newContent);
                    HBox.setHgrow(newContent, Priority.ALWAYS);
                    break;
                }
                case "annunci_lista": {
                    ListaAnnunciView listaAnnunciView = new ListaAnnunciView(controller);
                    Node newContent = listaAnnunciView.getRoot();
                    root.getChildren().set(1, newContent);
                    HBox.setHgrow(newContent, Priority.ALWAYS);
                    break;
                }
                case "offerte_inviate": {
                    OfferteInviateView offerteInvView = new OfferteInviateView(controller);
                    Node contentInv = offerteInvView.getRoot();
                    root.getChildren().set(1, contentInv);
                    HBox.setHgrow(contentInv, Priority.ALWAYS);
                    break;
                }
                case "offerte_ricevute": {
                    OfferteRicevuteView offerteRcvView = new OfferteRicevuteView(controller);
                    Node contentRcv = offerteRcvView.getRoot();
                    root.getChildren().set(1, contentRcv);
                    HBox.setHgrow(contentRcv, Priority.ALWAYS);
                    break;
                }
                case "oggetti": {
                    OggettiView oggettiView = new OggettiView(controller);
                    Node newContent = oggettiView.getRoot();
                    root.getChildren().set(1, newContent);
                    HBox.setHgrow(newContent, Priority.ALWAYS);
                    break;
                }
                case "statistiche": {
                    ReportView reportView = new ReportView(controller);
                    Node newContent = reportView.getRoot();
                    root.getChildren().set(1, newContent);
                    HBox.setHgrow(newContent, Priority.ALWAYS);
                    break;
                }
            }
        });
    }

    private Node createHomeContentArea() {
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(10));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 6, 16, 6));

        String nome = (controller.getUtenteCorrente() != null && controller.getUtenteCorrente().getNome() != null)
                ? controller.getUtenteCorrente().getNome()
                : "Utente";

        Label brand = new Label("UninaSwap");
        brand.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 20px; -fx-font-weight: 900;");
        Label sub = new Label("• Benvenuto, " + nome);
        sub.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logout = ghostButton("Logout", () -> {
            controller.logout(); // azzera sessione utente se serve
            LoginView loginView = new LoginView(stage, controller);
            Scene loginScene = new Scene(loginView.getRoot(), 560, 450);
            stage.setScene(loginScene);
            stage.setTitle("UninaSwap - Login");
        });

        header.getChildren().addAll(brand, sub, spacer, logout);
        content.setTop(header);

        VBox center = new VBox(16);
        center.setFillWidth(true);

        HBox heroRow = new HBox(16);
        heroRow.setAlignment(Pos.CENTER_LEFT);

        VBox heroCard = card(
                h1("Dashboard"),
                subtitle("Gestisci annunci, offerte e oggetti in un'unica schermata ✨")
        );

        VBox quickActions = card(
                title("Azioni rapide"),
                row(
                    primaryButton("Crea annuncio", () -> {
                        AnnunciView annunciView = new AnnunciView(controller);
                        annunciView.mostraCreaAnnuncioDialog(); // <--- chiama direttamente la creazione annuncio
                    }),
                    ghostButton("Gestisci annunci", () -> {
                        AnnunciView annunciView = new AnnunciView(controller);
                        Node newContent = annunciView.getRoot();
                        root.getChildren().set(1, newContent);
                        HBox.setHgrow(newContent, Priority.ALWAYS);
                    }),
                    ghostButton("Gestisci offerte", () -> {
                        OfferteRicevuteView offerteRcvView = new OfferteRicevuteView(controller);
                        Node contentRcv = offerteRcvView.getRoot();
                        root.getChildren().set(1, contentRcv);
                        HBox.setHgrow(contentRcv, Priority.ALWAYS);
                    })
                )
        );

        HBox.setHgrow(heroCard, Priority.ALWAYS);
        heroRow.getChildren().addAll(heroCard, quickActions);

        ListView<String> notifList = new ListView<>();
        notifList.setPrefHeight(200);
        notifList.getItems().clear();
        try {
            String matricola = controller.getUtenteCorrente() != null ? controller.getUtenteCorrente().getMatricola() : null;
            if (matricola != null) {
                List<String> notifiche = controller.getNotificheUtente(matricola);
                if (notifiche.isEmpty()) {
                    notifList.getItems().add("Nessuna notifica recente.");
                } else {
                    notifList.getItems().addAll(notifiche);
                }
            } else {
                notifList.getItems().add("Nessuna notifica, login richiesto.");
            }
        } catch (Exception e) {
            notifList.getItems().add("Errore nel caricamento notifiche.");
        }
        notifList.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-control-inner-background: rgba(255,255,255,0.04);" +
            "-fx-background-insets: 0;" +
            "-fx-text-fill: #EAF0FF;"
        );

        VBox notifCard = card(
                title("Notifiche recenti"),
                subtitle("Le ultime attività del tuo account"),
                notifList
        );

        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        int totAnnunci = 0, totOfferte = 0, totOggetti = 0, totAnnunciPersonali = 0;
        try {
            totAnnunci = controller.getAnnunciAttiviRaw().size();
            String matricola = controller.getUtenteCorrente() != null ? controller.getUtenteCorrente().getMatricola() : "";
            totAnnunciPersonali = controller.getAnnunciByUtente(matricola).size();
            totOfferte = controller.getOfferteByUtente(matricola).size();
            totOggetti = controller.getOggettiUtente(matricola).size();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        statsRow.getChildren().addAll(
                statBox("Annunci", String.valueOf(totAnnunci)),
                statBox("Offerte", String.valueOf(totOfferte)),
                statBox("Oggetti", String.valueOf(totOggetti)),
                statBox("Annunci Personali", String.valueOf(totAnnunciPersonali))
        );

        VBox statsCard = card(
                title("Statistiche rapide"),
                subtitle("Panoramica del tuo profilo"),
                statsRow
        );

        center.getChildren().addAll(heroRow, notifCard, statsCard);
        content.setCenter(center);

        return content;
    }

    private void openAnnunciGestisci() {
        AnnunciView annunciView = new AnnunciView(controller);
        Node newContent = annunciView.getRoot();
        root.getChildren().set(1, newContent);
        HBox.setHgrow(newContent, Priority.ALWAYS);
    }

    private VBox card(Node... children) {
        VBox card = new VBox(10, children);
        card.setPadding(new Insets(18));
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-width: 1;"
        );
        card.setEffect(new DropShadow(24, Color.color(0,0,0,0.45)));
        return card;
    }

    private HBox row(Node... children) {
        HBox r = new HBox(10, children);
        r.setAlignment(Pos.CENTER_LEFT);
        return r;
    }

    private Label h1(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 22px; -fx-font-weight: 900;");
        return l;
    }

    private Label title(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 16px; -fx-font-weight: 800;");
        return l;
    }

    private Label subtitle(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 12px;");
        return l;
    }

    private Button primaryButton(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
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
        return b;
    }

    private Button ghostButton(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
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
            "-fx-padding: 10 16; -fx-font-weight: 700;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 16; -fx-font-weight: 700;"
        ));
        return b;
    }

    private VBox statBox(String title, String value) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 12px; -fx-font-weight: 800;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: 900;");

        VBox box = new VBox(6, t, v);
        box.setPadding(new Insets(16));
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(160, 100);
        box.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, rgba(79,140,255,0.18), rgba(122,247,195,0.18));" +
            "-fx-background-radius: 14;" +
            "-fx-border-radius: 14;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-width: 1;"
        );
        box.setEffect(new DropShadow(14, Color.color(0,0,0,0.35)));
        return box;
    }

    public HBox getRoot() {
        return root;
    }

    private void navigate(String key) {
        System.out.println("Vai a: " + key);
    }
}
