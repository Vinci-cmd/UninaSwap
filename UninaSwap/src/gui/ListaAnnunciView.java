package gui;

import Controller.Controller;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ListaAnnunciView {

    private VBox root;
    private final Controller controller;

    private final ObservableList<Annuncio> masterData = FXCollections.observableArrayList();
    private FilteredList<Annuncio> filtered;
    private SortedList<Annuncio> sorted;

    private TableView<Annuncio> tableAnnunci;
    private ComboBox<String> cbCategoria;
    private ComboBox<String> cbTipologia;
    private TextField tfPrezzoMax;
    private TextField txtSearch;
    private Label emptyLabel;

    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(200));
    private List<String> categorie;

    public ListaAnnunciView(Controller controller) {
        this.controller = controller;
        createUI();
        reloadData();
    }

    private void createUI() {
        root = new VBox(16);
        root.setPadding(new Insets(16));
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);" +
            "-fx-font-family: 'Segoe UI','Roboto','Arial';"
        );

        Label title = new Label("Annunci disponibili");
        title.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 20px; -fx-font-weight: 900;");
        Label subtitle = new Label("Cerca opportunità di scambio o acquisto tra studenti.");
        subtitle.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 13px; -fx-font-weight: 600;");
        VBox header = new VBox(title, subtitle);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox filtersCard = card();
        filtersCard.setSpacing(10);
        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);

        cbTipologia = new ComboBox<>();
        cbTipologia.getItems().addAll("Tutte le tipologie", "vendita", "scambio", "regalo");
        cbTipologia.setValue("Tutte le tipologie");
        styleCombo(cbTipologia);
        cbTipologia.setOnAction(e -> applyFilters());

        cbCategoria = new ComboBox<>();
        cbCategoria.setPromptText("Tutte le categorie");
        cbCategoria.setValue(null);
        styleCombo(cbCategoria);
        cbCategoria.setOnAction(e -> applyFilters());

        searchDebounce.setOnFinished(ev -> applyFilters());

        tfPrezzoMax = styledTextField("Prezzo max");
        tfPrezzoMax.textProperty().addListener((obs, o, n) -> {
            searchDebounce.stop();
            searchDebounce.playFromStart();
        });

        txtSearch = styledTextField("Cerca per testo o codice…");
        txtSearch.textProperty().addListener((obs, o, n) -> {
            searchDebounce.stop();
            searchDebounce.playFromStart();
        });

        Button btnClear = ghostButton("Pulisci", () -> {
            txtSearch.clear();
            tfPrezzoMax.clear();
            cbTipologia.setValue("Tutte le tipologie");
            cbCategoria.setValue(null);
            applyFilters();
        });

        filters.getChildren().addAll(cbTipologia, cbCategoria, tfPrezzoMax, txtSearch, btnClear);
        filtersCard.getChildren().add(filters);

        VBox tableCard = card();
        tableCard.setSpacing(10);

        tableAnnunci = new TableView<>();
        styleTable(tableAnnunci);

        TableColumn<Annuncio, String> colCodice = new TableColumn<>("Codice");
        colCodice.setCellValueFactory(new PropertyValueFactory<>("codiceAnnuncio"));
        colCodice.setPrefWidth(120);

        TableColumn<Annuncio, String> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCategoria.setPrefWidth(160);

        TableColumn<Annuncio, String> colTipologia = new TableColumn<>("Tipologia");
        colTipologia.setCellValueFactory(new PropertyValueFactory<>("tipologia"));
        colTipologia.setPrefWidth(120);

        TableColumn<Annuncio, String> colDescrizione = new TableColumn<>("Descrizione");
        colDescrizione.setCellValueFactory(new PropertyValueFactory<>("descrizione"));
        colDescrizione.setPrefWidth(360);
        colDescrizione.setCellFactory(tc -> {
            Label lbl = new Label();
            lbl.setWrapText(true);
            lbl.setStyle("-fx-text-fill: #EAF0FF;");
            TableCell<Annuncio, String> cell = new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty || item == null ? null : lbl);
                    if (!empty && item != null) lbl.setText(item);
                }
            };
            cell.setPrefHeight(Region.USE_COMPUTED_SIZE);
            return cell;
        });

        TableColumn<Annuncio, Double> colPrezzo = new TableColumn<>("Prezzo");
        colPrezzo.setCellValueFactory(new PropertyValueFactory<>("prezzo"));
        colPrezzo.setPrefWidth(120);
        colPrezzo.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "" : String.format(Locale.ITALY, "€ %.2f", value));
                setStyle("-fx-text-fill: #7af7c3; -fx-font-weight:900; -fx-alignment:CENTER_RIGHT; -fx-padding:0 7 0 0;");
            }
        });

        TableColumn<Annuncio, String> colStato = new TableColumn<>("Stato");
        colStato.setCellValueFactory(new PropertyValueFactory<>("stato"));
        colStato.setPrefWidth(120);
        colStato.setCellFactory(tc -> badgeCell());

        tableAnnunci.getColumns().setAll(colCodice, colCategoria, colTipologia, colDescrizione, colPrezzo, colStato);
        tableAnnunci.setPrefHeight(440);

        tableAnnunci.setRowFactory(tv -> {
            TableRow<Annuncio> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
                    mostraDettaglioAnnuncio(row.getItem());
                }
            });
            row.indexProperty().addListener((obs, old, idx) -> {
                if (!row.isSelected()) row.setStyle(zebraStyle(idx.intValue()));
            });
            row.selectedProperty().addListener((o, w, is) -> row.setStyle(is ?
                "-fx-background-color: #4f8cff; -fx-border-color: #99b0f7; -fx-border-radius:10; -fx-background-radius:10; -fx-effect:dropshadow(two-pass-box,#0b1020,12,0.5,0,0);" :
                zebraStyle(row.getIndex())));
            row.hoverProperty().addListener((o, w, is) -> {
                if (!row.isEmpty() && !row.isSelected()) {
                    row.setStyle(is ? "-fx-background-color: rgba(122,247,195,0.11); -fx-border-radius:10;" : zebraStyle(row.getIndex()));
                }
            });
            return row;
        });

        MenuItem miDetail = new MenuItem("Dettaglio");
        miDetail.setOnAction(e -> {
            Annuncio a = tableAnnunci.getSelectionModel().getSelectedItem();
            if (a != null) mostraDettaglioAnnuncio(a);
        });
        MenuItem miOffer = new MenuItem("Invia Offerta");
        miOffer.setOnAction(e -> {
            Annuncio a = tableAnnunci.getSelectionModel().getSelectedItem();
            if (a != null) openDialogInvioOfferta(a);
        });
        tableAnnunci.setContextMenu(new ContextMenu(miDetail, miOffer));

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button bDetail = ghostButton("Dettaglio", () -> {
            Annuncio s = tableAnnunci.getSelectionModel().getSelectedItem();
            if (s == null) { warn("Seleziona un annuncio"); return; }
            mostraDettaglioAnnuncio(s);
        });
        Button bOffer = primaryButton("Invia Offerta", () -> {
            Annuncio s = tableAnnunci.getSelectionModel().getSelectedItem();
            if (s == null) { warn("Seleziona un annuncio per inviare un'offerta!"); return; }
            if (s.getMatricola().equals(controller.getUtenteCorrente().getMatricola())) {
                warn("Non puoi offrire sui tuoi annunci!"); return;
            }
            openDialogInvioOfferta(s);
        });
        actions.getChildren().addAll(bDetail, bOffer);

        emptyLabel = new Label("Nessun annuncio corrisponde ai filtri.");
        emptyLabel.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 12px;");
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        tableCard.getChildren().addAll(tableAnnunci, emptyLabel, actions);
        root.getChildren().addAll(header, filtersCard, tableCard);
    }

    private void reloadData() {
        try {
            String matricolaUtente = controller.getUtenteCorrente().getMatricola();
            masterData.setAll(controller.getAnnunciAttiviRaw().stream()
                .filter(a -> "attivo".equalsIgnoreCase(a.getStato()) && 
                             !a.getMatricola().equals(matricolaUtente))
                .toList());

            categorie = masterData.stream()
                .map(Annuncio::getCategoria)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .toList();
            
            cbCategoria.getItems().setAll(categorie);
            cbCategoria.setPromptText("Tutte le categorie");
            cbCategoria.setValue(null);

            if (filtered == null) {
                filtered = new FilteredList<>(masterData, a -> true);
                sorted = new SortedList<>(filtered);
                sorted.comparatorProperty().bind(tableAnnunci.comparatorProperty());
                tableAnnunci.setItems(sorted);
            }

            applyFilters();
        } catch (SQLException e) {
            warn("Errore nel caricamento annunci: " + e.getMessage());
        }
    }

    private void applyFilters() {
        final String tip = cbTipologia.getValue();
        final String cat = cbCategoria.getValue();
        final String prezzoStr = Optional.ofNullable(tfPrezzoMax.getText()).orElse("").trim();
        final String query = Optional.ofNullable(txtSearch.getText()).orElse("").trim().toLowerCase();

        filtered.setPredicate(a -> {
            if (a == null) return false;

            if (tip != null && !"Tutte le tipologie".equals(tip)) {
                if (!tip.equalsIgnoreCase(a.getTipologia())) return false;
            }
            if (cat != null && !cat.isBlank()) {
                if (!cat.equalsIgnoreCase(a.getCategoria())) return false;
            }
            if (!prezzoStr.isBlank()) {
                try {
                    double maxPrice = Double.parseDouble(prezzoStr.replace(",", "."));
                    if (a.getPrezzo() == null || a.getPrezzo() > maxPrice) return false;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
            if (!query.isBlank()) {
                return (a.getCategoria() != null && a.getCategoria().toLowerCase().contains(query)) ||
                    (a.getDescrizione() != null && a.getDescrizione().toLowerCase().contains(query)) ||
                    (a.getCodiceAnnuncio() != null && a.getCodiceAnnuncio().toLowerCase().contains(query));
            }
            return true;
        });

        emptyLabel.setVisible(filtered.isEmpty());
        emptyLabel.setManaged(filtered.isEmpty());
    }

    private void mostraDettaglioAnnuncio(Annuncio annuncio) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Dettaglio Annuncio");

        VBox card = card();
        card.setSpacing(12);

        String proprietarioStr = annuncio.getMatricola();
        try {
            Utente ut = controller.getUtenteByMatricola(annuncio.getMatricola());
            if (ut != null) {
                proprietarioStr = ut.getNome() + " " + ut.getCognome();
            }
        } catch (Exception ignored) {}

        Label l1 = l("Codice: " + annuncio.getCodiceAnnuncio());
        Label l2 = l("Categoria: " + annuncio.getCategoria());
        Label l3 = l("Tipologia: " + annuncio.getTipologia());
        
        HBox l4Box = new HBox(l("Stato: "), statoBadge(annuncio.getStato()));
        l4Box.setSpacing(10);
        l4Box.setAlignment(Pos.CENTER_LEFT);
        
        Label l5 = l("Prezzo: " + (annuncio.getPrezzo() != null ? ("€ " + String.format(Locale.ITALY, "%.2f", annuncio.getPrezzo())) : "N/A"));
        Label l6 = l("Proprietario: " + proprietarioStr);
        Label l7 = l("Data pubblicazione: " + (annuncio.getDataPubblicazione() != null ? annuncio.getDataPubblicazione().toString() : ""));
        Label l8 = l("Descrizione:");

        TextArea ta = styledTextArea();
        ta.setText(annuncio.getDescrizione());
        ta.setEditable(false);
        ta.setPrefRowCount(4);

        HBox btns = new HBox(10); 
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button chiudi = ghostButton("Chiudi", dialog::close);
        btns.getChildren().add(chiudi);

        card.getChildren().addAll(l1, l2, l3, l4Box, l5, l6, l7, l8, ta, btns);

        StackPane wrap = new StackPane(card);
        wrap.setPadding(new Insets(16));
        wrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);");

        dialog.setScene(new Scene(wrap, 520, 500));
        dialog.showAndWait();
    }

    private void openDialogInvioOfferta(Annuncio annuncio) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Invia offerta - " + annuncio.getDescrizione());

        VBox card = card();
        card.setSpacing(12);
        
        Label tipoLabel = l("Tipologia annuncio: " + annuncio.getTipologia());
        card.getChildren().add(tipoLabel);

        switch (annuncio.getTipologia()) {
            case "vendita" -> {
                Label prezzoRichiesto = l("Prezzo richiesto: €" + String.format(Locale.ITALY, "%.2f", annuncio.getPrezzo()));
                prezzoRichiesto.setStyle("-fx-text-fill: #7af7c3; -fx-font-size: 14px; -fx-font-weight: 800;");
                TextField tfPrezzo = styledTextField("Prezzo offerto");

                Button confermaOfferta = primaryButton("Fai un'offerta", () -> {
                    try {
                        if (tfPrezzo.getText().isBlank()) {
                            warn("Inserisci il prezzo offerto.");
                            return;
                        }
                        double prezzoOfferto = Double.parseDouble(tfPrezzo.getText().replace(",", "."));
                        
                        if (prezzoOfferto > annuncio.getPrezzo()) {
                             warn("La tua offerta non può superare il prezzo richiesto. Usa 'Compra Subito' se vuoi pagare il prezzo pieno.");
                             return;
                        }
                        
                        if (prezzoOfferto <= 0) {
                             warn("L'offerta deve essere maggiore di zero.");
                             return;
                        }

                        controller.inviaOfferta(annuncio.getCodiceAnnuncio(), "vendita", prezzoOfferto);
                        dialog.close();
                        warn("Offerta inviata!");
                    } catch (NumberFormatException ex) {
                        warn("Inserisci un prezzo valido.");
                    } catch (Exception ex) {
                        warn("Errore invio offerta: " + ex.getMessage());
                    }
                });

                Button compraSubito = successButton("Compra Subito", () -> {
                    double prezzoPieno = annuncio.getPrezzo();
                    
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Conferma Acquisto");
                    alert.setHeaderText("Confermi l'acquisto immediato?");
                    alert.setContentText("Stai per acquistare questo oggetto a €" + String.format(Locale.ITALY, "%.2f", prezzoPieno) + ".\n\nLa transazione sarà immediata e non richiederà l'approvazione del venditore.");
                    
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        try {
                            if (controller.compraSubito(annuncio.getCodiceAnnuncio())) {
                                dialog.close();
                                warn("Acquisto completato con successo!");
                                reloadData();
                            } else {
                                warn("Errore: Impossibile completare l'acquisto.");
                            }
                        } catch (Exception ex) {
                            warn("Errore acquisto: " + ex.getMessage());
                        }
                    }
                });

                HBox btns = new HBox(10, ghostButton("Annulla", dialog::close), confermaOfferta, compraSubito);
                btns.setAlignment(Pos.CENTER_RIGHT);
                card.getChildren().addAll(prezzoRichiesto, tfPrezzo, btns);
            }
            case "regalo" -> {
                Label motivazione = l("Scrivi un messaggio motivazionale:");
                TextArea taMessaggio = styledTextArea();
                taMessaggio.setPrefRowCount(3);
                Button conferma = primaryButton("Invia offerta", () -> {
                    try {
                        if (taMessaggio.getText().isBlank()) {
                            warn("Inserisci un messaggio motivazionale.");
                            return;
                        }
                        controller.inviaOfferta(annuncio.getCodiceAnnuncio(), "regalo", null, taMessaggio.getText());
                        dialog.close();
                        warn("Offerta inviata!");
                    } catch (Exception ex) {
                        warn("Errore invio offerta: " + ex.getMessage());
                    }
                });
                HBox btns = new HBox(10, ghostButton("Annulla", dialog::close), conferma);
                btns.setAlignment(Pos.CENTER_RIGHT);
                card.getChildren().addAll(motivazione, taMessaggio, btns);
            }
            case "scambio" -> {
                try {
                    List<Oggetto> oggettiPersonali = controller.getOggettiUtenteObj(controller.getUtenteCorrente().getMatricola())
                        .stream()
                        .filter(o -> o.getCodiceAnnuncio() == null)
                        .toList();
                    if (oggettiPersonali.isEmpty()) {
                        warn("Non hai oggetti disponibili per lo scambio!");
                        dialog.close();
                        return;
                    }
                    mostraDialogScambio(annuncio, oggettiPersonali);
                    dialog.close(); 
                } catch (Exception ex) {
                    warn("Errore caricamento oggetti: " + ex.getMessage());
                }
            }
        }
        
        if (!"scambio".equals(annuncio.getTipologia())) {
             StackPane wrap = new StackPane(card);
            wrap.setPadding(new Insets(16));
            wrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);");
            dialog.setScene(new Scene(wrap));
            dialog.showAndWait();
        }
    }

    private void mostraDialogScambio(Annuncio annuncio, List<Oggetto> oggettiDisponibili) {
        Stage scambioDialog = new Stage();
        scambioDialog.initModality(Modality.APPLICATION_MODAL);
        scambioDialog.setTitle("Seleziona oggetti per lo scambio");

        VBox scambioCard = card();
        scambioCard.setSpacing(12);

        Label lblSelect = l("Scegli i tuoi oggetti da proporre nello scambio:");
        ListView<Oggetto> listOggetti = new ListView<>();
        listOggetti.getItems().addAll(oggettiDisponibili);
        listOggetti.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listOggetti.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Oggetto oggetto, boolean empty) {
                super.updateItem(oggetto, empty);
                setText(empty || oggetto == null ? null : oggetto.getNome() + " (" + oggetto.getDescrizione() + ")");
                if (!empty && isSelected()) {
                    setStyle("-fx-background-color: #4f8cff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8;");
                } else if (!empty) {
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #EAF0FF; -fx-padding: 8;");
                } else {
                    setStyle("");
                }
            }
        });
        listOggetti.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-control-inner-background: transparent; -fx-border-radius: 12; -fx-background-radius: 12;");
        listOggetti.setPrefHeight(200);

        HBox scambioBtns = new HBox(10);
        scambioBtns.setAlignment(Pos.CENTER_RIGHT);
        Button scambioAnnulla = ghostButton("Annulla", scambioDialog::close);
        Button scambioConferma = primaryButton("Invia Offerta di Scambio", () -> {
            List<Oggetto> selezionati = listOggetti.getSelectionModel().getSelectedItems();
            if (selezionati.isEmpty()) {
                warn("Seleziona almeno un oggetto!");
                return;
            }
            List<String> codiciOggetti = selezionati.stream().map(Oggetto::getCodiceOggetto).toList();
            try {
                controller.inviaOfferta(annuncio.getCodiceAnnuncio(), "scambio", null);
                String codiceOfferta = controller.getUltimaOffertaScambioUtente();
                for (String codiceOggetto : codiciOggetti) {
                    controller.associaOggettoAdOfferta(codiceOfferta, codiceOggetto);
                }
                scambioDialog.close();
                warn("Offerta di scambio inviata!");
            } catch (Exception ex) {
                warn("Errore invio offerta: " + ex.getMessage());
            }
        });

        scambioBtns.getChildren().addAll(scambioAnnulla, scambioConferma);
        scambioCard.getChildren().addAll(lblSelect, listOggetti, scambioBtns);

        StackPane scambioWrap = new StackPane(scambioCard);
        scambioWrap.setPadding(new Insets(16));
        scambioWrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);");

        scambioDialog.setScene(new Scene(scambioWrap, 550, 400));
        scambioDialog.showAndWait();
    }


    private TextArea styledTextArea() {
        TextArea ta = new TextArea();
        ta.setWrapText(true);
        String baseStyle = "-fx-background-color: rgba(255,255,255,0.10); -fx-control-inner-background: rgba(16,20,30,0.35); -fx-text-fill: #EAF0FF; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 10 12; -fx-prompt-text-fill: rgba(234,240,255,0.45); -fx-border-color: transparent;";
        ta.setStyle(baseStyle);
        return ta;
    }

    private VBox card() {
        VBox card = new VBox();
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(255,255,255,0.10); -fx-border-width: 1;");
        card.setEffect(new DropShadow(24, Color.color(0, 0, 0, 0.45)));
        return card;
    }

    private Label l(String s) {
        Label lbl = new Label(s);
        lbl.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 12px; -fx-font-weight: 800;");
        return lbl;
    }

    private TextField styledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: #EAF0FF; -fx-background-radius: 12; -fx-padding: 10 12; -fx-prompt-text-fill: rgba(234,240,255,0.45); -fx-border-color: transparent;");
        return tf;
    }

    private <T> void styleCombo(ComboBox<T> cb) {
        cb.setStyle("-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: #EAF0FF; -fx-background-radius: 12; -fx-padding: 2 4; -fx-border-color: transparent;");
        cb.setOnShowing(e -> Platform.runLater(() -> {
            Node popup = cb.lookup(".combo-box-popup");
            if (popup != null) {
                popup.setStyle("-fx-background-color: rgba(24, 27, 35, 0.98); -fx-background-radius: 12; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-width: 1; -fx-border-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.4), 10, 0, 0, 2);");
            }
        }));
        styleComboItems(cb);
    }
    
    private <T> void styleComboItems(ComboBox<T> combo) {
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? (combo.getPromptText() == null ? "" : combo.getPromptText()) : String.valueOf(item));
                setStyle("-fx-text-fill: #EAF0FF; -fx-background-color: transparent;");
            }
        });
        combo.setCellFactory(lv -> {
            ListCell<T> cell = new ListCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.valueOf(item));
                    if (!empty) {
                        setStyle("-fx-text-fill: #EAF0FF; -fx-background-color: #181b23; -fx-padding: 8 12; -fx-font-size: 14px;");
                    } else {
                        setStyle("");
                    }
                }
            };
            cell.setOnMouseEntered(e -> {
                if (!cell.isEmpty()) {
                    cell.setStyle("-fx-text-fill: white; -fx-background-color: #4f8cff; -fx-padding: 8 12; -fx-font-size: 14px; -fx-background-radius: 8;");
                }
            });
            cell.setOnMouseExited(e -> {
                if (!cell.isEmpty()) {
                    cell.setStyle("-fx-text-fill: #EAF0FF; -fx-background-color: #181b23; -fx-padding: 8 12; -fx-font-size: 14px;");
                }
            });
            return cell;
        });
    }

    private Button primaryButton(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        final String baseStyle = "-fx-background-radius: 12; -fx-padding: 10 16; -fx-font-weight: 700; -fx-text-fill: white;";
        b.setStyle("-fx-background-color: #4f8cff;" + baseStyle);
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #3b6fe0;" + baseStyle));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #4f8cff;" + baseStyle));
        return b;
    }
    
    private Button successButton(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        final String baseStyle = "-fx-background-radius: 12; -fx-padding: 10 16; -fx-font-weight: 700; -fx-text-fill: white;";
        final String normalColor = "-fx-background-color: #43a047;"; 
        final String hoverColor = "-fx-background-color: #388e3c;"; 
        b.setStyle(normalColor + baseStyle);
        b.setOnMouseEntered(e -> b.setStyle(hoverColor + baseStyle));
        b.setOnMouseExited(e -> b.setStyle(normalColor + baseStyle));
        return b;
    }

    private Button ghostButton(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        final String baseStyle = "-fx-text-fill: #EAF0FF; -fx-border-color: rgba(255,255,255,0.20); -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10 16; -fx-font-weight: 700;";
        b.setStyle("-fx-background-color: transparent;" + baseStyle);
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: rgba(255,255,255,0.08);" + baseStyle));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent;" + baseStyle));
        return b;
    }
    
    private void styleTable(TableView<?> tv) {
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.setStyle("-fx-background-color: transparent; -fx-control-inner-background: #181b23;");
        tv.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                applyHeaderStyles(tv);
            }
        });
    }

    private void applyHeaderStyles(TableView<?> tv) {
        Platform.runLater(() -> {
            Pane headerBackground = (Pane) tv.lookup(".column-header-background");
            if (headerBackground != null) {
                headerBackground.setStyle("-fx-background-color: #101218; -fx-border-width: 0 0 2 0; -fx-border-color: #27304a;");
            }
            tv.lookupAll(".column-header").forEach(headerNode -> {
                headerNode.setStyle("-fx-background-color: transparent; -fx-padding: 8;");
                Label label = (Label) headerNode.lookup(".label");
                if (label != null) {
                    label.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 15px;");
                    label.setAlignment(Pos.CENTER_LEFT);
                }
            });
        });
    }
    
    private TableCell<Annuncio, String> badgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String stato, boolean empty) {
                super.updateItem(stato, empty);
                if (empty || stato == null) {
                    setGraphic(null);
                } else {
                    setGraphic(statoBadge(stato));
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private Label statoBadge(String stato) {
        Label badge = new Label(stato.toUpperCase());
        String bg = switch (stato.toLowerCase()) {
            case "attivo" -> "rgba(122,247,195,0.25)";
            case "concluso" -> "rgba(79,140,255,0.25)";
            case "scaduto" -> "rgba(255,107,107,0.25)";
            default -> "rgba(255,255,255,0.18)";
        };
        String color = switch (stato.toLowerCase()) {
            case "attivo" -> "#7af7c3";
            case "concluso" -> "#4f8cff";
            case "scaduto" -> "#ff6b6b";
            default -> "#EAF0FF";
        };
        badge.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-color: " + bg + "; -fx-background-radius: 999; -fx-padding: 4 8;");
        return badge;
    }
    
    private String zebraStyle(int idx) {
        return idx % 2 == 0 ? "-fx-background-color: rgba(255,255,255,0.03);" : "-fx-background-color: rgba(122,247,195,0.09);";
    }

    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}