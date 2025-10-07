package gui;

import Controller.Controller;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** ReportView embeddabile: si aggiorna automaticamente e mostra un loader durante il fetch. */
public class ReportView {

    private final Controller controller;

    // root e contenitori
    private final BorderPane root = new BorderPane();
    private StackPane chartHost;
    private ProgressIndicator loader;

    // UI numeri
    private Label lblTotaleOfferte;
    private Label lblVenditaTot, lblScambioTot, lblVenditaAcc, lblScambioAcc;

    // Tipologie
    private final String[] TIPI = new String[]{"vendita", "scambio"};

    // dati
    private final Map<String, Integer> totaliPerTipo = new LinkedHashMap<>();
    private final Map<String, Integer> accettatePerTipo = new LinkedHashMap<>();

    // grafico
    private DefaultCategoryDataset dataset;
    private JFreeChart chart;
    private ChartPanel chartPanel;

    public ReportView(Controller controller) {
        this.controller = controller;
        buildUI();
        loadDataAsync(); // carica subito

        // quando viene agganciata una Scene, rilancia un ciclo di layout + refresh
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    root.applyCss();
                    root.layout();
                    loadDataAsync();
                });
            }
        });
    }

    public Pane getRoot() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(16));

        // Top
        VBox topBox = new VBox(6);
        Label title = new Label("Statistiche & Report");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        ToolBar toolbar = new ToolBar();
        Button btnExportCsv = new Button("Esporta CSV");
        Button btnSavePng = new Button("Salva Grafico (PNG)");
        toolbar.getItems().addAll(btnExportCsv, btnSavePng);
        topBox.getChildren().addAll(title, toolbar);
        root.setTop(topBox);

        // Left
        VBox left = buildNumbersPane();
        root.setLeft(left);

        // Center: grafico + loader
        SwingNode swingNode = new SwingNode();
        dataset = new DefaultCategoryDataset();
        chart = ChartFactory.createBarChart(
                "Offerte per tipologia",
                "Tipologia",
                "Numero",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(700, 480)); // aiuta layout immediato

        chartHost = new StackPane();
        chartHost.setPadding(new Insets(8));
        SwingUtilities.invokeLater(() -> {
            swingNode.setContent(chartPanel);
            // 👇 forza subito un paint Swing
            chartPanel.revalidate();
            chartPanel.repaint();
        });
        chartHost.getChildren().add(swingNode);

        loader = new ProgressIndicator();
        loader.setMaxSize(90, 90);
        loader.setVisible(false);
        chartHost.getChildren().add(loader);

        root.setCenter(chartHost);

        // actions
        btnExportCsv.setOnAction(e -> exportCsv());
        btnSavePng.setOnAction(e -> saveChartPng());

        // 👇 forzo anche un primo layout lato FX, appena creato il centro
        Platform.runLater(() -> {
            root.applyCss();
            root.layout();
        });
    }

    private VBox buildNumbersPane() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8));
        box.setPrefWidth(320);
        box.setStyle("-fx-background-color: #20202010; -fx-border-color: #cccccc; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label section = new Label("Statistiche numeriche");
        section.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        lblTotaleOfferte = new Label("-");
        lblVenditaTot = new Label("-");
        lblScambioTot = new Label("-");
        lblVenditaAcc = new Label("-");
        lblScambioAcc = new Label("-");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        int r = 0;
        grid.add(new Label("Totale offerte:"), 0, r); grid.add(lblTotaleOfferte, 1, r++);
        grid.add(new Label("Totale VENDITA:"), 0, r); grid.add(lblVenditaTot, 1, r++);
        grid.add(new Label("Accettate VENDITA:"), 0, r); grid.add(lblVenditaAcc, 1, r++);
        grid.add(new Label("Totale SCAMBIO:"), 0, r); grid.add(lblScambioTot, 1, r++);
        grid.add(new Label("Accettate SCAMBIO:"), 0, r); grid.add(lblScambioAcc, 1, r++);

        box.getChildren().addAll(section, new Separator(), grid);
        return box;
    }

    /** Carica i dati in background e aggiorna la UI al termine. */
    private void loadDataAsync() {
        showLoader(true);

        Task<StatsData> task = new Task<>() {
            @Override
            protected StatsData call() {
                StatsData data = new StatsData();
                data.totale = controller.getTotaleOfferte();
                data.totaliPerTipo = new LinkedHashMap<>();
                data.accettatePerTipo = new LinkedHashMap<>();
                for (String t : TIPI) {
                    data.totaliPerTipo.put(t, controller.getTotaleOffertePerTipologia(t));
                    data.accettatePerTipo.put(t, controller.getOfferteAccettatePerTipologia(t));
                }
                return data;
            }
        };

        task.setOnSucceeded(ev -> {
            StatsData d = task.getValue();
            totaliPerTipo.clear(); totaliPerTipo.putAll(d.totaliPerTipo);
            accettatePerTipo.clear(); accettatePerTipo.putAll(d.accettatePerTipo);

            lblTotaleOfferte.setText(String.valueOf(d.totale));
            lblVenditaTot.setText(String.valueOf(totaliPerTipo.getOrDefault("vendita", 0)));
            lblVenditaAcc.setText(String.valueOf(accettatePerTipo.getOrDefault("vendita", 0)));
            lblScambioTot.setText(String.valueOf(totaliPerTipo.getOrDefault("scambio", 0)));
            lblScambioAcc.setText(String.valueOf(accettatePerTipo.getOrDefault("scambio", 0)));

            refreshDataset();
            showLoader(false);

            // 👇 forza layout FX e repaint Swing dopo update dataset
            Platform.runLater(() -> {
                root.applyCss();
                root.layout();
                chartHost.requestLayout();
            });
            SwingUtilities.invokeLater(() -> {
                chartPanel.revalidate();
                chartPanel.repaint();
            });
        });

        task.setOnFailed(ev -> {
            showLoader(false);
            Throwable ex = task.getException();
            showError("Errore caricamento statistiche: " + (ex != null ? ex.getMessage() : "sconosciuto"));
        });

        new Thread(task, "ReportView-Loader").start();
    }

    private void refreshDataset() {
        SwingUtilities.invokeLater(() -> {
            dataset.clear();
            for (Map.Entry<String, Integer> e : totaliPerTipo.entrySet()) {
                dataset.addValue(e.getValue(), "Totali", e.getKey().toUpperCase());
            }
            for (Map.Entry<String, Integer> e : accettatePerTipo.entrySet()) {
                dataset.addValue(e.getValue(), "Accettate", e.getKey().toUpperCase());
            }
            chart.setTitle("Offerte per tipologia (Totali vs Accettate)");
            if (chartPanel != null) {
                chartPanel.revalidate();
                chartPanel.repaint();
            }
        });
    }

    private void exportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Esporta statistiche (CSV)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("report_statistiche.csv");
        Window owner = root.getScene() == null ? null : root.getScene().getWindow();
        File f = fc.showSaveDialog(owner);
        if (f == null) return;

        try (FileWriter w = new FileWriter(f)) {
            w.write("Tipologia,Totali,Accettate\n");
            for (String t : TIPI) {
                int tot = totaliPerTipo.getOrDefault(t, 0);
                int acc = accettatePerTipo.getOrDefault(t, 0);
                w.write(t + "," + tot + "," + acc + "\n");
            }
            int sommaTot = totaliPerTipo.values().stream().mapToInt(Integer::intValue).sum();
            int sommaAcc = accettatePerTipo.values().stream().mapToInt(Integer::intValue).sum();
            w.write("TOTALE," + sommaTot + "," + sommaAcc + "\n");
        } catch (IOException ex) {
            showError("Errore export CSV: " + ex.getMessage());
        }
    }

    private void saveChartPng() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Salva grafico (PNG)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        fc.setInitialFileName("grafico_offerte.png");
        Window owner = root.getScene() == null ? null : root.getScene().getWindow();
        File f = fc.showSaveDialog(owner);
        if (f == null) return;

        try {
            BufferedImage img = chart.createBufferedImage(1200, 700);
            ImageIO.write(img, "png", f);
        } catch (IOException ex) {
            showError("Errore salvataggio PNG: " + ex.getMessage());
        }
    }

    private void showLoader(boolean show) {
        loader.setVisible(show);
        chartHost.setDisable(show);
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Errore");
        a.showAndWait();
    }

    private static class StatsData {
        int totale;
        Map<String, Integer> totaliPerTipo;
        Map<String, Integer> accettatePerTipo;
    }
}
