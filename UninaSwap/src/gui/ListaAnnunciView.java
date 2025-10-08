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
import model.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.*;
import java.util.function.Predicate;

/**
 * ListaAnnunciView — AGGIORNATA con stile e struttura identica ad AnnunciView
 * - Filtri stabili con FilteredList/SortedList
 * - Ricerca con debounce (200ms) per evitare ricarichi e glitch
 * - Stile UI uniforme: card, bottoni, campi input identici
 * - Layout e colori coordinati con AnnunciView
 */
public class ListaAnnunciView {

    private VBox root;
    private final Controller controller;

    // Dati
    private final ObservableList<Annuncio> masterData = FXCollections.observableArrayList();
    private FilteredList<Annuncio> filtered;
    private SortedList<Annuncio> sorted;

    // UI
    private TableView<Annuncio> tableAnnunci;
    private ComboBox<String> cbCategoria;
    private ComboBox<String> cbTipologia;
    private TextField tfPrezzoMax;
    private TextField txtSearch;
    private Label emptyLabel;

    // Filtri
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(200));
    private List<String> categorie; // snapshot per i filtri

    public ListaAnnunciView(Controller controller) {
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
        Label title = new Label("Annunci disponibili");
        title.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 20px; -fx-font-weight: 900;");
        Label subtitle = new Label("Cerca opportunità di scambio o acquisto tra studenti.");
        subtitle.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 13px; -fx-font-weight: 600;");
        VBox header = new VBox(title, subtitle);
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

        // Categoria
        cbCategoria = new ComboBox<>();
        cbCategoria.setPromptText("Tutte le categorie");
        cbCategoria.setValue(null); // null = tutte
        styleCombo(cbCategoria);
        styleComboItems(cbCategoria);
        cbCategoria.setOnAction(e -> applyFilters());

        // Prezzo Max (specifico per ListaAnnunciView)
        tfPrezzoMax = styledTextField("Prezzo max");
        tfPrezzoMax.textProperty().addListener((obs, o, n) -> {
            searchDebounce.stop();
            searchDebounce.setOnFinished(ev -> applyFilters());
            searchDebounce.playFromStart();
        });

        // Search
        txtSearch = styledTextField("Cerca per testo o codice…");
        txtSearch.textProperty().addListener((obs, o, n) -> {
            searchDebounce.stop();
            searchDebounce.setOnFinished(ev -> applyFilters());
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

        // ===== Card Tabella =====
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
                setText(empty || value == null ? "" : String.format("€ %.2f", value));
                setStyle("-fx-text-fill: #7af7c3; -fx-font-weight:900; -fx-alignment:CENTER_RIGHT; -fx-padding:0 7 0 0;");
            }
        });

        TableColumn<Annuncio, String> colStato = new TableColumn<>("Stato");
        colStato.setCellValueFactory(new PropertyValueFactory<>("stato"));
        colStato.setPrefWidth(120);
        colStato.setCellFactory(tc -> badgeCell());

        tableAnnunci.getColumns().setAll(colCodice, colCategoria, colTipologia, colDescrizione, colPrezzo, colStato);
        tableAnnunci.setPrefHeight(440);

        // Doppio click = Dettaglio
        tableAnnunci.setRowFactory(tv -> {
            TableRow<Annuncio> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
                    mostraDettaglioAnnuncio(row.getItem());
                }
            });
            // zebra + hover soft + selezione (stile AnnunciView)
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
        MenuItem miDetail = new MenuItem("Dettaglio");
        miDetail.setOnAction(e -> { 
            Annuncio a = tableAnnunci.getSelectionModel().getSelectedItem(); 
            if (a!=null) mostraDettaglioAnnuncio(a); 
        });
        MenuItem miOffer = new MenuItem("Invia Offerta");
        miOffer.setOnAction(e -> { 
            Annuncio a = tableAnnunci.getSelectionModel().getSelectedItem(); 
            if (a!=null) openDialogInvioOfferta(a); 
        });
        tableAnnunci.setContextMenu(new ContextMenu(miDetail, miOffer));

        // Actions bottom (stile AnnunciView)
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

        // Empty label
        emptyLabel = new Label("Nessun annuncio corrisponde ai filtri.");
        emptyLabel.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 12px;");
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        tableCard.getChildren().addAll(tableAnnunci, emptyLabel, actions);

        root.getChildren().addAll(header, filtersCard, tableCard);
    }

    // ============================== DATA ==============================
    private void reloadData() {
        try {
            masterData.clear();
            String matricolaUtente = controller.getUtenteCorrente().getMatricola();
            List<Annuncio> lista = controller.getAnnunciAttiviRaw().stream()
                    .filter(a -> !a.getMatricola().equals(matricolaUtente))
                    .collect(Collectors.toList());
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
                sorted.comparatorProperty().bind(tableAnnunci.comparatorProperty());
                tableAnnunci.setItems(sorted);
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
        final String prezzoStr = Optional.ofNullable(tfPrezzoMax.getText()).orElse("").trim();
        final String query = Optional.ofNullable(txtSearch.getText()).orElse("").trim().toLowerCase();

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
            // prezzo max
            if (!prezzoStr.isBlank()) {
                try {
                    double maxPrice = Double.parseDouble(prezzoStr.replace(",", "."));
                    if (a.getPrezzo() == null || a.getPrezzo() > maxPrice) return false;
                } catch (NumberFormatException ignored) {
                    return false; // prezzo non valido = nascondi tutto
                }
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
        
        Label l5 = l("Prezzo: " + (annuncio.getPrezzo() != null ? ("€ " + annuncio.getPrezzo()) : "N/A"));
        Label l6 = l("Proprietario: " + proprietarioStr);
        Label l7 = l("Data pubblicazione: " + (annuncio.getDataPubblicazione() != null ? annuncio.getDataPubblicazione().toString() : ""));
        Label l8 = l("Descrizione:");

        TextArea ta = styledTextArea("Descrizione...");
        ta.setText(annuncio.getDescrizione());
        ta.setEditable(false);
        ta.setWrapText(true);
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

        TextField tfPrezzo = styledTextField("Prezzo offerto");
        TextArea taMessaggio = styledTextArea("Motiva la tua richiesta...");
        taMessaggio.setPrefRowCount(3);

        HBox btns = new HBox(10); 
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button annulla = ghostButton("Annulla", dialog::close);
        Button conferma = primaryButton("Invia offerta", () -> {
            try {
                switch (annuncio.getTipologia()) {
                    case "vendita":
                        if (tfPrezzo.getText().isBlank()) {
                            warn("Inserisci il prezzo offerto.");
                            return;
                        }
                        double prezzo = Double.parseDouble(tfPrezzo.getText().replace(",", "."));
                        controller.inviaOfferta(annuncio.getCodiceAnnuncio(), "vendita", prezzo);
                        break;
                    case "regalo":
                        if (taMessaggio.getText().isBlank()) {
                            warn("Inserisci un messaggio motivazionale.");
                            return;
                        }
                        controller.inviaOfferta(annuncio.getCodiceAnnuncio(), "regalo", null, taMessaggio.getText());
                        break;
                    case "scambio":
                        // Logica scambio complessa rimane uguale
                        warn("Scambio non implementato in questa versione semplificata.");
                        return;
                }
                dialog.close();
                warn("Offerta inviata!");
            } catch (Exception ex) {
                warn("Errore invio offerta: " + ex.getMessage());
            }
        });

        switch (annuncio.getTipologia()) {
            case "vendita":
                Label prezzoRichiesto = l("Prezzo richiesto: €" + annuncio.getPrezzo());
                prezzoRichiesto.setStyle("-fx-text-fill: #7af7c3; -fx-font-size: 14px; -fx-font-weight: 800;");
                card.getChildren().addAll(tipoLabel, prezzoRichiesto, tfPrezzo);
                break;
            case "regalo":
                Label motivazione = l("Scrivi un messaggio motivazionale:");
                card.getChildren().addAll(tipoLabel, motivazione, taMessaggio);
                break;
             // CORREZIONE 4: Implementazione completa dello scambio - Sostituisci tutto il case "scambio" con questo:

            case "scambio":
                // FIXED: Ripristinata implementazione completa dello scambio
                String matricolaUtente = controller.getUtenteCorrente().getMatricola();
                List<Oggetto> oggettiPersonali;
                try {
                    oggettiPersonali = controller.getOggettiUtenteObj(matricolaUtente)
                            .stream()
                            .filter(o -> o.getCodiceAnnuncio() == null)
                            .collect(Collectors.toList());
                } catch (Exception ex) {
                    warn("Errore caricamento oggetti: " + ex.getMessage());
                    return;
                }
                
                if (oggettiPersonali.isEmpty()) {
                    warn("Non hai oggetti disponibili per lo scambio!");
                    return;
                }
                
                // Dialog per selezione oggetti
                Stage scambioDialog = new Stage();
                scambioDialog.initModality(Modality.APPLICATION_MODAL);
                scambioDialog.setTitle("Seleziona oggetti per lo scambio");
                
                VBox scambioCard = card();
                scambioCard.setSpacing(12);
                
                Label lblSelect = l("Scegli i tuoi oggetti da proporre nello scambio:");
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
                        setStyle(empty ? "" : "-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: #EAF0FF; -fx-padding: 8;");
                    }
                });
                listOggetti.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.08);" +
                    "-fx-control-inner-background: rgba(255,255,255,0.08);" +
                    "-fx-border-radius: 12;" +
                    "-fx-background-radius: 12;"
                );
                listOggetti.setPrefHeight(200);
                
                HBox scambioBtns = new HBox(10);
                scambioBtns.setAlignment(Pos.CENTER_RIGHT);
                Button scambioAnnulla = ghostButton("Annulla", scambioDialog::close);
                Button scambioConferma = primaryButton("Invia offerta di scambio", () -> {
                    List<Oggetto> selezionati = listOggetti.getSelectionModel().getSelectedItems();
                    if (selezionati == null || selezionati.isEmpty()) {
                        warn("Seleziona almeno un oggetto da proporre per lo scambio!");
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
                        scambioDialog.close();
                        dialog.close();
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
                return; // Non chiudere il dialog principale
        }

        btns.getChildren().addAll(annulla, conferma);
        card.getChildren().add(btns);

        StackPane wrap = new StackPane(card);
        wrap.setPadding(new Insets(16));
        wrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);");

        dialog.setScene(new Scene(wrap, 520, 400));
        dialog.showAndWait();
    }

    // ============================== Helpers UI (IDENTICI AD ANNUNCIVIEW) ==============================
 // AGGIUNGI questo metodo ai tuoi helper:
 // SOSTITUISCI il metodo styledTextArea() con questo:
    private TextArea styledTextArea(String prompt) {
        TextArea ta = new TextArea();
        ta.setPromptText(prompt);
        ta.setWrapText(true);

        // Stesso schema del TextField (blu scuro semitrasparente)
        String baseStyle = 
            "-fx-background-color: rgba(255,255,255,0.10);" +  // outer layer semi-trasparente
            "-fx-control-inner-background: rgba(16,20,30,0.35);" + // inner area, leggero scuro
            "-fx-control-inner-background-alt: rgba(16,20,30,0.35);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 10 12;" +
            "-fx-prompt-text-fill: rgba(234,240,255,0.45);" +
            "-fx-border-color: transparent;" +
            "-fx-background-insets: 0;";

        // Variante al focus (leggermente più chiara)
        String focusStyle = 
            "-fx-background-color: rgba(255,255,255,0.15);" +
            "-fx-control-inner-background: rgba(16,20,30,0.4);" +
            "-fx-control-inner-background-alt: rgba(16,20,30,0.4);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 10 12;" +
            "-fx-prompt-text-fill: rgba(234,240,255,0.45);" +
            "-fx-border-color: transparent;" +
            "-fx-background-insets: 0;";

        ta.setStyle(baseStyle);

        ta.focusedProperty().addListener((obs, oldVal, newVal) -> {
            ta.setStyle(newVal ? focusStyle : baseStyle);
        });

        return ta;
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

 // CORREZIONE 2: ComboBox popup scuro - Sostituisci i metodi styleCombo e styleComboItems con questi:

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
     
     if (cb.getEditor() != null) {
         cb.getEditor().setStyle(
             "-fx-background-color: transparent;" +
             "-fx-text-fill: #EAF0FF;" +
             "-fx-prompt-text-fill: rgba(234,240,255,0.45);"
         );
     }

     // AGGIUNTO: Styling per freccia personalizzata
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
         
         // Hover effect per gli items
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

     // Styling del popup ListView
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

    // ============================== TABELLA ==============================
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

    private Label statoBadge(String stato) {
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
        return badge;
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

    public VBox getRoot() {
        return root;
    }
}