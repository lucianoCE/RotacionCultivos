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
    private final char[] temporadaCultivo;
    private final String prioridad; // "ganancia" o "diversidad"

    public GreedyAgriculturalSolver(int cantParcelas, int cantTrimestres, int cantCultivos, double[] areaParcelas, double[] rendimientoCultivo, double[] precioCultivo, double[] costoMantCultivo, char[] temporadaCultivo, String prioridad) {
        this.cantParcelas = cantParcelas;
        this.cantTrimestres = cantTrimestres;
        this.cantCultivos = cantCultivos;
        this.areaParcelas = areaParcelas;
        this.rendimientoCultivo = rendimientoCultivo;
        this.precioCultivo = precioCultivo;
        this.costoMantCultivo = costoMantCultivo;
        this.temporadaCultivo = temporadaCultivo;
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
                int bestCrop = selectBestCrop(parcela, trimestre, cropFrequency);
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

     // Calcular diversidad usando el índice de Shannon
        double totalDiversityScore = 0.0;
        double totalCultivosGlobal = 0.0;
        int[] cultivoGlobalFrequencies = new int[cantCultivos];

        for (int i = 0; i < cantParcelas; i++) {
            double totalCultivos = 0;  // Total de cultivos en la parcela i
            for (int k = 1; k < cantCultivos; k++) {  // Empezar desde 1, ya que 0 significa "sin cultivo"
                totalCultivos += cropFrequency[i][k];  // Sumar las frecuencias de cultivos
            }

            if (totalCultivos > 0) {  // Si la parcela tiene cultivos (no es 0)
                double parcelDiversityScore = 0.0;  // Diversidad para esta parcela
                for (int k = 1; k < cantCultivos; k++) {
                    if (cropFrequency[i][k] > 0) {
                        double fk = cropFrequency[i][k] / totalCultivos;  // Frecuencia relativa
                        parcelDiversityScore += fk * Math.log(fk);
                    }
                }

                // Normalizar la diversidad de esta parcela
                parcelDiversityScore = -parcelDiversityScore / Math.log(cantCultivos-1);  // Dividir entre el logaritmo de los cultivos
                totalDiversityScore += parcelDiversityScore;  // Sumar a la diversidad total
            }
        }

        // Promediar la diversidad de todas las parcelas
        double normalizedDiversityScore = totalDiversityScore / cantParcelas;
        
        return new Result(cropPlan, totalProfit, normalizedDiversityScore);
    }

    private int selectBestCrop(int parcela, int trimestre, int[][] cropFrequency) {
        int bestCrop = 0; // 0 representa descanso

        if (prioridad.equals("ganancia")) {
            double maxProfit = Double.NEGATIVE_INFINITY;
            for (int cultivo = 0; cultivo < cantCultivos; cultivo++) {
            	char temporada;
            	if (trimestre % 2 == 0) 
            		temporada = 'V';
            	else 
            		temporada = 'I';
            	if (temporadaCultivo[cultivo] == temporada || temporadaCultivo[cultivo] == 'A') {
	                double profit = areaParcelas[parcela] * (rendimientoCultivo[cultivo] * (precioCultivo[cultivo] - costoMantCultivo[cultivo]));
	                if (profit > maxProfit) {
	                    maxProfit = profit;
	                    bestCrop = cultivo;
	                }
            	}
            }
        } else if (prioridad.equals("diversidad")) {
            double maxDiversityScore = Double.NEGATIVE_INFINITY;
            int[] totalCropFrequency = new int[cantCultivos];

            // Calcular frecuencia total de cada cultivo en todas las parcelas
            for (int p = 0; p < cantParcelas; p++) {
                for (int c = 0; c < cantCultivos; c++) {
                    totalCropFrequency[c] += cropFrequency[p][c];
                }
            }

            for (int cultivo = 1; cultivo < cantCultivos; cultivo++) { // Ignorar "sin cultivo" (0)
            	char temporada;
            	if (trimestre % 2 == 0) 
            		temporada = 'V';
            	else 
            		temporada = 'I';
            	if (temporadaCultivo[cultivo] == temporada || temporadaCultivo[cultivo] == 'A') {
	                double diversityScore = -totalCropFrequency[cultivo]; // Menor frecuencia global = mayor diversidad
	                if (diversityScore > maxDiversityScore) {
	                    maxDiversityScore = diversityScore;
	                    bestCrop = cultivo;
	                }
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
