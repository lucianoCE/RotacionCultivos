package rotacionCultivos;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

import rotacionCultivos.Main.AgriculturalData;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ParameterTuning {
    
    // Configuraciones a probar
    private static final int[] POPULATION_SIZES = {50, 100, 200};
    private static final int[] MAX_EVALUATIONS = {10000, 25000, 50000};
    private static final double[] CROSSOVER_PROBABILITIES = {0.8, 0.9, 1.0};
    private static final int NUMBER_OF_RUNS = 10;
    
    private static double distIndex = 10.0;

    public static void main(String[] args) {
        // Cargar datos del problema (usar tu código existente de lectura XML)
        AgriculturalData data = Main.readDataFromXML("src/main/resources/instancias/instanciaVariada.xml");
        
        // Crear problema
        AgriculturalOptimizationProblem problem = new AgriculturalOptimizationProblem(data.cantParcelas,
				data.cantFilas, data.cantSemestres, data.cantCultivos, data.areaParcelas,
				data.rendimientoCultivoChico, data.rendimientoCultivoMediano, data.rendimientoCultivoGrande,
				data.precioCultivo, data.costoMantCultivo, data.temporadaCultivo);

        // Archivo para guardar resultados
        try (FileWriter writer = new FileWriter("parameter_tuning_summary.csv")) {
            // Escribir encabezado
            writer.write("PopSize;MaxEvals;CrossProb;AvgProfit;StdProfit;AvgDiversity;StdDiversity;AvgTime;StdTime\n");

            // Probar todas las combinaciones
            for (int popSize : POPULATION_SIZES) {
                for (int maxEvals : MAX_EVALUATIONS) {
                    for (double crossProb : CROSSOVER_PROBABILITIES) {
                            System.out.printf("Testing: Pop=%d, Evals=%d, CrossProb=%.2f\n",
                                           popSize, maxEvals, crossProb);

                            // Ejecutar múltiples veces y recolectar métricas
                            double[] profits = new double[NUMBER_OF_RUNS];
                            double[] diversities = new double[NUMBER_OF_RUNS];
                            long[] executionTimes = new long[NUMBER_OF_RUNS];

                            for (int run = 0; run < NUMBER_OF_RUNS; run++) {
                                Result result = runExperiment(data, problem, popSize, maxEvals, 
                                                               crossProb, distIndex);
                                profits[run] = result.avgProfit;
                                diversities[run] = result.avgDiversity;
                                executionTimes[run] = result.executionTime;
                            }

                            // Calcular estadísticas
                            double avgProfit = calculateAverage(profits);
                            double stdProfit = calculateStandardDeviation(profits, avgProfit);
                            double avgDiversity = calculateAverage(diversities);
                            double stdDiversity = calculateStandardDeviation(diversities, avgDiversity);
                            double avgTime = calculateAverage(executionTimes);
                            double stdTime = calculateStandardDeviation(executionTimes, avgTime);

                            // Guardar resultados agregados
                            writer.write(String.format(Locale.US,
                                "%d;%d;%.2f;%.4f;%.4f;%.4f;%.4f;%.2f;%.2f\n",
                                popSize, maxEvals, crossProb,
                                avgProfit, stdProfit, avgDiversity, stdDiversity,
                                avgTime, stdTime));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static double calculateAverage(double[] values) {
        return Arrays.stream(values).average().orElse(0.0);
    }

    private static double calculateStandardDeviation(double[] values, double mean) {
        double variance = Arrays.stream(values)
                                .map(val -> Math.pow(val - mean, 2))
                                .average()
                                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private static double calculateAverage(long[] values) {
        return Arrays.stream(values).average().orElse(0.0);
    }

    private static double calculateStandardDeviation(long[] values, double mean) {
        double variance = Arrays.stream(values)
                                .mapToDouble(val -> Math.pow(val - mean, 2))
                                .average()
                                .orElse(0.0);
        return Math.sqrt(variance);
    }


    private static Result runExperiment(AgriculturalData data, AgriculturalOptimizationProblem problem,
                                      int populationSize, int maxEvaluations,
                                      double crossoverProbability, double distributionIndex) {
        long startTime = System.currentTimeMillis();

        // Configurar operadores
        double mutationProbability = 1.0 / problem.getNumberOfVariables();
        
        CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(crossoverProbability,
				distributionIndex);
		MutationOperator<IntegerSolution> mutation = new SeasonalIntegerMutation(mutationProbability,
				data.cantParcelas, data.cantFilas, data.temporadaCultivo);

        // Crear y ejecutar algoritmo
        Algorithm<List<IntegerSolution>> algorithm = new NSGAIIBuilder<>(
            problem,
            crossover,
            mutation,
            populationSize
        ).setMaxEvaluations(maxEvaluations)
         .setSolutionListEvaluator(new SequentialSolutionListEvaluator<>())
         .build();

        algorithm.run();

        // Obtener resultados
        List<IntegerSolution> population = algorithm.getResult();
        long executionTime = System.currentTimeMillis() - startTime;

        // Calcular métricas
        double avgProfit = calculateAverageProfit(population);
        double avgDiversity = calculateAverageDiversity(population);

        return new Result(avgProfit, avgDiversity, executionTime);
    }

    private static double calculateAverageProfit(List<IntegerSolution> population) {
        return population.stream()
                        .mapToDouble(solution -> -solution.getObjective(0)) // Negativo porque minimizamos
                        .average()
                        .orElse(0.0);
    }

    private static double calculateAverageDiversity(List<IntegerSolution> population) {
        return population.stream()
                        .mapToDouble(solution -> -solution.getObjective(1)) // Negativo porque minimizamos
                        .average()
                        .orElse(0.0);
    }

    private static class Result {
        final double avgProfit;
        final double avgDiversity;
        final long executionTime;

        Result(double avgProfit, double avgDiversity, long executionTime) {
            this.avgProfit = avgProfit;
            this.avgDiversity = avgDiversity;
            this.executionTime = executionTime;
        }
    }
}