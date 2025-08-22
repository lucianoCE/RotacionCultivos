package rotacionCultivos;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultXYDataset;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import rotacionCultivos.GreedyAgriculturalSolver.Result;
import rotacionCultivos.Main.AgriculturalData;

import java.io.*;
import java.util.*;

public class PlotParetoFromCSV {

    public static void main(String[] args) {
        // Input CSVs and their English output names
        Map<String, String> fileMapping = new LinkedHashMap<>();
        fileMapping.put("pareto_instanciaChica.csv", "pareto_smallInstance.png");
        fileMapping.put("pareto_instanciaMediana.csv", "pareto_mediumInstance.png");
        fileMapping.put("pareto_instanciaGrande.csv", "pareto_largeInstance.png");

        // Titles for the charts
        Map<String, String> titleMapping = new LinkedHashMap<>();
        titleMapping.put("pareto_instanciaChica.csv", "Pareto Front - Small Instance");
        titleMapping.put("pareto_instanciaMediana.csv", "Pareto Front - Medium Instance");
        titleMapping.put("pareto_instanciaGrande.csv", "Pareto Front - Large Instance");

        String baseDir = "frentes_pareto"; // both input and output folder

        File dir = new File(baseDir);
        if (!dir.exists()) {
            System.err.println("Directory not found: " + baseDir);
            return;
        }
        
        for (String csvFile : fileMapping.keySet()) {
        	String xmlFileName = csvFile
                    .replace("pareto_", "") 
                    .replace(".csv", ".xml");
        	String directorioInstancias = "src/main/resources/instancias/";
    		String xmlFilePath = directorioInstancias + xmlFileName;
    		AgriculturalData data = Main.readDataFromXML(xmlFilePath);
    		System.out.println(data.cantCultivos);
    		
            File inputFile = new File(baseDir, csvFile);
            File outputFile = new File(baseDir, fileMapping.get(csvFile));
            String chartTitle = titleMapping.get(csvFile);

            double[][] puntos = leerCSV(inputFile);

            DefaultXYDataset dataset = new DefaultXYDataset();
            dataset.addSeries("Solutions", puntos);

            // Run greedy solvers
            GreedyAgriculturalSolver solverDiversity = new GreedyAgriculturalSolver(
                    data.cantParcelas, data.cantSemestres,
                    data.cantCultivos, data.areaParcelas, data.rendimientoCultivoChico, 
                    data.rendimientoCultivoMediano, data.rendimientoCultivoGrande,
                    data.precioCultivo, data.costoMantCultivo, data.temporadaCultivo,
                    "diversidad");
            Result cropPlanDiversity = solverDiversity.solve();

            GreedyAgriculturalSolver solverProfit = new GreedyAgriculturalSolver(
                    data.cantParcelas, data.cantSemestres,
                    data.cantCultivos, data.areaParcelas, data.rendimientoCultivoChico, 
                    data.rendimientoCultivoMediano, data.rendimientoCultivoGrande,
                    data.precioCultivo, data.costoMantCultivo, data.temporadaCultivo,
                    "ganancia");
            Result cropPlanProfit = solverProfit.solve();
            
            AgriculturalOptimizationProblem problem = new AgriculturalOptimizationProblem(data.cantParcelas, data.cantFilas,
    				data.cantSemestres, data.cantCultivos, data.areaParcelas, data.rendimientoCultivoChico,
    				data.rendimientoCultivoMediano, data.rendimientoCultivoGrande, data.precioCultivo,
    				data.costoMantCultivo, data.temporadaCultivo);
            
            List<IntegerSolution> greedyProfitResult = solverProfit.initializePopulation(problem, cropPlanProfit.cropPlan,
    				cropPlanProfit.totalProfit, cropPlanProfit.diversityScore);

    		List<IntegerSolution> greedyDiversityResult = solverDiversity.initializePopulation(problem,
    				cropPlanDiversity.cropPlan, cropPlanDiversity.totalProfit, cropPlanDiversity.diversityScore);

            // Add diversity greedy solution
            double[][] diversityPoint = new double[2][1];
            diversityPoint[0][0] = -greedyDiversityResult.get(0).getObjective(0); // Profit
            diversityPoint[1][0] = -greedyDiversityResult.get(0).getObjective(1); // Diversity
            dataset.addSeries("Greedy Diversity", diversityPoint);
            System.out.println("Greedy-Diversity point: (" + diversityPoint[0][0] + ", " + diversityPoint[1][0] + ")");

            // Add profit greedy solution
            double[][] profitPoint = new double[2][1];
            profitPoint[0][0] = -greedyProfitResult.get(0).getObjective(0);
            profitPoint[1][0] = -greedyProfitResult.get(0).getObjective(1);
            dataset.addSeries("Greedy Profit", profitPoint);
            System.out.println("Greedy-Profit point: (" + profitPoint[0][0] + ", " + profitPoint[1][0] + ")");

            JFreeChart chart = ChartFactory.createScatterPlot(
                    chartTitle,
                    "Profit",
                    "Diversity",
                    dataset
            );

            try {
                ChartUtils.saveChartAsPNG(outputFile, chart, 800, 600);
                System.out.println("Saved: " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static double[][] leerCSV(File file) {
        List<Double> xValues = new ArrayList<>();
        List<Double> yValues = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                try {
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    xValues.add(x);
                    yValues.add(y);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double[][] puntos = new double[2][xValues.size()];
        for (int i = 0; i < xValues.size(); i++) {
            puntos[0][i] = - xValues.get(i);
            puntos[1][i] = - yValues.get(i);
        }

        return puntos;
    }
}
