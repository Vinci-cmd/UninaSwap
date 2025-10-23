package gui;

import Controller.Controller;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import model.Offerta;

import java.sql.SQLException;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportView {

    private VBox root;
    private final Controller controller;

    private PieChart pieChartOfferteInviate;
    private BarChart<String, Number> barChartOfferteAccettate;
    private Label lblPrezzoMinimo, lblPrezzoMedio, lblPrezzoMassimo;

    public ReportView(Controller controller) {
        this.controller = controller;
        createUI();
        refreshData();
    }

    private void createUI() {
        root = new VBox(16);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b1020, #121a36);-fx-font-family: 'Segoe UI','Roboto','Arial';");

        Label title = new Label("Statistiche & Report");
        title.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 20px; -fx-font-weight: 900;");

        VBox statsCard = card(); statsCard.setSpacing(10);
        Label statsTitle = new Label("Analisi Prezzi (Vendite Accettate)");
        statsTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(40); statsGrid.setVgap(10); statsGrid.setAlignment(Pos.CENTER);

        lblPrezzoMinimo = new Label("N/A");
        lblPrezzoMedio = new Label("N/A");
        lblPrezzoMassimo = new Label("N/A");

        statsGrid.add(createStatPane("Prezzo Minimo", lblPrezzoMinimo), 0, 0);
        statsGrid.add(createStatPane("Prezzo Medio", lblPrezzoMedio), 1, 0);
        statsGrid.add(createStatPane("Prezzo Massimo", lblPrezzoMassimo), 2, 0);
        statsCard.getChildren().addAll(statsTitle, statsGrid);

        pieChartOfferteInviate = new PieChart();
        stylePieChart(pieChartOfferteInviate);
        Label pieTitle = new Label("Offerte Inviate per Tipologia");
        pieTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        VBox pieCard = card(); pieCard.getChildren().add(new VBox(10, pieTitle, pieChartOfferteInviate));

        CategoryAxis xAxisBar = new CategoryAxis();
        NumberAxis yAxisBar = new NumberAxis();
        barChartOfferteAccettate = new BarChart<>(xAxisBar, yAxisBar);
        styleBarChart(barChartOfferteAccettate);
        Label barTitle = new Label("Offerte Inviate (Accettate per Tipo)");
        barTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        VBox barCard = card(); barCard.getChildren().add(new VBox(10, barTitle, barChartOfferteAccettate));

        GridPane chartsGrid = new GridPane();
        chartsGrid.setHgap(16); chartsGrid.setVgap(16);
        chartsGrid.add(pieCard, 0, 0);
        chartsGrid.add(barCard, 1, 0);

        root.getChildren().addAll(title, statsCard, chartsGrid);
    }

    public void refreshData() {
        try {
            String matricola = controller.getUtenteCorrente().getMatricola();

            List<Offerta> offerteInviate = controller.getOfferteInviateByUtente(matricola);
            List<Offerta> offerteRicevute = controller.getOfferteRicevuteByUtente(matricola);

            List<Offerta> venditeAccettate = offerteRicevute.stream()
                .filter(o -> "vendita".equalsIgnoreCase(o.getTipo()) && "accettata".equalsIgnoreCase(o.getStato()) && o.getPrezzoOfferto() != null)
                .collect(Collectors.toList());

            DoubleSummaryStatistics s = venditeAccettate.stream().mapToDouble(Offerta::getPrezzoOfferto).summaryStatistics();
            if (s.getCount() > 0) {
                lblPrezzoMinimo.setText(String.format(Locale.ITALY, "€ %.2f", s.getMin()));
                lblPrezzoMedio.setText(String.format(Locale.ITALY, "€ %.2f", s.getAverage()));
                lblPrezzoMassimo.setText(String.format(Locale.ITALY, "€ %.2f", s.getMax()));
            } else {
                lblPrezzoMinimo.setText("N/A"); lblPrezzoMedio.setText("N/A"); lblPrezzoMassimo.setText("N/A");
            }

            Map<String, Long> inviatePerTipo = offerteInviate.stream().collect(Collectors.groupingBy(Offerta::getTipo, Collectors.counting()));
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            inviatePerTipo.forEach((tipo, count) -> pieData.add(new PieChart.Data(tipo + " (" + count + ")", count)));
            pieChartOfferteInviate.getData().setAll(pieData);

            List<Offerta> inviateAccettate = offerteInviate.stream().filter(o -> "accettata".equalsIgnoreCase(o.getStato())).collect(Collectors.toList());
            Map<String, Long> accettatePerTipo = inviateAccettate.stream().collect(Collectors.groupingBy(Offerta::getTipo, Collectors.counting()));
            XYChart.Series<String, Number> series = new XYChart.Series<>(); series.setName("N. Offerte Accettate");
            accettatePerTipo.forEach((tipo, count) -> series.getData().add(new XYChart.Data<>(tipo, count)));
            barChartOfferteAccettate.getData().setAll(series); barChartOfferteAccettate.setLegendVisible(false);

        } catch (SQLException e) { warn("Errore nel caricamento delle statistiche: " + e.getMessage()); }
    }

    private VBox createStatPane(String title, Label valueLabel) {
        VBox pane = new VBox(5); pane.setAlignment(Pos.CENTER);
        Label t = new Label(title); t.setStyle("-fx-text-fill: #EAF0FF; -fx-font-size: 12px;");
        valueLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 22px; -fx-font-weight: 700;");
        pane.getChildren().addAll(t, valueLabel); return pane;
    }

    private VBox card() {
        VBox card = new VBox(); card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.06);-fx-background-radius: 18;-fx-border-radius: 18;-fx-border-color: rgba(255,255,255,0.10);-fx-border-width: 1;");
        card.setEffect(new DropShadow(24, Color.color(0, 0, 0, 0.45))); return card;
    }

    private void stylePieChart(PieChart chart) {
        chart.setTitle(null); chart.setLabelLineLength(20); chart.setLegendVisible(true); chart.setStyle("-fx-background-color: transparent;");
        chart.sceneProperty().addListener((o, os, ns) -> { if (ns != null) applyPieChartStyles(chart); });
    }

    private void applyPieChartStyles(PieChart chart) {
        Platform.runLater(() -> {
            chart.lookupAll(".chart-pie-label").forEach(n -> n.setStyle("-fx-fill: white; -fx-font-size: 11px;"));
            Node legend = chart.lookup(".chart-legend");
            if (legend != null) {
                legend.setStyle("-fx-background-color: transparent;");
                chart.lookupAll(".chart-legend .label").forEach(l -> l.setStyle("-fx-text-fill: white;"));
            }
        });
    }

    private void styleBarChart(BarChart<String, Number> chart) {
        chart.setTitle(null);
        chart.getXAxis().setStyle("-fx-tick-label-fill: white; -fx-font-size: 10px;");
        chart.getYAxis().setStyle("-fx-tick-label-fill: white;");
        chart.setStyle("-fx-background-color: transparent;");
        chart.sceneProperty().addListener((o, os, ns) -> { if (ns != null) applyBarChartStyles(chart); });
    }

    private void applyBarChartStyles(BarChart<String, Number> chart) {
        Platform.runLater(() -> {
            for (Node n : chart.lookupAll(".chart-bar")) n.setStyle("-fx-bar-fill: #4f8cff;");
            Node legend = chart.lookup(".chart-legend");
            if (legend != null) {
                legend.setStyle("-fx-background-color: transparent;");
                chart.lookupAll(".chart-legend .label").forEach(l -> l.setStyle("-fx-text-fill: white;"));
            }
        });
    }

    public VBox getRoot() { return root; }

    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
}
