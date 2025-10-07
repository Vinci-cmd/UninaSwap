package gui;

import Controller.Controller;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;

import java.sql.SQLException;

public class HomePageView {
    private HBox root;
    private final Controller controller;

    public HomePageView(Controller controller) {
        this.controller = controller;
        createUI();
    }

    private void createUI() {
        root = new HBox();

        // menu laterale
        SideMenuView sideMenu = new SideMenuView();

        // contenuto iniziale: home
        Node contentArea = createHomeContentArea();

        // layout principale
        root.getChildren().addAll(sideMenu.getRoot(), contentArea);
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        // navigazione menu laterale
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

                case "oggetti":
                    OggettiView oggettiView = new OggettiView(controller);
                    root.getChildren().set(1, oggettiView.getRoot());
                    break;
                

                // ...altri casi come già presenti
                case "statistiche": {
                    // integra il ReportView nel pannello centrale
                    ReportView reportView = new ReportView(controller);
                    Node newContent = reportView.getRoot();
                    root.getChildren().set(1, newContent);
                    HBox.setHgrow(newContent, Priority.ALWAYS);
                    break;
                }
            }
        });
    }

    /** Crea il contenuto della home */
    private VBox createHomeContentArea() {
        VBox contentArea = new VBox(15);
        contentArea.setPadding(new Insets(20));
        contentArea.setPrefWidth(700);

        String nome = controller.getUtenteCorrente() != null && controller.getUtenteCorrente().getNome() != null
                ? controller.getUtenteCorrente().getNome()
                : "Utente";
        Label welcomeLabel = new Label("Ciao, " + nome);
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        welcomeLabel.setAlignment(Pos.CENTER);

        // notifiche finte per ora
        VBox centerBox = new VBox(10);
        Label notifLabel = new Label("Notifiche recenti:");
        notifLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        ListView<String> notifList = new ListView<>();
        notifList.setPrefHeight(200);
        notifList.getItems().addAll(
                "Hai 2 offerte da accettare",
                "Scambio programmato domani",
                "Annuncio #123 ha ricevuto una nuova offerta"
        );
        centerBox.getChildren().addAll(notifLabel, notifList);
        centerBox.setAlignment(Pos.CENTER);

        // statistiche rapide senza bottone report
        HBox bottomBox = new HBox(30);
        bottomBox.setPadding(new Insets(15));
        bottomBox.setAlignment(Pos.CENTER);

        int totAnnunci = 0;
        int totOfferte = 0;
        int totOggetti = 0;
        int totAnnunciPersonali = 0;

        try {
            totAnnunci = controller.getAnnunciAttiviRaw().size();
            String matricola = controller.getUtenteCorrente() != null ? controller.getUtenteCorrente().getMatricola() : "";
            totAnnunciPersonali = controller.getAnnunciByUtente(matricola).size();
            totOfferte = controller.getOfferteByUtente(matricola).size();
            totOggetti = controller.getOggettiUtente(matricola).size();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        bottomBox.getChildren().addAll(
                statBox("Annunci", String.valueOf(totAnnunci)),
                statBox("Offerte", String.valueOf(totOfferte)),
                statBox("Oggetti", String.valueOf(totOggetti)),
                statBox("Annunci Personali", String.valueOf(totAnnunciPersonali))
        );

        contentArea.getChildren().addAll(welcomeLabel, centerBox, bottomBox);

        return contentArea;
    }

    private VBox statBox(String title, String value) {
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        VBox box = new VBox(5, t, v);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #e9eefa; -fx-border-radius: 5; -fx-background-radius: 5;");
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(150, 100);
        return box;
    }

    public HBox getRoot() {
        return root;
    }
}