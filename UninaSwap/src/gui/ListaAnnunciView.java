package gui;

import Controller.Controller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Annuncio;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ListaAnnunciView {
    private VBox root;
    private TableView<Annuncio> tableAnnunci;
    private Controller controller;

    private ComboBox<String> cbCategoria;
    private ComboBox<String> cbTipologia;
    private TextField tfPrezzoMax;
    private TextField txtSearch;

    public ListaAnnunciView(Controller controller) {
        this.controller = controller;
        createUI();
        loadAnnunciConFiltri();
    }

    private void createUI() {
        root = new VBox(12);
        root.setPadding(new Insets(14));

        // ------------ FILTRI --------------
        HBox filtriBox = new HBox(10);
        filtriBox.setAlignment(Pos.CENTER_LEFT);

        cbCategoria = new ComboBox<>();
        cbCategoria.setPromptText("Categoria");

        cbTipologia = new ComboBox<>();
        cbTipologia.getItems().setAll("", "vendita", "scambio", "regalo");
        cbTipologia.setPromptText("Tipologia");

        tfPrezzoMax = new TextField();
        tfPrezzoMax.setPromptText("Prezzo max");

        txtSearch = new TextField();
        txtSearch.setPromptText("Ricerca descrizione...");

        Button btnFiltra = new Button("Filtra");
        btnFiltra.setOnAction(e -> loadAnnunciConFiltri());

        filtriBox.getChildren().addAll(cbCategoria, cbTipologia, tfPrezzoMax, txtSearch, btnFiltra);

        // ------------- TABELLA ANNUNCI -------------
        tableAnnunci = new TableView<>();
        TableColumn<Annuncio, String> colCodice = new TableColumn<>("Cod.");
        colCodice.setCellValueFactory(new PropertyValueFactory<>("codiceAnnuncio"));
        TableColumn<Annuncio, String> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        TableColumn<Annuncio, String> colTipologia = new TableColumn<>("Tipologia");
        colTipologia.setCellValueFactory(new PropertyValueFactory<>("tipologia"));
        TableColumn<Annuncio, String> colDescrizione = new TableColumn<>("Descrizione");
        colDescrizione.setCellValueFactory(new PropertyValueFactory<>("descrizione"));
        TableColumn<Annuncio, Double> colPrezzo = new TableColumn<>("Prezzo");
        colPrezzo.setCellValueFactory(new PropertyValueFactory<>("prezzo"));
        TableColumn<Annuncio, String> colStato = new TableColumn<>("Stato");
        colStato.setCellValueFactory(new PropertyValueFactory<>("stato"));

        tableAnnunci.getColumns().addAll(colCodice, colCategoria, colTipologia, colDescrizione, colPrezzo, colStato);
        tableAnnunci.setPrefHeight(350);

        // --------------- INVIA OFFERTA -------------
        Button btnInviaOfferta = new Button("Invia Offerta");
        btnInviaOfferta.setOnAction(e -> {
            Annuncio selected = tableAnnunci.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Seleziona un annuncio per inviare un'offerta!");
                return;
            }
            if (selected.getMatricola().equals(controller.getUtenteCorrente().getMatricola())) {
                showAlert("Non puoi offrire sui tuoi annunci!");
                return;
            }
            openDialogInvioOfferta(selected);
        });

        root.getChildren().addAll(filtriBox, tableAnnunci, btnInviaOfferta);
    }

    private void loadAnnunciConFiltri() {
        try {
            String categoria = cbCategoria.getValue();
            String tipologia = cbTipologia.getValue();
            String prezzoStr = tfPrezzoMax.getText();
            String search = txtSearch.getText();

            // Carica solo annunci attivi
            List<Annuncio> annunci = controller.getAnnunciAttiviRaw();

            // Filtra per categoria
            if (categoria != null && !categoria.isBlank()) {
                annunci = annunci.stream().filter(a -> a.getCategoria().equalsIgnoreCase(categoria)).collect(Collectors.toList());
            }
            // Filtra per tipologia
            if (tipologia != null && !tipologia.isBlank()) {
                annunci = annunci.stream().filter(a -> a.getTipologia().equalsIgnoreCase(tipologia)).collect(Collectors.toList());
            }
            // Filtra prezzo max (solo per annunci vendita)
            if (prezzoStr != null && !prezzoStr.isBlank()) {
                try {
                    double max = Double.parseDouble(prezzoStr);
                    annunci = annunci.stream()
                        .filter(a -> a.getPrezzo() != null && a.getPrezzo() <= max)
                        .collect(Collectors.toList());
                } catch (NumberFormatException ignored) {}
            }
            // Filtra descrizione (opz)
            if (search != null && !search.isBlank()) {
                String low = search.toLowerCase();
                annunci = annunci.stream()
                        .filter(a -> a.getDescrizione() != null && a.getDescrizione().toLowerCase().contains(low))
                        .collect(Collectors.toList());
            }

            tableAnnunci.getItems().setAll(annunci);

            // Compila combo categoria una tantum
            Set<String> uniche = controller.getAnnunciAttiviRaw().stream()
                    .map(Annuncio::getCategoria)
                    .filter(cat -> cat != null && !cat.isBlank())
                    .collect(Collectors.toSet());
            cbCategoria.getItems().setAll("");
            cbCategoria.getItems().addAll(uniche);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Errore caricamento annunci: " + e.getMessage());
        }
    }

    private void openDialogInvioOfferta(Annuncio annuncio) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Invia offerta - " + annuncio.getDescrizione());

        VBox box = new VBox(12);
        box.setPadding(new Insets(14));

        Label tipoLabel = new Label("Tipologia annuncio: " + annuncio.getTipologia());

        // Queste UI cambiano a seconda della tipologia
        TextField tfPrezzo = new TextField();
        tfPrezzo.setPromptText("Prezzo offerto");
        TextArea taMessaggio = new TextArea();
        taMessaggio.setPromptText("Motiva la tua richiesta...");
        taMessaggio.setPrefRowCount(3);

        Button btnInvia = new Button("Invia offerta");
        btnInvia.setDefaultButton(true);

        switch (annuncio.getTipologia()) {
            case "vendita":
                box.getChildren().addAll(tipoLabel, new Label("Prezzo richiesto: €" + annuncio.getPrezzo()), tfPrezzo, btnInvia);
                btnInvia.setOnAction(e -> {
                    if (tfPrezzo.getText().isBlank()) {
                        showAlert("Inserisci il prezzo offerto.");
                        return;
                    }
                    try {
                        double prezzo = Double.parseDouble(tfPrezzo.getText());
                        controller.inviaOfferta(annuncio.getCodiceAnnuncio(), "vendita", prezzo);
                        dialog.close();
                        showAlert("Offerta inviata!");
                    } catch (Exception ex) {
                        showAlert("Errore invio offerta: " + ex.getMessage());
                    }
                });
                break;
            case "scambio":
                // In questa demo puoi solo proporre, estendibile con lista oggetti
                box.getChildren().addAll(tipoLabel, new Label("Proponi i tuoi oggetti in seguito (funzionalità avanzata)"), btnInvia);
                btnInvia.setOnAction(e -> {
                    // Da espandere con scelta oggetti
                    showAlert("Funzionalità scambio oggetti da completare!");
                    dialog.close();
                });
                break;
            case "regalo":
                box.getChildren().addAll(tipoLabel, new Label("Scrivi un messaggio motivazionale:"), taMessaggio, btnInvia);
                btnInvia.setOnAction(e -> {
                    if (taMessaggio.getText().isBlank()) {
                        showAlert("Inserisci un messaggio motivazionale.");
                        return;
                    }
                    try {
                        controller.inviaOfferta(annuncio.getCodiceAnnuncio(), "regalo", null);
                        dialog.close();
                        showAlert("Richiesta inviata!");
                    } catch (Exception ex) {
                        showAlert("Errore invio richiesta: " + ex.getMessage());
                    }
                });
                break;
        }

        dialog.setScene(new Scene(box, 350, 180));
        dialog.showAndWait();
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}
