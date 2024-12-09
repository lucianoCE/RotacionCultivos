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

import java.util.List;

public class Main extends AbstractAlgorithmRunner {
    public static void main(String[] args) {
        int cantParcelas = 5;
        int cantTrimestres = 4;
        int cantCultivos = 3; // Siempre ingresar uno más, ya que el 0 es como no cultivar nada.
        double[] areaParcelas = {1.0, 1.5, 1.2, 1.3, 1.1};
        double[] rendimientoCultivo = {0.0, 15.0, 12.0}; // in kg/ha (el primero es el no plantar)
        double[] precioCultivo = {0.0, 1.8, 2.2}; // in $/kg (el primero es no plantar, no tiene precio)
        double[] costoMantCultivo = {0.5, 0.6, 0.4}; // in $/ha

        // Create the problem instance
        AgriculturalOptimizationProblem problem = new AgriculturalOptimizationProblem(
            cantParcelas, cantTrimestres, cantCultivos, areaParcelas, rendimientoCultivo, precioCultivo, costoMantCultivo
        );

        // Define the operators
        double crossoverProbability = 0.75;
        double mutationProbability = 1.0 / problem.getNumberOfVariables();
        double distributionIndex = 10.0;

        CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(crossoverProbability, distributionIndex);
        MutationOperator<IntegerSolution> mutation = new IntegerPolynomialMutation(mutationProbability, distributionIndex);

        Algorithm<List<IntegerSolution>> algorithm = new NSGAIIBuilder<IntegerSolution>(
            problem,
            crossover,
            mutation,
            100 // Population size
        ).setMaxEvaluations(25000)
         .setSolutionListEvaluator(new SequentialSolutionListEvaluator<>())
         .build();

        // Execute the algorithm
        algorithm.run();

        // Retrieve the results
        List<IntegerSolution> population = algorithm.getResult();

        // Print the results
        printFinalSolutionSet(population);
    }
}
