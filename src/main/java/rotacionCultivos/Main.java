package rotacionCultivos;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.JMetalLogger;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main extends AbstractAlgorithmRunner {
	public static void main(String[] args) {
		JMetalLogger.logger.setUseParentHandlers(false);
		String xmlFilePath = "src/main/resources/instancias/instancia_1.xml"; // Ruta del archivo XML
		AgriculturalData data = readDataFromXML(xmlFilePath);

		// Medir tiempo total del algoritmo Greedy (diversidad)
		long startGreedyDiversity = System.nanoTime();
		GreedyAgriculturalSolver solverDiversity = new GreedyAgriculturalSolver(data.cantParcelas, data.cantSemestres,
				data.cantCultivos, data.areaParcelas, data.rendimientoCultivoChico, data.rendimientoCultivoMediano,
				data.rendimientoCultivoGrande, data.precioCultivo, data.costoMantCultivo, data.temporadaCultivo,
				"diversidad");
		Result cropPlanDiversity = solverDiversity.solve();
		long endGreedyDiversity = System.nanoTime();

		// Medir tiempo total del algoritmo Greedy (ganancia)
		long startGreedyProfit = System.nanoTime();
		GreedyAgriculturalSolver solverProfit = new GreedyAgriculturalSolver(data.cantParcelas, data.cantSemestres,
				data.cantCultivos, data.areaParcelas, data.rendimientoCultivoChico, data.rendimientoCultivoMediano,
				data.rendimientoCultivoGrande, data.precioCultivo, data.costoMantCultivo, data.temporadaCultivo,
				"ganancia");
		Result cropPlanProfit = solverProfit.solve();
		long endGreedyProfit = System.nanoTime();

		// Crear la instancia del problema
		AgriculturalOptimizationProblem problem = new AgriculturalOptimizationProblem(data.cantParcelas, data.cantFilas,
				data.cantSemestres, data.cantCultivos, data.areaParcelas, data.rendimientoCultivoChico,
				data.rendimientoCultivoMediano, data.rendimientoCultivoGrande, data.precioCultivo,
				data.costoMantCultivo, data.temporadaCultivo);

		// Medir tiempo total del algoritmo evolutivo NSGA-II
		long startNSGAII = System.nanoTime();

		double crossoverProbability = 0.5;
		double mutationProbability = 1.0 / problem.getNumberOfVariables();
		double distributionIndex = 10.0;

		CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(crossoverProbability, distributionIndex);
		MutationOperator<IntegerSolution> mutation = new SeasonalIntegerMutation(mutationProbability, data.cantParcelas,
				data.cantFilas, data.temporadaCultivo);

		Algorithm<List<IntegerSolution>> algorithm = new NSGAIIBuilder<>(
				problem,
				crossover, 
				mutation, 
				100 // Tamaño de la población
		).setMaxEvaluations(100000)
		.setSolutionListEvaluator(new SequentialSolutionListEvaluator<>())
		.build();

		algorithm.run();
		long endNSGAII = System.nanoTime();

		List<IntegerSolution> population = algorithm.getResult();

		// Guardar resultados en archivos separados
		printFinalSolutionSet(population);

		List<IntegerSolution> greedyProfitResult = solverProfit.initializePopulation(problem, cropPlanProfit.cropPlan,
				cropPlanProfit.totalProfit, cropPlanProfit.diversityScore);

		List<IntegerSolution> greedyDiversityResult = solverDiversity.initializePopulation(problem,
				cropPlanDiversity.cropPlan, cropPlanDiversity.totalProfit, cropPlanDiversity.diversityScore);

		System.out.println("-> Tiempos de ejecucion:");
		System.out.println(String.format("Tiempo de ejecución Greedy (diversidad): %.2f ms",
				(endGreedyDiversity - startGreedyDiversity) / 1_000_000.0));
		System.out.println(String.format("Tiempo de ejecución Greedy (ganancia): %.2f ms",
				(endGreedyProfit - startGreedyProfit) / 1_000_000.0));
		System.out.println(
				String.format("Tiempo de ejecución NSGA-II: %.2f ms", (endNSGAII - startNSGAII) / 1_000_000.0));

		System.out.println();
		System.out.println("-> Resultados:");

		System.out.println("- Greedy diversidad: ");
		System.out.println("Plan de rotacion: " + greedyDiversityResult.get(0).getVariables());
		System.out.println("Ganacia: " + -greedyDiversityResult.get(0).getObjective(0));
		System.out.println("Indice de diversidad: " + greedyDiversityResult.get(0).getObjective(1));

		System.out.println();

		System.out.println("- Greedy ganancia: ");
		System.out.println("Plan de rotacion: " + greedyProfitResult.get(0).getVariables());
		System.out.println("Ganacia: " + greedyProfitResult.get(0).getObjective(0));
		System.out.println("Indice de diversidad: " + greedyProfitResult.get(0).getObjective(1));

		System.out.println();

		System.out.println("Generando grafico de soluciones...");
		ScatterPlot.generateScatterPlot("FUN.csv", "scatter_plot.png", -greedyProfitResult.get(0).getObjective(0),
				-greedyProfitResult.get(0).getObjective(1), -greedyDiversityResult.get(0).getObjective(0),
				-greedyDiversityResult.get(0).getObjective(1));

		System.out.println("Guardando resultados en archivo excel...");
		// ExcelExporter.saveSolutionsToExcel("AE_results.xlsx",
		// greedyProfitResult.get(0), greedyDiversityResult.get(0), population, data);
		System.out.println("Todos los resutados se guardaron correctamente.");

	}

	/**
	 * Guarda las soluciones en un archivo CSV.
	 */
	@SuppressWarnings("unused")
	private static void saveSolutionsToCSV(String fileName, List<IntegerSolution> solutions, AgriculturalData data) {
		try (FileWriter writer = new FileWriter(fileName, false)) { // 'false' asegura sobrescritura
			// Escribir encabezado
			writer.append("Parcela,Semestre,Cultivo\n");

			for (IntegerSolution solution : solutions) {
				double totalProfit = 0;
				int variableCount = data.cantParcelas * data.cantSemestres;

				for (int i = 0; i < variableCount; i++) {
					int parcela = i / data.cantSemestres;
					int semestre = i % data.cantSemestres;
					int cultivo = solution.getVariable(i);

					// Calcular ganancia
					double area = data.areaParcelas[parcela];
					double rendimiento;
					if (area <= 200.0) {
						rendimiento = data.rendimientoCultivoChico[cultivo];
					} else {
						if (area <= 500.0)
							rendimiento = data.rendimientoCultivoMediano[cultivo];
						else
							rendimiento = data.rendimientoCultivoGrande[cultivo];
					}
					double profit = area
							* (rendimiento * (data.precioCultivo[cultivo] - data.costoMantCultivo[cultivo]));
					totalProfit += profit;

					// Escribir línea al CSV
					writer.append(String.format("%d,%d,%d\n", parcela, semestre, cultivo));
				}

				// Escribir ganancia total al final de la solución
				writer.append(String.format("Total Profit,%.2f\n", totalProfit));
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
			int cantFilas = Integer.parseInt(root.getElementsByTagName("cantFilas").item(0).getTextContent());
			int cantSemestres = Integer.parseInt(root.getElementsByTagName("cantSemestres").item(0).getTextContent());
			int cantCultivos = Integer.parseInt(root.getElementsByTagName("cantCultivos").item(0).getTextContent()) + 1; // Se
																															// agrega
																															// 1
																															// para
																															// contabilizar
																															// la
																															// opción
																															// de
																															// no
																															// plantar,
																															// es
																															// decir:
																															// "Descanso".

			double[] areaParcelas = parseArray(root.getElementsByTagName("areaParcelas").item(0).getTextContent());
			String[] nombreCultivo = Stream.concat(Stream.of("Descanso"), // el primer cultivo (0) es 'Descanso'
					Arrays.stream(
							parseArrayString(root.getElementsByTagName("nombreCultivo").item(0).getTextContent())))
					.toArray(String[]::new);
			double[] rendimientoCultivoChico = Stream.concat(Stream.of(0.0),
					Arrays.stream(
							parseArray(root.getElementsByTagName("rendimientoCultivoChico").item(0).getTextContent()))
							.boxed())
					.mapToDouble(Double::doubleValue).toArray();
			double[] rendimientoCultivoMediano = Stream.concat(Stream.of(0.0),
					Arrays.stream(
							parseArray(root.getElementsByTagName("rendimientoCultivoMediano").item(0).getTextContent()))
							.boxed())
					.mapToDouble(Double::doubleValue).toArray();
			double[] rendimientoCultivoGrande = Stream.concat(Stream.of(0.0),
					Arrays.stream(
							parseArray(root.getElementsByTagName("rendimientoCultivoGrande").item(0).getTextContent()))
							.boxed())
					.mapToDouble(Double::doubleValue).toArray();
			double[] precioCultivo = Stream.concat(Stream.of(0.0), Arrays
					.stream(parseArray(root.getElementsByTagName("precioCultivo").item(0).getTextContent())).boxed())
					.mapToDouble(Double::doubleValue).toArray();
			double[] costoMantCultivo = Stream.concat(Stream.of(0.0), Arrays
					.stream(parseArray(root.getElementsByTagName("costoMantCultivo").item(0).getTextContent())).boxed())
					.mapToDouble(Double::doubleValue).toArray();
			char[] temporadaCultivo = Stream
					.concat(Stream.of('A'),
							new String(parseCharArray(
									root.getElementsByTagName("temporadaCultivo").item(0).getTextContent())).chars()
									.mapToObj(c -> (char) c))
					.map(Object::toString).collect(Collectors.joining()).toCharArray();

			return new AgriculturalData(cantParcelas, cantFilas, cantSemestres, cantCultivos, areaParcelas,
					nombreCultivo, rendimientoCultivoChico, rendimientoCultivoMediano, rendimientoCultivoGrande,
					precioCultivo, costoMantCultivo, temporadaCultivo);

		} catch (Exception e) {
			throw new RuntimeException("Error leyendo el archivo XML: " + e.getMessage(), e);
		}
	}

	private static double[] parseArray(String data) {
		return Arrays.stream(data.split(",")).map(String::trim).mapToDouble(Double::parseDouble).toArray();
	}

	private static char[] parseCharArray(String data) {
		return Arrays.stream(data.split(",")).map(String::trim).map(s -> s.charAt(0))
				.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString().toCharArray();
	}

	private static String[] parseArrayString(String data) {
		return Arrays.stream(data.split(",")).map(String::trim).toArray(String[]::new);
	}

	public static class AgriculturalData {
		int cantParcelas;
		int cantFilas;
		int cantSemestres;
		int cantCultivos;
		double[] areaParcelas;
		String[] nombreCultivo;
		double[] rendimientoCultivoChico;
		double[] rendimientoCultivoMediano;
		double[] rendimientoCultivoGrande;
		double[] precioCultivo;
		double[] costoMantCultivo;
		char[] temporadaCultivo;

		AgriculturalData(int cantParcelas, int cantFilas, int cantSemestres, int cantCultivos, double[] areaParcelas,
				String[] nombreCultivo, double[] rendimientoCultivoChico, double[] rendimientoCultivoMediano,
				double[] rendimientoCultivoGrande, double[] precioCultivo, double[] costoMantCultivo,
				char[] temporadaCultivo) {
			this.cantParcelas = cantParcelas;
			this.cantFilas = cantFilas;
			this.cantSemestres = cantSemestres;
			this.cantCultivos = cantCultivos;
			this.areaParcelas = areaParcelas;
			this.nombreCultivo = nombreCultivo;
			this.rendimientoCultivoChico = rendimientoCultivoChico;
			this.rendimientoCultivoMediano = rendimientoCultivoMediano;
			this.rendimientoCultivoGrande = rendimientoCultivoGrande;
			this.precioCultivo = precioCultivo;
			this.costoMantCultivo = costoMantCultivo;
			this.temporadaCultivo = temporadaCultivo;
		}
	}
}
