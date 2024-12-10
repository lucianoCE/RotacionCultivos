package rotacionCultivos;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import rotacionCultivos.GreedyAgriculturalSolver.Result;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main extends AbstractAlgorithmRunner {
	public static void main(String[] args) {
	    String xmlFilePath = "src/main/resources/instancias/instancia_3.xml"; // Ruta del archivo XML
	    AgriculturalData data = readDataFromXML(xmlFilePath);

	    GreedyAgriculturalSolver solverProfit = new GreedyAgriculturalSolver(
	        data.cantParcelas, data.cantTrimestres, data.cantCultivos, data.areaParcelas, 
	        data.rendimientoCultivo, data.precioCultivo, data.costoMantCultivo, "ganancia");
	    Result cropPlanProfit = solverProfit.solve();

	    GreedyAgriculturalSolver solverDiversity = new GreedyAgriculturalSolver(
		        data.cantParcelas, data.cantTrimestres, data.cantCultivos, data.areaParcelas, 
		        data.rendimientoCultivo, data.precioCultivo, data.costoMantCultivo, "diversidad");
	    Result cropPlanDiversity = solverDiversity.solve();

	    // Crear la instancia del problema
	    AgriculturalOptimizationProblem problem = new AgriculturalOptimizationProblem(
	        data.cantParcelas, data.cantTrimestres, data.cantCultivos,
	        data.areaParcelas, data.rendimientoCultivo, data.precioCultivo, data.costoMantCultivo
	    );

	    // Inicializar población Greedy
	    List<IntegerSolution> greedyProfitResult = solverProfit.initializePopulation(problem, cropPlanProfit.cropPlan, cropPlanProfit.totalProfit, cropPlanProfit.diversityScore);
	    
	    List<IntegerSolution> greedyDiversityResult = solverDiversity.initializePopulation(problem, cropPlanDiversity.cropPlan, cropPlanDiversity.totalProfit, cropPlanDiversity.diversityScore);
	    
	    
	    // Definir operadores para NSGA-II
	    double crossoverProbability = 0.9;
	    double mutationProbability = 1.0 / problem.getNumberOfVariables();
	    double distributionIndex = 10.0;

	    CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(crossoverProbability, distributionIndex);
	    MutationOperator<IntegerSolution> mutation = new IntegerPolynomialMutation(mutationProbability, distributionIndex);

	    // Crear y ejecutar NSGA-II
	    Algorithm<List<IntegerSolution>> algorithm = new NSGAIIBuilder<>(
	        problem,
	        crossover,
	        mutation,
	        100 // Tamaño de la población
	    ).setMaxEvaluations(25000)
	     .setSolutionListEvaluator(new SequentialSolutionListEvaluator<>())
	     .build();

	    algorithm.run();

	    List<IntegerSolution> population = algorithm.getResult();

	    // Guardar resultados en archivos separados
	    printFinalSolutionSet(population);
	    
	    System.out.print("Greedy diversidad: ");
	    System.out.println(greedyDiversityResult);
	    saveSolutionsToCSV("greedy_diversity_results.csv", greedyDiversityResult, data);
	    System.out.print("Greedy ganancia: ");
	    System.out.println(greedyProfitResult);
	    saveSolutionsToCSV("greedy_profit_results.csv", greedyProfitResult, data);

	    System.out.println("Resultados guardados en archivos CSV.");
	}

	/**
	 * Guarda las soluciones en un archivo CSV.
	 */
	private static void saveSolutionsToCSV(String fileName, List<IntegerSolution> solutions, AgriculturalData data) {
	    try (FileWriter writer = new FileWriter(fileName, false)) { // 'false' asegura sobrescritura
	        // Escribir encabezado
	        writer.append("Parcela,Trimestre,Cultivo,Ganancia\n");

	        for (IntegerSolution solution : solutions) {
	            double totalProfit = 0;
	            int variableCount = data.cantParcelas * data.cantTrimestres;

	            for (int i = 0; i < variableCount; i++) {
	                int parcela = i / data.cantTrimestres;
	                int trimestre = i % data.cantTrimestres;
	                int cultivo = solution.getVariable(i);

	                // Calcular ganancia
	                double profit = data.areaParcelas[parcela] *
	                                (data.rendimientoCultivo[cultivo] *
	                                (data.precioCultivo[cultivo] - data.costoMantCultivo[cultivo]));
	                totalProfit += profit;

	                // Escribir línea al CSV
	                writer.append(String.format("%d,%d,%d,%.2f\n", parcela, trimestre, cultivo, profit));
	            }

	            // Escribir ganancia total al final de la solución
	            writer.append(String.format(",,Total Profit,%.2f\n", totalProfit));
	        }
	    } catch (IOException e) {
	        System.err.println("Error al escribir el archivo CSV: " + e.getMessage());
	    }
	}


    private static AgriculturalData readDataFromXML(String filePath) {
        try {
            // Crear un documento XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(filePath));

            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();

            // Leer los datos desde el XML
            int cantParcelas = Integer.parseInt(root.getElementsByTagName("cantParcelas").item(0).getTextContent());
            int cantTrimestres = Integer.parseInt(root.getElementsByTagName("cantTrimestres").item(0).getTextContent());
            int cantCultivos = Integer.parseInt(root.getElementsByTagName("cantCultivos").item(0).getTextContent());

            double[] areaParcelas = parseArray(root.getElementsByTagName("areaParcelas").item(0).getTextContent());
            double[] rendimientoCultivo = parseArray(root.getElementsByTagName("rendimientoCultivo").item(0).getTextContent());
            double[] precioCultivo = parseArray(root.getElementsByTagName("precioCultivo").item(0).getTextContent());
            double[] costoMantCultivo = parseArray(root.getElementsByTagName("costoMantCultivo").item(0).getTextContent());

            return new AgriculturalData(cantParcelas, cantTrimestres, cantCultivos, areaParcelas, rendimientoCultivo, precioCultivo, costoMantCultivo);

        } catch (Exception e) {
            throw new RuntimeException("Error leyendo el archivo XML: " + e.getMessage(), e);
        }
    }

    private static double[] parseArray(String data) {
        return Arrays.stream(data.split(","))
                     .map(String::trim)
                     .mapToDouble(Double::parseDouble)
                     .toArray();
    }

    private static class AgriculturalData {
        int cantParcelas;
        int cantTrimestres;
        int cantCultivos;
        double[] areaParcelas;
        double[] rendimientoCultivo;
        double[] precioCultivo;
        double[] costoMantCultivo;

        AgriculturalData(int cantParcelas, int cantTrimestres, int cantCultivos, double[] areaParcelas, double[] rendimientoCultivo, double[] precioCultivo, double[] costoMantCultivo) {
            this.cantParcelas = cantParcelas;
            this.cantTrimestres = cantTrimestres;
            this.cantCultivos = cantCultivos;
            this.areaParcelas = areaParcelas;
            this.rendimientoCultivo = rendimientoCultivo;
            this.precioCultivo = precioCultivo;
            this.costoMantCultivo = costoMantCultivo;
        }
    }
}
