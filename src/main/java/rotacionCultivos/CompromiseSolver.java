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
        double maxGanancia = points.stream().mapToDouble(p -> p.ganancia).min().orElse(0);
        double maxDiversidad = points.stream().mapToDouble(p -> p.diversidad).min().orElse(0);

        Point best = null;
        double bestDist = Double.MAX_VALUE;
        for (Point p : points) {
            double dist = Math.sqrt(Math.pow((maxGanancia - p.ganancia), 2) +
                    Math.pow((maxDiversidad - p.diversidad), 2));
            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    public static Point calculateCompromiseGreedy(List<Point> points, Point greedyProfit, Point greedyDiversity) {
        double idealGanancia = greedyProfit.ganancia;
        double idealDiversidad = greedyDiversity.diversidad;

        Point best = null;
        double bestDist = Double.MAX_VALUE;
        for (Point p : points) {
            double dist = Math.sqrt(Math.pow((idealGanancia - p.ganancia), 2) +
                    Math.pow((idealDiversidad - p.diversidad), 2));
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
            System.out.println("Solución sin resultados de greedy:");
            System.out.println("Compromise Solution: Ganancia=" + -compromise.ganancia +
                    ", Diversidad=" + -compromise.diversidad);

            Point compromiseGreedy = calculateCompromiseGreedy(points, profitPoint, diversityPoint);
            System.out.println("Solución con resultados de greedy:");
            System.out.println("Compromise Solution: Ganancia=" + -compromiseGreedy.ganancia +
                    ", Diversidad=" + -compromiseGreedy.diversidad);
        }

    }
}
