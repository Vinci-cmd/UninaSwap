package gui;

import Controller.Controller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import model.Annuncio;
import model.Offerta;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportView {

    private VBox root;
    private final Controller controller;

    // UI Elements
    private ComboBox<String> cbPeriodo;
    private PieChart pieChartTipologie;
    private BarChart<String, Number> barChartOfferte;
    private LineChart<String, Number> lineChartAndamento;

    // Statistiche Veloci
    private Label lblTotAnnunciValue;
    private Label lblTotOfferteInviateValue;
    private Label lblTassoSuccessoValue;


    public ReportView(Controller controller) {
        this.controller = controller;
        createUI();
        loadStatistiche();
    }

    private void createUI() {
        root = new VBox(16);
        root.setPadding(new Insets(16));
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);" +
            "-fx-font-family: 'Segoe UI','Roboto','Arial';"
        );

        // Header
        Label title = new Label("Statistiche & Report");
        title.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 20px; -fx-font-weight: 900;");

        // Filtri
        HBox filtriBox = new HBox(10);
        filtriBox.setAlignment(Pos.CENTER_LEFT);
        cbPeriodo = new ComboBox<>();
        cbPeriodo.getItems().addAll("Sempre");
        cbPeriodo.setValue("Sempre");
        styleCombo(cbPeriodo);
        styleComboItems(cbPeriodo);
        
        Button btnAggiorna = primaryButton("Aggiorna", this::loadStatistiche);
        Label lblPeriodo = new Label("Periodo:");
        lblPeriodo.setStyle("-fx-text-fill: #EAF0FF;");
        filtriBox.getChildren().addAll(lblPeriodo, cbPeriodo, btnAggiorna);


        // Statistiche Veloci
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(40);
        statsGrid.setVgap(10);
        
        lblTotAnnunciValue = new Label("0");
        lblTotOfferteInviateValue = new Label("0");
        lblTassoSuccessoValue = new Label("0%");

        statsGrid.add(createStatPane("Annunci Totali", lblTotAnnunciValue), 0, 0);
        statsGrid.add(createStatPane("Offerte Inviate", lblTotOfferteInviateValue), 1, 0);
        statsGrid.add(createStatPane("Tasso Successo Offerte", lblTassoSuccessoValue), 2, 0);

        VBox statsCard = card();
        statsCard.getChildren().add(statsGrid);


        // Grafici
        pieChartTipologie = new PieChart();
        stylePieChart(pieChartTipologie, "Distribuzione Annunci per Tipologia");

        CategoryAxis xAxisBar = new CategoryAxis();
        NumberAxis yAxisBar = new NumberAxis();
        barChartOfferte = new BarChart<>(xAxisBar, yAxisBar);
        styleBarChart(barChartOfferte, "Confronto Offerte Inviate vs Ricevute");

        CategoryAxis xAxisLine = new CategoryAxis();
        NumberAxis yAxisLine = new NumberAxis();
        lineChartAndamento = new LineChart<>(xAxisLine, yAxisLine);
        styleLineChart(lineChartAndamento, "Andamento Annunci/Offerte (Funzionalità da implementare)");


        // Layout grafici
        GridPane chartsGrid = new GridPane();
        chartsGrid.setHgap(16);
        chartsGrid.setVgap(16);

        VBox pieCard = card();
        pieCard.getChildren().add(pieChartTipologie);
        chartsGrid.add(pieCard, 0, 0);

        VBox barCard = card();
        barCard.getChildren().add(barChartOfferte);
        chartsGrid.add(barCard, 1, 0);
        
        VBox lineCard = card();
        lineCard.getChildren().add(lineChartAndamento);
        GridPane.setColumnSpan(lineCard, 2);
        chartsGrid.add(lineCard, 0, 1);


        root.getChildren().addAll(title, filtriBox, statsCard, chartsGrid);
    }

    private void loadStatistiche() {
        try {
            String matricola = controller.getUtenteCorrente().getMatricola();

            // --- Statistiche Veloci ---
            List<Annuncio> annunci = controller.getAnnunciByUtente(matricola);
            List<Offerta> offerteInviate = controller.getOfferteInviateByUtente(matricola);

            lblTotAnnunciValue.setText(String.valueOf(annunci.size()));
            lblTotOfferteInviateValue.setText(String.valueOf(offerteInviate.size()));
            
            long offerteAccettate = offerteInviate.stream().filter(o -> "accettata".equalsIgnoreCase(o.getStato())).count();
            double tassoSuccesso = (offerteInviate.isEmpty()) ? 0.0 : (double) offerteAccettate / offerteInviate.size();
            lblTassoSuccessoValue.setText(String.format("%.1f%%", tassoSuccesso * 100));


            // --- Dati per Grafico a Torta ---
            Map<String, Integer> annunciPerTipo = new HashMap<>();
            for (Annuncio a : annunci) {
                annunciPerTipo.merge(a.getTipologia(), 1, Integer::sum);
            }
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : annunciPerTipo.entrySet()) {
                pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
            pieChartTipologie.setData(pieChartData);


            // --- Dati per Grafico a Barre ---
            List<Offerta> offerteRicevute = controller.getOfferteRicevuteByUtente(matricola);

            XYChart.Series<String, Number> seriesInviate = new XYChart.Series<>();
            seriesInviate.setName("Offerte Inviate");
            seriesInviate.getData().add(new XYChart.Data<>("Totale", offerteInviate.size()));

            XYChart.Series<String, Number> seriesRicevute = new XYChart.Series<>();
            seriesRicevute.setName("Offerte Ricevute");
            seriesRicevute.getData().add(new XYChart.Data<>("Totale", offerteRicevute.size()));
            barChartOfferte.getData().setAll(seriesInviate, seriesRicevute);

            // --- Dati per Grafico a Linee (Placeholder) ---
            lineChartAndamento.getData().clear();

        } catch (SQLException e) {
            warn("Errore nel caricamento delle statistiche: " + e.getMessage());
        }
    }

    private VBox createStatPane(String title, Label valueLabel) {
        VBox pane = new VBox(5);
        pane.setAlignment(Pos.CENTER);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #A8B1C6; -fx-font-size: 12px;");
        valueLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 22px; -fx-font-weight: 700;");
        pane.getChildren().addAll(titleLabel, valueLabel);
        return pane;
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
        return b;
    }
    
    private void styleCombo(ComboBox<?> cb) {
        cb.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-text-fill: #EAF0FF;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 2 4;" +
            "-fx-border-color: transparent;"
        );
    }
    
    private <T> void styleComboItems(ComboBox<T> combo) {
        combo.setButtonCell(new ListCell<>() {
            @Override 
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.valueOf(item));
                setStyle("-fx-text-fill: #EAF0FF; -fx-background-color: transparent;");
            }
        });
    }

    private void stylePieChart(PieChart chart, String title) {
        chart.setTitle(title);
        chart.setLabelLineLength(20);
        chart.setLegendVisible(true);
        chart.setStyle(
            "-fx-background-color: transparent;" +
            ".chart-title { -fx-text-fill: #EAF0FF; -fx-font-size: 14px; -fx-font-weight: bold; }" +
            ".chart-pie-label { -fx-fill: #EAF0FF; -fx-font-size: 11px; }" +
            ".chart-legend { -fx-background-color: transparent; }"
        );
    }

    private void styleBarChart(BarChart<String, Number> chart, String title) {
        chart.setTitle(title);
        chart.setLegendVisible(true);
        chart.setStyle(
            "-fx-background-color: transparent;" +
            ".chart-title { -fx-text-fill: #EAF0FF; -fx-font-size: 14px; -fx-font-weight: bold; }" +
            ".chart-legend { -fx-background-color: transparent; }"
        );
        chart.getXAxis().setStyle("-fx-tick-label-fill: #A8B1C6;");
        chart.getYAxis().setStyle("-fx-tick-label-fill: #A8B1C6;");
    }
    
    private void styleLineChart(LineChart<String, Number> chart, String title) {
        chart.setTitle(title);
        chart.setLegendVisible(true);
        chart.setStyle(
            "-fx-background-color: transparent;" +
            ".chart-title { -fx-text-fill: #EAF0FF; -fx-font-size: 14px; -fx-font-weight: bold; }" +
            ".chart-legend { -fx-background-color: transparent; }"
        );
        chart.getXAxis().setStyle("-fx-tick-label-fill: #A8B1C6;");
        chart.getYAxis().setStyle("-fx-tick-label-fill: #A8B1C6;");
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