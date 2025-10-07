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
import model.Offerta;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

public class OfferteInviateView {
    private VBox root;
    private Controller controller;
    private TableView<Offerta> tableOfferte;
    private TextField tfSearch;
    private ComboBox<String> cbTipologia;
    private ComboBox<String> cbStato;

    public OfferteInviateView(Controller controller) {
        this.controller = controller;
        createUI();
        loadOfferte();
    }

    private void createUI() {
        root = new VBox(14);
        root.setPadding(new Insets(16));

        Label lblTitle = new Label("Le mie Offerte Inviate");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // --- FILTRI ---
        HBox filtriBox = new HBox(10);
        filtriBox.setAlignment(Pos.CENTER_LEFT);

        cbTipologia = new ComboBox<>();
        cbTipologia.setPromptText("Tipologia");
        cbTipologia.getItems().addAll("vendita", "scambio", "regalo");
        cbTipologia.setOnAction(e -> loadOfferteConFiltri());

        cbStato = new ComboBox<>();
        cbStato.setPromptText("Stato");
        cbStato.getItems().addAll("inviata", "accettata", "rifiutata");
        cbStato.setOnAction(e -> loadOfferteConFiltri());

        tfSearch = new TextField();
        tfSearch.setPromptText("Cerca per codice annuncio...");
        tfSearch.setOnKeyReleased(e -> loadOfferteConFiltri());

        filtriBox.getChildren().addAll(cbTipologia, cbStato, tfSearch);

        // --- TABELLA ---
        tableOfferte = new TableView<>();
        TableColumn<Offerta, String> colCodice = new TableColumn<>("Cod. Offerta");
        colCodice.setCellValueFactory(new PropertyValueFactory<>("codiceOfferta"));
        colCodice.setPrefWidth(100);

        TableColumn<Offerta, String> colAnnuncio = new TableColumn<>("Cod. Annuncio");
        colAnnuncio.setCellValueFactory(new PropertyValueFactory<>("codiceAnnuncio"));
        colAnnuncio.setPrefWidth(100);

        TableColumn<Offerta, String> colTipo = new TableColumn<>("Tipologia");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(80);

        TableColumn<Offerta, String> colStato = new TableColumn<>("Stato");
        colStato.setCellValueFactory(new PropertyValueFactory<>("stato"));
        colStato.setPrefWidth(80);

        TableColumn<Offerta, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(cd -> {
            java.sql.Date d = cd.getValue().getData();
            return new javafx.beans.property.SimpleStringProperty(
                d != null ? new SimpleDateFormat("dd/MM/yyyy").format(d) : ""
            );
        });
        colData.setPrefWidth(80);

        // Colonna Azioni (Annulla)
        TableColumn<Offerta, Void> colAzioni = new TableColumn<>("Azioni");
        colAzioni.setCellFactory(param -> new TableCell<Offerta, Void>() {
            private final Button btnAnnulla = new Button("Annulla");
            {
                btnAnnulla.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white;");
                btnAnnulla.setOnAction(event -> {
                    Offerta offerta = getTableView().getItems().get(getIndex());
                    annullaOfferta(offerta);
                });
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Offerta offerta = getTableView().getItems().get(getIndex());
                    // Mostra bottone solo per offerte "inviata"
                    if ("inviata".equals(offerta.getStato())) {
                        setGraphic(btnAnnulla);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        colAzioni.setPrefWidth(80);

        tableOfferte.getColumns().addAll(colCodice, colAnnuncio, colTipo, colStato, colData, colAzioni);
        tableOfferte.setPrefHeight(350);

        // Doppio click per dettaglio
        tableOfferte.setRowFactory(tv -> {
            TableRow<Offerta> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Offerta offerta = row.getItem();
                    mostraDettaglioOfferta(offerta);
                }
            });
            return row;
        });

        root.getChildren().addAll(lblTitle, filtriBox, tableOfferte);
    }

    private void loadOfferte() {
        try {
            String matricola = controller.getUtenteCorrente().getMatricola();
            List<Offerta> offerte = controller.getOfferteInviateByUtente(matricola);
            tableOfferte.getItems().setAll(offerte);
        } catch (SQLException e) {
            showAlert("Errore caricamento offerte: " + e.getMessage());
        }
    }

    private void loadOfferteConFiltri() {
        try {
            String matricola = controller.getUtenteCorrente().getMatricola();
            List<Offerta> lista = controller.getOfferteInviateByUtente(matricola);

            String tipologia = cbTipologia.getValue();
            String stato = cbStato.getValue();
            String search = tfSearch.getText();

            // Filtro tipologia
            if (tipologia != null && !tipologia.isBlank()) {
                lista = lista.stream()
                        .filter(o -> o.getTipo().equalsIgnoreCase(tipologia))
                        .collect(Collectors.toList());
            }

            // Filtro stato
            if (stato != null && !stato.isBlank()) {
                lista = lista.stream()
                        .filter(o -> o.getStato().equalsIgnoreCase(stato))
                        .collect(Collectors.toList());
            }

            // Filtro ricerca
            if (search != null && !search.isBlank()) {
                String filtro = search.trim().toLowerCase();
                lista = lista.stream()
                        .filter(o -> (o.getCodiceAnnuncio() != null && 
                                     o.getCodiceAnnuncio().toLowerCase().contains(filtro)))
                        .collect(Collectors.toList());
            }

            tableOfferte.getItems().setAll(lista);
        } catch (SQLException e) {
            showAlert("Errore ricerca offerte: " + e.getMessage());
        }
    }

    private void annullaOfferta(Offerta offerta) {
        if (!"inviata".equals(offerta.getStato())) {
            showAlert("Puoi annullare solo offerte ancora in stato 'inviata'.");
            return;
        }

        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION, 
            "Sei sicuro di voler annullare l'offerta " + offerta.getCodiceOfferta() + "?", 
            ButtonType.YES, ButtonType.NO);
        conferma.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    // Cambia stato a "annullata" o elimina completamente
                    boolean ok = controller.eliminaOfferta(offerta.getCodiceOfferta());
                    if (ok) {
                        showAlert("Offerta annullata con successo!");
                        loadOfferteConFiltri(); // Ricarica tabella
                    } else {
                        showAlert("Errore durante l'annullamento dell'offerta.");
                    }
                } catch (Exception e) {
                    showAlert("Errore annullamento offerta: " + e.getMessage());
                }
            }
        });
    }

    private void mostraDettaglioOfferta(Offerta offerta) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Dettaglio Offerta - " + offerta.getCodiceOfferta());

        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setPrefWidth(350);

        Label lblCodice = new Label("Codice Offerta: " + offerta.getCodiceOfferta());
        lblCodice.setStyle("-fx-font-weight: bold;");
        Label lblAnnuncio = new Label("Annuncio: " + offerta.getCodiceAnnuncio());
        Label lblTipo = new Label("Tipologia: " + offerta.getTipo());
        Label lblStato = new Label("Stato: " + offerta.getStato());
        Label lblData = new Label("Data: " + (offerta.getData() != null ? 
            new SimpleDateFormat("dd/MM/yyyy").format(offerta.getData()) : "N/A"));

        box.getChildren().addAll(lblCodice, lblAnnuncio, lblTipo, lblStato, lblData);

        // Prezzo per vendite
        if ("vendita".equals(offerta.getTipo()) && offerta.getPrezzoOfferto() != null) {
            Label lblPrezzo = new Label("Prezzo offerto: €" + offerta.getPrezzoOfferto());
            box.getChildren().add(lblPrezzo);
        }

        // Messaggio per regali
        if (offerta.getMessaggio() != null && !offerta.getMessaggio().isBlank()) {
            Label lblMsgTitle = new Label("Messaggio:");
            lblMsgTitle.setStyle("-fx-font-weight: bold;");
            TextArea taMessaggio = new TextArea(offerta.getMessaggio());
            taMessaggio.setEditable(false);
            taMessaggio.setWrapText(true);
            taMessaggio.setPrefRowCount(3);
            box.getChildren().addAll(lblMsgTitle, taMessaggio);
        }

        Button btnChiudi = new Button("Chiudi");
        btnChiudi.setOnAction(e -> dialog.close());
        box.getChildren().add(btnChiudi);

        dialog.setScene(new Scene(box));
        dialog.showAndWait();
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}
