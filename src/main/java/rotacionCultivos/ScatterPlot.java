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
	public static void generateScatterPlot(String csvFilePath, String outputImagePath, double gananciaGreedyG, double diversidadGreedyG,
			double gananciaGreedyD, double diversidadGreedyD) {
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
		dataset.addSeries(serieGreedyDiversidad);
		dataset.addSeries(serieGreedyProfit);

		// Crear el gráfico
		JFreeChart scatterPlot = ChartFactory.createScatterPlot("Resultados de Soluciones", "Ganancia", // Eje X
				"Diversidad", // Eje Y
				dataset, PlotOrientation.VERTICAL, true, // Incluye leyenda
				true, // Incluye tooltips
				false // No URLs
		);

		// Configurar el renderer para agregar las líneas de los greedy
		XYPlot plot = scatterPlot.getXYPlot();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

		// Línea vertical para ganancia máxima
		renderer.setSeriesPaint(1, Color.GREEN);
		plot.setDomainGridlinePaint(Color.GREEN);
		plot.addDomainMarker(
				new org.jfree.chart.plot.ValueMarker(gananciaGreedyG, Color.GREEN, new BasicStroke(2.0f)));

		// Línea horizontal para diversidad máxima
		renderer.setSeriesPaint(2, Color.BLUE);
		plot.setRangeGridlinePaint(Color.BLUE);
		plot.addRangeMarker(
				new org.jfree.chart.plot.ValueMarker(diversidadGreedyD, Color.BLUE, new BasicStroke(2.0f)));

		// Ajustar el eje X
		double minGanancia = Math.min(dataset.getSeries(0).getMinX(),gananciaGreedyD);
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
