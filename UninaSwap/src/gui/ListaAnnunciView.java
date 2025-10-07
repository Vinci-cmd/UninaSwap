package gui;

import Controller.Controller;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        cbCategoria.getItems().add("Categoria");
        cbCategoria.setValue("Categoria");
        // Popola una sola volta all’avvio
        try {
            Set<String> uniche = controller.getAnnunciAttiviRaw().stream()
                    .map(Annuncio::getCategoria)
                    .filter(cat -> cat != null && !cat.isBlank())
                    .collect(Collectors.toSet());
            cbCategoria.getItems().addAll(uniche);
        } catch (Exception ignored) {}
        cbCategoria.setOnAction(e -> loadAnnunciConFiltri());

        cbTipologia = new ComboBox<>();
        cbTipologia.getItems().add("Tipologia");
        cbTipologia.getItems().addAll("vendita", "scambio", "regalo");
        cbTipologia.setValue("Tipologia");
        cbTipologia.setOnAction(e -> loadAnnunciConFiltri());

        tfPrezzoMax = new TextField();
        tfPrezzoMax.setPromptText("Prezzo max");
        tfPrezzoMax.setOnKeyReleased(e -> loadAnnunciConFiltri());

        txtSearch = new TextField();
        txtSearch.setPromptText("Ricerca descrizione...");
        txtSearch.setOnKeyReleased(e -> loadAnnunciConFiltri());

        filtriBox.getChildren().addAll(cbCategoria, cbTipologia, tfPrezzoMax, txtSearch);

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

        // Doppio click riga --> mostra dettaglio annuncio
        tableAnnunci.setRowFactory(tv -> {
            TableRow<Annuncio> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Annuncio annuncio = row.getItem();
                    mostraDettaglioAnnuncio(annuncio);
                }
            });
            return row;
        });

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

            // Carica solo annunci attivi DI ALTRI (NON i tuoi)
            String matricolaUtente = controller.getUtenteCorrente().getMatricola();
            List<Annuncio> annunci = controller.getAnnunciAttiviRaw().stream()
                    .filter(a -> !a.getMatricola().equals(matricolaUtente))
                    .collect(Collectors.toList());

            // Filtra per categoria se diverso da "Categoria"
            if (categoria != null && !"Categoria".equals(categoria)) {
                annunci = annunci.stream().filter(a -> a.getCategoria().equalsIgnoreCase(categoria)).collect(Collectors.toList());
            }
            // Filtra per tipologia se diverso da "Tipologia"
            if (tipologia != null && !"Tipologia".equals(tipologia)) {
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
                // --- GESTIONE SELEZIONE OGGETTI PERSONALI PER SCAMBIO ---
                String matricolaUtente = controller.getUtenteCorrente().getMatricola();
                List<Oggetto> oggettiPersonali;
                try {
                    oggettiPersonali = controller.getOggettiUtenteObj(matricolaUtente)
                            .stream()
                            .filter(o -> o.getCodiceAnnuncio() == null) // Solo oggetti NON associati
                            .collect(Collectors.toList());
                } catch (Exception ex) {
                    showAlert("Errore caricamento oggetti: " + ex.getMessage());
                    return;
                }
                Label lblSelect = new Label("Scegli i tuoi oggetti da proporre nello scambio:");
                ListView<Oggetto> listOggetti = new ListView<>();
                listOggetti.getItems().addAll(oggettiPersonali);
                listOggetti.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                listOggetti.setCellFactory(lv -> new ListCell<Oggetto>() {
                    @Override
                    protected void updateItem(Oggetto oggetto, boolean empty) {
                        super.updateItem(oggetto, empty);
                        setText(empty || oggetto == null ?
                                null :
                                oggetto.getNome() + " - " + oggetto.getCategoria() + " (" + oggetto.getDescrizione() + ")");
                    }
                });
                box.getChildren().addAll(tipoLabel, lblSelect, listOggetti, btnInvia);

                btnInvia.setOnAction(e -> {
                    List<Oggetto> selezionati = listOggetti.getSelectionModel().getSelectedItems();
                    if (selezionati == null || selezionati.isEmpty()) {
                        showAlert("Seleziona almeno un oggetto da proporre per lo scambio!");
                        return;
                    }
                    List<String> codiciOggetti = selezionati.stream()
                            .map(Oggetto::getCodiceOggetto)
                            .collect(Collectors.toList());
                    try {
                        controller.inviaOffertaConOggetti(annuncio.getCodiceAnnuncio(), codiciOggetti);
                        dialog.close();
                        showAlert("Offerta di scambio inviata!");
                    } catch (Exception ex) {
                        showAlert("Errore invio offerta: " + ex.getMessage());
                    }
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
                        controller.inviaOfferta(annuncio.getCodiceAnnuncio(), "regalo", null, taMessaggio.getText());
                        dialog.close();
                        showAlert("Richiesta inviata!");
                    } catch (Exception ex) {
                        showAlert("Errore invio richiesta: " + ex.getMessage());
                    }
                });
                break;
        }

        dialog.setScene(new Scene(box, 400, 350));
        dialog.showAndWait();
    }

    private void mostraDettaglioAnnuncio(Annuncio annuncio) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Dettaglio Annuncio");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        String proprietarioStr = annuncio.getMatricola();
        try {
            Utente ut = controller.getUtenteByMatricola(annuncio.getMatricola());
            if (ut != null) {
                proprietarioStr = ut.getNome() + " " + ut.getCognome();
            }
        } catch (Exception ignored) {}

        VBox box = new VBox(14);
        box.setPadding(new Insets(14));
        box.setPrefWidth(320);

        box.getChildren().addAll(
            new Label("Codice: " + annuncio.getCodiceAnnuncio()),
            new Label("Categoria: " + annuncio.getCategoria()),
            new Label("Tipologia: " + annuncio.getTipologia()),
            new Label("Stato: " + annuncio.getStato()),
            new Label("Prezzo: " + (annuncio.getPrezzo() != null ? ("€ " + annuncio.getPrezzo()) : "N/A")),
            new Label("Proprietario: " + proprietarioStr),
            new Label("Data pubblicazione: " + (annuncio.getDataPubblicazione() != null ? annuncio.getDataPubblicazione().toString() : "")),
            new Label("Descrizione:"),
            new TextArea(annuncio.getDescrizione()) {{
                setEditable(false);
                setWrapText(true);
                setPrefRowCount(4);
                setStyle("-fx-opacity: 1; -fx-background-color: #fafaff;");
            }}
        );
        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}
