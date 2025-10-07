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
import model.Oggetto;

import java.sql.SQLException;
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
        root = new VBox(12);
        root.setPadding(new Insets(14));

        Label lblTitolo = new Label("Gestione Oggetti Personali");
        lblTitolo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // --- FILTRI AVANZATI ---
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
        cbCategoria.getItems().addAll(categorieGlobali); // Nessun "Categoria" tra le voci reali!
        cbCategoria.setPromptText("Categoria");
        // Filtro interattivo live
        cbCategoria.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            String input = newVal.toLowerCase();
            List<String> filtered = categorieGlobali.stream()
                    .filter(cat -> cat.toLowerCase().contains(input))
                    .collect(Collectors.toList());
            if (!(filtered.size() == cbCategoria.getItems().size() && cbCategoria.getItems().containsAll(filtered)))
                cbCategoria.getItems().setAll(filtered.isEmpty() ? categorieGlobali : filtered);
            // AGGIUNGI QUESTA RIGA per filtro live tabella:
            loadOggettiConFiltri();
        });

        cbAnnuncio = new ComboBox<>();
        cbAnnuncio.getItems().addAll("Associazione", "Non associato", "Associato");
        cbAnnuncio.setValue("Associazione");
        cbAnnuncio.setOnAction(e -> loadOggettiConFiltri());

        tfSearch = new TextField();
        tfSearch.setPromptText("Cerca oggetto...");
        tfSearch.setOnKeyReleased(e -> loadOggettiConFiltri());

        filtriBox.getChildren().addAll(cbCategoria, cbAnnuncio, tfSearch);

        // --- TABELLA OGGETTI ---
        tableOggetti = new TableView<>();
        TableColumn<Oggetto, String> colCodice = new TableColumn<>("Codice");
        colCodice.setCellValueFactory(new PropertyValueFactory<>("codiceOggetto"));
        TableColumn<Oggetto, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        TableColumn<Oggetto, String> colDescr = new TableColumn<>("Descrizione");
        colDescr.setCellValueFactory(new PropertyValueFactory<>("descrizione"));
        tableOggetti.getColumns().addAll(colCodice, colNome, colDescr);
        tableOggetti.setPrefHeight(250);

        // Doppio click per dettaglio
        tableOggetti.setRowFactory(tv -> {
            TableRow<Oggetto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Oggetto oggetto = row.getItem();
                    openOggettoDialog(oggetto);
                }
            });
            return row;
        });

        // --- BOX PULSANTI ---
        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        Button btnAggiungi = new Button("Aggiungi");
        Button btnModifica = new Button("Modifica");
        Button btnElimina = new Button("Elimina");

        btnAggiungi.setOnAction(e -> openOggettoDialog(null));
        btnModifica.setOnAction(e -> {
            Oggetto selected = tableOggetti.getSelectionModel().getSelectedItem();
            if (selected != null) dialogModificaOggetto(selected);
            else showAlert("Seleziona un oggetto da modificare!");
        });
        btnElimina.setOnAction(e -> {
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

        btnBox.getChildren().addAll(btnAggiungi, btnModifica, btnElimina);
        root.getChildren().addAll(lblTitolo, filtriBox, tableOggetti, btnBox);
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
                        .filter(o -> o.getCategoria().toLowerCase().contains(categoria.toLowerCase()))
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
                        .filter(o -> o.getNome().toLowerCase().contains(filtro)
                              || o.getCategoria().toLowerCase().contains(filtro)
                              || o.getDescrizione().toLowerCase().contains(filtro))
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

        VBox box = new VBox(10);
        box.setPadding(new Insets(16));

        if (oggetto == null) {
            TextField tfNome = new TextField();
            tfNome.setPromptText("Nome");
            TextArea taDescrizione = new TextArea();
            taDescrizione.setPromptText("Descrizione");
            taDescrizione.setPrefRowCount(3);

            ComboBox<String> cbCategoriaDialog = new ComboBox<>();
            cbCategoriaDialog.setEditable(true);
            cbCategoriaDialog.setPromptText("Categoria");
            cbCategoriaDialog.getItems().addAll(categorieGlobali);
            cbCategoriaDialog.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                String input = newVal.toLowerCase();
                List<String> filtered = categorieGlobali.stream()
                        .filter(cat -> cat.toLowerCase().contains(input))
                        .collect(Collectors.toList());
                if (!(filtered.size() == cbCategoriaDialog.getItems().size()
                        && cbCategoriaDialog.getItems().containsAll(filtered)))
                    cbCategoriaDialog.getItems().setAll(filtered.isEmpty() ? categorieGlobali : filtered);
            });

            Button btnSalva = new Button("Salva");
            btnSalva.setDefaultButton(true);
            btnSalva.setOnAction(e -> {
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

            box.getChildren().addAll(
                    new Label("Nome:"), tfNome,
                    new Label("Descrizione:"), taDescrizione,
                    new Label("Categoria:"), cbCategoriaDialog,
                    btnSalva
            );
        } else {
            Label lblNome = new Label("Nome: " + oggetto.getNome());
            Label lblDescr = new Label("Descrizione: " + oggetto.getDescrizione());
            Label lblCategoria = new Label("Categoria: " + oggetto.getCategoria());
            Label lblAnnuncio = new Label("Annuncio collegato: " + (oggetto.getCodiceAnnuncio() != null ? oggetto.getCodiceAnnuncio() : "Non associato"));

            box.getChildren().addAll(lblNome, lblDescr, lblCategoria, lblAnnuncio);

            if (oggetto.getCodiceAnnuncio() != null) {
                Button btnAnnulla = new Button("Annulla richiesta");
                btnAnnulla.setOnAction(e -> {
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
        }

        dialog.setScene(new Scene(box, 350, 250));
        dialog.showAndWait();
    }

    private void dialogModificaOggetto(Oggetto oggetto) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Modifica Oggetto");

        VBox box = new VBox(10);
        box.setPadding(new Insets(16));

        TextField tfNome = new TextField(oggetto.getNome());
        tfNome.setPromptText("Nome");
        TextArea taDescrizione = new TextArea(oggetto.getDescrizione());
        taDescrizione.setPromptText("Descrizione");
        taDescrizione.setPrefRowCount(3);

        ComboBox<String> cbCategoriaDialog = new ComboBox<>();
        cbCategoriaDialog.setEditable(true);
        cbCategoriaDialog.setPromptText("Categoria");
        cbCategoriaDialog.getItems().addAll(categorieGlobali);
        cbCategoriaDialog.setValue(oggetto.getCategoria());
        cbCategoriaDialog.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            String input = newVal.toLowerCase();
            List<String> filtered = categorieGlobali.stream()
                    .filter(cat -> cat.toLowerCase().contains(input))
                    .collect(Collectors.toList());
            if (!(filtered.size() == cbCategoriaDialog.getItems().size()
                    && cbCategoriaDialog.getItems().containsAll(filtered)))
                cbCategoriaDialog.getItems().setAll(filtered.isEmpty() ? categorieGlobali : filtered);
        });

        Button btnSalva = new Button("Salva Modifiche");
        btnSalva.setDefaultButton(true);
        btnSalva.setOnAction(e -> {
            String nome = tfNome.getText().trim();
            String descr = taDescrizione.getText().trim();
            String categoria = cbCategoriaDialog.getEditor().getText().trim();
            if (nome.isBlank() || descr.isBlank() || categoria.isBlank()) {
                showAlert("Inserisci tutti i campi.");
                return;
            }
            try {
                oggetto.setNome(nome);
                oggetto.setDescrizione(descr);
                oggetto.setCategoria(categoria);
                controller.modificaOggetto(oggetto);
                dialog.close();
                loadOggettiConFiltri();
            } catch (Exception ex) {
                showAlert("Errore salvataggio: " + ex.getMessage());
            }
        });

        box.getChildren().addAll(
                new Label("Nome:"), tfNome,
                new Label("Descrizione:"), taDescrizione,
                new Label("Categoria:"), cbCategoriaDialog,
                btnSalva
        );
        dialog.setScene(new Scene(box, 300, 270));
        dialog.showAndWait();
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}
