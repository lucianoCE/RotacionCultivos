package rotacionCultivos;

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
import java.util.List;

public class ParetoEstimate {

    public static void main(String[] args) {
    	List<String> instancias = new ArrayList<>();
		instancias.add("instanciaChica.xml");
		instancias.add("instanciaMediana.xml");
		instancias.add("instanciaGrande.xml");	

		List<List<IntegerSolution>> solucionesPorInstancia = new ArrayList<>();
		List<String> nombresInstancias = List.of("instanciaChica", "instanciaMediana", "instanciaGrande", "instanciaVariada");
		
		for (String archivoInstancia : instancias) {
			
			String directorioInstancias = "src/main/resources/instancias/";
			String xmlFilePath = directorioInstancias + archivoInstancia;
			AgriculturalData data = Main.readDataFromXML(xmlFilePath);

			// Crear la instancia del problema
			AgriculturalOptimizationProblem problem = new AgriculturalOptimizationProblem(data.cantParcelas,
					data.cantFilas, data.cantSemestres, data.cantCultivos, data.areaParcelas,
					data.rendimientoCultivoChico, data.rendimientoCultivoMediano, data.rendimientoCultivoGrande,
					data.precioCultivo, data.costoMantCultivo, data.temporadaCultivo);
			
			List<IntegerSolution> solucionesDeInstancia = new ArrayList<>();
            List<Double> ganancias = new ArrayList<>();
            List<Double> diversidades= new ArrayList<>();
	
			for (int i = 0; i<30; i++) {
				// Medir tiempo total del algoritmo evolutivo NSGA-II
				long startNSGAII = System.nanoTime();
	
				double crossoverProbability = 0.9;
				double mutationProbability = 1.0 / problem.getNumberOfVariables();
				double distributionIndex = 10.0;
	
				CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(crossoverProbability,
						distributionIndex);
				MutationOperator<IntegerSolution> mutation = new SeasonalIntegerMutation(mutationProbability,
						data.cantParcelas, data.cantFilas, data.temporadaCultivo);
	
				Algorithm<List<IntegerSolution>> algorithm = new NSGAIIBuilder<>(problem, crossover, mutation, 200 // Tamaño  de la población
				).setMaxEvaluations(50000).setSolutionListEvaluator(new SequentialSolutionListEvaluator<>()).build();
	
				algorithm.run();
				long endNSGAII = System.nanoTime();
	
				List<IntegerSolution> population = algorithm.getResult();
				solucionesDeInstancia.addAll(population);
				
				for (IntegerSolution solution : population) {
                    ganancias.add(solution.getObjective(0));
                    diversidades.add(solution.getObjective(1));
                }
			}
			solucionesPorInstancia.add(obtenerNoDominadas(solucionesDeInstancia));
            calcularEstadisticas(ganancias, nombresInstancias.get(instancias.indexOf(archivoInstancia)), "Ganancia");
            calcularEstadisticas(diversidades, nombresInstancias.get(instancias.indexOf(archivoInstancia)), "Diversidad");
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

        // Comparación de distribuciones con Mann-Whitney U-Test (ejemplo)
        if (fitnesses.size() > 1) {
            double[] distribucion1 = fitnesses.subList(0, fitnesses.size() / 2).stream().mapToDouble(Double::doubleValue).toArray();
            double[] distribucion2 = fitnesses.subList(fitnesses.size() / 2, fitnesses.size()).stream().mapToDouble(Double::doubleValue).toArray();

            KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest();
            double pValue = ksTest.kolmogorovSmirnovTest(distribucion1, distribucion2);

            System.out.println("p-value (Kolmogorov-Smirnov Test): " + pValue);
        }
        System.out.println();
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
                return false; // solucion1 no domina porque es peor en este objetivo
            } else if (solucion1.getObjective(i) < solucion2.getObjective(i)) {
                mejorEnAlMenosUnObjetivo = true;
            }
        }

        return mejorEnAlMenosUnObjetivo; // solucion1 domina solo si es mejor en al menos un objetivo
    }
}
