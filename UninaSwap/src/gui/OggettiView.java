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
import model.Oggetto;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class OggettiView {

    private VBox root;
    private final Controller controller;

    // Dati
    private final ObservableList<Oggetto> masterData = FXCollections.observableArrayList();
    private FilteredList<Oggetto> filtered;
    private SortedList<Oggetto> sorted;

    // UI
    private TableView<Oggetto> table;
    private ComboBox<String> cbCategoria;
    private ComboBox<String> cbAssociazione; // "Associazione" | "Non associato" | "Associato"
    private TextField tfSearch;
    private Label emptyLabel;

    // Filtri
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(200));
    private List<String> categorie; // derivate dai tuoi oggetti

    public OggettiView(Controller controller) {
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

        Label title = new Label("I miei Oggetti");
        title.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 20px; -fx-font-weight: 900;");
        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox filtersCard = card();
        filtersCard.setSpacing(10);
        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);

        cbAssociazione = new ComboBox<>();
        cbAssociazione.getItems().addAll("Associazione", "Non associato", "Associato");
        cbAssociazione.setValue("Associazione");
        styleCombo(cbAssociazione);
        cbAssociazione.setOnAction(e -> applyFilters());

        cbCategoria = new ComboBox<>();
        cbCategoria.setPromptText("Tutte le categorie");
        cbCategoria.setValue(null);
        styleCombo(cbCategoria);
        cbCategoria.setOnAction(e -> applyFilters());

        tfSearch = styledTextField("Cerca per nome/categoria/descrizione…");
        tfSearch.textProperty().addListener((obs, o, n) -> {
            searchDebounce.stop();
            searchDebounce.setOnFinished(ev -> applyFilters());
            searchDebounce.playFromStart();
        });

        Button btnClear = ghostButton("Pulisci", () -> {
            tfSearch.clear();
            cbAssociazione.setValue("Associazione");
            cbCategoria.setValue(null);
            applyFilters();
        });

        filters.getChildren().addAll(cbAssociazione, cbCategoria, tfSearch, btnClear);
        filtersCard.getChildren().add(filters);

        VBox tableCard = card();
        tableCard.setSpacing(10);

        table = new TableView<>();
        styleTable(table);

        TableColumn<Oggetto, String> cCod = new TableColumn<>("Codice");
        cCod.setCellValueFactory(new PropertyValueFactory<>("codiceOggetto"));
        cCod.setPrefWidth(140);

        TableColumn<Oggetto, String> cNome = new TableColumn<>("Nome");
        cNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        cNome.setPrefWidth(180);

        TableColumn<Oggetto, String> cCat = new TableColumn<>("Categoria");
        cCat.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        cCat.setPrefWidth(160);

        TableColumn<Oggetto, String> cDesc = new TableColumn<>("Descrizione");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descrizione"));
        cDesc.setPrefWidth(360);
        cDesc.setCellFactory(col -> {
            Label lbl = new Label();
            lbl.setWrapText(true);
            lbl.setStyle("-fx-text-fill: #EAF0FF;");
            TableCell<Oggetto, String> cell = new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    setGraphic(empty || s == null ? null : lbl);
                    if (!empty && s != null) lbl.setText(s);
                }
            };
            cell.setPrefHeight(Region.USE_COMPUTED_SIZE);
            return cell;
        });

        TableColumn<Oggetto, String> cAssoc = new TableColumn<>("Associazione");
        cAssoc.setCellValueFactory(new PropertyValueFactory<>("codiceAnnuncio"));
        cAssoc.setPrefWidth(140);
        cAssoc.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String codiceAnnuncio, boolean empty) {
                super.updateItem(codiceAnnuncio, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    String stato = (codiceAnnuncio == null || codiceAnnuncio.isBlank()) ? "Non associato" : "Associato";
                    Label badge = new Label(stato.toUpperCase());
                    String bg = "Associato".equals(stato) ? "rgba(79,140,255,0.25)" : "rgba(255,255,255,0.18)";
                    String color = "Associato".equals(stato) ? "#4f8cff" : "#EAF0FF";
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
        });

        table.getColumns().setAll(cCod, cNome, cCat, cDesc, cAssoc);
        table.setPrefHeight(440);

        table.setRowFactory(tv -> {
            TableRow<Oggetto> row = new TableRow<>();
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
        MenuItem miEdit = new MenuItem("Dettaglio");
        miEdit.setOnAction(e -> {
            Oggetto a = table.getSelectionModel().getSelectedItem();
            if (a != null) openDialog(a);
        });
        MenuItem miDel = new MenuItem("Elimina");
        miDel.setOnAction(e -> {
            Oggetto a = table.getSelectionModel().getSelectedItem();
            if (a != null) confirmDelete(a);
        });
        table.setContextMenu(new ContextMenu(miNew, miEdit, miDel));

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button bNew = primaryButton("Crea", () -> openDialog(null));
        Button bEdit = ghostButton("Dettaglio", () -> {
            Oggetto s = table.getSelectionModel().getSelectedItem();
            if (s == null) { warn("Seleziona un oggetto"); return; }
            openDialog(s);
        });
        Button bDel = ghostButton("Elimina", () -> {
            Oggetto s = table.getSelectionModel().getSelectedItem();
            if (s == null) { warn("Seleziona un oggetto"); return; }
            confirmDelete(s);
        });
        actions.getChildren().addAll(bNew, bEdit, bDel);

        emptyLabel = new Label("Nessun oggetto corrisponde ai filtri.");
        emptyLabel.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 12px;");
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        tableCard.getChildren().addAll(table, emptyLabel, actions);

        root.getChildren().addAll(header, filtersCard, tableCard);
    }

    // ============================== DATA ==============================
    private void reloadData() {
        try {
            String matricola = controller.getUtenteCorrente().getMatricola();
            masterData.setAll(controller.getOggettiUtenteObj(matricola));

            categorie = masterData.stream()
                .map(Oggetto::getCategoria)
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
            warn("Errore nel caricamento oggetti: " + e.getMessage());
        }
    }

    private void applyFilters() {
        final String cat = cbCategoria.getValue();
        final String assoc = cbAssociazione.getValue();
        final String query = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase();

        filtered.setPredicate(o -> {
            if (o == null) return false;

            boolean catMatch = (cat == null || cat.isBlank() || cat.equalsIgnoreCase(o.getCategoria()));
            if (!catMatch) return false;

            if (assoc != null && !"Associazione".equals(assoc)) {
                if ("Non associato".equals(assoc) && o.getCodiceAnnuncio() != null && !o.getCodiceAnnuncio().isBlank()) return false;
                if ("Associato".equals(assoc) && (o.getCodiceAnnuncio() == null || o.getCodiceAnnuncio().isBlank())) return false;
            }

            if (!query.isEmpty()) {
                String n = Optional.ofNullable(o.getNome()).orElse("").toLowerCase();
                String c = Optional.ofNullable(o.getCategoria()).orElse("").toLowerCase();
                String d = Optional.ofNullable(o.getDescrizione()).orElse("").toLowerCase();
                return n.contains(query) || c.contains(query) || d.contains(query);
            }
            return true;
        });

        boolean empty = filtered.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
    }

    // ============================== DIALOG ==============================
    private void openDialog(Oggetto existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Nuovo Oggetto" : "Dettaglio Oggetto");

        VBox card = card();
        card.setSpacing(12);

        if (existing == null) {
        	VBox form = new VBox(10);
        	TextField nome = styledTextField("Nome");
        	TextField categoria = styledTextField("Categoria");
        	TextArea descrizione = styledTextArea("Descrizione");
        	descrizione.setPrefRowCount(3);

        	// ogni campo: etichetta sopra + controllo sotto
        	VBox campoNome = new VBox(6, l("Nome"), nome);
        	VBox campoCategoria = new VBox(6, l("Categoria"), categoria);
        	VBox campoDescrizione = new VBox(6, l("Descrizione"), descrizione);

        	// allineamento e larghezza
        	form.setFillWidth(true);
        	nome.setMaxWidth(Double.MAX_VALUE);
        	categoria.setMaxWidth(Double.MAX_VALUE);
        	descrizione.setMaxWidth(Double.MAX_VALUE);

        	form.getChildren().addAll(campoNome, campoCategoria, campoDescrizione);

        	// poi i bottoni come prima
        	HBox btns = new HBox(10);
        	btns.setAlignment(Pos.CENTER_RIGHT);
        	Button annulla = ghostButton("Annulla", dialog::close);
        	Button conferma = primaryButton("Crea", () -> {
        	    String n = Optional.ofNullable(nome.getText()).orElse("").trim();
        	    String c = Optional.ofNullable(categoria.getText()).orElse("").trim();
        	    String d = Optional.ofNullable(descrizione.getText()).orElse("").trim();

        	    if (n.isBlank() || c.isBlank() || d.isBlank()) {
        	        warn("Compila tutti i campi obbligatori.");
        	        return;
        	    }
        	    try {
        	        Oggetto nuovo = new Oggetto(null, n, d, c, null);
        	        controller.creaOggetto(nuovo);
        	        dialog.close();
        	        reloadData();
        	    } catch (Exception ex) {
        	        warn("Errore salvataggio: " + ex.getMessage());
        	    }
        	});


        	btns.getChildren().addAll(annulla, conferma);

        	// aggiungi al card
        	card.getChildren().addAll(form, btns);
        	
        } else {
            // Dettaglio stile AnnunciView: etichette + badge associazione + azione “Annulla associazione” se presente
            Label lNome = l("Nome: " + Optional.ofNullable(existing.getNome()).orElse(""));
            Label lCat = l("Categoria: " + Optional.ofNullable(existing.getCategoria()).orElse(""));
            Label lDescTitle = l("Descrizione:");
            TextArea ta = styledTextArea(null);
            ta.setText(Optional.ofNullable(existing.getDescrizione()).orElse(""));
            ta.setEditable(false);
            ta.setPrefRowCount(4);

            Label lAssocTitle = l("Associazione:");
            Label assocBadge = new Label((existing.getCodiceAnnuncio() == null || existing.getCodiceAnnuncio().isBlank()) ? "NON ASSOCIATO" : "ASSOCIATO");
            String bg = (existing.getCodiceAnnuncio() == null || existing.getCodiceAnnuncio().isBlank()) ? "rgba(255,255,255,0.18)" : "rgba(79,140,255,0.25)";
            String color = (existing.getCodiceAnnuncio() == null || existing.getCodiceAnnuncio().isBlank()) ? "#EAF0FF" : "#4f8cff";
            assocBadge.setStyle(
                "-fx-text-fill: " + color + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: 800;" +
                "-fx-background-color: " + bg + ";" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 4 8;"
            );
            HBox assocBox = new HBox(10, lAssocTitle, assocBadge);
            assocBox.setAlignment(Pos.CENTER_LEFT);

            HBox btns = new HBox(10);
            btns.setAlignment(Pos.CENTER_RIGHT);
            Button chiudi = ghostButton("Chiudi", dialog::close);

            if (existing.getCodiceAnnuncio() != null && !existing.getCodiceAnnuncio().isBlank()) {
            	Button annullaAssoc = ghostButton("Annulla associazione", () -> {
            	    Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            	        "Vuoi rimuovere l'associazione all'annuncio?\nL'annuncio collegato verrà marcato come SCADUTO.",
            	        ButtonType.YES, ButtonType.NO);
            	    conf.setHeaderText(null);
            	    conf.showAndWait().ifPresent(bt -> {
            	        if (bt == ButtonType.YES) {
            	            try {
            	                controller.disassociaOggettoEChiudiAnnuncio(existing.getCodiceOggetto());
            	                dialog.close();
            	                reloadData();
            	                warn("Associazione rimossa. Annuncio marcato come SCADUTO.");
            	            } catch (Exception ex) {
            	                warn("Errore annullamento: " + ex.getMessage());
            	            }
            	        }
            	    });
            	});
                btns.getChildren().addAll(annullaAssoc, chiudi);
            } else {
                btns.getChildren().add(chiudi);
            }

            card.getChildren().addAll(lNome, lCat, lDescTitle, ta, assocBox, btns);
        }

        StackPane wrap = new StackPane(card);
        wrap.setPadding(new Insets(16));
        wrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);");

        dialog.setScene(new Scene(wrap, 520, existing == null ? 360 : 420));
        dialog.showAndWait();
    }

    private void confirmDelete(Oggetto a) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Vuoi eliminare l'oggetto selezionato?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText("Elimina oggetto");
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    controller.eliminaOggetto(a.getCodiceOggetto());
                    reloadData();
                } catch (SQLException e) {
                    warn("Errore eliminazione: " + e.getMessage());
                }
            }
        });
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

    private TextArea styledTextArea(String prompt) {
        TextArea ta = new TextArea();
        if (prompt != null) ta.setPromptText(prompt);
        ta.setWrapText(true);
        ta.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-control-inner-background: rgba(16,20,30,0.35);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 10 12;" +
            "-fx-prompt-text-fill: rgba(234,240,255,0.45);" +
            "-fx-border-color: transparent;"
        );
        return ta;
    }

    private void styleCombo(ComboBox<?> cb) {
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

        styleComboItems(cb);
    }

    private <T> void styleComboItems(ComboBox<T> combo) {
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? (combo.getPromptText() == null ? "" : combo.getPromptText()) : String.valueOf(item));
                setStyle("-fx-text-fill: #EAF0FF; -fx-background-color: transparent;");
            }
        });

        combo.setCellFactory(lv -> {
            ListCell<T> cell = new ListCell<>() {
                @Override protected void updateItem(T item, boolean empty) {
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

    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public VBox getRoot() { return root; }
}
