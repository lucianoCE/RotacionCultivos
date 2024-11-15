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
            upperLimit.add(cantCultivos);
        }

        setVariableBounds(lowerLimit, upperLimit);
    }
    
    @Override
    public void evaluate(IntegerSolution solution) {
        double totalProfit = 0.0;
        double diversityScore = 0.0;
        
        // Matriz de cultivos asignados
        int[][] cropPlan = new int[cantParcelas][cantTrimestres];
        for (int i = 0; i < cantParcelas; i++) {
            for (int t = 0; t < cantTrimestres; t++) {
                cropPlan[i][t] = 1;
            }
        }

        // Calcular el rendimiento total (función CT)
        for (int i = 0; i < cantParcelas; i++) {
            for (int t = 0; t < cantTrimestres; t++) {
                int crop = cropPlan[i][t];
                if (crop > 0) { // Si no está en descanso
                    totalProfit += areaParcelas[i] * (rendimientoCultivo[crop] * precioCultivo[crop] - costoMantCultivo[crop]);
                }
            }
        }
        solution.setObjective(0, -totalProfit); // Negativo para maximizar

        // Calcular la diversidad (función DS usando índice de Shannon)
        int[][] cropFrequency = new int[cantParcelas][cantCultivos];
        for (int i = 0; i < cantParcelas; i++) {
        	for (int t = 0; t < cantTrimestres; t++) {
        		cropFrequency[i][cropPlan[i][t]] += 1;
            }
        }

        for (int i = 0; i < cantParcelas; i++) {
        	for (int k = 0; k < cantCultivos; k++) {
	            if (cropFrequency[i][k] > 0) {
	                double fk = cropFrequency[i][k];
	                diversityScore += fk * Math.log(fk);
	            }
        	}
        }
        diversityScore = -diversityScore / Math.log(cantCultivos);
        solution.setObjective(1, -diversityScore); // Negativo para maximizar
    }
}
