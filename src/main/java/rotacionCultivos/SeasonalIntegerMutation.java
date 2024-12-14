package rotacionCultivos;

import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.Random;

@SuppressWarnings("serial")
public class SeasonalIntegerMutation implements MutationOperator<IntegerSolution> {
    private final double mutationProbability;
    private final char[] temporadaCultivo;
    private final Random random = new Random();

    public SeasonalIntegerMutation(double mutationProbability, char[] temporadaCultivo) {
        this.mutationProbability = mutationProbability;
        this.temporadaCultivo = temporadaCultivo;
    }

    @Override
    public IntegerSolution execute(IntegerSolution solution) {
        for (int i = 0; i < solution.getVariables().size(); i++) {
            if (random.nextDouble() < mutationProbability) {
                int trimestre = i % (solution.getVariables().size() / solution.getNumberOfObjectives());
                char temporada = (trimestre % 2 == 0) ? 'V' : 'I';

                // Generate a valid crop
                int nuevoCultivo;
                do {
                    nuevoCultivo = random.nextInt(temporadaCultivo.length);
                } while (temporadaCultivo[nuevoCultivo] != 'A' && temporadaCultivo[nuevoCultivo] != temporada);

                // Assign the valid crop
                solution.setVariable(i, nuevoCultivo);
            }
        }
        return solution;
    }

    @Override
    public double getMutationProbability() {
        return mutationProbability;
    }
}
