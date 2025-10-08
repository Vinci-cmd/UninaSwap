package gui;

import Controller.Controller;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import model.Annuncio;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class AnnunciView {

    private VBox root;
    private final Controller controller;

    // Dati
    private final ObservableList<Annuncio> masterData = FXCollections.observableArrayList();
    private FilteredList<Annuncio> filtered;
    private SortedList<Annuncio> sorted;

    // UI
    private TableView<Annuncio> table;
    private ComboBox<String> cbCategoria;
    private ComboBox<String> cbTipologia;
    private TextField tfSearch;
    private Label emptyLabel;

    // Filtri
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(200));
    private List<String> categorie; // snapshot per i filtri

    public AnnunciView(Controller controller) {
        this.controller = controller;
        createUI();
        reloadData(); // carica dati UNA volta e li filtra
    }

    // ============================== UI ==============================
    private void createUI() {
        root = new VBox(16);
        root.setPadding(new Insets(16));
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);" +
            "-fx-font-family: 'Segoe UI','Roboto','Arial';"
        );

        // Header minimale
        Label title = new Label("I miei Annunci");
        title.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 20px; -fx-font-weight: 900;");
        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER_LEFT);

        // ===== Card Filtri =====
        VBox filtersCard = card();
        filtersCard.setSpacing(10);

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);

        // Tipologia
        cbTipologia = new ComboBox<>();
        cbTipologia.getItems().addAll("Tutte le tipologie", "vendita", "scambio", "regalo");
        cbTipologia.setValue("Tutte le tipologie");
        styleCombo(cbTipologia);
        styleComboItems(cbTipologia);
        cbTipologia.setOnAction(e -> applyFilters());

        // Categoria (NON editabile qui: evitiamo glitch. La ricerca testuale la gestiamo in tfSearch)
        cbCategoria = new ComboBox<>();
        cbCategoria.setPromptText("Tutte le categorie");
        cbCategoria.setValue(null); // null = tutte
        styleCombo(cbCategoria);
        styleComboItems(cbCategoria);
        cbCategoria.setOnAction(e -> applyFilters());

        // Search
        tfSearch = styledTextField("Cerca per testo o codice…");
        tfSearch.textProperty().addListener((obs, o, n) -> {
            searchDebounce.stop();
            searchDebounce.setOnFinished(ev -> applyFilters());
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

        // ===== Card Tabella =====
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

        // AGGIORNATO: Stile prezzo da ListaAnnunciView (verde #7af7c3, font-weight 900, allineamento a destra)
        TableColumn<Annuncio, Double> cPrice = new TableColumn<>("Prezzo");
        cPrice.setCellValueFactory(new PropertyValueFactory<>("prezzo"));
        cPrice.setPrefWidth(120);
        cPrice.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? "" : String.format("€ %.2f", value));
                setStyle("-fx-text-fill: #7af7c3; -fx-font-weight:900; -fx-alignment:CENTER_RIGHT; -fx-padding:0 7 0 0;");
            }
        });

        TableColumn<Annuncio, String> cState = new TableColumn<>("Stato");
        cState.setCellValueFactory(new PropertyValueFactory<>("stato"));
        cState.setPrefWidth(120);
        cState.setCellFactory(col -> badgeCell());

        table.getColumns().setAll(cCod, cCat, cTip, cDesc, cPrice, cState);
        table.setPrefHeight(440);

        // Doppio click = Modifica
        table.setRowFactory(tv -> {
            TableRow<Annuncio> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
                    openDialog(row.getItem());
                }
            });
            // zebra + hover soft + selezione (AGGIORNATO con colori ListaAnnunciView)
            row.indexProperty().addListener((obs, old, idx) -> {
                if (!row.isSelected()) row.setStyle(zebraStyle(idx.intValue(), row.isSelected()));
            });
            row.selectedProperty().addListener((o,w,is) -> {
                row.setStyle(is ? 
                    "-fx-background-color: #4f8cff; -fx-border-color: #99b0f7; -fx-border-radius:10; -fx-background-radius:10; -fx-effect:dropshadow(two-pass-box,#0b1020,12,0.5,0,0);" : 
                    zebraStyle(row.getIndex(), false));
            });
            row.hoverProperty().addListener((o,w,is) -> {
                if (!row.isEmpty() && !row.isSelected()) {
                    row.setStyle(is ? "-fx-background-color: rgba(122,247,195,0.11); -fx-border-radius:10;" : zebraStyle(row.getIndex(), false));
                }
            });
            return row;
        });

        // Context menu
        MenuItem miNew = new MenuItem("Nuovo");
        miNew.setOnAction(e -> openDialog(null));
        MenuItem miEdit = new MenuItem("Modifica");
        miEdit.setOnAction(e -> { Annuncio a = table.getSelectionModel().getSelectedItem(); if (a!=null) openDialog(a); });
        MenuItem miDel = new MenuItem("Elimina");
        miDel.setOnAction(e -> { Annuncio a = table.getSelectionModel().getSelectedItem(); if (a!=null) confirmDelete(a); });
        table.setContextMenu(new ContextMenu(miNew, miEdit, miDel));

        // Actions bottom (pulite, niente header buttons)
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

        // Empty label
        emptyLabel = new Label("Nessun annuncio corrisponde ai filtri.");
        emptyLabel.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 12px;");
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        tableCard.getChildren().addAll(table, emptyLabel, actions);

        root.getChildren().addAll(header, filtersCard, tableCard);
    }
    // ============================== DATA ==============================
    private void reloadData() {
        try {
            masterData.clear();
            List<Annuncio> lista = controller.getAnnunciByUtente(controller.getUtenteCorrente().getMatricola());
            masterData.addAll(lista);

            // categorie (snapshot ordinato)
            Set<String> cats = new TreeSet<>();
            for (Annuncio a : lista) {
                if (a.getCategoria() != null && !a.getCategoria().isBlank()) cats.add(a.getCategoria());
            }
            categorie = new ArrayList<>(cats);
            cbCategoria.getItems().setAll(categorie);
            cbCategoria.setPromptText("Tutte le categorie");
            cbCategoria.setValue(null); // "tutte"

            if (filtered == null) {
                filtered = new FilteredList<>(masterData, a -> true);
                sorted = new SortedList<>(filtered);
                sorted.comparatorProperty().bind(table.comparatorProperty());
                table.setItems(sorted);
            } else {
                // già inizializzati: solo refresh items & filtri
                filtered.setPredicate(null);
            }

            applyFilters();
        } catch (SQLException e) {
            warn("Errore nel caricamento annunci: " + e.getMessage());
        }
    }

    private void applyFilters() {
        final String tip = cbTipologia.getValue();
        final String cat = cbCategoria.getValue();
        final String query = Optional.ofNullable(tfSearch.getText()).orElse("").trim().toLowerCase();

        Predicate<Annuncio> p = a -> {
            if (a == null) return false;

            // tipologia
            if (tip != null && !"Tutte le tipologie".equals(tip)) {
                if (a.getTipologia()==null || !a.getTipologia().equalsIgnoreCase(tip)) return false;
            }
            // categoria
            if (cat != null && !cat.isBlank()) {
                if (a.getCategoria()==null || !a.getCategoria().equalsIgnoreCase(cat)) return false;
            }
            // text search
            if (!query.isBlank()) {
                boolean hit =
                    (a.getCategoria()!=null && a.getCategoria().toLowerCase().contains(query)) ||
                    (a.getTipologia()!=null && a.getTipologia().toLowerCase().contains(query)) ||
                    (a.getDescrizione()!=null && a.getDescrizione().toLowerCase().contains(query)) ||
                    (a.getCodiceAnnuncio()!=null && a.getCodiceAnnuncio().toLowerCase().contains(query));
                if (!hit) return false;
            }
            return true;
        };

        filtered.setPredicate(p);

        boolean empty = filtered.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
    }

    // ============================== DIALOG ==============================
    private void openDialog(Annuncio existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing==null ? "Nuovo Annuncio" : "Modifica Annuncio");

        VBox card = card();
        card.setSpacing(12);

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);

        // Categoria (editabile solo nel dialog)
        ComboBox<String> catBox = new ComboBox<>();
        catBox.setEditable(true);
        catBox.getItems().setAll(categorie);
        catBox.setValue(existing==null ? null : existing.getCategoria());
        styleCombo(catBox); styleComboItems(catBox);

        // Tipologia
        ComboBox<String> tipBox = new ComboBox<>();
        tipBox.getItems().addAll("vendita","scambio","regalo");
        tipBox.setValue(existing==null ? null : existing.getTipologia());
        styleCombo(tipBox); styleComboItems(tipBox);

        // Descrizione
        TextField desc = styledTextField("Descrizione");
        desc.setText(existing!=null ? Optional.ofNullable(existing.getDescrizione()).orElse("") : "");

        // Prezzo (solo vendita)
        TextField prezzo = styledTextField("Prezzo");
        applyNumericFormatter(prezzo);
        if (existing!=null && existing.getPrezzo()!=null) prezzo.setText(existing.getPrezzo().toString());

        HBox prezzoBox = new HBox(6, new Label("Prezzo"), prezzo);
        prezzoBox.setAlignment(Pos.CENTER_LEFT);
        prezzoBox.setVisible(existing!=null && "vendita".equalsIgnoreCase(existing.getTipologia()));
        tipBox.setOnAction(e -> prezzoBox.setVisible("vendita".equalsIgnoreCase(tipBox.getValue())));

        // Stato (solo in modifica)
        ComboBox<String> stateBox = new ComboBox<>();
        stateBox.getItems().addAll("attivo","scaduto","in attesa");
        if (existing!=null) stateBox.setValue(existing.getStato());
        styleCombo(stateBox); styleComboItems(stateBox);

        int r=0;
        form.add(l("Categoria"),0,r); form.add(catBox,1,r++);
        form.add(l("Tipologia"),0,r); form.add(tipBox,1,r++);
        form.add(l("Descrizione"),0,r); form.add(desc,1,r++);
        form.add(l("Prezzo"),0,r); form.add(prezzoBox,1,r++);
        if (existing!=null) { form.add(l("Stato"),0,r); form.add(stateBox,1,r++); }

        HBox btns = new HBox(10); btns.setAlignment(Pos.CENTER_RIGHT);
        Button annulla = ghostButton("Annulla", dialog::close);
        Button conferma = primaryButton(existing==null ? "Crea" : "Aggiorna", () -> {
            // validazione minima
            String categoria = Optional.ofNullable(catBox.getEditor().getText()).orElse("").trim();
            String tip = tipBox.getValue();
            String d = Optional.ofNullable(desc.getText()).orElse("").trim();
            if (categoria.isBlank() || tip==null || d.isBlank() || (existing!=null && (stateBox.getValue()==null || stateBox.getValue().isBlank()))) {
                warn("Compila tutti i campi obbligatori.");
                return;
            }
            double price = 0.0;
            if ("vendita".equalsIgnoreCase(tip)) {
                try { price = Double.parseDouble(Optional.ofNullable(prezzo.getText()).orElse("0").replace(",", ".")); }
                catch (Exception ex) { warn("Prezzo non valido."); return; }
            }
            try {
                boolean ok;
                if (existing==null) {
                    ok = controller.creaAnnuncio(categoria, tip, d, price);
                } else {
                    ok = controller.modificaAnnuncio(existing.getCodiceAnnuncio(), categoria, tip, d, price, stateBox.getValue());
                }
                if (!ok) { warn("Operazione non riuscita."); return; }
                dialog.close();
                reloadData(); // ricarico in modo pulito
            } catch (SQLException ex) {
                warn("Errore salvataggio: " + ex.getMessage());
            }
        });
        btns.getChildren().addAll(annulla, conferma);

        card.getChildren().addAll(form, btns);

        StackPane wrap = new StackPane(card);
        wrap.setPadding(new Insets(16));
        wrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);");

        dialog.setScene(new Scene(wrap, 520, existing==null ? 330 : 390));
        dialog.showAndWait();
    }

    private void confirmDelete(Annuncio a) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Vuoi eliminare l'annuncio selezionato?", ButtonType.YES, ButtonType.NO);
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

 // ============================== Helpers UI FIXED ==============================
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
        card.setEffect(new DropShadow(24, Color.color(0,0,0,0.45)));
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

    // FIXED: Styling completo per ComboBox con popup scuro e freccia personalizzata
    private void styleCombo(ComboBox<?> cb) {
        cb.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 2 4;" +
            "-fx-border-color: transparent;" +
            // AGGIUNTO: Styling completo per popup e freccia
            "-fx-popup-background: rgba(24,27,35,0.95);" +
            "-fx-selection-bar: #4f8cff;" +
            "-fx-selection-bar-text: white;"
        );
        
        // AGGIUNTO: Styling per l'editor (se editabile)
        if (cb.getEditor() != null) {
            cb.getEditor().setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #EAF0FF;" +
                "-fx-prompt-text-fill: rgba(234,240,255,0.45);"
            );
        }

        // AGGIUNTO: Applica CSS personalizzato per popup scuro e freccia
        cb.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                cb.lookup(".arrow-button").setStyle(
                    "-fx-background-color: transparent;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: transparent;"
                );
                cb.lookup(".arrow").setStyle(
                    "-fx-background-color: #EAF0FF;" +
                    "-fx-shape: \"M 0 0 h 7 l -3.5 4 z\";" +  // Freccia personalizzata
                    "-fx-scale-shape: true;" +
                    "-fx-padding: 2;"
                );
            }
        });
    }

    // FIXED: Styling completo per gli items della ComboBox con popup scuro
    private <T> void styleComboItems(ComboBox<T> combo) {
        // ButtonCell (quello che si vede quando chiusa)
        combo.setButtonCell(new ListCell<>() {
            @Override 
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? 
                    (combo.getPromptText() == null ? "" : combo.getPromptText()) : 
                    String.valueOf(item));
                setStyle("-fx-text-fill: #EAF0FF; -fx-background-color: transparent;");
            }
        });

        // CellFactory (gli items nel dropdown)
        combo.setCellFactory(lv -> {
            ListCell<T> cell = new ListCell<>() {
                @Override 
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.valueOf(item));
                    
                    if (empty) {
                        setStyle("");
                    } else {
                        setStyle(
                            "-fx-text-fill: #EAF0FF;" +
                            "-fx-background-color: transparent;" +
                            "-fx-padding: 8 12;" +
                            "-fx-font-size: 14px;"
                        );
                    }
                }
            };
            
            // AGGIUNTO: Hover effect per gli items
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
                        "-fx-background-color: transparent;" +
                        "-fx-padding: 8 12;" +
                        "-fx-font-size: 14px;"
                    );
                }
            });
            
            return cell;
        });

        // AGGIUNTO: Styling del popup ListView
        combo.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                // Trova e stylizza il popup
                combo.getScene().getRoot().lookupAll(".list-view").forEach(node -> {
                    if (node instanceof ListView) {
                        node.setStyle(
                            "-fx-background-color: rgba(24,27,35,0.98);" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-color: rgba(255,255,255,0.15);" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 12;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 2);"
                        );
                    }
                });
            }
        });
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
    
    // ============================== TABELLA (COLORI AGGIORNATI) ==============================
    private void styleTable(TableView<?> tv) {
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // AGGIORNATO: Utilizzando i colori della ListaAnnunciView
        tv.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-control-inner-background: #181b23;" +  // Cambiato da rgba(255,255,255,0.04)
            "-fx-background-insets: 0;" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-selection-bar: #4f8cff;" +
            "-fx-selection-bar-text: white;" +
            "-fx-selection-bar-non-focused: #3b6fe0;" +
            "-fx-table-header-background: #101218;"  // Aggiunto per gli header
        );
        
        // Aggiunto: Styling per gli header delle colonne
        tv.skinProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                for (TableColumn<?, ?> col : tv.getColumns()) {
                    col.setStyle(
                        "-fx-background-color: #101218; " +
                        "-fx-text-fill: #ffffff; " +
                        "-fx-font-weight: 900; " +
                        "-fx-font-size: 15px; " +
                        "-fx-border-width: 0 0 2 0; " +
                        "-fx-border-color: #27304a;"
                    );
                }
            }
        });
    }
    
    private TableCell<Annuncio, Double> priceCell() {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ITALY);
        s.setDecimalSeparator(',');
        s.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", s);
        return new TableCell<>() {
            @Override protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : df.format(value));
                setStyle("-fx-text-fill: #EAF0FF; -fx-alignment: CENTER_RIGHT; -fx-padding: 0 10 0 0;");
            }
        };
    }
    
    private TableCell<Annuncio, String> badgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String stato, boolean empty) {
                super.updateItem(stato, empty);
                if (empty || stato == null) { setGraphic(null); setText(null); return; }
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
        };
    }
    
    // AGGIORNATO: Colori zebra da ListaAnnunciView
    private String zebraStyle(int idx, boolean selected) {
        if (selected) return "-fx-background-color: #4f8cff; -fx-effect:dropshadow(two-pass-box,#0b1020,8,0.25,0,0);";
        return idx % 2 == 0 ?
                "-fx-background-color: rgba(255,255,255,0.03);" :
                "-fx-background-color: rgba(122,247,195,0.09);";
    }
    
    private void applyNumericFormatter(TextField tf) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            return newText.matches("\\d*[\\.,]?\\d*") ? change : null;
        };
        StringConverter<Double> conv = new DoubleStringConverter() {
            @Override public Double fromString(String s) {
                if (s == null || s.isBlank()) return null;
                return Double.valueOf(s.replace(",", "."));
            }
        };
        tf.setTextFormatter(new TextFormatter<>(conv, null, filter));
    }
    
    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
    
    public void mostraCreaAnnuncioDialog() {
    	openDialog(null);
    	}

    public VBox getRoot() { return root; }
}