package gui;

import Controller.Controller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import model.Annuncio;
import model.Offerta;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportView {

    private VBox root;
    private final Controller controller;

    // UI Elements
    private PieChart pieChartTipologie;
    private BarChart<String, Number> barChartOfferte;

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

        // --- Grafico a Torta con Titolo Personalizzato ---
        pieChartTipologie = new PieChart();
        stylePieChart(pieChartTipologie);
        Label pieTitle = new Label("Distribuzione Annunci per Tipologia");
        pieTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        VBox pieChartContainer = new VBox(10, pieTitle, pieChartTipologie);
        pieChartContainer.setAlignment(Pos.TOP_CENTER);
        VBox pieCard = card();
        pieCard.getChildren().add(pieChartContainer);

        // --- Grafico a Barre con Titolo Personalizzato ---
        CategoryAxis xAxisBar = new CategoryAxis();
        NumberAxis yAxisBar = new NumberAxis();
        barChartOfferte = new BarChart<>(xAxisBar, yAxisBar);
        styleBarChart(barChartOfferte);
        Label barTitle = new Label("Confronto Offerte Inviate vs Ricevute");
        barTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        VBox barChartContainer = new VBox(10, barTitle, barChartOfferte);
        barChartContainer.setAlignment(Pos.TOP_CENTER);
        VBox barCard = card();
        barCard.getChildren().add(barChartContainer);

        // --- Layout dei grafici ---
        GridPane chartsGrid = new GridPane();
        chartsGrid.setHgap(16);
        chartsGrid.setVgap(16);
        chartsGrid.add(pieCard, 0, 0);
        chartsGrid.add(barCard, 1, 0);

        root.getChildren().addAll(title, statsCard, chartsGrid);
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
            Map<String, Long> annunciPerTipo = annunci.stream()
                .collect(Collectors.groupingBy(Annuncio::getTipologia, Collectors.counting()));

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            annunciPerTipo.forEach((tipo, count) -> pieChartData.add(new PieChart.Data(tipo, count)));

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

        } catch (SQLException e) {
            warn("Errore nel caricamento delle statistiche: " + e.getMessage());
        }
    }

    private VBox createStatPane(String title, Label valueLabel) {
        VBox pane = new VBox(5);
        pane.setAlignment(Pos.CENTER);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 12px;");
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
        card.setEffect(new DropShadow(24, Color.color(0, 0, 0, 0.45)));
        return card;
    }

    private void stylePieChart(PieChart chart) {
        chart.setTitle(null); // Rimuoviamo il titolo interno
        chart.setLabelLineLength(20);
        chart.setLegendVisible(true);
        chart.setStyle(
            "-fx-background-color: transparent;" +
            ".chart-pie-label { -fx-fill: #EAF0FF; -fx-font-size: 11px; }" +
            ".chart-legend { -fx-background-color: transparent; }" +
            ".chart-legend .label { -fx-text-fill: white; }"
        );
    }

    private void styleBarChart(BarChart<String, Number> chart) {
        chart.setTitle(null); // Rimuoviamo il titolo interno
        chart.setLegendVisible(true);
        chart.getXAxis().setStyle("-fx-tick-label-fill: white;");
        chart.getYAxis().setStyle("-fx-tick-label-fill: white;");
        chart.setStyle(
            "-fx-background-color: transparent;" +
            ".chart-legend { -fx-background-color: transparent; }" +
            ".chart-legend .label { -fx-text-fill: white; }"
        );
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