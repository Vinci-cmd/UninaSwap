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
import model.Offerta;
import model.Utente;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.function.Predicate;

public class OfferteRicevuteView {

    private VBox root;
    private final Controller controller;

    // Dati
    private final ObservableList<Offerta> masterData = FXCollections.observableArrayList();
    private FilteredList<Offerta> filtered;
    private SortedList<Offerta> sorted;

    // UI
    private TableView<Offerta> tableOfferte;
    private ComboBox<String> cbTipologia;
    private ComboBox<String> cbStato;
    private TextField tfSearch;
    private Label emptyLabel;

    // Filtri
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(200));

    public OfferteRicevuteView(Controller controller) {
        this.controller = controller;
        createUI();
        reloadData();
    }

    // ============================== UI ==============================
    private void createUI() {
        root = new VBox(16);
        root.setPadding(new Insets(16));
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);" +
            "-fx-font-family: 'Segoe UI','Roboto','Arial';"
        );

        // Header
        Label title = new Label("Offerte Ricevute");
        title.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 20px; -fx-font-weight: 900;");
        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER_LEFT);

        // ===== Card Filtri =====
        VBox filtersCard = card();
        filtersCard.setSpacing(10);

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);

        cbTipologia = new ComboBox<>();
        cbTipologia.getItems().addAll("Tutte le tipologie", "vendita", "scambio", "regalo");
        cbTipologia.setValue("Tutte le tipologie");
        styleCombo(cbTipologia);
        styleComboItems(cbTipologia);
        cbTipologia.setOnAction(e -> applyFilters());

        cbStato = new ComboBox<>();
        cbStato.getItems().addAll("Tutti gli stati", "inviata", "accettata", "rifiutata");
        cbStato.setValue("Tutti gli stati");
        styleCombo(cbStato);
        styleComboItems(cbStato);
        cbStato.setOnAction(e -> applyFilters());

        tfSearch = styledTextField("Cerca per codice annuncio/mittente...");
        tfSearch.textProperty().addListener((obs, o, n) -> {
            searchDebounce.stop();
            searchDebounce.setOnFinished(ev -> applyFilters());
            searchDebounce.playFromStart();
        });

        Button btnClear = ghostButton("Pulisci", () -> {
            tfSearch.clear();
            cbTipologia.setValue("Tutte le tipologie");
            cbStato.setValue("Tutti gli stati");
            applyFilters();
        });

        filters.getChildren().addAll(cbTipologia, cbStato, tfSearch, btnClear);
        filtersCard.getChildren().add(filters);

        // ===== Card Tabella =====
        VBox tableCard = card();
        tableCard.setSpacing(10);

        tableOfferte = new TableView<>();
        styleTable(tableOfferte);

        TableColumn<Offerta, String> colCodice = new TableColumn<>("Cod. Offerta");
        colCodice.setCellValueFactory(new PropertyValueFactory<>("codiceOfferta"));
        colCodice.setPrefWidth(120);

        TableColumn<Offerta, String> colAnnuncio = new TableColumn<>("Cod. Annuncio");
        colAnnuncio.setCellValueFactory(new PropertyValueFactory<>("codiceAnnuncio"));
        colAnnuncio.setPrefWidth(120);

        TableColumn<Offerta, String> colMittente = new TableColumn<>("Mittente");
        colMittente.setCellValueFactory(cellData -> {
            try {
                Utente u = controller.getUtenteByMatricola(cellData.getValue().getMatricola());
                String nome = u != null ? u.getNome() + " " + u.getCognome() : cellData.getValue().getMatricola();
                return new javafx.beans.property.SimpleStringProperty(nome);
            } catch (Exception e) {
                return new javafx.beans.property.SimpleStringProperty("?");
            }
        });
        colMittente.setPrefWidth(150);

        TableColumn<Offerta, String> colTipo = new TableColumn<>("Tipologia");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(100);

        TableColumn<Offerta, String> colStato = new TableColumn<>("Stato");
        colStato.setCellValueFactory(new PropertyValueFactory<>("stato"));
        colStato.setPrefWidth(120);
        colStato.setCellFactory(col -> badgeCell());

        TableColumn<Offerta, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(cd -> {
            java.sql.Date d = cd.getValue().getData();
            return new javafx.beans.property.SimpleStringProperty(
                d != null ? new SimpleDateFormat("dd/MM/yyyy").format(d) : ""
            );
        });
        colData.setPrefWidth(100);

        TableColumn<Offerta, Void> colAzioni = new TableColumn<>("Azioni");
        colAzioni.setCellFactory(param -> new TableCell<>() {
            private final Button btnAccetta = new Button("Accetta");
            private final Button btnRifiuta = new Button("Rifiuta");
            private final HBox box = new HBox(6, btnAccetta, btnRifiuta);
            {
                box.setAlignment(Pos.CENTER);
                btnAccetta.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8; -fx-padding: 4 8;");
                btnRifiuta.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 8; -fx-padding: 4 8;");
                btnAccetta.setOnAction(event -> gestisciAccetta(getTableView().getItems().get(getIndex())));
                btnRifiuta.setOnAction(event -> gestisciRifiuta(getTableView().getItems().get(getIndex())));
            }
            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()
                        || !"inviata".equals(getTableView().getItems().get(getIndex()).getStato())) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
        colAzioni.setPrefWidth(150);

        tableOfferte.getColumns().addAll(colCodice, colAnnuncio, colMittente, colTipo, colStato, colData, colAzioni);
        tableOfferte.setPrefHeight(440);

        tableOfferte.setRowFactory(tv -> {
            TableRow<Offerta> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
                    mostraDettaglioOfferta(row.getItem());
                }
            });
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
        
        emptyLabel = new Label("Nessuna offerta corrisponde ai filtri.");
        emptyLabel.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 12px;");
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        tableCard.getChildren().addAll(tableOfferte, emptyLabel);

        root.getChildren().addAll(header, filtersCard, tableCard);
    }
    
    // ============================== DATA ==============================
    private void reloadData() {
        try {
            String matricola = controller.getUtenteCorrente().getMatricola();
            masterData.clear();
            masterData.addAll(controller.getOfferteRicevuteByUtente(matricola));

            if (filtered == null) {
                filtered = new FilteredList<>(masterData, p -> true);
                sorted = new SortedList<>(filtered);
                sorted.comparatorProperty().bind(tableOfferte.comparatorProperty());
                tableOfferte.setItems(sorted);
            }

            applyFilters();
        } catch (SQLException e) {
            warn("Errore nel caricamento delle offerte: " + e.getMessage());
        }
    }

    private void applyFilters() {
        final String tipologia = cbTipologia.getValue();
        final String stato = cbStato.getValue();
        final String search = Optional.ofNullable(tfSearch.getText()).orElse("").trim().toLowerCase();

        Predicate<Offerta> p = o -> {
            if (o == null) return false;

            boolean tipologiaMatch = (tipologia == null || "Tutte le tipologie".equals(tipologia) || o.getTipo().equalsIgnoreCase(tipologia));
            boolean statoMatch = (stato == null || "Tutti gli stati".equals(stato) || o.getStato().equalsIgnoreCase(stato));
            boolean searchMatch = (search.isBlank() ||
                (o.getCodiceAnnuncio() != null && o.getCodiceAnnuncio().toLowerCase().contains(search)) ||
                (utenteStringSafe(o.getMatricola()).toLowerCase().contains(search)));

            return tipologiaMatch && statoMatch && searchMatch;
        };

        filtered.setPredicate(p);
        
        boolean empty = filtered.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
    }

    private String utenteStringSafe(String matricola) {
        try {
            Utente u = controller.getUtenteByMatricola(matricola);
            return u != null ? (u.getNome() + " " + u.getCognome()) : matricola;
        } catch(Exception e) {
            return "?";
        }
    }

    // ============================== ACTIONS ==============================
    private void gestisciAccetta(Offerta offerta) {
        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION,
            "Accettare questa offerta? Le altre per lo stesso annuncio verranno rifiutate!", ButtonType.YES, ButtonType.NO);
        conferma.setHeaderText("Accetta Offerta");
        conferma.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    if (controller.accettaOfferta(offerta.getCodiceOfferta())) {
                        reloadData();
                    } else {
                        warn("Errore durante l'accettazione dell'offerta.");
                    }
                } catch (Exception e) {
                    warn("Errore accettazione offerta: " + e.getMessage());
                }
            }
        });
    }

    private void gestisciRifiuta(Offerta offerta) {
        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION,
            "Vuoi rifiutare questa offerta?", ButtonType.YES, ButtonType.NO);
        conferma.setHeaderText("Rifiuta Offerta");
        conferma.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    if (controller.rifiutaOfferta(offerta.getCodiceOfferta())) {
                        reloadData();
                    } else {
                        warn("Errore durante il rifiuto dell'offerta.");
                    }
                } catch (Exception e) {
                    warn("Errore rifiuto offerta: " + e.getMessage());
                }
            }
        });
    }

    private void mostraDettaglioOfferta(Offerta offerta) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Dettaglio Offerta Ricevuta - " + offerta.getCodiceOfferta());

        VBox card = card();
        card.setSpacing(12);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        int r = 0;
        grid.add(l("Codice Offerta:"), 0, r); grid.add(new Label(offerta.getCodiceOfferta()), 1, r++);
        grid.add(l("Annuncio:"), 0, r); grid.add(new Label(offerta.getCodiceAnnuncio()), 1, r++);
        grid.add(l("Mittente:"), 0, r); grid.add(new Label(utenteStringSafe(offerta.getMatricola())), 1, r++);
        grid.add(l("Tipologia:"), 0, r); grid.add(new Label(offerta.getTipo()), 1, r++);
        grid.add(l("Stato:"), 0, r); grid.add(new Label(offerta.getStato()), 1, r++);
        grid.add(l("Data:"), 0, r); grid.add(new Label(offerta.getData() != null ? new SimpleDateFormat("dd/MM/yyyy").format(offerta.getData()) : "N/A"), 1, r++);
        
        if ("vendita".equals(offerta.getTipo()) && offerta.getPrezzoOfferto() != null) {
            grid.add(l("Prezzo offerto:"), 0, r);
            grid.add(new Label("€" + String.format("%.2f", offerta.getPrezzoOfferto())), 1, r++);
        }
        
        if (offerta.getMessaggio() != null && !offerta.getMessaggio().isBlank()) {
            Label lblMsgTitle = new Label("Messaggio:");
            lblMsgTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #EAF0FF;");
            TextArea taMessaggio = new TextArea(offerta.getMessaggio());
            taMessaggio.setEditable(false);
            taMessaggio.setWrapText(true);
            taMessaggio.setPrefRowCount(3);
            styleTextArea(taMessaggio);
            grid.add(lblMsgTitle, 0, r++);
            grid.add(taMessaggio, 0, r++, 2, 1);
        }

        Button btnChiudi = primaryButton("Chiudi", dialog::close);
        HBox buttons = new HBox(btnChiudi);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        card.getChildren().addAll(grid, buttons);

        StackPane wrap = new StackPane(card);
        wrap.setPadding(new Insets(16));
        wrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);");
        
        dialog.setScene(new Scene(wrap));
        dialog.showAndWait();
    }

    // ============================== Helpers UI ==============================
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

    private void styleTextArea(TextArea ta) {
        ta.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 10 12;" +
            "-fx-prompt-text-fill: rgba(234,240,255,0.45);" +
            "-fx-border-color: transparent;"
        );
    }
    
    private void styleCombo(ComboBox<?> cb) {
        cb.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 2 4;" +
            "-fx-border-color: transparent;" +
            "-fx-popup-background: rgba(24,27,35,0.95);" +
            "-fx-selection-bar: #4f8cff;" +
            "-fx-selection-bar-text: white;"
        );
        
        if (cb.getEditor() != null) {
            cb.getEditor().setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #EAF0FF;" +
                "-fx-prompt-text-fill: rgba(234,240,255,0.45);"
            );
        }

        cb.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                cb.lookup(".arrow-button").setStyle(
                    "-fx-background-color: transparent;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: transparent;"
                );
                cb.lookup(".arrow").setStyle(
                    "-fx-background-color: #EAF0FF;" +
                    "-fx-shape: \"M 0 0 h 7 l -3.5 4 z\";" +
                    "-fx-scale-shape: true;" +
                    "-fx-padding: 2;"
                );
            }
        });
    }

    private <T> void styleComboItems(ComboBox<T> combo) {
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

        combo.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
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
    
    private void styleTable(TableView<?> tv) {
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-control-inner-background: #181b23;" +
            "-fx-background-insets: 0;" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-selection-bar: #4f8cff;" +
            "-fx-selection-bar-text: white;" +
            "-fx-selection-bar-non-focused: #3b6fe0;" +
            "-fx-table-header-background: #101218;"
        );
        
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

    private TableCell<Offerta, String> badgeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String stato, boolean empty) {
                super.updateItem(stato, empty);
                if (empty || stato == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(stato.toUpperCase());
                String bg = switch (stato.toLowerCase()) {
                    case "accettata" -> "rgba(122,247,195,0.25)";
                    case "rifiutata" -> "rgba(255,107,107,0.25)";
                    default -> "rgba(255,255,255,0.18)";
                };
                String color = switch (stato.toLowerCase()) {
                    case "accettata" -> "#7af7c3";
                    case "rifiutata" -> "#ff6b6b";
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
    
    private String zebraStyle(int idx, boolean selected) {
        if (selected) return "-fx-background-color: #4f8cff; -fx-effect:dropshadow(two-pass-box,#0b1020,8,0.25,0,0);";
        return idx % 2 == 0 ?
                "-fx-background-color: rgba(255,255,255,0.03);" :
                "-fx-background-color: rgba(122,247,195,0.09);";
    }

    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
    
    public VBox getRoot() { return root; }
}