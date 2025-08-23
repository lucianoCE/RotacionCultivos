package rotacionCultivos;

import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.io.*;
import java.util.*;

public class CompromiseSolver {

    static class Point {
        double ganancia;
        double diversidad;
        Point(double g, double d) { this.ganancia = g; this.diversidad = d; }
    }

    public static Point calculateCompromiseAbsolute(List<Point> points) {
        // Ideal utópico por componente
        double idealGanancia = points.stream().mapToDouble(p -> p.ganancia).min().orElse(0);
        double idealDiversidad = points.stream().mapToDouble(p -> p.diversidad).min().orElse(0);

        // Rango de cada objetivo
        double minGanancia = points.stream().mapToDouble(p -> p.ganancia).min().orElse(1);
        double maxGanancia = points.stream().mapToDouble(p -> p.ganancia).max().orElse(1);
        double rangeGanancia = maxGanancia - minGanancia;

        double minDiversidad = points.stream().mapToDouble(p -> p.diversidad).min().orElse(1);
        double maxDiversidad = points.stream().mapToDouble(p -> p.diversidad).max().orElse(1);
        double rangeDiversidad = maxDiversidad - minDiversidad;

        Point best = null;
        double bestDist = Double.MAX_VALUE;

        for (Point p : points) {
            double normGanancia = (p.ganancia - idealGanancia) / rangeGanancia;
            double normDiversidad = (p.diversidad - idealDiversidad) / rangeDiversidad;
            double dist = Math.sqrt(normGanancia * normGanancia + normDiversidad * normDiversidad);

            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    public static Point calculateCompromiseGreedy(List<Point> points, Point greedyProfit, Point greedyDiversity) {
        // Punto ideal definido por los greedy
        double idealGanancia = greedyProfit.ganancia;
        double idealDiversidad = greedyDiversity.diversidad;

        // Rango de cada objetivo
        double minGanancia = points.stream().mapToDouble(p -> p.ganancia).min().orElse(1);
        double maxGanancia = points.stream().mapToDouble(p -> p.ganancia).max().orElse(1);
        double rangeGanancia = maxGanancia - minGanancia;

        double minDiversidad = points.stream().mapToDouble(p -> p.diversidad).min().orElse(1);
        double maxDiversidad = points.stream().mapToDouble(p -> p.diversidad).max().orElse(1);
        double rangeDiversidad = maxDiversidad - minDiversidad;

        Point best = null;
        double bestDist = Double.MAX_VALUE;

        for (Point p : points) {
            double normGanancia = (p.ganancia - idealGanancia) / rangeGanancia;
            double normDiversidad = (p.diversidad - idealDiversidad) / rangeDiversidad;
            double dist = Math.sqrt(normGanancia * normGanancia + normDiversidad * normDiversidad);

            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    public static List<Point> readCSV(String path) throws IOException {
        List<Point> points = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                double ganancia = Double.parseDouble(tokens[0].trim());
                double diversidad = Double.parseDouble(tokens[1].trim());
                points.add(new Point(ganancia, diversidad));
            }
        }
        return points;
    }

    public static void main(String[] args) throws IOException {
        Set<String> files = new HashSet<>();
        files.add("instanciaChica");
        files.add("instanciaMediana");
        files.add("instanciaGrande");

        for (String file : files) {
            System.out.println("\nCompromiso para " + file);
            List<Point> points = readCSV("frentes_pareto/pareto_" + file + ".csv");

            Main.AgriculturalData data = Main.readDataFromXML("src/main/resources/instancias/"+ file +".xml");
            GreedyAgriculturalSolver solverDiversity = new GreedyAgriculturalSolver(
                    data.cantParcelas, data.cantSemestres,
                    data.cantCultivos, data.areaParcelas, data.rendimientoCultivoChico,
                    data.rendimientoCultivoMediano, data.rendimientoCultivoGrande,
                    data.precioCultivo, data.costoMantCultivo, data.temporadaCultivo,
                    "diversidad");
            GreedyAgriculturalSolver.Result cropPlanDiversity = solverDiversity.solve();

            GreedyAgriculturalSolver solverProfit = new GreedyAgriculturalSolver(
                    data.cantParcelas, data.cantSemestres,
                    data.cantCultivos, data.areaParcelas, data.rendimientoCultivoChico,
                    data.rendimientoCultivoMediano, data.rendimientoCultivoGrande,
                    data.precioCultivo, data.costoMantCultivo, data.temporadaCultivo,
                    "ganancia");
            GreedyAgriculturalSolver.Result cropPlanProfit = solverProfit.solve();

            AgriculturalOptimizationProblem problem = new AgriculturalOptimizationProblem(data.cantParcelas, data.cantFilas,
                    data.cantSemestres, data.cantCultivos, data.areaParcelas, data.rendimientoCultivoChico,
                    data.rendimientoCultivoMediano, data.rendimientoCultivoGrande, data.precioCultivo,
                    data.costoMantCultivo, data.temporadaCultivo);

            List<IntegerSolution> greedyProfitResult = solverProfit.initializePopulation(problem, cropPlanProfit.cropPlan,
                    cropPlanProfit.totalProfit, cropPlanProfit.diversityScore);

            List<IntegerSolution> greedyDiversityResult = solverDiversity.initializePopulation(problem,
                    cropPlanDiversity.cropPlan, cropPlanDiversity.totalProfit, cropPlanDiversity.diversityScore);

            Point profitPoint = new Point(greedyProfitResult.get(0).getObjective(0), greedyProfitResult.get(0).getObjective(1));
            Point diversityPoint = new Point(greedyDiversityResult.get(0).getObjective(0), greedyDiversityResult.get(0).getObjective(1));



            Point compromise = calculateCompromiseAbsolute(points);
            System.out.println("Solución sin resultados de greedy: Ganancia=" + -compromise.ganancia +
                    ", Diversidad=" + -compromise.diversidad);

            Point compromiseGreedy = calculateCompromiseGreedy(points, profitPoint, diversityPoint);
            System.out.println("Solución con resultados de greedy Ganancia=" + -compromiseGreedy.ganancia +
                    ", Diversidad=" + -compromiseGreedy.diversidad);
        }

    }
}
