package com.nap.pos.infrastructure.io;

import com.nap.pos.domain.exception.TechnicalException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Genera plantillas Excel (.xlsx) con la estructura esperada por el importador.
 * Columnas requeridas en amarillo, opcionales en gris claro.
 * Una segunda hoja contiene instrucciones de uso.
 */
@Component
public class GeneradorPlantilla {

    private static final String[] COLS_PRODUCTO    = {"codigo_barras", "nombre", "precio_compra", "precio_venta", "stock", "nit_proveedor"};
    private static final boolean[] REQ_PRODUCTO     = {false, true, true, true, true, false};
    private static final String[] EJEMPLO_PRODUCTO  = {"P001", "Agua 500ml", "800", "1200", "50", ""};

    private static final String[] INSTRUCCIONES_PRODUCTO = {
            "INSTRUCCIONES DE USO — Plantilla de Productos",
            "",
            "• Columnas en AMARILLO son obligatorias. Las grises son opcionales.",
            "• codigo_barras: puede dejarse vacío si el producto no tiene código.",
            "• precio_compra y precio_venta: use punto '.' como separador decimal. Ej: 1500.50",
            "• stock: número entero mayor o igual a 0.",
            "• nit_proveedor: NIT exacto del proveedor registrado en el sistema.",
            "  Si el NIT no coincide, el producto se guarda sin proveedor (se asigna después).",
            "• No modifique los nombres de los encabezados para que el sistema los detecte automáticamente."
    };

    private static final String[] COLS_CLIENTE     = {"nombre", "cedula", "celular", "direccion", "monto_credito", "plazo_pago"};
    private static final boolean[] REQ_CLIENTE      = {true, false, false, false, false, false};
    private static final String[] EJEMPLO_CLIENTE   = {"Juan García", "12345678", "3001234567", "Calle 5 #10-20", "500000", "30"};

    private static final String[] INSTRUCCIONES_CLIENTE = {
            "INSTRUCCIONES DE USO — Plantilla de Clientes",
            "",
            "• Columnas en AMARILLO son obligatorias. Las grises son opcionales.",
            "• nombre: nombre completo del cliente.",
            "• cedula: número de cédula. Si se omite, no se podrá detectar duplicados automáticamente.",
            "• monto_credito: límite de crédito aprobado. Dejar vacío si el cliente no tiene crédito.",
            "• plazo_pago: escribir 15 (quince días) o 30 (treinta días).",
            "  Dejar vacío si el cliente no tiene crédito habilitado.",
            "• No modifique los nombres de los encabezados para que el sistema los detecte automáticamente."
    };

    public void generarPlantillaProductos(File destino) {
        generarPlantilla(destino, COLS_PRODUCTO, REQ_PRODUCTO, EJEMPLO_PRODUCTO, INSTRUCCIONES_PRODUCTO);
    }

    public void generarPlantillaClientes(File destino) {
        generarPlantilla(destino, COLS_CLIENTE, REQ_CLIENTE, EJEMPLO_CLIENTE, INSTRUCCIONES_CLIENTE);
    }

    private void generarPlantilla(File destino, String[] columnas, boolean[] requeridos,
                                   String[] ejemplo, String[] instrucciones) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            CellStyle estiloRequerido = crearEstilo(wb, IndexedColors.YELLOW,       true);
            CellStyle estiloOpcional  = crearEstilo(wb, IndexedColors.GREY_25_PERCENT, true);
            CellStyle estiloEjemplo   = crearEstiloEjemplo(wb);

            // ── Hoja de datos ────────────────────────────────────────────
            Sheet datos = wb.createSheet("Datos");

            Row encabezado = datos.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = encabezado.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(requeridos[i] ? estiloRequerido : estiloOpcional);
                datos.setColumnWidth(i, 5500);
            }

            Row filaEjemplo = datos.createRow(1);
            for (int i = 0; i < ejemplo.length; i++) {
                Cell cell = filaEjemplo.createCell(i);
                cell.setCellValue(ejemplo[i]);
                cell.setCellStyle(estiloEjemplo);
            }

            // ── Hoja de instrucciones ────────────────────────────────────
            Sheet hojaInstr = wb.createSheet("Instrucciones");
            hojaInstr.setColumnWidth(0, 18000);
            for (int i = 0; i < instrucciones.length; i++) {
                Row row  = hojaInstr.createRow(i);
                Cell cell = row.createCell(0);
                cell.setCellValue(instrucciones[i]);
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                wb.write(fos);
            }

        } catch (Exception e) {
            throw new TechnicalException("Error al generar la plantilla: " + e.getMessage());
        }
    }

    private CellStyle crearEstilo(XSSFWorkbook wb, IndexedColors color, boolean negrita) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = wb.createFont();
        font.setBold(negrita);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearEstiloEjemplo(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
