package gui;

import Controller.Controller;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.animation.PauseTransition;
import model.Oggetto;
import model.Annuncio;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OggettiView {
    private VBox root;
    private final Controller controller;
    private TableView<Oggetto> tableOggetti;
    private ComboBox<String> cbCategoria;
    private ComboBox<String> cbAnnuncio;
    private TextField tfSearch;
    private Set<String> categorieGlobali = Collections.emptySet();
    private final ObservableList<Oggetto> masterData = FXCollections.observableArrayList();
    private FilteredList<Oggetto> filteredData;
    private SortedList<Oggetto> sortedData;
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
    private Label emptyLabel;

    public OggettiView(Controller controller) {
        this.controller = controller;
        createUI();
        loadData();
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
                .map(Annuncio::getCategoria)
                .filter(cat -> cat != null && !cat.isBlank())
                .collect(Collectors.toSet());
        } catch (Exception e) {
             System.err.println("Error loading global categories: " + e.getMessage());
        }

        cbCategoria = new ComboBox<>();
        cbCategoria.setEditable(true);
        cbCategoria.setPromptText("Categoria");
        cbCategoria.getItems().addAll(categorieGlobali);
        styleCombo(cbCategoria);
        cbCategoria.setOnAction(e -> applyFilters());
        cbCategoria.getEditor().setOnAction(e -> applyFilters());


        cbAnnuncio = new ComboBox<>();
        cbAnnuncio.getItems().addAll("Tutti", "Non associato", "Associato");
        cbAnnuncio.setValue("Tutti");
        styleCombo(cbAnnuncio);
        cbAnnuncio.setOnAction(e -> applyFilters());

        tfSearch = styledTextField("Cerca oggetto...");
        searchDebounce.setOnFinished(event -> applyFilters());
        tfSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            searchDebounce.stop();
            searchDebounce.playFromStart();
        });

        Button btnClear = ghostButton("Pulisci", () -> {
            tfSearch.clear();
            cbCategoria.getEditor().clear();
            cbCategoria.setValue(null);
            cbAnnuncio.setValue("Tutti");
            applyFilters();
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

        TableColumn<Oggetto, String> colCatTable = new TableColumn<>("Categoria");
        colCatTable.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCatTable.setPrefWidth(140);

        TableColumn<Oggetto, String> colAnnuncioTable = new TableColumn<>("Annuncio");
        colAnnuncioTable.setCellValueFactory(new PropertyValueFactory<>("codiceAnnuncio"));
        colAnnuncioTable.setCellFactory(tc -> new TableCell<>() {
             @Override
             protected void updateItem(String item, boolean empty) {
                 super.updateItem(item, empty);
                 if (empty || item == null || item.isBlank()) {
                     setText("Non associato");
                     setStyle("-fx-text-fill: #A8B1C6; -fx-alignment: CENTER;");
                 } else {
                     setText(item);
                     setStyle("-fx-text-fill: #7af7c3; -fx-alignment: CENTER;");
                 }
             }
         });
        colAnnuncioTable.setPrefWidth(120);


        tableOggetti.getColumns().addAll(colCodice, colNome, colDescr, colCatTable, colAnnuncioTable);
        tableOggetti.setPrefHeight(300);

        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableOggetti.comparatorProperty());
        tableOggetti.setItems(sortedData);

        tableOggetti.setRowFactory(tv -> {
            TableRow<Oggetto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    openOggettoDialog(row.getItem());
                }
            });
            row.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                     row.setStyle("-fx-background-color: #4f8cff; -fx-border-color: #99b0f7; -fx-border-radius:10; -fx-background-radius:10; -fx-effect:dropshadow(two-pass-box,#0b1020,12,0.5,0,0);");
                } else {
                     row.setStyle(zebraStyle(row.getIndex()));
                }
            });
            row.indexProperty().addListener((obs, oldIndex, newIndex) -> {
                if (!row.isSelected()) {
                    row.setStyle(zebraStyle(newIndex.intValue()));
                }
            });
            row.hoverProperty().addListener((obs, wasHovered, isHovered) -> {
                if (!row.isEmpty() && !row.isSelected()) {
                    row.setStyle(isHovered
                        ? "-fx-background-color: rgba(122,247,195,0.11); -fx-border-radius:10;"
                        : zebraStyle(row.getIndex()));
                }
            });
            Platform.runLater(() -> {
                 if (!row.isSelected()) row.setStyle(zebraStyle(row.getIndex()));
             });
            return row;
        });

        MenuItem miDetail = new MenuItem("Dettaglio/Modifica");
        miDetail.setOnAction(e -> {
            Oggetto a = tableOggetti.getSelectionModel().getSelectedItem();
            if (a != null) openOggettoDialog(a);
        });
        tableOggetti.setContextMenu(new ContextMenu(miDetail));

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_LEFT);

        Button btnAggiungi = primaryButton("Aggiungi", () -> openOggettoDialog(null));
        Button btnElimina = ghostButton("Elimina", () -> {
            Oggetto selected = tableOggetti.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (selected.getCodiceAnnuncio() != null && !selected.getCodiceAnnuncio().isBlank()){
                     showAlert(Alert.AlertType.WARNING, "Impossibile eliminare un oggetto associato a un annuncio. Rimuovi prima l'associazione.");
                     return;
                }
                if (conferma("Sicuro di voler eliminare l'oggetto '" + selected.getNome() + "'?")) {
                    try {
                        controller.eliminaOggetto(selected.getCodiceOggetto());
                        showAlert(Alert.AlertType.INFORMATION, "Oggetto eliminato.");
                        loadData();
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Errore eliminazione: " + ex.getMessage());
                    }
                }
            } else showAlert(Alert.AlertType.WARNING, "Seleziona un oggetto da eliminare!");
        });
        btnBox.getChildren().addAll(btnAggiungi, btnElimina);

        emptyLabel = new Label("Nessun oggetto trovato o corrispondente ai filtri.");
        emptyLabel.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 12px;");
        emptyLabel.visibleProperty().bind(Bindings.isEmpty(tableOggetti.getItems()));
        emptyLabel.managedProperty().bind(emptyLabel.visibleProperty());


        tableCard.getChildren().addAll(tableOggetti, emptyLabel, btnBox);

        root.getChildren().addAll(lblTitolo, filtersCard, tableCard);
    }

    private void loadData() {
        try {
            String matricola = controller.getUtenteCorrente().getMatricola();
            masterData.setAll(controller.getOggettiUtenteObj(matricola));
            Platform.runLater(this::applyFilters);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Errore caricamento oggetti: " + e.getMessage());
            masterData.clear();
        }
    }

    private void applyFilters() {
        String categoria = cbCategoria.getValue();
        String editorText = cbCategoria.getEditor().getText();
        final String categoriaFilter = (categoria != null && !categoria.isBlank()) ? categoria.trim().toLowerCase() :
                                       (editorText != null && !editorText.isBlank()) ? editorText.trim().toLowerCase() : "";

        String search = tfSearch.getText();
        String associazione = cbAnnuncio.getValue();

        final String searchFilter = search == null ? "" : search.trim().toLowerCase();
        final String associazioneFilter = associazione == null ? "Tutti" : associazione;


        filteredData.setPredicate(oggetto -> {
            boolean categoriaMatch = categoriaFilter.isEmpty() ||
                (oggetto.getCategoria() != null && oggetto.getCategoria().toLowerCase().contains(categoriaFilter));

            boolean associazioneMatch = "Tutti".equals(associazioneFilter) ||
                ("Non associato".equals(associazioneFilter) && (oggetto.getCodiceAnnuncio() == null || oggetto.getCodiceAnnuncio().isBlank())) ||
                ("Associato".equals(associazioneFilter) && (oggetto.getCodiceAnnuncio() != null && !oggetto.getCodiceAnnuncio().isBlank()));


            boolean searchMatch = searchFilter.isEmpty() ||
                (oggetto.getNome() != null && oggetto.getNome().toLowerCase().contains(searchFilter)) ||
                (oggetto.getCategoria() != null && oggetto.getCategoria().toLowerCase().contains(searchFilter)) ||
                (oggetto.getDescrizione() != null && oggetto.getDescrizione().toLowerCase().contains(searchFilter)) ||
                (oggetto.getCodiceOggetto() != null && oggetto.getCodiceOggetto().toLowerCase().contains(searchFilter));


            return categoriaMatch && associazioneMatch && searchMatch;
        });
    }

    private boolean conferma(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, messaggio, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.setTitle("Conferma");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private void openOggettoDialog(Oggetto oggetto) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(oggetto == null ? "Nuovo Oggetto" : "Modifica Oggetto");

        VBox box = card();
        box.setSpacing(12);

        boolean isNew = (oggetto == null);

        TextField tfNome = styledTextField("Nome");
        TextArea taDescrizione = styledTextArea("Descrizione");
        taDescrizione.setPrefRowCount(3);
        ComboBox<String> cbCategoriaDialog = new ComboBox<>();
        cbCategoriaDialog.setEditable(true);
        cbCategoriaDialog.setPromptText("Categoria");
        cbCategoriaDialog.getItems().addAll(categorieGlobali);
        styleCombo(cbCategoriaDialog);

        if (!isNew) {
            tfNome.setText(oggetto.getNome());
            taDescrizione.setText(oggetto.getDescrizione());
            cbCategoriaDialog.setValue(oggetto.getCategoria());
        }

        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button btnAnnulla = ghostButton("Annulla", dialog::close);
        Button btnSalva = primaryButton(isNew ? "Salva" : "Aggiorna", () -> {
            String nome = tfNome.getText().trim();
            String descr = taDescrizione.getText().trim();
            String categoria = cbCategoriaDialog.getEditor().getText().trim();
            if (nome.isBlank() || descr.isBlank() || categoria.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Inserisci tutti i campi.");
                return;
            }
            try {
                if (isNew) {
                    Oggetto nuovo = new Oggetto(null, nome, descr, categoria, null);
                    controller.creaOggetto(nuovo);
                } else {
                     Oggetto updatedOggetto = new Oggetto(oggetto.getCodiceOggetto(), nome, descr, categoria, oggetto.getCodiceAnnuncio());
                     controller.modificaOggetto(updatedOggetto);
                }
                dialog.close();
                loadData();
            } catch (Exception ex) {
                 showAlert(Alert.AlertType.ERROR, "Errore salvataggio: " + ex.getMessage());
            }
        });
        btns.getChildren().addAll(btnAnnulla, btnSalva);

        box.getChildren().addAll(
                l("Nome:"), tfNome,
                l("Descrizione:"), taDescrizione,
                l("Categoria:"), cbCategoriaDialog
        );

         if (!isNew) {
             Label lblAnnuncio = l("Annuncio collegato: " + (oggetto.getCodiceAnnuncio() != null ? oggetto.getCodiceAnnuncio() : "Non associato"));
             box.getChildren().add(lblAnnuncio);

             if (oggetto.getCodiceAnnuncio() != null && !oggetto.getCodiceAnnuncio().isBlank()) {
                 Button btnDisassocia = ghostButton("Rimuovi associazione", () -> {
                     if (conferma("Sicuro di voler rimuovere l'associazione? L'annuncio '" + oggetto.getCodiceAnnuncio() + "' verrà eliminato.")) {
                         try {
                             String codiceAnnuncioDaEliminare = oggetto.getCodiceAnnuncio();
                             controller.aggiornaCodiceAnnuncioOggetto(oggetto.getCodiceOggetto(), null);
                             controller.eliminaAnnuncio(codiceAnnuncioDaEliminare);
                             showAlert(Alert.AlertType.INFORMATION, "Associazione rimossa e annuncio eliminato.");
                             dialog.close();
                             loadData();
                         } catch (Exception ex) {
                             showAlert(Alert.AlertType.ERROR,"Errore rimozione associazione: " + ex.getMessage());
                         }
                     }
                 });
                 btnDisassocia.setStyle(btnDisassocia.getStyle() + "-fx-text-fill: #ff6b6b; -fx-border-color: #ff6b6b;");
                 box.getChildren().add(btnDisassocia);
             }
         }

        box.getChildren().add(btns);

        StackPane wrap = new StackPane(box);
        wrap.setPadding(new Insets(12));
        wrap.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);");

        dialog.setScene(new Scene(wrap, 420, isNew ? 400 : 380));
        dialog.showAndWait();
    }


    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(type == Alert.AlertType.ERROR ? "Errore" : type == Alert.AlertType.WARNING ? "Attenzione" : "Info");
        a.showAndWait();
    }

    private String zebraStyle(int idx) {
        if (idx < 0) return "-fx-background-color: rgba(255,255,255,0.03);";
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
        ta.setWrapText(true);
        ta.setStyle(
             "-fx-control-inner-background: rgba(255,255,255,0.08);" +
             "-fx-background-color: rgba(255,255,255,0.10);" +
             "-fx-text-fill: #EAF0FF;" +
             "-fx-background-radius: 12;" +
             "-fx-border-radius: 12;" +
             "-fx-padding: 10 12;" +
             "-fx-prompt-text-fill: rgba(234,240,255,0.45);" +
             "-fx-border-color: transparent;" +
             "-fx-focus-color: transparent;" +
             "-fx-faint-focus-color: transparent;"
        );
        Platform.runLater(() -> {
            Node verticalBar = ta.lookup(".scroll-bar:vertical");
            if(verticalBar != null) {
                verticalBar.setStyle("-fx-background-color: transparent;");
                Node thumb = verticalBar.lookup(".thumb");
                if (thumb != null) thumb.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 6;");
            }
            Node horizontalBar = ta.lookup(".scroll-bar:horizontal");
            if(horizontalBar != null) {
                horizontalBar.setStyle("-fx-background-color: transparent;");
                Node thumb = horizontalBar.lookup(".thumb");
                if (thumb != null) thumb.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 6;");
            }
        });
        return ta;
    }

    private <T> void styleCombo(ComboBox<T> cb) {
         cb.setStyle(
             "-fx-background-color: rgba(255,255,255,0.10);" +
             "-fx-text-fill: #EAF0FF;" +
             "-fx-background-radius: 12;" +
             "-fx-padding: 2 4;" +
             "-fx-border-color: transparent;"
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
                Node arrowButton = cb.lookup(".arrow-button");
                if (arrowButton != null) {
                    arrowButton.setStyle("-fx-background-color: transparent; -fx-padding: 0 4 0 0;");
                    Node arrow = arrowButton.lookup(".arrow");
                     if (arrow != null) {
                          arrow.setStyle("-fx-background-color: #EAF0FF; -fx-shape: \"M 0 0 h 7 l -3.5 4 z\"; -fx-scale-shape: true;");
                     }
                }
            }
        });
        cb.setOnShowing(e -> Platform.runLater(() -> {
            Node popup = cb.lookup(".combo-box-popup");
            if (popup != null) {
                ListView<?> lv = (ListView<?>) popup.lookup(".list-view");
                if (lv != null) {
                     lv.setStyle(
                         "-fx-background-color: rgba(24, 27, 35, 0.98);" +
                         "-fx-background-radius: 12;" +
                         "-fx-border-color: rgba(255, 255, 255, 0.15);" +
                         "-fx-border-width: 1;" +
                         "-fx-border-radius: 12;"
                     );
                    ScrollBar verticalScrollBar = (ScrollBar) lv.lookup(".scroll-bar:vertical");
                    if (verticalScrollBar != null) {
                        verticalScrollBar.setStyle("-fx-background-color: transparent;");
                         Node thumb = verticalScrollBar.lookup(".thumb");
                         if (thumb != null) thumb.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 6;");
                    }
                }
                popup.setEffect(new DropShadow(10, Color.color(0, 0, 0, 0.4)));
            }
        }));
         styleComboItems(cb);
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
    }

    private Button primaryButton(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        final String baseStyle = "-fx-background-radius: 12; -fx-padding: 10 16; -fx-font-weight: 700; -fx-text-fill: white;";
        final String normalStyle = "-fx-background-color: #4f8cff;" + baseStyle;
        final String hoverStyle = "-fx-background-color: #3b6fe0;" + baseStyle;
        b.setStyle(normalStyle);
        b.setOnMouseEntered(e -> b.setStyle(hoverStyle));
        b.setOnMouseExited(e -> b.setStyle(normalStyle));
        return b;
    }

    private Button ghostButton(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        final String baseStyle = "-fx-text-fill: #EAF0FF; -fx-border-color: rgba(255,255,255,0.20); -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10 16; -fx-font-weight: 700;";
        final String normalStyle = "-fx-background-color: transparent;" + baseStyle;
        final String hoverStyle = "-fx-background-color: rgba(255,255,255,0.08);" + baseStyle;
        b.setStyle(normalStyle);
        b.setOnMouseEntered(e -> b.setStyle(hoverStyle));
        b.setOnMouseExited(e -> b.setStyle(normalStyle));
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

    public VBox getRoot() {
        return root;
    }
}