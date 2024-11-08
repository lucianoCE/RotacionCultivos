package rotacionCultivos;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;


public class RotacionCultivos extends AbstractIntegerProblem{
	
	private int cantTemporadas;
	private int cantParcelas;
	private float[] areaParcelas;

	public RotacionCultivos(int cantT, int cantP, float[] areaP) {
		setNumberOfVariables(1);
		setNumberOfObjectives(2);
		setName("RotacionCultivos");
		cantTemporadas = cantT;
		cantParcelas = cantP;
		areaParcelas = areaP;
		
	}
	
}
