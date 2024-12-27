package rotacionCultivos;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.*;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.WFGHypervolume;
import org.uma.jmetal.util.front.Front;
import org.uma.jmetal.util.front.impl.*;
import org.uma.jmetal.util.point.PointSolution;
import org.uma.jmetal.util.point.impl.ArrayPoint;

import rotacionCultivos.Main.AgriculturalData;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ParameterTuning {
    
    private static final int[] POPULATION_SIZES = {50, 100, 200};
    private static final int[] MAX_EVALUATIONS = {10000, 25000, 50000};
    private static final double[] CROSSOVER_PROBABILITIES = {0.8, 0.9, 1.0};
    private static final int NUMBER_OF_RUNS = 10;
    
    private static double distIndex = 10.0;

    private static class Result {
        final double avgProfit;
        final double avgDiversity;
        final double hypervolume;
        final long executionTime;

        Result(double avgProfit, double avgDiversity, double hypervolume, long executionTime) {
            this.avgProfit = avgProfit;
            this.avgDiversity = avgDiversity;
            this.hypervolume = hypervolume;
            this.executionTime = executionTime;
        }
    }

    public static void main(String[] args) {
        AgriculturalData data = Main.readDataFromXML("src/main/resources/instancias/instanciaVariada.xml");
        
        AgriculturalOptimizationProblem problem = new AgriculturalOptimizationProblem(data.cantParcelas,
                data.cantFilas, data.cantSemestres, data.cantCultivos, data.areaParcelas,
                data.rendimientoCultivoChico, data.rendimientoCultivoMediano, data.rendimientoCultivoGrande,
                data.precioCultivo, data.costoMantCultivo, data.temporadaCultivo);

        try (FileWriter writer = new FileWriter("parameter_tuning_summary.csv")) {
            writer.write("PopSize;MaxEvals;CrossProb;AvgProfit;StdProfit;AvgDiversity;StdDiversity;AvgHypervolume;StdHypervolume;AvgTime;StdTime\n");

            for (int popSize : POPULATION_SIZES) {
                for (int maxEvals : MAX_EVALUATIONS) {
                    for (double crossProb : CROSSOVER_PROBABILITIES) {
                            System.out.printf("Testing: Pop=%d, Evals=%d, CrossProb=%.2f\n",
                                           popSize, maxEvals, crossProb);

                            double[] profits = new double[NUMBER_OF_RUNS];
                            double[] diversities = new double[NUMBER_OF_RUNS];
                            double[] hypervolumes = new double[NUMBER_OF_RUNS];
                            long[] executionTimes = new long[NUMBER_OF_RUNS];

                            for (int run = 0; run < NUMBER_OF_RUNS; run++) {
                                Result result = runExperiment(data, problem, popSize, maxEvals, 
                                                               crossProb, distIndex);
                                profits[run] = result.avgProfit;
                                diversities[run] = result.avgDiversity;
                                hypervolumes[run] = result.hypervolume;
                                executionTimes[run] = result.executionTime;
                            }

                            double avgProfit = calculateAverage(profits);
                            double stdProfit = calculateStandardDeviation(profits, avgProfit);
                            double avgDiversity = calculateAverage(diversities);
                            double stdDiversity = calculateStandardDeviation(diversities, avgDiversity);
                            double avgHypervolume = calculateAverage(hypervolumes);
                            double stdHypervolume = calculateStandardDeviation(hypervolumes, avgHypervolume);
                            double avgTime = calculateAverage(executionTimes);
                            double stdTime = calculateStandardDeviation(executionTimes, avgTime);

                            writer.write(String.format(Locale.US,
                                "%d;%d;%.2f;%.4f;%.4f;%.4f;%.4f;%.4f;%.4f;%.2f;%.2f\n",
                                popSize, maxEvals, crossProb,
                                avgProfit, stdProfit, avgDiversity, stdDiversity,
                                avgHypervolume, stdHypervolume,
                                avgTime, stdTime));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static double calculateHypervolume(List<IntegerSolution> population) {
        // Number of objectives
        int numberOfObjectives = population.get(0).getNumberOfObjectives();

        // Find min and max values for normalization
        double[] minValues = new double[numberOfObjectives];
        double[] maxValues = new double[numberOfObjectives];
        Arrays.fill(minValues, Double.MAX_VALUE);
        Arrays.fill(maxValues, Double.MIN_VALUE);

        for (IntegerSolution solution : population) {
            for (int i = 0; i < numberOfObjectives; i++) {
                double value = -solution.getObjective(i);
                minValues[i] = Math.min(minValues[i], value);
                maxValues[i] = Math.max(maxValues[i], value);
            }
        }

        List<PointSolution> normalizedPopulation = new ArrayList<>();
        for (IntegerSolution solution : population) {
            PointSolution pointSolution = new PointSolution(numberOfObjectives);
            for (int i = 0; i < numberOfObjectives; i++) {
                double value = -solution.getObjective(i);
                double normalizedValue = (value - minValues[i]) / (maxValues[i] - minValues[i]);
                pointSolution.setObjective(i, normalizedValue);
            }
            normalizedPopulation.add(pointSolution);
        }

        // Define the reference point
        PointSolution referencePoint = new PointSolution(numberOfObjectives);
        for (int i = 0; i < numberOfObjectives; i++) {
            referencePoint.setObjective(i, 1.1); 
        }

        // Create a Front object for the reference
        List<PointSolution> referencePoints = new ArrayList<>();
        referencePoints.add(referencePoint);
        Front referenceFront = new ArrayFront(referencePoints);

        // Compute hypervolume using WFGHypervolume with the referenceFront
        WFGHypervolume<PointSolution> hypervolume = new WFGHypervolume<>(referenceFront);
        return hypervolume.evaluate(normalizedPopulation);
    }


    // Métodos existentes sin cambios
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

        double mutationProbability = 1.0 / problem.getNumberOfVariables();
        
        CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(crossoverProbability,
                distributionIndex);
        MutationOperator<IntegerSolution> mutation = new SeasonalIntegerMutation(mutationProbability,
                data.cantParcelas, data.cantFilas, data.temporadaCultivo);

        Algorithm<List<IntegerSolution>> algorithm = new NSGAIIBuilder<>(
            problem,
            crossover,
            mutation,
            populationSize
        ).setMaxEvaluations(maxEvaluations)
         .setSolutionListEvaluator(new SequentialSolutionListEvaluator<>())
         .build();

        algorithm.run();

        List<IntegerSolution> population = algorithm.getResult();
        long executionTime = System.currentTimeMillis() - startTime;

        double avgProfit = calculateAverageProfit(population);
        double avgDiversity = calculateAverageDiversity(population);
        double hypervolume = calculateHypervolume(population);

        return new Result(avgProfit, avgDiversity, hypervolume, executionTime);
    }

    private static double calculateAverageProfit(List<IntegerSolution> population) {
        return population.stream()
                        .mapToDouble(solution -> -solution.getObjective(0))
                        .average()
                        .orElse(0.0);
    }

    private static double calculateAverageDiversity(List<IntegerSolution> population) {
        return population.stream()
                        .mapToDouble(solution -> -solution.getObjective(1))
                        .average()
                        .orElse(0.0);
    }
}