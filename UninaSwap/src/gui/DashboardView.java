package gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.Annuncio;
import model.Offerta;
import model.Utente;
import service.Service;

import java.sql.SQLException;
import java.util.List;

public class DashboardView {

    private VBox root;
    private Utente utente;
    private Service service;

    private ListView<String> annunciList;
    private ListView<String> offerteList;
    private Button btnRefreshAnnunci;
    private Button btnRefreshOfferte;
    private Button btnAccetta;
    private Button btnRifiuta;

    private Annuncio selectedAnnuncio;
    private Offerta selectedOfferta;

    public DashboardView(Utente utente, Service service) {
        this.utente = utente;
        this.service = service;

        root = new VBox(15);
        root.setPadding(new Insets(20));

        Label lblWelcome = new Label("Benvenuto, " + utente.getNome() + "!");
        lblWelcome.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Lista annunci
        annunciList = new ListView<>();
        annunciList.setPrefHeight(200);
        annunciList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int index = annunciList.getSelectionModel().getSelectedIndex();
                if (index >= 0) {
                    selectedAnnuncio = service.getAnnunciByUtente(utente.getMatricola()).get(index);
                    caricaOfferte();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        btnRefreshAnnunci = new Button("Aggiorna Annunci");
        btnRefreshAnnunci.setOnAction(e -> caricaAnnunci());

        // Lista offerte
        offerteList = new ListView<>();
        offerteList.setPrefHeight(150);
        offerteList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int index = offerteList.getSelectionModel().getSelectedIndex();
                if (index >= 0 && selectedAnnuncio != null) {
                    selectedOfferta = service.getOfferteByAnnuncio(selectedAnnuncio.getCodiceAnnuncio()).get(index);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        btnRefreshOfferte = new Button("Aggiorna Offerte");
        btnRefreshOfferte.setOnAction(e -> caricaOfferte());

        btnAccetta = new Button("Accetta Offerta");
        btnAccetta.setOnAction(e -> {
            if (selectedOfferta != null) {
                try {
                    service.accettaOfferta(selectedOfferta.getCodiceOfferta());
                    caricaAnnunci();
                    caricaOfferte();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        btnRifiuta = new Button("Rifiuta Offerta");
        btnRifiuta.setOnAction(e -> {
            if (selectedOfferta != null) {
                try {
                    service.rifiutaOfferta(selectedOfferta.getCodiceOfferta());
                    caricaOfferte();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        HBox offerteButtons = new HBox(10, btnAccetta, btnRifiuta, btnRefreshOfferte);

        root.getChildren().addAll(lblWelcome, annunciList, btnRefreshAnnunci, new Label("Offerte:"), offerteList, offerteButtons);

        caricaAnnunci();
    }

    private void caricaAnnunci() {
        try {
            List<Annuncio> annunci = service.getAnnunciByUtente(utente.getMatricola());
            annunciList.getItems().clear();
            for (Annuncio a : annunci) {
                String item = "[" + a.getTipologia() + "] " + a.getDescrizione() + " - " + a.getStato();
                annunciList.getItems().add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            annunciList.getItems().clear();
            annunciList.getItems().add("Errore nel caricamento degli annunci");
        }
    }

    private void caricaOfferte() {
        if (selectedAnnuncio == null) return;
        try {
            List<Offerta> offerte = service.getOfferteByAnnuncio(selectedAnnuncio.getCodiceAnnuncio());
            offerteList.getItems().clear();
            for (Offerta o : offerte) {
                String item = "[" + o.getTipo() + "] " + (o.getPrezzoOfferto() != null ? o.getPrezzoOfferto() + "€" : "") + " - " + o.getStato();
                offerteList.getItems().add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            offerteList.getItems().clear();
            offerteList.getItems().add("Errore nel caricamento delle offerte");
        }
    }

    public VBox getRoot() {
        return root;
    }
}
