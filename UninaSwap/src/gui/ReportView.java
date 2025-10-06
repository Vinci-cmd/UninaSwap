package gui;

import Controller.Controller;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.SwingUtilities;
import java.awt.*;

public class ReportView {

    private Stage stage;
    private Controller controller;

    public ReportView(Controller controller) {
        this.controller = controller;
        createAndShow();
    }

    private void createAndShow() {
        stage = new Stage();
        BorderPane root = new BorderPane();

        // Titolo
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("Report Statistiche UninaSwap");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 10px; -fx-alignment: center;");

        // Creiamo dataset statistiche
        DefaultCategoryDataset dataset = createDataset();

        // SwingNode per mostrare grafico Swing integrato in JavaFX
        SwingNode swingNode = new SwingNode();
        createSwingContent(swingNode, dataset);

        // Layout centrale con padding
        VBox centerBox = new VBox(10);
        centerBox.setPadding(new Insets(10));
        centerBox.getChildren().addAll(titleLabel, swingNode);

        root.setCenter(centerBox);

        // Bottoni nella parte inferiore
        Button closeBtn = new Button("Chiudi");
        closeBtn.setStyle("-fx-pref-width: 100px; -fx-pref-height: 35px;");
        closeBtn.setOnAction(e -> stage.close());

        Button refreshBtn = new Button("Aggiorna");
        refreshBtn.setStyle("-fx-pref-width: 100px; -fx-pref-height: 35px;");
        refreshBtn.setOnAction(e -> {
            // Ricarica i dati e aggiorna il grafico
            DefaultCategoryDataset newDataset = createDataset();
            createSwingContent(swingNode, newDataset);
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setStyle("-fx-padding: 15; -fx-alignment: center;");
        buttonBox.getChildren().addAll(refreshBtn, closeBtn);
        root.setBottom(buttonBox);

        Scene scene = new Scene(root, 700, 500);
        stage.setScene(scene);
        stage.setTitle("Report Statistiche UninaSwap");
        stage.setResizable(true);
        stage.show();
    }

    // Crea contenuto Swing da inserire in SwingNode
    private void createSwingContent(final SwingNode swingNode, DefaultCategoryDataset dataset) {
        SwingUtilities.invokeLater(() -> {
            JFreeChart chart = ChartFactory.createBarChart(
                    "Statistiche Offerte per Tipologia",
                    "Tipologia",
                    "Numero offerte",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false);

            // Personalizzazione del grafico
            customizeChart(chart);

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(650, 400));
            chartPanel.setMouseWheelEnabled(true);
            swingNode.setContent(chartPanel);
        });
    }

    // Personalizza l'aspetto del grafico
    private void customizeChart(JFreeChart chart) {
        CategoryPlot plot = chart.getCategoryPlot();
        
        // Colori personalizzati per le barre
        BarRenderer renderer = new BarRenderer();
        renderer.setSeriesPaint(0, new Color(52, 152, 219)); // Blu per "Offerte"
        renderer.setSeriesPaint(1, new Color(46, 204, 113)); // Verde per "Accettate"
        
        plot.setRenderer(renderer);
        
        // Personalizzazione colori sfondo
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // Font titolo
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        
        // Font assi
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    // Prepara dataset dalle statistiche del controller
    private DefaultCategoryDataset createDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        Controller.ReportData data = controller.getReportData();

        if (data != null) {
            // Serie "Offerte" - totale offerte per tipologia
            dataset.addValue(data.offerteVendita, "Offerte", "Vendita");
            dataset.addValue(data.offerteScambio, "Offerte", "Scambio");
            dataset.addValue(data.offerteRegalo, "Offerte", "Regalo");

            // Serie "Accettate" - offerte accettate per tipologia
            dataset.addValue(data.accettateVendita, "Accettate", "Vendita");
            dataset.addValue(data.accettateScambio, "Accettate", "Scambio");
            dataset.addValue(data.accettateRegalo, "Accettate", "Regalo");
        } else {
            // Dati di esempio se getReportData() ritorna null
            dataset.addValue(0, "Offerte", "Vendita");
            dataset.addValue(0, "Offerte", "Scambio");
            dataset.addValue(0, "Offerte", "Regalo");
            dataset.addValue(0, "Accettate", "Vendita");
            dataset.addValue(0, "Accettate", "Scambio");
            dataset.addValue(0, "Accettate", "Regalo");
        }

        return dataset;
    }
}
