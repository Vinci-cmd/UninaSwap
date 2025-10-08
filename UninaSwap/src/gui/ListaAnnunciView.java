package gui;

import Controller.Controller;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.*;

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
    private Label emptyLabel;

    public ListaAnnunciView(Controller controller) {
        this.controller = controller;
        createUI();
        loadAnnunciConFiltri();
    }

private void createUI() {
    root = new VBox();
    root.setPadding(new Insets(24));
    root.setSpacing(12);
    root.setFillWidth(true);
    root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);" +
        "-fx-font-family: 'Segoe UI', 'Roboto', 'Arial';");

    Label title = new Label("Annunci disponibili");
    title.setStyle("-fx-text-fill: #fff; -fx-font-size: 23px; -fx-font-weight: 900;");
    Label subtitle = new Label("Cerca opportunità di scambio o acquisto tra studenti.");
    subtitle.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 13px; -fx-font-weight: 600;");
    VBox header = new VBox(title, subtitle);

    HBox filtriBox = new HBox(10);
    filtriBox.setAlignment(Pos.CENTER_LEFT);

    cbTipologia = styledComboScuro();
    cbTipologia.getItems().addAll("Tutte le tipologie", "vendita", "scambio", "regalo");
    cbTipologia.setValue("Tutte le tipologie");
    styleComboTextWhite(cbTipologia);
    cbTipologia.setOnAction(e -> loadAnnunciConFiltri());

    cbCategoria = styledComboScuro();
    cbCategoria.setPromptText("Tutte le categorie");
    cbCategoria.setValue(null);
    styleComboTextWhite(cbCategoria);
    try {
        Set<String> uniche = controller.getAnnunciAttiviRaw().stream()
            .map(Annuncio::getCategoria)
            .filter(cat -> cat != null && !cat.isBlank())
            .collect(Collectors.toSet());
        cbCategoria.getItems().addAll(uniche);
    } catch (Exception ignored) {}
    cbCategoria.setOnAction(e -> loadAnnunciConFiltri());

    tfPrezzoMax = styledFieldScuro("Prezzo max");
    tfPrezzoMax.setOnKeyReleased(e -> loadAnnunciConFiltri());
    txtSearch = styledFieldScuro("Ricerca testo/codice");
    txtSearch.setOnKeyReleased(e -> loadAnnunciConFiltri());

    filtriBox.getChildren().addAll(cbTipologia, cbCategoria, tfPrezzoMax, txtSearch);

    VBox card = new VBox(18);
    card.setAlignment(Pos.TOP_LEFT);
    card.setPadding(new Insets(22));
    card.setMaxWidth(675); // Larghezza contenitore!
    card.setStyle("-fx-background-color: #181b23;"
        + "-fx-background-radius: 18; -fx-border-radius: 18;"
        + "-fx-border-color: #212849;"
        + "-fx-border-width: 1;");
    card.setEffect(new DropShadow(16, Color.color(0,0,0,0.45)));
    card.getChildren().addAll(header, filtriBox);

    tableAnnunci = new TableView<>();
    tableAnnunci.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

    TableColumn<Annuncio, String> colCodice = new TableColumn<>("Codice");
    colCodice.setCellValueFactory(new PropertyValueFactory<>("codiceAnnuncio"));
    colCodice.setPrefWidth(85);

    TableColumn<Annuncio, String> colCategoria = new TableColumn<>("Categoria");
    colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
    colCategoria.setPrefWidth(95);

    TableColumn<Annuncio, String> colTipologia = new TableColumn<>("Tipologia");
    colTipologia.setCellValueFactory(new PropertyValueFactory<>("tipologia"));
    colTipologia.setPrefWidth(90);

    TableColumn<Annuncio, String> colDescrizione = new TableColumn<>("Descrizione");
    colDescrizione.setCellValueFactory(new PropertyValueFactory<>("descrizione"));
    colDescrizione.setPrefWidth(210);
    colDescrizione.setCellFactory(tc -> {
        TableCell<Annuncio, String> cell = new TableCell<>() {
            final Label lbl = new Label();
            { lbl.setWrapText(true); lbl.setStyle("-fx-text-fill: #fff; -fx-font-size: 15px;"); setGraphic(lbl); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                lbl.setText(empty || item == null ? "" : item);
                setGraphic(empty ? null : lbl);
            }
        };
        cell.setPrefHeight(Region.USE_COMPUTED_SIZE);
        return cell;
    });

    TableColumn<Annuncio, Double> colPrezzo = new TableColumn<>("Prezzo");
    colPrezzo.setCellValueFactory(new PropertyValueFactory<>("prezzo"));
    colPrezzo.setPrefWidth(70);
    colPrezzo.setCellFactory(tc -> new TableCell<>() {
        @Override protected void updateItem(Double value, boolean empty) {
            super.updateItem(value, empty);
            setText(empty || value == null ? "" : String.format("€ %.2f", value));
            setStyle("-fx-text-fill: #7af7c3; -fx-font-weight:900; -fx-alignment:CENTER_RIGHT; -fx-padding:0 7 0 0;");
        }
    });

    TableColumn<Annuncio, String> colStato = new TableColumn<>("Stato");
    colStato.setCellValueFactory(new PropertyValueFactory<>("stato"));
    colStato.setPrefWidth(110);
    colStato.setCellFactory(tc -> new TableCell<>() {
        @Override protected void updateItem(String stato, boolean empty) {
            super.updateItem(stato, empty);
            if (empty || stato == null) { setGraphic(null); setText(null); }
            else {
                Label badge = new Label(stato.toUpperCase());
                badge.setStyle(
                    "-fx-background-radius: 20; -fx-padding: 6 22;" +
                    "-fx-font-weight:900; -fx-font-size: 14px; -fx-text-fill: #181b23;" +
                    "-fx-background-color:" + (
                        "attivo".equalsIgnoreCase(stato) ? "#21e070" :
                        "scaduto".equalsIgnoreCase(stato) ? "#ff4f54" : "#3177eb"
                    ) + ";");
                setGraphic(badge); setAlignment(Pos.CENTER);
            }
        }
    });

    tableAnnunci.getColumns().setAll(
        colCodice, colCategoria, colTipologia, colDescrizione, colPrezzo, colStato);
    tableAnnunci.setPrefHeight(280);
    tableAnnunci.setPrefWidth(660); // UGUALE alla sommatoria colonne o 1-2 px in più!
    tableAnnunci.setStyle("-fx-background-color: transparent;" +
        "-fx-control-inner-background: #181b23;" +
        "-fx-table-header-background: #101218;");
    tableAnnunci.skinProperty().addListener((obs, oldV, newV) -> {
        if (newV != null)
            for (TableColumn<?, ?> col : tableAnnunci.getColumns())
                col.setStyle("-fx-background-color: #101218; -fx-text-fill: #ffffff; -fx-font-weight:900; -fx-font-size:15px;" +
                        "-fx-border-width:0 0 2 0; -fx-border-color:#27304a;");
    });

    tableAnnunci.setRowFactory(tv -> {
        TableRow<Annuncio> row = new TableRow<>();
        row.setOnMouseClicked(ev -> {
            if (!row.isEmpty() && ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2)
                mostraDettaglioAnnuncio(row.getItem());
        });
        row.indexProperty().addListener((obs, old, idx) -> { row.setStyle(zebraStyle(idx.intValue(), row.isSelected())); });
        row.selectedProperty().addListener((o, w, sel) -> {
            row.setStyle(sel ?
                "-fx-background-color: #4f8cff; -fx-border-color: #99b0f7; -fx-border-radius:10; -fx-background-radius:10; -fx-effect:dropshadow(two-pass-box,#0b1020,12,0.5,0,0);" :
                zebraStyle(row.getIndex(), false));
        });
        row.hoverProperty().addListener((o, w, is) -> {
            if (!row.isEmpty() && !row.isSelected())
                row.setStyle(is ? "-fx-background-color: rgba(122,247,195,0.11); -fx-border-radius:10;" : zebraStyle(row.getIndex(), false));
        });
        return row;
    });

    emptyLabel = new Label("Nessun annuncio corrisponde ai filtri.");
    emptyLabel.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 13px; -fx-padding:18 0 0 0;");
    emptyLabel.setVisible(false);

    Button btnInviaOfferta = styledPrimaryButton("Invia Offerta");
    btnInviaOfferta.setOnAction(e -> {
        Annuncio selected = tableAnnunci.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Seleziona un annuncio per inviare un'offerta!"); return; }
        if (selected.getMatricola().equals(controller.getUtenteCorrente().getMatricola())) {
            showAlert("Non puoi offrire sui tuoi annunci!"); return;
        }
        openDialogInvioOfferta(selected);
    });

    card.getChildren().addAll(tableAnnunci, emptyLabel, btnInviaOfferta);

    StackPane centerWrap = new StackPane(card);
    StackPane.setAlignment(card, Pos.CENTER);
    centerWrap.setPadding(new Insets(20));
    root.getChildren().add(centerWrap);
    VBox.setVgrow(centerWrap, Priority.ALWAYS);
}




    private ComboBox<String> styledComboScuro() {
        ComboBox<String> cb = new ComboBox<>();
        cb.setStyle("-fx-background-color: #181b23; -fx-text-fill:#fff;" +
            "-fx-background-radius:10; -fx-font-size:15px; -fx-font-weight:900;" +
            "-fx-prompt-text-fill: #c4e3ff; -fx-border-color: transparent;");
        return cb;
    }
    private void styleComboTextWhite(ComboBox<String> cb) {
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty||item==null ? (cb.getPromptText()!=null? cb.getPromptText() : "") : item);
                setStyle("-fx-text-fill: #ffffff; -fx-background-color: #181b23; -fx-font-weight:900;");
            }
        });
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty||item==null ? null : item);
                setStyle(empty ? "" : "-fx-text-fill: #ffffff; -fx-background-color: #181b23;");
            }
        });
    }
    private TextField styledFieldScuro(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#181b23;-fx-text-fill:#f8faff;-fx-background-radius:10;-fx-font-size:15px;"+
                "-fx-padding:8 12;-fx-prompt-text-fill:#b5bccc;-fx-border-color:transparent;");
        return tf;
    }
    private Button styledPrimaryButton(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color:#4f8cff;-fx-text-fill:white;-fx-background-radius:10;-fx-padding:10 28;"+
                "-fx-font-weight:900;-fx-font-size:15px;");
        b.setOnMouseEntered(e->b.setStyle("-fx-background-color:#3b6fe0;-fx-text-fill:white;-fx-background-radius:10;-fx-padding:10 28;-fx-font-weight:900;-fx-font-size:15px;"));
        b.setOnMouseExited(e->b.setStyle("-fx-background-color:#4f8cff;-fx-text-fill:white;-fx-background-radius:10;-fx-padding:10 28;-fx-font-weight:900;-fx-font-size:15px;"));
        return b;
    }
    private String zebraStyle(int idx, boolean selected) {
        if(selected) return "-fx-background-color: #4f8cff; -fx-effect:dropshadow(two-pass-box,#0b1020,8,0.25,0,0);";
        return idx%2==0?
                "-fx-background-color: rgba(255,255,255,0.03);":
                "-fx-background-color: rgba(122,247,195,0.09);";
    }

    private void loadAnnunciConFiltri() {
        try {
            String categoria = cbCategoria.getValue();
            String tipologia = cbTipologia.getValue();
            String prezzoStr = tfPrezzoMax.getText();
            String search = txtSearch.getText();

            String matricolaUtente = controller.getUtenteCorrente().getMatricola();
            List<Annuncio> annunci = controller.getAnnunciAttiviRaw().stream()
                    .filter(a -> !a.getMatricola().equals(matricolaUtente))
                    .collect(Collectors.toList());

            if (tipologia != null && !"Tutte le tipologie".equals(tipologia)) {
                annunci = annunci.stream().filter(a -> a.getTipologia().equalsIgnoreCase(tipologia)).collect(Collectors.toList());
            }
            if (categoria != null && !categoria.isBlank()) {
                annunci = annunci.stream().filter(a -> a.getCategoria().equalsIgnoreCase(categoria)).collect(Collectors.toList());
            }
            if (prezzoStr != null && !prezzoStr.isBlank()) {
                try {
                    double max = Double.parseDouble(prezzoStr);
                    annunci = annunci.stream()
                        .filter(a -> a.getPrezzo() != null && a.getPrezzo() <= max)
                        .collect(Collectors.toList());
                } catch (NumberFormatException ignored) {}
            }
            if (search != null && !search.isBlank()) {
                String low = search.toLowerCase();
                annunci = annunci.stream()
                        .filter(a ->
                            (a.getDescrizione() != null && a.getDescrizione().toLowerCase().contains(low)) ||
                            (a.getCodiceAnnuncio() != null && a.getCodiceAnnuncio().toLowerCase().contains(low)))
                        .collect(Collectors.toList());
            }

            tableAnnunci.getItems().setAll(annunci);
            emptyLabel.setVisible(annunci.isEmpty());

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Errore caricamento annunci: " + e.getMessage());
        }
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

        VBox box = new VBox(16);
        box.setPadding(new Insets(28));
        box.setPrefWidth(430);
        box.setStyle("-fx-background-color: #181b23; -fx-background-radius: 16;");

        Label l1 = new Label("Codice: " + annuncio.getCodiceAnnuncio());
        l1.setStyle("-fx-text-fill:#c4e3ff;-fx-font-weight:800; -fx-font-size:17px;");
        Label l2 = new Label("Categoria: " + annuncio.getCategoria());
        l2.setStyle("-fx-text-fill:#eaf0ff;-fx-font-size:16px;");
        Label l3 = new Label("Tipologia: " + annuncio.getTipologia());
        l3.setStyle("-fx-text-fill:#eaf0ff;-fx-font-size:16px;");
        HBox l4Box = new HBox(new Label("Stato: ") {{ setStyle("-fx-text-fill:#b5bacd; -fx-font-size:16px;"); }}, statoBadge(annuncio.getStato()));
        l4Box.setSpacing(10);
        l4Box.setAlignment(Pos.CENTER_LEFT);
        Label l5 = new Label("Prezzo: " + (annuncio.getPrezzo() != null ? ("€ " + annuncio.getPrezzo()) : "N/A"));
        l5.setStyle("-fx-text-fill:#A8B1C6;-fx-font-size:16px;");
        Label l6 = new Label("Proprietario: " + proprietarioStr);
        l6.setStyle("-fx-text-fill:#b79cff;-fx-font-size:16px;");
        Label l7 = new Label("Data pubblicazione: " + (annuncio.getDataPubblicazione() != null ? annuncio.getDataPubblicazione().toString() : ""));
        l7.setStyle("-fx-text-fill:#A8B1C6;-fx-font-size:15px;");
        Label l8 = new Label("Descrizione:");
        l8.setStyle("-fx-text-fill:#bbbbd8;-fx-font-size:16px;");

        TextArea ta = new TextArea(annuncio.getDescrizione());
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(4);
        ta.setStyle(
        	    "-fx-control-inner-background: #191e2d;" +
        	    "-fx-background-color: #191e2d;" +
        	    "-fx-text-fill: #fff;" +
        	    "-fx-font-size:16px;" +
        	    "-fx-font-weight:900;" +
        	    "-fx-border-radius: 10;" +
        	    "-fx-background-radius: 10;" +
        	    "-fx-padding:10 13;" +
        	    "-fx-prompt-text-fill: #b5bccc;");
        
        box.getChildren().addAll(l1, l2, l3, l4Box, l5, l6, l7, l8, ta);
        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
    }

    private Label statoBadge(String stato) {
        Label badge = new Label(stato.toUpperCase());
        badge.setStyle("-fx-background-radius: 17; -fx-padding: 6 21;"+
                "-fx-font-weight: 900; -fx-font-size: 13.7px;-fx-text-fill:#181b23;-fx-effect:dropshadow(three-pass-box,#191e2d,3,0.5,0,0);"+
                "-fx-background-color:" + (
                "attivo".equalsIgnoreCase(stato) ? "#2be070" :
                "scaduto".equalsIgnoreCase(stato) ? "#ee6878" : "#4290ed"
        ) + ";");
        return badge;
    }

    private void openDialogInvioOfferta(Annuncio annuncio) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Invia offerta - " + annuncio.getDescrizione());

        VBox box = new VBox(22);
        box.setPadding(new Insets(28));
        box.setMaxWidth(440);
        box.setStyle("-fx-background-color: #181b23; -fx-background-radius: 16;");

        Label tipoLabel = new Label("Tipologia annuncio: " + annuncio.getTipologia());
        tipoLabel.setStyle("-fx-text-fill: #c4e3ff; -fx-font-size: 17px;-fx-font-weight:800;");

        TextField tfPrezzo = styledFieldScuro("Prezzo offerto");
        TextArea taMessaggio = new TextArea();
        taMessaggio.setPromptText("Motiva la tua richiesta...");
        taMessaggio.setPrefRowCount(3);
        taMessaggio.setStyle(
        	    "-fx-control-inner-background: #191e2d;" +
        	    "-fx-background-color: #191e2d;" +
        	    "-fx-text-fill: #fff;" +
        	    "-fx-font-size:16px;" +
        	    "-fx-font-weight:900;" +
        	    "-fx-border-radius: 10;" +
        	    "-fx-background-radius: 10;" +
        	    "-fx-padding:10 13;" +
        	    "-fx-prompt-text-fill: #b5bccc;");

        Button btnInvia = styledPrimaryButton("Invia offerta");
        btnInvia.setDefaultButton(true);

        switch (annuncio.getTipologia()) {
            case "vendita":
                box.getChildren().addAll(
                    tipoLabel,
                    new Label("Prezzo richiesto: €" + annuncio.getPrezzo()) {{
                        setStyle("-fx-text-fill:#7af7c3; -fx-font-size:16px;-fx-font-weight:800;");
                    }},
                    tfPrezzo,
                    btnInvia
                );
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
                String matricolaUtente = controller.getUtenteCorrente().getMatricola();
                List<Oggetto> oggettiPersonali;
                try {
                    oggettiPersonali = controller.getOggettiUtenteObj(matricolaUtente)
                            .stream()
                            .filter(o -> o.getCodiceAnnuncio() == null)
                            .collect(Collectors.toList());
                } catch (Exception ex) {
                    showAlert("Errore caricamento oggetti: " + ex.getMessage());
                    return;
                }
                Label lblSelect = new Label("Scegli i tuoi oggetti da proporre nello scambio:");
                lblSelect.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 15px; -fx-font-weight: bold;");
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
                        setStyle("-fx-background-color: #11141f; -fx-text-fill: #f0f6fc;");
                    }
                });
                listOggetti.setStyle("-fx-background-color: #171c28; -fx-control-inner-background: #171c28; -fx-border-radius:11;-fx-background-radius:11;");

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
                        for (String codiceOggetto : codiciOggetti) {
                            controller.aggiornaCodiceAnnuncioOggetto(codiceOggetto, annuncio.getCodiceAnnuncio());
                        }
                        dialog.close();
                        showAlert("Offerta di scambio inviata!");
                    } catch (Exception ex) {
                        showAlert("Errore invio offerta: " + ex.getMessage());
                    }
                });
                break;
            case "regalo":
                box.getChildren().addAll(tipoLabel, new Label("Scrivi un messaggio motivazionale:") {{
                    setStyle("-fx-text-fill:#f4f4fa;-fx-font-size:16px;");
                }}, taMessaggio, btnInvia);
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
        dialog.setScene(new Scene(box, 470, 430));
        dialog.showAndWait();
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}
