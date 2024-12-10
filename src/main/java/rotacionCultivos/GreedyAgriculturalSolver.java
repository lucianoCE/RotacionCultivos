package rotacionCultivos;

import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import java.util.ArrayList;
import java.util.List;

public class GreedyAgriculturalSolver {

    private final int cantParcelas;
    private final int cantTrimestres;
    private final int cantCultivos;
    private final double[] areaParcelas;
    private final double[] rendimientoCultivo;
    private final double[] precioCultivo;
    private final double[] costoMantCultivo;
    private final String prioridad; // "ganancia" o "diversidad"

    public GreedyAgriculturalSolver(int cantParcelas, int cantTrimestres, int cantCultivos, double[] areaParcelas, double[] rendimientoCultivo, double[] precioCultivo, double[] costoMantCultivo, String prioridad) {
        this.cantParcelas = cantParcelas;
        this.cantTrimestres = cantTrimestres;
        this.cantCultivos = cantCultivos;
        this.areaParcelas = areaParcelas;
        this.rendimientoCultivo = rendimientoCultivo;
        this.precioCultivo = precioCultivo;
        this.costoMantCultivo = costoMantCultivo;
        this.prioridad = prioridad;
    }

    public Result solve() {
        int[][] cropPlan = new int[cantParcelas][cantTrimestres];
        double totalProfit = 0.0;

        // Crear matriz para frecuencias de cultivos
        int[][] cropFrequency = new int[cantParcelas][cantCultivos];

        // Iterar sobre cada parcela y trimestre
        for (int parcela = 0; parcela < cantParcelas; parcela++) {
            for (int trimestre = 0; trimestre < cantTrimestres; trimestre++) {
                int bestCrop = selectBestCrop(parcela, cropFrequency);
                cropPlan[parcela][trimestre] = bestCrop;

                // Calcular ganancia
                if (bestCrop > 0) { // Si no está en descanso
                    totalProfit += areaParcelas[parcela] * (rendimientoCultivo[bestCrop] * (precioCultivo[bestCrop] - costoMantCultivo[bestCrop]));
                }

                // Actualizar frecuencia del cultivo
                if (bestCrop > 0) {
                    cropFrequency[parcela][bestCrop] += 1;
                }
            }
        }

        // Calcular diversidad
        double totalDiversityScore = 0.0;
        for (int parcela = 0; parcela < cantParcelas; parcela++) {
            double totalCultivos = 0;
            for (int cultivo = 1; cultivo < cantCultivos; cultivo++) { // Ignorar "sin cultivo" (0)
                totalCultivos += cropFrequency[parcela][cultivo];
            }

            if (totalCultivos > 0) {
                double parcelDiversityScore = 0.0;
                for (int cultivo = 1; cultivo < cantCultivos; cultivo++) {
                    if (cropFrequency[parcela][cultivo] > 0) {
                        double fk = cropFrequency[parcela][cultivo] / totalCultivos;
                        parcelDiversityScore += fk * Math.log(fk);
                    }
                }
                parcelDiversityScore = -parcelDiversityScore / Math.log(cantCultivos);
                totalDiversityScore += parcelDiversityScore;
            }
        }
        double normalizedDiversityScore = totalDiversityScore / cantParcelas;

        return new Result(cropPlan, totalProfit, normalizedDiversityScore);
    }

    private int selectBestCrop(int parcela, int[][] cropFrequency) {
        int bestCrop = 0; // 0 representa descanso

        if (prioridad.equals("ganancia")) {
            double maxProfit = Double.NEGATIVE_INFINITY;
            for (int cultivo = 0; cultivo < cantCultivos; cultivo++) {
                double profit = areaParcelas[parcela] * (rendimientoCultivo[cultivo] * (precioCultivo[cultivo] - costoMantCultivo[cultivo]));
                if (profit > maxProfit) {
                    maxProfit = profit;
                    bestCrop = cultivo;
                }
            }
        } else if (prioridad.equals("diversidad")) {
            double maxDiversityScore = Double.NEGATIVE_INFINITY;
            for (int cultivo = 1; cultivo < cantCultivos; cultivo++) { // Ignorar "sin cultivo" (0)
                double diversityScore = -cropFrequency[parcela][cultivo]; // Menor frecuencia = mayor diversidad
                if (diversityScore > maxDiversityScore) {
                    maxDiversityScore = diversityScore;
                    bestCrop = cultivo;
                }
            }
        }

        return bestCrop;
    }

    public List<IntegerSolution> initializePopulation(AbstractIntegerProblem problem, int[][] cropPlan, double totalProfit, double diversityScore) {
        List<IntegerSolution> initialPopulation = new ArrayList<>();
        IntegerSolution solution = problem.createSolution();

        // Mapear cropPlan (2D) a un vector unidimensional en IntegerSolution
        for (int i = 0; i < cantParcelas; i++) {
            for (int t = 0; t < cantTrimestres; t++) {
                int variableIndex = i * cantTrimestres + t;
                solution.setVariable(variableIndex, cropPlan[i][t]);
            }
        }

        // Asignar objetivos
        solution.setObjective(0, -totalProfit); // Maximizar ganancia (negativo para minimizar)
        solution.setObjective(1, -diversityScore); // Maximizar diversidad (negativo para minimizar)

        initialPopulation.add(solution);
        return initialPopulation;
    }

    public static class Result {
        public final int[][] cropPlan;
        public final double totalProfit;
        public final double diversityScore;

        public Result(int[][] cropPlan, double totalProfit, double diversityScore) {
            this.cropPlan = cropPlan;
            this.totalProfit = totalProfit;
            this.diversityScore = diversityScore;
        }
    }
}
