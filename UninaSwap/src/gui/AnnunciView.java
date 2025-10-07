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
import model.Annuncio;

import java.sql.SQLException;
import java.sql.Date;
import java.util.List;

public class AnnunciView {

    private VBox root;
    private TableView<Annuncio> tableAnnunci;
    private Controller controller;

    public AnnunciView(Controller controller) {
        this.controller = controller;
        createUI();
        loadAnnunci();
    }

    private void createUI() {
        root = new VBox(10);
        root.setPadding(new Insets(12));

        // Pulsante home
        Button btnHome = new Button("⌂");
        btnHome.setOnAction(e -> {
            // Sostituisci con la logica per tornare alla home
            System.out.println("Torna alla home!");
        });

        HBox topBar = new HBox(btnHome);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestisci i tuoi Annunci");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        tableAnnunci = new TableView<>();
        TableColumn<Annuncio, String> colCodice = new TableColumn<>("Codice");
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
        tableAnnunci.setPrefHeight(300);

        Button btnCrea = new Button("Crea");
        Button btnModifica = new Button("Modifica");
        Button btnElimina = new Button("Elimina");
        btnCrea.setOnAction(e -> openAnnuncioDialog(null));
        btnModifica.setOnAction(e -> {
            Annuncio sel = tableAnnunci.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Seleziona un Annuncio");
                return;
            }
            openAnnuncioDialog(sel);
        });
        btnElimina.setOnAction(e -> {
            Annuncio sel = tableAnnunci.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Seleziona un Annuncio");
                return;
            }
            eliminaAnnuncio(sel);
        });
        HBox actions = new HBox(10, btnCrea, btnModifica, btnElimina);
        actions.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(topBar, title, tableAnnunci, actions);
    }

    private void loadAnnunci() {
        try {
            List<Annuncio> annunci = controller.getAnnunciByUtente(controller.getUtenteCorrente().getMatricola());
            tableAnnunci.getItems().setAll(annunci);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Errore caricamento annunci: " + e.getMessage());
        }
    }

    private void openAnnuncioDialog(Annuncio existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Nuovo Annuncio" : "Modifica Annuncio");
        GridPane form = new GridPane();
        form.setPadding(new Insets(12));
        form.setHgap(10);
        form.setVgap(10);
        TextField txtCategoria = new TextField();
        ComboBox<String> cbTipologia = new ComboBox<>();
        cbTipologia.getItems().addAll("vendita", "scambio", "regalo");
        TextField txtDescrizione = new TextField();
        TextField txtPrezzo = new TextField();
        Label lblPrezzo = new Label("Prezzo:");
        HBox prezzoBox = new HBox(5, lblPrezzo, txtPrezzo);
        prezzoBox.setAlignment(Pos.CENTER_LEFT);

        // Stato solo per modifica
        ComboBox<String> cbStato = new ComboBox<>();
        cbStato.getItems().addAll("attivo", "scaduto", "in attesa");

        if (existing != null) {
            txtCategoria.setText(existing.getCategoria());
            cbTipologia.setValue(existing.getTipologia());
            txtDescrizione.setText(existing.getDescrizione());
            txtPrezzo.setText(existing.getPrezzo() != null ? existing.getPrezzo().toString() : "");
            cbStato.setValue(existing.getStato());
        }

        prezzoBox.setVisible("vendita".equals(cbTipologia.getValue()));
        cbTipologia.setOnAction(e -> prezzoBox.setVisible("vendita".equals(cbTipologia.getValue())));

        form.addRow(0, new Label("Categoria:"), txtCategoria);
        form.addRow(1, new Label("Tipologia:"), cbTipologia);
        form.addRow(2, new Label("Descrizione:"), txtDescrizione);
        form.add(prezzoBox, 1, 3);

        if (existing != null) {
            form.addRow(4, new Label("Stato:"), cbStato);
        }

        Button btnConferma = new Button(existing == null ? "Crea" : "Aggiorna");
        btnConferma.setOnAction(e -> {
            if (!validateForm(txtCategoria, cbTipologia, txtDescrizione, txtPrezzo, cbStato, existing != null)) return;
            try {
                boolean ok;
                if (existing == null) {
                    ok = controller.creaAnnuncio(
                        txtCategoria.getText(),
                        cbTipologia.getValue(),
                        txtDescrizione.getText(),
                        "vendita".equals(cbTipologia.getValue()) ? Double.parseDouble(txtPrezzo.getText()) : 0.0
                    );
                } else {
                    ok = controller.modificaAnnuncio(
                        existing.getCodiceAnnuncio(),
                        txtCategoria.getText(),
                        cbTipologia.getValue(),
                        txtDescrizione.getText(),
                        "vendita".equals(cbTipologia.getValue()) ? Double.parseDouble(txtPrezzo.getText()) : 0.0,
                        cbStato.getValue()
                    );
                }
                if (!ok) {
                    showAlert("Operazione non riuscita");
                    return;
                }
                dialog.close();
                loadAnnunci(); // <-- aggiorna dopo conferma
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Errore salvataggio: " + ex.getMessage());
            }
        });

        VBox box = new VBox(12, form, btnConferma);
        box.setPadding(new Insets(12));
        dialog.setScene(new Scene(box, 450, existing == null ? 260 : 320));
        dialog.showAndWait();
    }

    private boolean validateForm(TextField cat, ComboBox<String> tip, TextField desc, TextField prezzo, ComboBox<String> stato, boolean isEdit) {
        if (cat.getText().isBlank()
            || tip.getValue() == null
            || desc.getText().isBlank()
            || ("vendita".equals(tip.getValue()) && prezzo.getText().isBlank())
            || (isEdit && stato.getValue() == null)) {
            showAlert("Compila tutti i campi obbligatori.");
            return false;
        }
        if ("vendita".equals(tip.getValue())) {
            try {
                Double.parseDouble(prezzo.getText());
            } catch (NumberFormatException e) {
                showAlert("Prezzo non valido.");
                return false;
            }
        }
        return true;
    }

    private void eliminaAnnuncio(Annuncio sel) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Eliminare l'annuncio selezionato?", ButtonType.YES, ButtonType.NO);
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    controller.eliminaAnnuncio(sel.getCodiceAnnuncio());
                    loadAnnunci(); // <-- aggiorna dopo eliminazione
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Errore eliminazione: " + e.getMessage());
                }
            }
        });
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}
