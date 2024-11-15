package rotacionCultivos;

import java.util.List;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.spea2.SPEA2Builder;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import rotacionCultivos.AgriculturalOptimizationProblem;

public class Main {
    public static void main(String[] args) {
        // Define your instance parameters
        int cantParcelas = 5;
        int cantTrimestres = 4;
        int cantCultivos = 3;
        double[] areaParcelas = {1.0, 1.5, 1.2, 1.3, 1.1};
        double[] rendimientoCultivo = {10.0, 15.0, 12.0}; // in kg/ha
        double[] precioCultivo = {2.0, 1.8, 2.2}; // in $/kg
        double[] costoMantCultivo = {0.5, 0.6, 0.4}; // in $/ha

        // Create the problem instance
        AgriculturalOptimizationProblem problem = new AgriculturalOptimizationProblem(
            cantParcelas, cantTrimestres, cantCultivos, areaParcelas, rendimientoCultivo, precioCultivo, costoMantCultivo
        );

        // Select an algorithm (e.g., NSGA-II)
        System.out.println("bobr");
    }
}
