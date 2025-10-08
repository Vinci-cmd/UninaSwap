package gui;

import Controller.Controller;
import javafx.collections.FXCollections;
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
import model.Oggetto;

import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OggettiView {
    private VBox root;
    private Controller controller;
    private TableView<Oggetto> tableOggetti;
    private ComboBox<String> cbCategoria;
    private ComboBox<String> cbAnnuncio;
    private TextField tfSearch;
    private Set<String> categorieGlobali;

    public OggettiView(Controller controller) {
        this.controller = controller;
        createUI();
        loadOggettiConFiltri();
    }

private void createUI() {
    root = new VBox(16);
    root.setPadding(new Insets(16));
    root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);"
            + "-fx-font-family: 'Segoe UI','Roboto','Arial';");

    Label lblTitolo = new Label("Gestione Oggetti Personali");
    lblTitolo.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 19px; -fx-font-weight: 900;");

    VBox filtersCard = card();
    filtersCard.setSpacing(8);

    HBox filtriBox = new HBox(10);
    filtriBox.setAlignment(Pos.CENTER_LEFT);

    try {
        categorieGlobali = controller.getAnnunciAttiviRaw().stream()
                .map(a -> a.getCategoria())
                .filter(cat -> cat != null && !cat.isBlank())
                .collect(Collectors.toSet());
    } catch (Exception ignored) {
        categorieGlobali = Set.of();
    }

    cbCategoria = new ComboBox<>();
    cbCategoria.setEditable(true);
    cbCategoria.setPromptText("Categoria");
    cbCategoria.getItems().addAll(categorieGlobali);
    styleCombo(cbCategoria);
    styleComboItems(cbCategoria);
    // Listener SOLO per filtro (attivo su selezione/cambio, NON per ogni lettera digitata!)
    cbCategoria.setOnAction(e -> loadOggettiConFiltri());
    cbCategoria.getEditor().setOnAction(e -> loadOggettiConFiltri());
    cbCategoria.getEditor().setOnKeyReleased(e -> {
        // Applica filtri solo su invio/cambio, NON aggiorna la lista della ComboBox
        loadOggettiConFiltri();
    });

    cbAnnuncio = new ComboBox<>();
    cbAnnuncio.getItems().addAll("Associazione", "Non associato", "Associato");
    cbAnnuncio.setValue("Associazione");
    styleCombo(cbAnnuncio);
    styleComboItems(cbAnnuncio);
    cbAnnuncio.setOnAction(e -> loadOggettiConFiltri());

    tfSearch = styledTextField("Cerca oggetto...");
    tfSearch.setOnKeyReleased(e -> loadOggettiConFiltri());

    // Tasto Pulisci come nelle altre classi
    Button btnClear = ghostButton("Pulisci", () -> {
        tfSearch.clear();
        cbCategoria.getEditor().clear();
        cbCategoria.setValue(null);
        cbAnnuncio.setValue("Associazione");
        loadOggettiConFiltri();
    });

    filtriBox.getChildren().addAll(cbCategoria, cbAnnuncio, tfSearch, btnClear);
    filtersCard.getChildren().add(filtriBox);

    VBox tableCard = card();
    tableCard.setSpacing(10);

    tableOggetti = new TableView<>();
    styleTable(tableOggetti);

    TableColumn<Oggetto, String> colCodice = new TableColumn<>("Codice");
    colCodice.setCellValueFactory(new PropertyValueFactory<>("codiceOggetto"));
    colCodice.setPrefWidth(120);

    TableColumn<Oggetto, String> colNome = new TableColumn<>("Nome");
    colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
    colNome.setPrefWidth(160);

    TableColumn<Oggetto, String> colDescr = new TableColumn<>("Descrizione");
    colDescr.setCellValueFactory(new PropertyValueFactory<>("descrizione"));
    colDescr.setPrefWidth(300);

    tableOggetti.getColumns().addAll(colCodice, colNome, colDescr);
    tableOggetti.setPrefHeight(300);

    tableOggetti.setRowFactory(tv -> {
        TableRow<Oggetto> row = new TableRow<>();
        row.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                openOggettoDialog(row.getItem());
            }
        });
        row.selectedProperty().addListener((obs, was, isSel) -> {
            row.setStyle(isSel
                ? "-fx-background-color: #4f8cff; -fx-border-color: #99b0f7; -fx-border-radius:10; -fx-background-radius:10; -fx-effect:dropshadow(two-pass-box,#0b1020,12,0.5,0,0);"
                : zebraStyle(row.getIndex(), false)
            );
        });
        row.indexProperty().addListener((obs, old, idx) -> {
            if (!row.isSelected()) row.setStyle(zebraStyle(idx.intValue(), row.isSelected()));
        });
        row.hoverProperty().addListener((obs, old, hovered) -> {
            if (!row.isEmpty() && !row.isSelected())
                row.setStyle(hovered
                    ? "-fx-background-color: rgba(122,247,195,0.11); -fx-border-radius:10;"
                    : zebraStyle(row.getIndex(), false)
                );
        });
        return row;
    });

    MenuItem miDetail = new MenuItem("Dettaglio");
    miDetail.setOnAction(e -> { 
        Oggetto a = tableOggetti.getSelectionModel().getSelectedItem(); 
        if (a!=null) openOggettoDialog(a); 
    });
    tableOggetti.setContextMenu(new ContextMenu(miDetail));

    HBox btnBox = new HBox(10);
    btnBox.setAlignment(Pos.CENTER_LEFT);

    Button btnAggiungi = primaryButton("Aggiungi", () -> openOggettoDialog(null));
    Button btnElimina = ghostButton("Elimina", () -> {
        Oggetto selected = tableOggetti.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (conferma("Sicuro di voler eliminare questo oggetto?")) {
                try {
                    controller.eliminaOggetto(selected.getCodiceOggetto());
                    showAlert("Oggetto eliminato.");
                    loadOggettiConFiltri();
                } catch (Exception ex) {
                    showAlert("Errore eliminazione: " + ex.getMessage());
                }
            }
        } else showAlert("Seleziona un oggetto da eliminare!");
    });
    btnBox.getChildren().addAll(btnAggiungi, btnElimina);

    tableCard.getChildren().addAll(tableOggetti, btnBox);

    root.getChildren().addAll(lblTitolo, filtersCard, tableCard);
}


    private void loadOggettiConFiltri() {
        try {
            String matricola = controller.getUtenteCorrente().getMatricola();
            List<Oggetto> lista = controller.getOggettiUtenteObj(matricola);

            String categoria = cbCategoria.getEditor().getText().trim();
            String search = tfSearch.getText();
            String associazione = cbAnnuncio.getValue();

            if (!categoria.isBlank()) {
                lista = lista.stream()
                        .filter(o -> o.getCategoria() != null && o.getCategoria().toLowerCase().contains(categoria.toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (associazione != null && !"Associazione".equals(associazione)) {
                if ("Non associato".equals(associazione))
                    lista = lista.stream().filter(o -> o.getCodiceAnnuncio() == null).collect(Collectors.toList());
                else
                    lista = lista.stream().filter(o -> o.getCodiceAnnuncio() != null).collect(Collectors.toList());
            }

            if (search != null && !search.isBlank()) {
                String filtro = search.trim().toLowerCase();
                lista = lista.stream()
                        .filter(o -> (o.getNome() != null && o.getNome().toLowerCase().contains(filtro))
                                || (o.getCategoria() != null && o.getCategoria().toLowerCase().contains(filtro))
                                || (o.getDescrizione() != null && o.getDescrizione().toLowerCase().contains(filtro)))
                        .collect(Collectors.toList());
            }

            tableOggetti.getItems().setAll(lista);
        } catch (SQLException e) {
            showAlert("Errore caricamento oggetti: " + e.getMessage());
        }
    }

    private boolean conferma(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, messaggio, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait();
        return alert.getResult() == ButtonType.YES;
    }

private void openOggettoDialog(Oggetto oggetto) {
    Stage dialog = new Stage();
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.setTitle(oggetto == null ? "Nuovo Oggetto" : "Dettaglio Oggetto");

    VBox box = card();
    box.setSpacing(12);

    if (oggetto == null) {
        TextField tfNome = styledTextField("Nome");
        TextArea taDescrizione = styledTextArea("Descrizione");
        taDescrizione.setPrefRowCount(3);

        ComboBox<String> cbCategoriaDialog = new ComboBox<>();
        cbCategoriaDialog.setEditable(true);
        cbCategoriaDialog.setPromptText("Categoria");
        cbCategoriaDialog.getItems().addAll(categorieGlobali);
        styleCombo(cbCategoriaDialog);
        styleComboItems(cbCategoriaDialog);
        // NESSUN listener qui!

        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button btnAnnulla = ghostButton("Annulla", dialog::close);
        Button btnSalva = primaryButton("Salva", () -> {
            String nome = tfNome.getText().trim();
            String descr = taDescrizione.getText().trim();
            String categoria = cbCategoriaDialog.getEditor().getText().trim();
            if (nome.isBlank() || descr.isBlank() || categoria.isBlank()) {
                showAlert("Inserisci tutti i campi.");
                return;
            }
            try {
                Oggetto nuovo = new Oggetto(null, nome, descr, categoria, null);
                controller.creaOggetto(nuovo);
                dialog.close();
                loadOggettiConFiltri();
            } catch (Exception ex) {
                showAlert("Errore salvataggio: " + ex.getMessage());
            }
        });
        btns.getChildren().addAll(btnAnnulla, btnSalva);

        box.getChildren().addAll(
                l("Nome:"), tfNome,
                l("Descrizione:"), taDescrizione,
                l("Categoria:"), cbCategoriaDialog,
                btns
        );
    } else {
        Label lblNome = l("Nome: " + oggetto.getNome());
        Label lblDescr = l("Descrizione: " + oggetto.getDescrizione());
        Label lblCategoria = l("Categoria: " + oggetto.getCategoria());
        Label lblAnnuncio = l("Annuncio collegato: " + (oggetto.getCodiceAnnuncio() != null ? oggetto.getCodiceAnnuncio() : "Non associato"));

        HBox detailBtns = new HBox(10);
        detailBtns.setAlignment(Pos.CENTER_RIGHT);
        Button btnChiudi = ghostButton("Chiudi", dialog::close);
        detailBtns.getChildren().add(btnChiudi);

        box.getChildren().addAll(lblNome, lblDescr, lblCategoria, lblAnnuncio);

        if (oggetto.getCodiceAnnuncio() != null) {
            Button btnAnnulla = ghostButton("Annulla richiesta", () -> {
                if (conferma("Sicuro di voler rimuovere l'associazione all'annuncio?")) {
                    try {
                        controller.aggiornaCodiceAnnuncioOggetto(oggetto.getCodiceOggetto(), null);
                        showAlert("Collegamento annuncio rimosso!");
                        dialog.close();
                        loadOggettiConFiltri();
                    } catch (Exception ex) {
                        showAlert("Errore annullamento: " + ex.getMessage());
                    }
                }
            });
            box.getChildren().add(btnAnnulla);
        }
        
        box.getChildren().add(detailBtns);
    }

    StackPane wrap = new StackPane(box);
    wrap.setPadding(new Insets(12));
    wrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);");

    dialog.setScene(new Scene(wrap, 420, 380));
    dialog.showAndWait();
}


    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private String zebraStyle(int idx, boolean selected) {
        if (selected) return "-fx-background-color: #4f8cff; -fx-effect:dropshadow(two-pass-box,#0b1020,8,0.25,0,0);";
        return idx % 2 == 0 ?
                "-fx-background-color: rgba(255,255,255,0.03);" :
                "-fx-background-color: rgba(122,247,195,0.09);";
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
        card.setEffect(new DropShadow(24, Color.color(0,0,0,0.45)));
        return card;
    }
    
    private Label l(String s) {
        Label lbl = new Label(s);
        lbl.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 13px; -fx-font-weight: 700;");
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
        ta.setPromptText(prompt);
        ta.setStyle(
            "-fx-control-inner-background: #181b23;" +
            "-fx-control-inner-background-alt: #181b23;" +
            "-fx-background-color: #181b23;" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-resize-cursor: none;" +
            "-fx-region-resize: none;" +
            "-fx-padding: 10 12;" +
            "-fx-prompt-text-fill: rgba(234,240,255,0.45);" +
            "-fx-border-color: transparent;" +
            "-fx-background-insets: 0;" +
            "-fx-focus-color: transparent;" +
            "-fx-faint-focus-color: transparent;"
        );
        ta.focusedProperty().addListener((obs, oldVal, newVal) -> {
            ta.setStyle(
                "-fx-control-inner-background: #181b23;" +
                "-fx-control-inner-background-alt: #181b23;" +
                "-fx-background-color: #181b23;" +
                "-fx-text-fill: #EAF0FF;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-padding: 10 12;" +
                "-fx-resize-cursor: none;" +
                "-fx-region-resize: none;" +
                "-fx-prompt-text-fill: rgba(234,240,255,0.45);" +
                "-fx-border-color: transparent;" +
                "-fx-background-insets: 0;" +
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;"
            );
        });
        ta.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getRoot().lookupAll(".corner").forEach(node ->
                    node.setStyle("-fx-background-color: transparent;"));
                newScene.getRoot().lookupAll(".resize-corner").forEach(node ->
                    node.setStyle("-fx-background-color: transparent;"));
            }
        });
        return ta;
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
                if (cb.lookup(".arrow-button")!=null)
                    cb.lookup(".arrow-button").setStyle(
                        "-fx-background-color: transparent;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: transparent;"
                    );
                if (cb.lookup(".arrow")!=null)
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
            @Override protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ?
                        (combo.getPromptText() == null ? "" : combo.getPromptText()) :
                        String.valueOf(item));
                setStyle("-fx-text-fill: #EAF0FF; -fx-background-color: transparent;");
            }
        });
        combo.setCellFactory(lv -> {
            ListCell<T> cell = new ListCell<>() {
                @Override protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.valueOf(item));
                    if (empty) setStyle("");
                    else setStyle(
                        "-fx-text-fill: #EAF0FF;" +
                        "-fx-background-color: transparent;" +
                        "-fx-padding: 8 12;" +
                        "-fx-font-size: 14px;");
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
        combo.showingProperty().addListener((obs, was, is) -> {
            if (is) {
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
    
    public VBox getRoot() { 
        return root; 
    }
}