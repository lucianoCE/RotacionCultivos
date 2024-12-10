package rotacionCultivos;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Main extends AbstractAlgorithmRunner {
    public static void main(String[] args) {
        String xmlFilePath = "src/main/resources/instancias/instancia_1.xml"; // Ruta del archivo XML
        AgriculturalData data = readDataFromXML(xmlFilePath);

        // Crear la instancia del problema
        AgriculturalOptimizationProblem problem = new AgriculturalOptimizationProblem(
            data.cantParcelas, data.cantTrimestres, data.cantCultivos,
            data.areaParcelas, data.rendimientoCultivo, data.precioCultivo, data.costoMantCultivo
        );

        // Definir los operadores
        double crossoverProbability = 0.9;
        double mutationProbability = 1.0 / problem.getNumberOfVariables();
        double distributionIndex = 10.0;

        CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(crossoverProbability, distributionIndex);
        MutationOperator<IntegerSolution> mutation = new IntegerPolynomialMutation(mutationProbability, distributionIndex);

        Algorithm<List<IntegerSolution>> algorithm = new NSGAIIBuilder<IntegerSolution>(
            problem,
            crossover,
            mutation,
            100 // Tamaño de la población
        ).setMaxEvaluations(25000)
         .setSolutionListEvaluator(new SequentialSolutionListEvaluator<>())
         .build();

        algorithm.run();

        List<IntegerSolution> population = algorithm.getResult();

        printFinalSolutionSet(population);
    }

    private static AgriculturalData readDataFromXML(String filePath) {
        try {
            // Crear un documento XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(filePath));

            document.getDocumentElement().normalize();

            Element root = document.getDocumentElement();

            // Leer los datos desde el XML
            int cantParcelas = Integer.parseInt(root.getElementsByTagName("cantParcelas").item(0).getTextContent());
            int cantTrimestres = Integer.parseInt(root.getElementsByTagName("cantTrimestres").item(0).getTextContent());
            int cantCultivos = Integer.parseInt(root.getElementsByTagName("cantCultivos").item(0).getTextContent());

            double[] areaParcelas = parseArray(root.getElementsByTagName("areaParcelas").item(0).getTextContent());
            double[] rendimientoCultivo = parseArray(root.getElementsByTagName("rendimientoCultivo").item(0).getTextContent());
            double[] precioCultivo = parseArray(root.getElementsByTagName("precioCultivo").item(0).getTextContent());
            double[] costoMantCultivo = parseArray(root.getElementsByTagName("costoMantCultivo").item(0).getTextContent());

            return new AgriculturalData(cantParcelas, cantTrimestres, cantCultivos, areaParcelas, rendimientoCultivo, precioCultivo, costoMantCultivo);

        } catch (Exception e) {
            throw new RuntimeException("Error leyendo el archivo XML: " + e.getMessage(), e);
        }
    }

    private static double[] parseArray(String data) {
        return Arrays.stream(data.split(","))
                     .map(String::trim)
                     .mapToDouble(Double::parseDouble)
                     .toArray();
    }

    private static class AgriculturalData {
        int cantParcelas;
        int cantTrimestres;
        int cantCultivos;
        double[] areaParcelas;
        double[] rendimientoCultivo;
        double[] precioCultivo;
        double[] costoMantCultivo;

        AgriculturalData(int cantParcelas, int cantTrimestres, int cantCultivos, double[] areaParcelas, double[] rendimientoCultivo, double[] precioCultivo, double[] costoMantCultivo) {
            this.cantParcelas = cantParcelas;
            this.cantTrimestres = cantTrimestres;
            this.cantCultivos = cantCultivos;
            this.areaParcelas = areaParcelas;
            this.rendimientoCultivo = rendimientoCultivo;
            this.precioCultivo = precioCultivo;
            this.costoMantCultivo = costoMantCultivo;
        }
    }
}
