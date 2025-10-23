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
import java.util.function.UnaryOperator;

public class AnnunciView {

    private VBox root;
    private final Controller controller;

    private final ObservableList<Annuncio> masterData = FXCollections.observableArrayList();
    private FilteredList<Annuncio> filtered;
    private SortedList<Annuncio> sorted;

    private TableView<Annuncio> table;
    private ComboBox<String> cbCategoria;
    private ComboBox<String> cbTipologia;
    private TextField tfSearch;
    private Label emptyLabel;

    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(200));
    private List<String> categorie;

    public AnnunciView(Controller controller) {
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

        Label title = new Label("I miei Annunci");
        title.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 20px; -fx-font-weight: 900;");
        HBox header = new HBox(title);
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

        tfSearch = styledTextField("Cerca per testo o codice…");
        searchDebounce.setOnFinished(ev -> applyFilters());
        tfSearch.textProperty().addListener((obs, o, n) -> {
            searchDebounce.stop();
            searchDebounce.playFromStart();
        });

        Button btnClear = ghostButton("Pulisci", () -> {
            tfSearch.clear();
            cbTipologia.setValue("Tutte le tipologie");
            cbCategoria.setValue(null);
            applyFilters();
        });

        filters.getChildren().addAll(cbTipologia, cbCategoria, tfSearch, btnClear);
        filtersCard.getChildren().add(filters);

        VBox tableCard = card();
        tableCard.setSpacing(10);

        table = new TableView<>();
        styleTable(table);

        TableColumn<Annuncio, String> cCod = new TableColumn<>("Codice");
        cCod.setCellValueFactory(new PropertyValueFactory<>("codiceAnnuncio"));
        cCod.setPrefWidth(120);

        TableColumn<Annuncio, String> cCat = new TableColumn<>("Categoria");
        cCat.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        cCat.setPrefWidth(160);

        TableColumn<Annuncio, String> cTip = new TableColumn<>("Tipologia");
        cTip.setCellValueFactory(new PropertyValueFactory<>("tipologia"));
        cTip.setPrefWidth(120);

        TableColumn<Annuncio, String> cDesc = new TableColumn<>("Descrizione");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descrizione"));
        cDesc.setPrefWidth(360);
        cDesc.setCellFactory(col -> {
            Label lbl = new Label();
            lbl.setWrapText(true);
            lbl.setStyle("-fx-text-fill: #EAF0FF;");
            TableCell<Annuncio, String> cell = new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    setGraphic(empty || s == null ? null : lbl);
                    if (!empty && s != null) lbl.setText(s);
                }
            };
            cell.setPrefHeight(Region.USE_COMPUTED_SIZE);
            return cell;
        });

        TableColumn<Annuncio, Double> cPrice = new TableColumn<>("Prezzo");
        cPrice.setCellValueFactory(new PropertyValueFactory<>("prezzo"));
        cPrice.setPrefWidth(120);
        cPrice.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "" : String.format(Locale.ITALY, "€ %.2f", value));
                setStyle("-fx-text-fill: #7af7c3; -fx-font-weight:900; -fx-alignment:CENTER_RIGHT; -fx-padding:0 7 0 0;");
            }
        });

        TableColumn<Annuncio, String> cState = new TableColumn<>("Stato");
        cState.setCellValueFactory(new PropertyValueFactory<>("stato"));
        cState.setPrefWidth(120);
        cState.setCellFactory(col -> badgeCell());
        table.getColumns().setAll(cCod, cCat, cTip, cDesc, cPrice, cState);
        table.setPrefHeight(440);

        table.setRowFactory(tv -> {
            TableRow<Annuncio> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
                    openDialog(row.getItem());
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

        MenuItem miNew = new MenuItem("Nuovo");
        miNew.setOnAction(e -> openDialog(null));
        MenuItem miEdit = new MenuItem("Modifica");
        miEdit.setOnAction(e -> { Annuncio a = table.getSelectionModel().getSelectedItem(); if (a != null) openDialog(a); });
        MenuItem miDel = new MenuItem("Elimina");
        miDel.setOnAction(e -> { Annuncio a = table.getSelectionModel().getSelectedItem(); if (a != null) confirmDelete(a); });
        table.setContextMenu(new ContextMenu(miNew, miEdit, miDel));

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button bNew = primaryButton("Crea", () -> openDialog(null));
        Button bEdit = ghostButton("Modifica", () -> {
            Annuncio s = table.getSelectionModel().getSelectedItem();
            if (s == null) { warn("Seleziona un annuncio"); return; }
            openDialog(s);
        });
        Button bDel = ghostButton("Elimina", () -> {
            Annuncio s = table.getSelectionModel().getSelectedItem();
            if (s == null) { warn("Seleziona un annuncio"); return; }
            confirmDelete(s);
        });
        actions.getChildren().addAll(bNew, bEdit, bDel);

        emptyLabel = new Label("Nessun annuncio corrisponde ai filtri.");
        emptyLabel.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 12px;");
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        tableCard.getChildren().addAll(table, emptyLabel, actions);

        root.getChildren().addAll(header, filtersCard, tableCard);
    }

    private void reloadData() {
        try {
            masterData.setAll(controller.getAnnunciByUtente(controller.getUtenteCorrente().getMatricola()));

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
                filtered = new FilteredList<>(masterData, p -> true);
                sorted = new SortedList<>(filtered);
                sorted.comparatorProperty().bind(table.comparatorProperty());
                table.setItems(sorted);
            }

            applyFilters();
        } catch (SQLException e) {
            warn("Errore nel caricamento annunci: " + e.getMessage());
        }
    }

    private void applyFilters() {
        final String tip = cbTipologia.getValue();
        final String cat = cbCategoria.getValue();
        final String query = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase();

        filtered.setPredicate(a -> {
            if (a == null) return false;

            boolean tipMatch = (tip == null || "Tutte le tipologie".equals(tip) || tip.equalsIgnoreCase(a.getTipologia()));
            boolean catMatch = (cat == null || cat.isBlank() || cat.equalsIgnoreCase(a.getCategoria()));
            if (!tipMatch || !catMatch) return false;

            if (!query.isEmpty()) {
                return (a.getCodiceAnnuncio() != null && a.getCodiceAnnuncio().toLowerCase().contains(query)) ||
                    (a.getCategoria() != null && a.getCategoria().toLowerCase().contains(query)) ||
                    (a.getDescrizione() != null && a.getDescrizione().toLowerCase().contains(query));
            }
            return true;
        });

        boolean empty = filtered.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
    }

    // ============================== DIALOG ==============================
