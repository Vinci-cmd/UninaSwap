package gui;

import Controller.Controller;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;

import java.sql.SQLException;

public class HomePageView {
    private HBox root;
    private Controller controller;

    public HomePageView(Controller controller) {
        this.controller = controller;
        createUI();
    }

    private void createUI() {
        root = new HBox();

        SideMenuView sideMenu = new SideMenuView();

        VBox contentArea = new VBox(15);
        contentArea.setPadding(new Insets(20));
        contentArea.setPrefWidth(700);

        Label welcomeLabel = new Label("Ciao, " + controller.getUtenteCorrente().getNome());
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        welcomeLabel.setAlignment(Pos.CENTER);

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

        HBox bottomBox = new HBox(30);
        bottomBox.setPadding(new Insets(15));
        bottomBox.setAlignment(Pos.CENTER);

        int totAnnunci = 0;
        int totOfferte = 0;
        int totOggetti = 0;
        int totAnnunciPersonali = 0;

        try {
            totAnnunci = controller.getAnnunciAttiviRaw().size();
            totAnnunciPersonali = controller.getAnnunciByUtente(controller.getUtenteCorrente().getMatricola()).size();
            totOfferte = controller.getOfferteByUtente(controller.getUtenteCorrente().getMatricola()).size();
            totOggetti = controller.getOggettiUtente(controller.getUtenteCorrente().getMatricola()).size();
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

        root.getChildren().addAll(sideMenu.getRoot(), contentArea);

        // Gestione navigazione menu laterale
        sideMenu.setOnMenuSelection(key -> {
            switch (key) {
                case "annunci_gestisci":
                    AnnunciView annunciView = new AnnunciView(controller);
                    root.getChildren().set(1, annunciView.getRoot());
                    break;
                case "annunci_lista":
                    VBox listaBox = new VBox(new Label("Lista Annunci Pubblici - da implementare"));
                    listaBox.setAlignment(Pos.CENTER);
                    listaBox.setPrefWidth(700);
                    root.getChildren().set(1, listaBox);
                    break;
                case "offerte":
                    VBox offertaBox = new VBox(new Label("Gestione Offerte - da implementare"));
                    offertaBox.setAlignment(Pos.CENTER);
                    offertaBox.setPrefWidth(700);
                    root.getChildren().set(1, offertaBox);
                    break;
                case "statistiche":
                    VBox statsBox = new VBox(new Label("Statistiche - da implementare"));
                    statsBox.setAlignment(Pos.CENTER);
                    statsBox.setPrefWidth(700);
                    root.getChildren().set(1, statsBox);
                    break;
            }
        });
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
