package rotacionCultivos;

import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class AgriculturalOptimizationProblem extends AbstractIntegerProblem {
	
	private final int cantParcelas; // cantParcelasúmero de parcelas
    private final int cantTrimestres; // Número de trimestres
    private final int cantCultivos; // Tipos de cultivos
    private final double[] areaParcelas; // Vector de áreas de parcelas
    private final double[] rendimientoCultivo; // Rendimiento por cultivo en kg/ha
    private final double[] precioCultivo; // Precio de venta por cultivo en $/kg
    private final double[] costoMantCultivo; // Costo de mantenimiento por cultivo en $/ha

    public AgriculturalOptimizationProblem(int cantParcelas, int cantTrimestres, int cantCultivos, double[] areaParcelas, double[] rendimientoCultivo, double[] precioCultivo, double[] costoMantCultivo) {
        this.cantParcelas = cantParcelas;
        this.cantTrimestres = cantTrimestres;
        this.cantCultivos = cantCultivos;
        this.areaParcelas = areaParcelas;
        this.rendimientoCultivo = rendimientoCultivo;
        this.precioCultivo = precioCultivo;
        this.costoMantCultivo = costoMantCultivo;

        // Definir el número de variables (cantParcelas*cantTrimestres) y sus límites
        setNumberOfVariables(cantParcelas * cantTrimestres);
        setNumberOfObjectives(2); // Maximizar rendimiento y diversidad
        setName("AgriculturalOptimizationProblem");

        // Definir el rango de valores (0 a cantCultivos) para los cultivos
        List<Integer> lowerLimit = new ArrayList<>();
        List<Integer> upperLimit = new ArrayList<>();

        for (int i = 0; i < getNumberOfVariables(); i++) {
            lowerLimit.add(0);
            upperLimit.add(cantCultivos - 1); // la cantidad de cultivos está indexada desde 1.
        }

        setVariableBounds(lowerLimit, upperLimit);
    }
    
    @Override
    public void evaluate(IntegerSolution solution) {
        double totalProfit = 0.0;
        double totalDiversityScore = 0.0; // Almacena la diversidad total

        // Matriz de cultivos asignados
        int[][] cropPlan = new int[cantParcelas][cantTrimestres];

        // Mapeo de variables de la solución a la matriz cropPlan
        for (int i = 0; i < cantParcelas; i++) {
            for (int t = 0; t < cantTrimestres; t++) {
                int variableIndex = i * cantTrimestres + t; // Cálculo del índice de la variable
                cropPlan[i][t] = solution.getVariable(variableIndex); // Asignación de la variable
            }
        }

        // Calcular el rendimiento total (función CT)
        for (int i = 0; i < cantParcelas; i++) {
            for (int t = 0; t < cantTrimestres; t++) {
                int crop = cropPlan[i][t]; // Tipo de cultivo plantado en la parcela i en el trimestre t.
                if (crop > 0) { // Si no está en descanso
                    totalProfit += areaParcelas[i] * (rendimientoCultivo[crop] * (precioCultivo[crop] - costoMantCultivo[crop]));
                } else {
                	totalProfit -= costoMantCultivo[crop];
                }
            }
        }
        solution.setObjective(0, -totalProfit); // Negativo para maximizar
        
        // CÁLCULO DE DIVERSIDAD (función DS usando índice de Shannon)
        
        // Primero se obtiene la frecuencia de cada cultivo en cada parcela (para el cálculo de la fr).
        int[][] cropFrequency = new int[cantParcelas][cantCultivos];

        // Calcular las frecuencias absolutas, excluyendo las parcelas sin cultivo
        for (int i = 0; i < cantParcelas; i++) { 
            for (int t = 0; t < cantTrimestres; t++) {
                if (cropPlan[i][t] > 0) { // Ignorar parcelas sin cultivo (0)
                    cropFrequency[i][cropPlan[i][t]] += 1;
                }
            }
        }

        // Calcular la diversidad para cada parcela
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
                parcelDiversityScore = -parcelDiversityScore / Math.log(cantCultivos);  // Dividir entre el logaritmo de los cultivos
                totalDiversityScore += parcelDiversityScore;  // Sumar a la diversidad total
            }
        }

        // Promediar la diversidad de todas las parcelas
        double normalizedDiversityScore = totalDiversityScore / cantParcelas;
        solution.setObjective(1, -normalizedDiversityScore);  // Negativo para maximizar
    }
}