private void openDialog(Annuncio existing) {
    Stage dialog = new Stage();
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.setTitle(existing == null ? "Nuovo Annuncio" : "Modifica Annuncio");

    VBox card = card();
    card.setSpacing(12);

    GridPane form = new GridPane();
    form.setHgap(10); form.setVgap(10);

    // Categoria
    ComboBox<String> catBox = new ComboBox<>();
    catBox.setEditable(true);
    catBox.getItems().setAll(categorie);
    catBox.setValue(existing == null ? null : existing.getCategoria());
    styleCombo(catBox);

    // Tipologia
    ComboBox<String> tipBox = new ComboBox<>();
    tipBox.getItems().addAll("vendita", "scambio", "regalo");
    tipBox.setValue(existing == null ? null : existing.getTipologia());
    styleCombo(tipBox);

    // Descrizione
    TextField desc = styledTextField("Descrizione");
    desc.setText(existing != null ? existing.getDescrizione() : "");

    // Prezzo (solo vendita)
    TextField prezzo = styledTextField("Prezzo");
    applyNumericFormatter(prezzo);
    if (existing != null && existing.getPrezzo() != null) {
        prezzo.setText(String.format(Locale.ITALY, "%.2f", existing.getPrezzo()));
        applyNumericFormatter(prezzo);
        if (existing != null && existing.getPrezzo() != null) {
            prezzo.setText(String.format(Locale.ITALY, "%.2f", existing.getPrezzo()));
        }
        
        Label prezzoLabel = l("Prezzo");
        tipBox.valueProperty().addListener((obs, oldV, newV) -> {
            boolean isVendita = "vendita".equalsIgnoreCase(newV);
            prezzoLabel.setVisible(isVendita);
            prezzo.setVisible(isVendita);
        });
        prezzoLabel.setVisible("vendita".equalsIgnoreCase(tipBox.getValue()));
        prezzo.setVisible("vendita".equalsIgnoreCase(tipBox.getValue()));

        ComboBox<model.Oggetto> oggettiBox = new ComboBox<>();
        if (existing == null) {
            try {
                List<model.Oggetto> tuttiOggetti = controller.getOggettiByUtente(controller.getUtenteCorrente().getMatricola());
                List<model.Oggetto> oggettiDisponibili = tuttiOggetti.stream()
                    .filter(o -> o.getCodiceAnnuncio() == null || o.getCodiceAnnuncio().isEmpty())
                    .toList();
                
                oggettiBox.getItems().setAll(oggettiDisponibili);
                oggettiBox.setPromptText("Seleziona oggetto da associare");
                oggettiBox.setCellFactory(list -> new ListCell<>() {
                    @Override protected void updateItem(model.Oggetto item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNome() + " (" + item.getCodiceOggetto() + ")");
                    }
                });
                oggettiBox.setButtonCell(new ListCell<>() {
                    @Override protected void updateItem(model.Oggetto item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNome() + " (" + item.getCodiceOggetto() + ")");
                    }
                });
            } catch (SQLException e) {
                warn("Errore nel caricamento oggetti: " + e.getMessage());
            }
        }

        ComboBox<String> stateBox = null;
        if (existing != null) {
            stateBox = new ComboBox<>();
            stateBox.getItems().addAll("attivo", "scaduto", "in attesa");
            stateBox.setValue(existing.getStato());
            styleCombo(stateBox);
        }

        int r = 0;
        form.add(l("Categoria"), 0, r); form.add(catBox, 1, r++);
        form.add(l("Tipologia"), 0, r); form.add(tipBox, 1, r++);
        form.add(l("Descrizione"), 0, r); form.add(desc, 1, r++);
        form.add(prezzoLabel, 0, r); form.add(prezzo, 1, r++);
        
        if (existing == null) {
            form.add(l("Oggetto"), 0, r); form.add(oggettiBox, 1, r++);
        } else {
            form.add(l("Stato"), 0, r); form.add(stateBox, 1, r);
        }

        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button annulla = ghostButton("Annulla", dialog::close);
        final ComboBox<String> finalStateBox = stateBox;
        Button conferma = primaryButton(existing == null ? "Crea" : "Aggiorna", () -> {
            String categoria = catBox.getEditor().getText() != null ? catBox.getEditor().getText().trim() : "";
            String tip = tipBox.getValue();
            String d = desc.getText() != null ? desc.getText().trim() : "";

            if (categoria.isEmpty() || tip == null || d.isEmpty() || (existing != null && (finalStateBox.getValue() == null || finalStateBox.getValue().isEmpty()))) {
                warn("Compila tutti i campi obbligatori.");
                return;
            }

            double price = 0.0;
            if ("vendita".equalsIgnoreCase(tip)) {
                try {
                    price = Double.parseDouble(prezzo.getText().replace(",", "."));
                } catch (NumberFormatException | NullPointerException ex) {
                    warn("Prezzo non valido."); return;
                }
            }
            try {
                if (existing == null) {
                    model.Oggetto selezionato = oggettiBox.getValue();
                    if (selezionato == null) {
                        warn("Devi associare un tuo oggetto all'annuncio.");
                        return;
                    }
                    controller.creaAnnuncio(categoria, tip, d, price, selezionato.getCodiceOggetto());
                } else {
                    controller.modificaAnnuncio(existing.getCodiceAnnuncio(), categoria, tip, d, price, finalStateBox.getValue());
                }
                dialog.close();
                reloadData();
            } catch (SQLException ex) {
                warn("Errore salvataggio: " + ex.getMessage());
            }
        });
        btns.getChildren().addAll(annulla, conferma);

        card.getChildren().addAll(form, btns);

        StackPane wrap = new StackPane(card);
        wrap.setPadding(new Insets(16));
        wrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);");

        dialog.setScene(new Scene(wrap, 520, existing == null ? 380 : 440));
        dialog.showAndWait();
    }
    Label prezzoLabel = l("Prezzo");
    tipBox.valueProperty().addListener((obs, oldV, newV) -> {
        boolean isVendita = "vendita".equalsIgnoreCase(newV);
        prezzoLabel.setVisible(isVendita);
        prezzo.setVisible(isVendita);
    });
    prezzoLabel.setVisible("vendita".equalsIgnoreCase(tipBox.getValue()));
    prezzo.setVisible("vendita".equalsIgnoreCase(tipBox.getValue()));

    // Oggetto: includi anche l'oggetto già associato all'annuncio
    ComboBox<model.Oggetto> oggettiBox = new ComboBox<>();
    try {
        String me = controller.getUtenteCorrente().getMatricola();
        List<model.Oggetto> miei = controller.getOggettiByUtente(me);

        String codiceAssociatoCorrenteTmp = null;
        if (existing != null) {
            List<model.Oggetto> oggettiAnnuncio = controller.getOggettiByAnnuncio(existing.getCodiceAnnuncio());
            codiceAssociatoCorrenteTmp = oggettiAnnuncio.isEmpty() ? null : oggettiAnnuncio.get(0).getCodiceOggetto();
        }
        final String codiceAssociatoCorrente = codiceAssociatoCorrenteTmp;

        List<model.Oggetto> disponibili = miei.stream()
            .filter(o -> o.getCodiceAnnuncio() == null || o.getCodiceAnnuncio().isEmpty()
                   || (codiceAssociatoCorrente != null && o.getCodiceOggetto().equals(codiceAssociatoCorrente)))
            .toList();

        oggettiBox.getItems().setAll(disponibili);
        oggettiBox.setPromptText("Seleziona oggetto da associare");

        if (existing != null && codiceAssociatoCorrente != null) {
            for (model.Oggetto o : disponibili) {
                if (o.getCodiceOggetto().equals(codiceAssociatoCorrente)) {
                    oggettiBox.setValue(o);
                    break;
                }
            }
        }
        oggettiBox.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(model.Oggetto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNome() + " (" + item.getCodiceOggetto() + ")");
            }
        });
        oggettiBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(model.Oggetto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNome() + " (" + item.getCodiceOggetto() + ")");
            }
        });
        styleCombo(oggettiBox);
    } catch (SQLException e) {
        warn("Errore nel caricamento oggetti: " + e.getMessage());
    }

    int r = 0;
    form.add(l("Categoria"), 0, r); form.add(catBox, 1, r++);
    form.add(l("Tipologia"), 0, r); form.add(tipBox, 1, r++);
    form.add(l("Descrizione"), 0, r); form.add(desc, 1, r++);
    form.add(prezzoLabel, 0, r); form.add(prezzo, 1, r++);
    form.add(l("Oggetto"), 0, r); form.add(oggettiBox, 1, r++);

    HBox btns = new HBox(10);
    btns.setAlignment(Pos.CENTER_RIGHT);
    Button annulla = ghostButton("Annulla", dialog::close);

    Button conferma = primaryButton(existing == null ? "Crea" : "Aggiorna", () -> {
        String categoria = catBox.getEditor().getText() != null ? catBox.getEditor().getText().trim() : "";
        String tip = tipBox.getValue();
        String d = desc.getText() != null ? desc.getText().trim() : "";

        if (categoria.isEmpty() || tip == null || d.isEmpty()) {
            warn("Compila tutti i campi obbligatori.");
            return;
        }

        model.Oggetto selezionato = oggettiBox.getValue();
        if (selezionato == null) {
            warn("Devi associare un tuo oggetto all'annuncio.");
            return;
        }

        double price = 0.0;
        if ("vendita".equalsIgnoreCase(tip)) {
            try {
                price = Double.parseDouble(prezzo.getText().replace(",", "."));
            } catch (NumberFormatException | NullPointerException ex) {
                warn("Prezzo non valido."); return;
            }
        }

        try {
            if (existing == null) {
                // Creazione + associazione atomica lato Controller (stato = attivo)
                controller.creaAnnuncio(categoria, tip, d, price, selezionato.getCodiceOggetto());
            } else {
                // 1) Aggiorna dati annuncio (Controller normalizza prezzo NULL se non vendita)
                controller.modificaAnnuncio(existing.getCodiceAnnuncio(), categoria, tip, d, price, existing.getStato());

                // 2) Gestione (ri)associazione oggetto
                List<model.Oggetto> oggettiAnnuncio = controller.getOggettiByAnnuncio(existing.getCodiceAnnuncio());
                String codiceOggettoCorrente = oggettiAnnuncio.isEmpty() ? null : oggettiAnnuncio.get(0).getCodiceOggetto();

                if (codiceOggettoCorrente == null || !codiceOggettoCorrente.equals(selezionato.getCodiceOggetto())) {
                    if (codiceOggettoCorrente != null) {
                        controller.rimuoviAssociazioneAnnuncioOggetto(codiceOggettoCorrente);
                    }
                    // Associa selezionato e riporta annuncio ATTIVO
                    controller.associaOggettoEAttivaAnnuncio(selezionato.getCodiceOggetto(), existing.getCodiceAnnuncio());
                }
            }
            dialog.close();
            reloadData();
        } catch (SQLException ex) {
            warn("Errore salvataggio: " + ex.getMessage());
        }
    });

    btns.getChildren().addAll(annulla, conferma);

    card.getChildren().addAll(form, btns);

    StackPane wrap = new StackPane(card);
    wrap.setPadding(new Insets(16));
    wrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);");

    dialog.setScene(new Scene(wrap, 520, existing == null ? 360 : 420));
    dialog.showAndWait();
}


    private void confirmDelete(Annuncio a) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Vuoi eliminare l'annuncio selezionato?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText("Elimina annuncio");
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    controller.eliminaAnnuncio(a.getCodiceAnnuncio());
                    reloadData();
                } catch (SQLException e) {
                    warn("Errore eliminazione: " + e.getMessage());
                }
            }
        });
    }

    private VBox card() {
        VBox card = new VBox();
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-background-radius: 18;" +
            "-fx-border-radius: 18;" +
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-width: 1;"
        );
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
        tf.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 12;" +
            "-fx-prompt-text-fill: rgba(234,240,255,0.45);" +
            "-fx-border-color: transparent;"
        );
        return tf;
    }
    private <T> void styleCombo(ComboBox<T> cb) {
        cb.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 2 4;" +
            "-fx-border-color: transparent;"
        );
        cb.setOnShowing(e -> Platform.runLater(() -> {
            Node popup = cb.lookup(".combo-box-popup");
            if (popup != null) {
                popup.setStyle(
                    "-fx-background-color: rgba(24, 27, 35, 0.98);" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: rgba(255, 255, 255, 0.15);" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 12;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.4), 10, 0, 0, 2);"
                );
            }
        }));
        styleComboItems(cb); // già generico
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
                        setStyle(
                            "-fx-text-fill: #EAF0FF;" +
                            "-fx-background-color: #181b23;" +
                            "-fx-padding: 8 12;" +
                            "-fx-font-size: 14px;"
                        );
                    } else {
                        setStyle("");
                    }
                }
            };
            cell.setOnMouseEntered(e -> {
                if (!cell.isEmpty()) {
                    cell.setStyle(
                        "-fx-text-fill: white;" +
                        "-fx-background-color: #4f8cff;" +
                        "-fx-padding: 8 12;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 8;"
                    );
                }
            });
            cell.setOnMouseExited(e -> {
                if (!cell.isEmpty()) {
                    cell.setStyle(
                        "-fx-text-fill: #EAF0FF;" +
                        "-fx-background-color: #181b23;" +
                        "-fx-padding: 8 12;" +
                        "-fx-font-size: 14px;"
                    );
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
        tv.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-control-inner-background: #181b23;"
        );

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
                headerBackground.setStyle(
                    "-fx-background-color: #101218;" +
                    "-fx-border-width: 0 0 2 0;" +
                    "-fx-border-color: #27304a;"
                );
            }

            tv.lookupAll(".column-header").forEach(headerNode -> {
                headerNode.setStyle("-fx-background-color: transparent; -fx-padding: 8;");
                Label label = (Label) headerNode.lookup(".label");
                if (label != null) {
                    label.setStyle(
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: 900;" +
                        "-fx-font-size: 15px;"
                    );
                    label.setAlignment(Pos.CENTER_LEFT);
                }
            });
        });
    }
    
    private String zebraStyle(int idx) {
        return idx % 2 == 0 ?
            "-fx-background-color: rgba(255,255,255,0.03);" :
            "-fx-background-color: rgba(122,247,195,0.09);";
    }
    
    private void applyNumericFormatter(TextField tf) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*[,.]?\\d{0,2}")) {
                return change;
            }
            return null;
        };
        tf.setTextFormatter(new TextFormatter<>(filter));
    }
    
    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
    
    public void mostraCreaAnnuncioDialog() {
        openDialog(null);
    }

    
    private TableCell<Annuncio, String> badgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String stato, boolean empty) {
                super.updateItem(stato, empty);
                if (empty || stato == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(stato.toUpperCase());
                    String bg = switch (stato.toLowerCase()) {
                        case "attivo" -> "rgba(122,247,195,0.25)";
                        case "scaduto" -> "rgba(255,107,107,0.25)";
                        default -> "rgba(255,255,255,0.18)";
                    };
                    String color = switch (stato.toLowerCase()) {
                        case "attivo" -> "#7af7c3";
                        case "scaduto" -> "#ff6b6b";
                        default -> "#EAF0FF";
                    };
                    badge.setStyle(
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-background-color: " + bg + ";" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 4 8;"
                    );
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }
    public VBox getRoot() { return root; }
}