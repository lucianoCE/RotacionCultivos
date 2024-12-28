package rotacionCultivos;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;
import rotacionCultivos.Main.AgriculturalData;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParetoEstimate {
    public static void main(String[] args) {
        List<String> instancias = new ArrayList<>();
        instancias.add("instanciaChica.xml");
        instancias.add("instanciaMediana.xml");
        instancias.add("instanciaGrande.xml");  

        List<List<IntegerSolution>> solucionesPorInstancia = new ArrayList<>();
        List<String> nombresInstancias = List.of("instanciaChica", "instanciaMediana", "instanciaGrande");

        // Store results for each instance and each execution
        List<List<List<Double>>> gananciasPorInstancia = new ArrayList<>();
        List<List<List<Double>>> diversidadesPorInstancia = new ArrayList<>();
        
        // Initialize lists for each instance
        for (int i = 0; i < instancias.size(); i++) {
            gananciasPorInstancia.add(new ArrayList<>());
            diversidadesPorInstancia.add(new ArrayList<>());
        }

        for (String archivoInstancia : instancias) {
            int instanceIndex = instancias.indexOf(archivoInstancia);
            
            String directorioInstancias = "src/main/resources/instancias/";
            String xmlFilePath = directorioInstancias + archivoInstancia;
            AgriculturalData data = Main.readDataFromXML(xmlFilePath);

            AgriculturalOptimizationProblem problem = new AgriculturalOptimizationProblem(
                data.cantParcelas, data.cantFilas, data.cantSemestres, data.cantCultivos, 
                data.areaParcelas, data.rendimientoCultivoChico, data.rendimientoCultivoMediano, 
                data.rendimientoCultivoGrande, data.precioCultivo, data.costoMantCultivo, 
                data.temporadaCultivo
            );
            
            List<IntegerSolution> solucionesDeInstancia = new ArrayList<>();
            List<Double> ganancias = new ArrayList<>();
            List<Double> diversidades = new ArrayList<>();

            for (int i = 0; i < 30; i++) {
                double crossoverProbability = 0.9;
                double mutationProbability = 1.0 / problem.getNumberOfVariables();
                double distributionIndex = 10.0;

                CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(crossoverProbability, distributionIndex);
                MutationOperator<IntegerSolution> mutation = new SeasonalIntegerMutation(
                    mutationProbability, data.cantParcelas, data.cantFilas, data.temporadaCultivo
                );

                Algorithm<List<IntegerSolution>> algorithm = new NSGAIIBuilder<>(
                    problem, crossover, mutation, 200
                ).setMaxEvaluations(50000)
                 .setSolutionListEvaluator(new SequentialSolutionListEvaluator<>())
                 .build();

                algorithm.run();

                List<IntegerSolution> population = algorithm.getResult();
                solucionesDeInstancia.addAll(population);
                
                for (IntegerSolution solution : population) {
                    ganancias.add(solution.getObjective(0));
                    diversidades.add(solution.getObjective(1));
                }
            }
            
            solucionesPorInstancia.add(obtenerNoDominadas(solucionesDeInstancia));
            calcularEstadisticas(ganancias, nombresInstancias.get(instanceIndex), "Ganancia");
            calcularEstadisticas(diversidades, nombresInstancias.get(instanceIndex), "Diversidad");
        }

        ScatterPlot.guardarFrentePareto(solucionesPorInstancia, nombresInstancias);
    }

    private static void calcularEstadisticas(List<Double> fitnesses, String nombreInstancia, String objetivo) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        fitnesses.forEach(stats::addValue);

        double mejorFitness = stats.getMin();
        double promedioFitness = stats.getMean();
        double desviacionEstandar = stats.getStandardDeviation();

        System.out.println("Instancia: " + nombreInstancia);
        System.out.println("Objetivo: " + objetivo);
        System.out.println("Mejor Fitness: " + mejorFitness);
        System.out.println("Fitness Promedio: " + promedioFitness);
        System.out.println("Desviación Estándar: " + desviacionEstandar);

        // Kolmogorov-Smirnov Test 
        if (fitnesses.size() > 1) {
            double[] distribucion1 = fitnesses.subList(0, fitnesses.size() / 2)
                                            .stream()
                                            .mapToDouble(Double::doubleValue)
                                            .toArray();
            double[] distribucion2 = fitnesses.subList(fitnesses.size() / 2, fitnesses.size())
                                            .stream()
                                            .mapToDouble(Double::doubleValue)
                                            .toArray();

            KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();
            double pValueKS = ksTest.kolmogorovSmirnovTest(distribucion1, distribucion2);
            System.out.println("p-value (Kolmogorov-Smirnov Test): " + pValueKS);

            // Perform Friedman Test
            List<List<Double>> distribuciones = new ArrayList<>();
            for (int i = 0; i < fitnesses.size(); i += fitnesses.size() / 3) {
                distribuciones.add(fitnesses.subList(i, Math.min(i + fitnesses.size() / 3, fitnesses.size())));
            }
            realizarTestDeFriedman(distribuciones);
        }
        System.out.println();
    }

    private static void realizarTestDeFriedman(List<List<Double>> distribuciones) {
        int k = distribuciones.size();  // Number of treatments (instances)
        int n = distribuciones.get(0).size();  // Number of blocks (runs)
        
        // Create a matrix where rows are blocks (runs) and columns are treatments (instances)
        double[][] data = new double[n][k];
        for (int i = 0; i < k; i++) {
            List<Double> distribucion = distribuciones.get(i);
            for (int j = 0; j < n; j++) {
                data[j][i] = distribucion.get(j);
            }
        }
        
        // Calculate ranks within each block
        double[][] ranks = new double[n][k];
        for (int i = 0; i < n; i++) {
            double[] blockValues = data[i];
            
            // Create array of indices and sort them based on values
            Integer[] indices = new Integer[k];
            for (int j = 0; j < k; j++) {
                indices[j] = j;
            }
            
            Arrays.sort(indices, (a, b) -> Double.compare(blockValues[a], blockValues[b]));
            
            int rank = 1;
            int j = 0;
            while (j < k) {
                double currentValue = blockValues[indices[j]];
                int tied = 1;
                int start = j;
                
                while (j + 1 < k && blockValues[indices[j + 1]] == currentValue) {
                    j++;
                    tied++;
                }
                
                double averageRank = rank + (tied - 1) / 2.0;
                
                for (int t = 0; t < tied; t++) {
                    ranks[i][indices[start + t]] = averageRank;
                }
                
                rank += tied;
                j++;
            }
        }
        
        // Calculate column sums (R_j)
        double[] columnSums = new double[k];
        for (int j = 0; j < k; j++) {
            for (int i = 0; i < n; i++) {
                columnSums[j] += ranks[i][j];
            }
        }
        
        // Calculate Friedman statistic
        double sumSquaredRanks = 0;
        for (double columnSum : columnSums) {
            sumSquaredRanks += columnSum * columnSum;
        }
        
        double chiSquaredStatistic = (12.0 / (n * k * (k + 1))) * sumSquaredRanks - 3 * n * (k + 1);
        
        // Calculate p-value
        ChiSquaredDistribution chiSquaredDistribution = new ChiSquaredDistribution(k - 1);
        double pValue = 1 - chiSquaredDistribution.cumulativeProbability(chiSquaredStatistic);
        
        System.out.println("\nFriedman Test Results:");
        System.out.println("Number of treatments (k): " + k);
        System.out.println("Number of blocks (n): " + n);
        System.out.println("Chi-squared statistic: " + chiSquaredStatistic);
        System.out.println("Degrees of freedom: " + (k - 1));
        System.out.println("p-value: " + pValue);
        
        System.out.println("\nAverage ranks por instancia:");
        for (int j = 0; j < k; j++) {
            System.out.printf("Instancia %d: %.2f%n", j + 1, columnSums[j] / n);
        }
    }
    
        
    public static <S extends Solution<?>> List<S> obtenerNoDominadas(List<S> soluciones) {
        List<S> noDominadas = new ArrayList<>();
        for (S solucion1 : soluciones) {
            boolean esDominada = false;
            for (S solucion2 : soluciones) {
                if (domina(solucion2, solucion1)) {
                    esDominada = true;
                    break;
                }
            }
            if (!esDominada) {
                noDominadas.add(solucion1);
            }
        }
        return noDominadas;
    }

    private static boolean domina(Solution<?> solucion1, Solution<?> solucion2) {
        boolean mejorEnAlMenosUnObjetivo = false;

        for (int i = 0; i < solucion1.getNumberOfObjectives(); i++) {
            if (solucion1.getObjective(i) > solucion2.getObjective(i)) {
                return false;
            } else if (solucion1.getObjective(i) < solucion2.getObjective(i)) {
                mejorEnAlMenosUnObjetivo = true;
            }
        }

        return mejorEnAlMenosUnObjetivo;
    }
}