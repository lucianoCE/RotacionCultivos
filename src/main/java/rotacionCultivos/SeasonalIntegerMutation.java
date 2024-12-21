package rotacionCultivos;

import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("serial")
public class SeasonalIntegerMutation implements MutationOperator<IntegerSolution> {
    private final double mutationProbability;
    private final int cantParcelas;
    private final int cantFilas; // Cantidad de filas de la cuadrícula
    private final char[] temporadaCultivo;
    private final Random random = new Random();

    public SeasonalIntegerMutation(double mutationProbability, int cantParcelas, int cantFilas, char[] temporadaCultivo) {
        this.mutationProbability = mutationProbability;
        this.cantParcelas = cantParcelas;
        this.cantFilas = cantFilas;
        this.temporadaCultivo = temporadaCultivo;
    }

    @Override
    public IntegerSolution execute(IntegerSolution solution) {
        if (random.nextDouble() >= mutationProbability) {
            return solution; // No se realiza mutación
        }
        int cantColumnas = (int) Math.ceil((double) cantParcelas / cantFilas);

        // Seleccionar una parcela inicial (indexada desde 1)
        int parcelaInicial = random.nextInt(cantParcelas) + 1;

        // Determinar la temporada del trimestre
        int trimestre = (parcelaInicial - 1) % (solution.getVariables().size() / cantParcelas);
        char temporada = (trimestre % 2 == 0) ? 'V' : 'I';

        // Seleccionar un cultivo válido para la temporada
        int nuevoCultivo;
        do {
            nuevoCultivo = random.nextInt(temporadaCultivo.length);
        } while (temporadaCultivo[nuevoCultivo] != 'A' && temporadaCultivo[nuevoCultivo] != temporada);

        List<Integer> parcelasCercanas = obtenerParcelasCercanas(parcelaInicial, cantParcelas, cantFilas, cantColumnas);
        for (int parcela : parcelasCercanas) { 
            int index = (parcela - 1) + trimestre * cantParcelas; // Cambiar los cultivos de las parcelas cercanas
            solution.setVariable(index, nuevoCultivo);
        }

        return solution;
    }

    private List<Integer> obtenerParcelasCercanas(int parcela, int cantParcelas, int cantFilas, int cantColumnas) {
        List<Integer> cercanas = new ArrayList<>();
        int fila = (parcela - 1) / cantColumnas;
        int columna = (parcela - 1) % cantColumnas;

        // Iterar sobre filas y columnas adyacentes
        for (int i = -1; i <= 1; i++) { // Filas adyacentes
            for (int j = -1; j <= 1; j++) { // Columnas adyacentes
                if (i == 0 && j == 0) continue; // Saltar la parcela actual
                int nuevaFila = fila + i;
                int nuevaColumna = columna + j;
                int nuevaParcela = nuevaFila * cantColumnas + nuevaColumna + 1;
                // Validar que la nueva parcela esté dentro de los límites
                if (nuevaFila >= 0 && nuevaFila < cantFilas && nuevaParcela <= cantParcelas && nuevaColumna >= 0 && nuevaColumna < cantColumnas) {
                    cercanas.add(nuevaParcela);
                }
            }
        }
        cercanas.add(parcela); // Incluir la parcela actual
        return cercanas;
    }

    @Override
    public double getMutationProbability() {
        return mutationProbability;
    }
}
