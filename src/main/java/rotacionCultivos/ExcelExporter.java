package rotacionCultivos;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import rotacionCultivos.Main.AgriculturalData;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelExporter {

	public static void saveSolutionsToExcel(String fileName, IntegerSolution greedyProfit,
	        IntegerSolution greedyDiversity, List<IntegerSolution> solutions, AgriculturalData data) {
	    Workbook workbook = new XSSFWorkbook();
	    Sheet sheet = workbook.createSheet("Solutions");

	    // Crear estilos para encabezados y valores monetarios
	    CellStyle headerStyle = workbook.createCellStyle();
	    Font headerFont = workbook.createFont();
	    headerFont.setBold(true);
	    headerStyle.setFont(headerFont);

	    CellStyle currencyStyle = workbook.createCellStyle();
	    DataFormat format = workbook.createDataFormat();
	    currencyStyle.setDataFormat(format.getFormat("$#,##0.00"));

	    int rowNum = 0;

	    // Agregar soluciones greedy con tabla de cultivos al lado
	    rowNum = addSolutionWithCultivoTable(sheet, "Solución Greedy Ganancia", greedyProfit, data, headerStyle, currencyStyle, rowNum);
	    rowNum = addSolutionToSheet(sheet, "Solución Greedy Diversidad", greedyDiversity, data, headerStyle, currencyStyle, rowNum);

	    // Agregar soluciones del algoritmo evolutivo
	    int solutionCounter = 1;
	    for (IntegerSolution solution : solutions) {
	        rowNum = addSolutionToSheet(sheet, "Solución " + solutionCounter++, solution, data, headerStyle, currencyStyle, rowNum);
	    }

	    // Ajustar automáticamente el tamaño de las columnas
	    for (int col = 0; col <= data.cantSemestres + 6; col++) {
	        sheet.autoSizeColumn(col);
	    }

	    // Guardar el archivo Excel
	    try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
	        workbook.write(fileOut);
	        workbook.close();
	    } catch (IOException e) {
	        System.err.println("Error al escribir el archivo Excel: " + e.getMessage());
	    }
	}

	// Método para agregar soluciones con tabla de cultivos al lado
	private static int addSolutionWithCultivoTable(Sheet sheet, String solutionName, IntegerSolution solution,
	        AgriculturalData data, CellStyle headerStyle, CellStyle currencyStyle, int startRow) {
	    int rowNum = startRow;

	    // Crear encabezado para la solución
	    Row headerRow = sheet.createRow(rowNum++);
	    Cell solucionHeader = headerRow.createCell(0);
	    solucionHeader.setCellValue(solutionName);
	    solucionHeader.setCellStyle(headerStyle);

	    Cell parcelaHeader = headerRow.createCell(1);
	    parcelaHeader.setCellValue("Parcela");
	    parcelaHeader.setCellStyle(headerStyle);

	    for (int t = 0; t < data.cantSemestres; t++) {
	        Cell cell = headerRow.createCell(t + 2);
	        cell.setCellValue("Semestre " + (t + 1));
	        cell.setCellStyle(headerStyle);
	    }

	    Cell gananciaHeader = headerRow.createCell(data.cantSemestres + 2);
	    gananciaHeader.setCellValue("Ganancia Total");
	    gananciaHeader.setCellStyle(headerStyle);

	    Cell diversidadHeader = headerRow.createCell(data.cantSemestres + 3);
	    diversidadHeader.setCellValue("Diversidad");
	    diversidadHeader.setCellStyle(headerStyle);

	    // Encabezado de tabla de cultivos al lado
	    Cell cultivoHeader1 = headerRow.createCell(data.cantSemestres + 4);
	    cultivoHeader1.setCellValue("Nro Cultivo");
	    cultivoHeader1.setCellStyle(headerStyle);

	    Cell cultivoHeader2 = headerRow.createCell(data.cantSemestres + 5);
	    cultivoHeader2.setCellValue("Nombre Cultivo");
	    cultivoHeader2.setCellStyle(headerStyle);

	    // Agregar filas de solución y tabla de cultivos
	    for (int parcela = 0; parcela < data.cantParcelas; parcela++) {
	        Row row = sheet.createRow(rowNum++);

	        // Agregar número de parcela
	        Cell parcelaCell = row.createCell(1);
	        parcelaCell.setCellValue("Parcela " + (parcela + 1));

	        // Llenar datos de cultivos por semestre
	        for (int semestre = 0; semestre < data.cantSemestres; semestre++) {
	            int variableIndex = parcela * data.cantSemestres + semestre;
	            int cultivo = solution.getVariable(variableIndex);
	            row.createCell(semestre + 2).setCellValue(cultivo);
	        }

	        // Agregar ganancia y diversidad solo en la primera fila de la solución
	        if (parcela == 0) {
	            double totalProfit = -solution.getObjective(0); // Negativo porque se maximizó
	            double diversity = -solution.getObjective(1); // Negativo porque se maximizó
	            Cell profitCell = row.createCell(data.cantSemestres + 2);
	            profitCell.setCellValue(totalProfit);
	            profitCell.setCellStyle(currencyStyle);
	            row.createCell(data.cantSemestres + 3).setCellValue(diversity);
	        }

	        // Agregar tabla de cultivos al lado
	        for (int i = 0; i < data.nombreCultivo.length; i++) {
	            Row cultivoRow = sheet.getRow(startRow + i + 1);
	            if (cultivoRow == null) cultivoRow = sheet.createRow(startRow + i + 1);

	            cultivoRow.createCell(data.cantSemestres + 4).setCellValue(i); // Número de cultivo
	            cultivoRow.createCell(data.cantSemestres + 5).setCellValue(data.nombreCultivo[i]); // Nombre del cultivo
	        }
	    }

	    // Agregar una fila en blanco para separar las soluciones
	    return rowNum + 1;
	}

	private static int addSolutionToSheet(Sheet sheet, String solutionName, IntegerSolution solution,
			AgriculturalData data, CellStyle headerStyle, CellStyle currencyStyle, int startRow) {
		int rowNum = startRow;

		// Crear encabezado para la solución
		Row headerRow = sheet.createRow(rowNum++);
		Cell solucionHeader = headerRow.createCell(0);
		solucionHeader.setCellValue(solutionName);
		solucionHeader.setCellStyle(headerStyle);

		Cell parcelaHeader = headerRow.createCell(1);
		parcelaHeader.setCellValue("Parcela");
		parcelaHeader.setCellStyle(headerStyle);

		for (int t = 0; t < data.cantSemestres; t++) {
			Cell cell = headerRow.createCell(t + 2);
			cell.setCellValue("Semestre " + (t + 1));
			cell.setCellStyle(headerStyle);
		}

		Cell gananciaHeader = headerRow.createCell(data.cantSemestres + 2);
		gananciaHeader.setCellValue("Ganancia Total");
		gananciaHeader.setCellStyle(headerStyle);

		Cell diversidadHeader = headerRow.createCell(data.cantSemestres + 3);
		diversidadHeader.setCellValue("Diversidad");
		diversidadHeader.setCellStyle(headerStyle);

		for (int parcela = 0; parcela < data.cantParcelas; parcela++) {
			Row row = sheet.createRow(rowNum++);

			// Agregar número de parcela
			Cell parcelaCell = row.createCell(1);
			parcelaCell.setCellValue("Parcela " + (parcela + 1));

			// Llenar datos de cultivos por semestre
			for (int semestre = 0; semestre < data.cantSemestres; semestre++) {
				int variableIndex = parcela * data.cantSemestres + semestre;
				int cultivo = solution.getVariable(variableIndex);
				row.createCell(semestre + 2).setCellValue(cultivo);
			}

			// Agregar ganancia y diversidad solo en la primera fila de la solución
			if (parcela == 0) {
				double totalProfit = -solution.getObjective(0); // Negativo porque se maximizó
				double diversity = -solution.getObjective(1); // Negativo porque se maximizó
				Cell profitCell = row.createCell(data.cantSemestres + 2);
				profitCell.setCellValue(totalProfit);
				profitCell.setCellStyle(currencyStyle);
				row.createCell(data.cantSemestres + 3).setCellValue(diversity);
			}
		}

		// Agregar una fila en blanco para separar las soluciones
		return rowNum + 1;
	}

	private static int addCultivoTable(Sheet sheet, AgriculturalData data, CellStyle headerStyle, int startRow) {
	    int rowNum = startRow;

	    // Crear encabezado de la tabla
	    Row headerRow = sheet.createRow(rowNum++);
	    Cell nroCultivoHeader = headerRow.createCell(1);
	    nroCultivoHeader.setCellValue("Nro Cultivo");
	    nroCultivoHeader.setCellStyle(headerStyle);

	    Cell nombreCultivoHeader = headerRow.createCell(1);
	    nombreCultivoHeader.setCellValue("Nombre Cultivo");
	    nombreCultivoHeader.setCellStyle(headerStyle);

	    // Agregar filas con números y nombres de cultivos
	    for (int i = 0; i < data.nombreCultivo.length; i++) {
	        Row row = sheet.createRow(rowNum++);
	        row.createCell(1).setCellValue(i); // Número de cultivo
	        row.createCell(2).setCellValue(data.nombreCultivo[i]); // Nombre del cultivo
	    }

	    return rowNum;
	}
}
