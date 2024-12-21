package rotacionCultivos;

import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("serial")
public class AgriculturalOptimizationProblem extends AbstractIntegerProblem {
	
	private final int cantParcelas; // cantParcelasúmero de parcelas
	private final int cantFilas;
    private final int cantTrimestres; // Número de trimestres
    private final int cantCultivos; // Tipos de cultivos
    private final double[] areaParcelas; // Vector de áreas de parcelas
    private final double[] rendimientoCultivoChico; // Rendimiento por cultivo en kg/ha
    private final double[] rendimientoCultivoMediano; // Rendimiento por cultivo en kg/ha
    private final double[] rendimientoCultivoGrande; // Rendimiento por cultivo en kg/ha
    private final double[] precioCultivo; // Precio de venta por cultivo en $/kg
    private final double[] costoMantCultivo; // Costo de mantenimiento por cultivo en $/ha
    private final char[] temporadaCultivo;
    
    // Auxiliares
    private final int[] cultivosVerano;
    private int indiceVerano;
    private final int[] cultivosInvierno;
    private int indiceInvierno;

    public AgriculturalOptimizationProblem(int cantParcelas, int cantFilas, int cantTrimestres, int cantCultivos, double[] areaParcelas, double[] rendimientoCultivoChico, double[] rendimientoCultivoMediano, double[] rendimientoCultivoGrande, double[] precioCultivo, double[] costoMantCultivo, char[] temporadaCultivo) {
        this.cantParcelas = cantParcelas;
        this.cantFilas = cantFilas;
        this.cantTrimestres = cantTrimestres;
        this.cantCultivos = cantCultivos;
        this.areaParcelas = areaParcelas;
        this.rendimientoCultivoChico = rendimientoCultivoChico;
        this.rendimientoCultivoMediano = rendimientoCultivoMediano;
        this.rendimientoCultivoGrande = rendimientoCultivoGrande;
        this.precioCultivo = precioCultivo;
        this.costoMantCultivo = costoMantCultivo;
        this.temporadaCultivo = temporadaCultivo;

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
        
        // Auxiliares
        this.indiceVerano = 0;
        this.indiceInvierno = 0;
        List<Integer> listaCultivosVerano = new ArrayList<>();
        List<Integer> listaCultivosInvierno = new ArrayList<>();
        for (int c = 0; c < cantCultivos; c++) {
        	if (temporadaCultivo[c] == 'V') {
        		listaCultivosVerano.add(c);
        	} else if (temporadaCultivo[c] == 'I') {
        		listaCultivosInvierno.add(c);
        	} else {
        		listaCultivosVerano.add(c);
        		listaCultivosInvierno.add(c);
        	}
        }
        // Ordenar listaCultivosVerano en base a precioCultivo
        listaCultivosVerano.sort((a, b) -> Double.compare(precioCultivo[b], precioCultivo[a]));

        // Ordenar listaCultivosInvierno en base a precioCultivo
        listaCultivosInvierno.sort((a, b) -> Double.compare(precioCultivo[b], precioCultivo[a]));
        
        // Convertir listaCultivosVerano a un array de int
        this.cultivosVerano = listaCultivosVerano.stream().mapToInt(Integer::intValue).toArray();

        // Convertir listaCultivosInvierno a un array de int
        this.cultivosInvierno = listaCultivosInvierno.stream().mapToInt(Integer::intValue).toArray();

    }
    
    @Override
    public IntegerSolution createSolution() {
        IntegerSolution solution = super.createSolution();
        Random random = new Random();

        for (int i = 0; i < cantParcelas; i++) {
            for (int t = 0; t < cantTrimestres; t++) {
                int variableIndex = i * cantTrimestres + t;

                // Determinar la temporada del trimestre: par = verano, impar = invierno
                char temporadaTrimestre = (t % 2 == 0) ? 'V' : 'I';

                // Asignar un cultivo válido según la temporada
                if (temporadaTrimestre == 'V') {
                    solution.setVariable(variableIndex, cultivosVerano[random.nextInt(cultivosVerano.length)]);
                } else {
                    solution.setVariable(variableIndex, cultivosInvierno[random.nextInt(cultivosInvierno.length)]);
                }
            }
        }

        return solution;
    }
    
    @Override
    public void evaluate(IntegerSolution solution) {
    	
    	repairSolution(solution);
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
                double area = areaParcelas[i];
                double rendimiento;
                if (area <= 200.0) {
                	rendimiento = rendimientoCultivoChico[crop];
                }else {
                	if (area <= 500.0)
                		rendimiento = rendimientoCultivoMediano[crop];
                	else 
                		rendimiento = rendimientoCultivoGrande[crop];
                }
                if (crop > 0) { // Si no está en descanso
                    totalProfit += area * (rendimiento * (precioCultivo[crop] - costoMantCultivo[crop]));
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
                parcelDiversityScore = -parcelDiversityScore / Math.log(cantCultivos-1);  // Dividir entre el logaritmo de los cultivos
                totalDiversityScore += parcelDiversityScore;  // Sumar a la diversidad total
            }
        }

        // Promediar la diversidad de todas las parcelas
        double normalizedDiversityScore = totalDiversityScore / cantParcelas;
        solution.setObjective(1, -normalizedDiversityScore);  // Negativo para maximizar
    }
    
    
    private void repairSolution(IntegerSolution solution) {
        for (int i = 0; i < cantParcelas; i++) {
            for (int t = 0; t < cantTrimestres; t++) {
                int variableIndex = i * cantTrimestres + t;
                int cultivo = solution.getVariable(variableIndex); // Cultivo actual asignado

                // Obtener la temporada del trimestre: par = verano ('V'), impar = invierno ('I')
                char temporadaTrimestre = (t % 2 == 0) ? 'V' : 'I';

                // Validar si el cultivo es compatible con la temporada
                if (!esCultivoValido(cultivo, temporadaTrimestre)) {
                    // Buscar un cultivo válido para la temporada actual
                    int nuevoCultivo = encontrarCultivoValido(temporadaTrimestre);
                    solution.setVariable(variableIndex, nuevoCultivo); // Reemplazar el cultivo
                }
            }
        }
    }
    
    private boolean esCultivoValido(int cultivo, char temporadaTrimestre) {
        if (cultivo == 0) { // Cultivo 0 (descanso) siempre es válido
            return true;
        }
        char temporadaCultivoActual = temporadaCultivo[cultivo];
        return temporadaCultivoActual == 'A' || temporadaCultivoActual == temporadaTrimestre;
    }
    
    private int encontrarCultivoValido(char temporadaTrimestre) {
    	if (temporadaTrimestre == 'V') {
    		int indice = this.indiceVerano;
    		this.indiceVerano = (this.indiceVerano + 1) % this.cultivosVerano.length;
    		return this.cultivosVerano[indice];
    	} else {
    		int indice = this.indiceInvierno;
    		this.indiceInvierno = (this.indiceInvierno + 1) % this.cultivosInvierno.length;
    		return this.cultivosInvierno[indice];
    	}
    }
    
}
