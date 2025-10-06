package gui;

import Controller.Controller;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Annuncio;
import model.Offerta;

import java.sql.SQLException;
import java.util.List;

public class DashboardView {
    private VBox root;
    private Controller controller;

    private ListView<String> annunciListView;
    private ListView<String> offerteListView;

    private Button refreshAnnunciBtn;
    private Button refreshOfferteBtn;
    private Button accettaOffertaBtn;
    private Button rifiutaOffertaBtn;

    private Annuncio selectedAnnuncio;
    private Offerta selectedOfferta;

    public DashboardView(Controller controller) {
        this.controller = controller;
        createUI();
        loadAnnunci();
    }

    private void createUI() {
        root = new VBox(15);
        root.setPadding(new Insets(20));

        Label welcomeLabel = new Label("Benvenuto, " + controller.getUtenteCorrente().getNome() + "!");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Lista annunci
        annunciListView = new ListView<>();
        annunciListView.setPrefHeight(200);
        annunciListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                try {
                    List<Annuncio> annunciRaw = controller.getAnnunciAttiviRaw();
                    int index = annunciListView.getSelectionModel().getSelectedIndex();
                    selectedAnnuncio = annunciRaw.get(index);
                    loadOfferte();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Errore caricamento annunci");
                }
            }
        });

        refreshAnnunciBtn = new Button("Aggiorna Annunci");
        refreshAnnunciBtn.setOnAction(e -> loadAnnunci());

        // Lista offerte
        offerteListView = new ListView<>();
        offerteListView.setPrefHeight(150);
        offerteListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                try {
                    if (selectedAnnuncio != null) {
                        List<Offerta> offerteRaw = controller.getOfferteByAnnuncio(selectedAnnuncio.getCodiceAnnuncio());
                        int index = offerteListView.getSelectionModel().getSelectedIndex();
                        selectedOfferta = offerteRaw.get(index);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Errore caricamento offerte");
                }
            }
        });

        refreshOfferteBtn = new Button("Aggiorna Offerte");
        refreshOfferteBtn.setOnAction(e -> loadOfferte());

        accettaOffertaBtn = new Button("Accetta Offerta");
        accettaOffertaBtn.setOnAction(e -> {
            if (selectedOfferta != null) {
                if (controller.accettaOfferta(selectedOfferta.getCodiceOfferta())) {
                    loadAnnunci();
                    loadOfferte();
                    showAlert("Offerta accettata!");
                }
            }
        });

        rifiutaOffertaBtn = new Button("Rifiuta Offerta");
        rifiutaOffertaBtn.setOnAction(e -> {
            if (selectedOfferta != null) {
                if (controller.rifiutaOfferta(selectedOfferta.getCodiceOfferta())) {
                    loadOfferte();
                    showAlert("Offerta rifiutata!");
                }
            }
        });

        HBox offerteButtons = new HBox(10, accettaOffertaBtn, rifiutaOffertaBtn, refreshOfferteBtn);
        
        Button reportButton = new Button("Mostra Report");
        reportButton.setOnAction(e -> {
            System.out.println("Pulsante mostra report cliccato");
            try {
                System.out.println("Creando ReportView...");
                new ReportView(controller);
                System.out.println("ReportView creato con successo");
            } catch (Exception ex) {
                System.out.println("ERRORE durante creazione ReportView:");
                ex.printStackTrace();
            }
        });
        
        

        
        // Inserisci il pulsante alla fine dei children nel layout root
        root.getChildren().add(reportButton);

        root.getChildren().addAll(
                welcomeLabel,
                new Label("Annunci attivi:"),
                annunciListView,
                refreshAnnunciBtn,
                new Label("Offerte sull'annuncio selezionato:"),
                offerteListView,
                offerteButtons
        );
        


    }

    private void loadAnnunci() {
        ObservableList<String> annunci = controller.getAnnunciAttiviFormatted();
        annunciListView.setItems(annunci);
    }

    private void loadOfferte() {
        if (selectedAnnuncio == null) {
            offerteListView.setItems(FXCollections.observableArrayList("Seleziona un annuncio."));
            return;
        }
        ObservableList<String> offerte = controller.getStoricoOfferte();
        offerteListView.setItems(offerte);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informazione");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
 // Aggiunta nel createUI di DashboardView.java



    public VBox getRoot() {
        return root;
    }
}

