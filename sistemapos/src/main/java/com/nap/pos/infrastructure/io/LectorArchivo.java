package com.nap.pos.infrastructure.io;

import com.nap.pos.domain.exception.TechnicalException;
import com.opencsv.CSVReader;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lee archivos CSV y Excel (.xlsx/.xls) y devuelve su contenido como listas de strings.
 * La detección del formato es automática por extensión de archivo.
 */
@Component
public class LectorArchivo {

    /**
     * Devuelve la primera fila del archivo (encabezados de columnas).
     */
    public List<String> leerEncabezados(File archivo) {
        List<String[]> todas = leerTodasLasFilas(archivo);
        if (todas.isEmpty()) return List.of();
        return Arrays.asList(todas.get(0));
    }

    /**
     * Devuelve todas las filas del archivo excepto la primera (encabezado).
     */
    public List<String[]> leerFilas(File archivo) {
        List<String[]> todas = leerTodasLasFilas(archivo);
        if (todas.size() <= 1) return List.of();
        return new ArrayList<>(todas.subList(1, todas.size()));
    }

    private List<String[]> leerTodasLasFilas(File archivo) {
        String nombre = archivo.getName().toLowerCase();
        try {
            if (nombre.endsWith(".csv")) {
                return leerCsv(archivo);
            } else if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
                return leerExcel(archivo);
            } else {
                throw new TechnicalException(
                        "Formato no soportado. Use .csv, .xlsx o .xls");
            }
        } catch (TechnicalException e) {
            throw e;
        } catch (Exception e) {
            throw new TechnicalException("Error al leer el archivo: " + e.getMessage());
        }
    }

    private List<String[]> leerCsv(File archivo) throws Exception {
        List<String[]> filas = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(archivo))) {
            String[] fila;
            while ((fila = reader.readNext()) != null) {
                filas.add(fila);
            }
        }
        return filas;
    }

    private List<String[]> leerExcel(File archivo) throws Exception {
        List<String[]> filas = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int numCols = 0;

            for (Row row : sheet) {
                // Determinar ancho de columnas por la primera fila
                if (numCols == 0) {
                    numCols = row.getLastCellNum();
                }
                String[] valores = new String[numCols];
                for (int i = 0; i < numCols; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    valores[i] = (cell == null) ? "" : formatter.formatCellValue(cell).trim();
                }
                filas.add(valores);
            }
        }
        return filas;
    }
}
