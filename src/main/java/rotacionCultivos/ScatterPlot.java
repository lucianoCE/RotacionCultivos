package rotacionCultivos;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ScatterPlot {
	public static void generateScatterPlot(String csvFilePath, String outputImagePath, double gananciaGreedyG,
			double diversidadGreedyG, double gananciaGreedyD, double diversidadGreedyD) {
		String line;
		String csvSplitBy = ",";
		XYSeries series = new XYSeries("Soluciones");

		try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
			while ((line = br.readLine()) != null) {
				String[] values = line.split(csvSplitBy);

				// Asume que los valores están en las columnas 1 y 2 (index 0 y 1)
				double x = -Double.parseDouble(values[0]); // Ganancia
				double y = -Double.parseDouble(values[1]); // Diversidad

				series.add(x, y);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		XYSeries serieGreedyProfit = new XYSeries("Greedy Profit");
		serieGreedyProfit.add(gananciaGreedyG, diversidadGreedyG);

		XYSeries serieGreedyDiversidad = new XYSeries("Greedy Diversidad");
		serieGreedyDiversidad.add(gananciaGreedyD, diversidadGreedyD);

		// Crear el dataset
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series);
		dataset.addSeries(serieGreedyProfit);
		dataset.addSeries(serieGreedyDiversidad);

		// Crear el gráfico
		JFreeChart scatterPlot = ChartFactory.createScatterPlot("Resultados de Soluciones", "Ganancia", // Eje X
				"Diversidad", // Eje Y
				dataset, PlotOrientation.VERTICAL, true, // Incluye leyenda
				true, // Incluye tooltips
				false // No URLs
		);

		// Configurar el renderer para personalizar los puntos
		XYPlot plot = scatterPlot.getXYPlot();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

		// Deshabilitar las líneas para todas las series
		renderer.setSeriesLinesVisible(0, false);
		renderer.setSeriesLinesVisible(1, false);
		renderer.setSeriesLinesVisible(2, false);

		// Configurar los puntos de las series
		renderer.setSeriesPaint(0, Color.RED); // Color de la serie principal
		renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6)); // Círculos

		renderer.setSeriesPaint(1, Color.MAGENTA); // Color de Greedy Profit
		renderer.setSeriesShape(1, new java.awt.geom.Rectangle2D.Double(-3, -3, 6, 6)); // Cuadrados

		renderer.setSeriesPaint(2, Color.BLUE); // Color de Greedy Diversidad
		renderer.setSeriesShape(2, new java.awt.geom.Rectangle2D.Double(-3, -3, 6, 6)); // Cuadrados

		// Línea vertical para ganancia máxima
		plot.addDomainMarker(new org.jfree.chart.plot.ValueMarker(gananciaGreedyG, Color.MAGENTA, new BasicStroke(2.0f)));

		// Línea vertical para diversidad máxima
		plot.addRangeMarker(new org.jfree.chart.plot.ValueMarker(diversidadGreedyD, Color.BLUE, new BasicStroke(2.0f)));

		plot.setRenderer(renderer);

		// Ajustar el eje X
		double minGanancia = Math.min(dataset.getSeries(0).getMinX(), gananciaGreedyD);
		double maxGanancia = Math.max(dataset.getSeries(0).getMaxX(), gananciaGreedyG);
		double rangoGanancia = maxGanancia - minGanancia;
		plot.getDomainAxis().setRange(0, maxGanancia + rangoGanancia * 0.05);

		// Ajustar el eje Y
		double minDiversidad = Math.min(dataset.getSeries(0).getMinY(), diversidadGreedyG);
		double maxDiversidad = Math.max(dataset.getSeries(0).getMaxY(), diversidadGreedyD);
		double rangoDiversidad = maxDiversidad - minDiversidad;
		plot.getRangeAxis().setRange(0, maxDiversidad + rangoDiversidad * 0.05);

		// Guardar el gráfico como imagen
		try {
			ChartUtils.saveChartAsPNG(new File(outputImagePath), scatterPlot, 800, 600);
			System.out.println("Gráfico guardado en: " + outputImagePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
