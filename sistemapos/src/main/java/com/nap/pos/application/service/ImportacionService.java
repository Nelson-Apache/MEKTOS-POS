package com.nap.pos.application.service;

import com.nap.pos.application.dto.importacion.*;
import com.nap.pos.domain.model.Cliente;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.model.Proveedor;
import com.nap.pos.domain.model.enums.PlazoPago;
import com.nap.pos.domain.repository.ClienteRepository;
import com.nap.pos.domain.repository.ProductoRepository;
import com.nap.pos.domain.repository.ProveedorRepository;
import com.nap.pos.infrastructure.io.GeneradorPlantilla;
import com.nap.pos.infrastructure.io.LectorArchivo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ImportacionService {

    private final LectorArchivo      lectorArchivo;
    private final GeneradorPlantilla generadorPlantilla;
    private final ProductoRepository productoRepository;
    private final ClienteRepository  clienteRepository;
    private final ProveedorRepository proveedorRepository;

    // ── Plantillas ──────────────────────────────────────────────────────

    public void generarPlantillaProductos(File destino) {
        generadorPlantilla.generarPlantillaProductos(destino);
    }

    public void generarPlantillaClientes(File destino) {
        generadorPlantilla.generarPlantillaClientes(destino);
    }

    // ── Lectura de encabezados ──────────────────────────────────────────

    public List<String> leerEncabezados(File archivo) {
        return lectorArchivo.leerEncabezados(archivo);
    }

    // ── Auto-detección de mapeo ─────────────────────────────────────────

    /**
     * Compara los encabezados del archivo contra los nombres canónicos de cada campo
     * (normalizado: minúsculas + sin tildes + sin espacios extra).
     * Retorna un mapa campo → índice de columna para los campos que coincidan.
     */
    public Map<CampoImportacion, Integer> autoDetectarMapeo(
            List<String> encabezados, TipoEntidad tipo) {

        Map<CampoImportacion, Integer> mapeo = new LinkedHashMap<>();

        List<CampoImportacion> camposDelTipo = Arrays.stream(CampoImportacion.values())
                .filter(c -> c.getTipo() == tipo)
                .toList();

        for (int i = 0; i < encabezados.size(); i++) {
            String norm = normalizar(encabezados.get(i));
            for (CampoImportacion campo : camposDelTipo) {
                if (!mapeo.containsKey(campo) && coincide(norm, campo)) {
                    mapeo.put(campo, i);
                    break;
                }
            }
        }
        return mapeo;
    }

    // ── Validación (sin guardar) ────────────────────────────────────────

    /**
     * Valida todas las filas del archivo con el mapeo indicado.
     * Si algún campo requerido no está mapeado, se añade un error de configuración (fila 0).
     * Retorna lista vacía si todo es correcto.
     */
    public List<FilaError> validarProductos(File archivo, Map<CampoImportacion, Integer> mapeo) {
        List<FilaError> errores = new ArrayList<>();

        for (CampoImportacion campo : CampoImportacion.values()) {
            if (campo.getTipo() == TipoEntidad.PRODUCTO
                    && campo.isRequerido()
                    && !mapeo.containsKey(campo)) {
                errores.add(new FilaError(0, campo.getLabel(),
                        "Campo requerido sin columna mapeada en el archivo"));
            }
        }
        if (!errores.isEmpty()) return errores;

        List<String[]> filas = lectorArchivo.leerFilas(archivo);
        for (int i = 0; i < filas.size(); i++) {
            validarFilaProducto(filas.get(i), mapeo, i + 2, errores);
        }
        return errores;
    }

    public List<FilaError> validarClientes(File archivo, Map<CampoImportacion, Integer> mapeo) {
        List<FilaError> errores = new ArrayList<>();

        for (CampoImportacion campo : CampoImportacion.values()) {
            if (campo.getTipo() == TipoEntidad.CLIENTE
                    && campo.isRequerido()
                    && !mapeo.containsKey(campo)) {
                errores.add(new FilaError(0, campo.getLabel(),
                        "Campo requerido sin columna mapeada en el archivo"));
            }
        }
        if (!errores.isEmpty()) return errores;

        List<String[]> filas = lectorArchivo.leerFilas(archivo);
        for (int i = 0; i < filas.size(); i++) {
            validarFilaCliente(filas.get(i), mapeo, i + 2, errores);
        }
        return errores;
    }

    private void validarFilaProducto(String[] fila, Map<CampoImportacion, Integer> mapeo,
                                      int numFila, List<FilaError> errores) {
        String nombre = getValor(fila, mapeo, CampoImportacion.NOMBRE_PRODUCTO);
        if (nombre.isBlank()) {
            errores.add(new FilaError(numFila, "Nombre", "El nombre es obligatorio"));
        }

        validarDecimalPositivo(fila, mapeo, CampoImportacion.PRECIO_COMPRA,
                "Precio de compra", true, numFila, errores);
        validarDecimalPositivo(fila, mapeo, CampoImportacion.PRECIO_VENTA,
                "Precio de venta", true, numFila, errores);

        String stock = getValor(fila, mapeo, CampoImportacion.STOCK);
        if (stock.isBlank()) {
            errores.add(new FilaError(numFila, "Stock", "El stock es obligatorio"));
        } else {
            try {
                int s = Integer.parseInt(stock.trim());
                if (s < 0) errores.add(new FilaError(numFila, "Stock", "No puede ser negativo"));
            } catch (NumberFormatException e) {
                errores.add(new FilaError(numFila, "Stock",
                        "Debe ser un número entero. Valor: '" + stock + "'"));
            }
        }
    }

    private void validarFilaCliente(String[] fila, Map<CampoImportacion, Integer> mapeo,
                                     int numFila, List<FilaError> errores) {
        String nombre = getValor(fila, mapeo, CampoImportacion.NOMBRE_CLIENTE);
        if (nombre.isBlank()) {
            errores.add(new FilaError(numFila, "Nombre", "El nombre es obligatorio"));
        }

        validarDecimalPositivo(fila, mapeo, CampoImportacion.MONTO_CREDITO,
                "Monto crédito", false, numFila, errores);

        String plazo = getValor(fila, mapeo, CampoImportacion.PLAZO_PAGO);
        if (!plazo.isBlank() && !plazo.equals("15") && !plazo.equals("30")) {
            errores.add(new FilaError(numFila, "Plazo de pago",
                    "Debe ser '15' o '30'. Valor recibido: '" + plazo + "'"));
        }
    }

    /** Valida que el valor de un campo sea un decimal >= 0 (o > 0 si es requerido). */
    private void validarDecimalPositivo(String[] fila, Map<CampoImportacion, Integer> mapeo,
                                         CampoImportacion campo, String label,
                                         boolean obligatorio, int numFila, List<FilaError> errores) {
        String valor = getValor(fila, mapeo, campo);
        if (valor.isBlank()) {
            if (obligatorio) errores.add(new FilaError(numFila, label, "El campo es obligatorio"));
            return;
        }
        try {
            BigDecimal v = new BigDecimal(valor.replace(",", "."));
            if (obligatorio && v.compareTo(BigDecimal.ZERO) <= 0) {
                errores.add(new FilaError(numFila, label, "Debe ser mayor que cero"));
            } else if (!obligatorio && v.compareTo(BigDecimal.ZERO) < 0) {
                errores.add(new FilaError(numFila, label, "No puede ser negativo"));
            }
        } catch (NumberFormatException e) {
            errores.add(new FilaError(numFila, label, "Valor numérico inválido: '" + valor + "'"));
        }
    }

    // ── Detección de duplicados ─────────────────────────────────────────

    public List<DuplicadoEncontrado> detectarDuplicadosProductos(
            File archivo, Map<CampoImportacion, Integer> mapeo) {

        List<DuplicadoEncontrado> duplicados = new ArrayList<>();
        List<String[]> filas = lectorArchivo.leerFilas(archivo);

        for (int i = 0; i < filas.size(); i++) {
            final String[] fila  = filas.get(i);
            final int numFila    = i + 2;
            String codigo        = getValor(fila, mapeo, CampoImportacion.CODIGO_BARRAS);

            if (!codigo.isBlank()) {
                productoRepository.findByCodigoBarras(codigo).ifPresent(existente -> {
                    String actuales = existente.getNombre() + " | $" + existente.getPrecioVenta();
                    String nuevos   = getValor(fila, mapeo, CampoImportacion.NOMBRE_PRODUCTO)
                            + " | $" + getValor(fila, mapeo, CampoImportacion.PRECIO_VENTA);
                    duplicados.add(new DuplicadoEncontrado(numFila, codigo, actuales, nuevos));
                });
            }
        }
        return duplicados;
    }

    public List<DuplicadoEncontrado> detectarDuplicadosClientes(
            File archivo, Map<CampoImportacion, Integer> mapeo) {

        List<DuplicadoEncontrado> duplicados = new ArrayList<>();
        List<String[]> filas = lectorArchivo.leerFilas(archivo);

        for (int i = 0; i < filas.size(); i++) {
            final String[] fila = filas.get(i);
            final int numFila   = i + 2;
            String cedula       = getValor(fila, mapeo, CampoImportacion.CEDULA);

            if (!cedula.isBlank()) {
                clienteRepository.findByCedula(cedula).ifPresent(existente -> {
                    String actuales = existente.getNombre() + " | " + nvl(existente.getCelular());
                    String nuevos   = getValor(fila, mapeo, CampoImportacion.NOMBRE_CLIENTE)
                            + " | " + getValor(fila, mapeo, CampoImportacion.CELULAR);
                    duplicados.add(new DuplicadoEncontrado(numFila, cedula, actuales, nuevos));
                });
            }
        }
        return duplicados;
    }

    // ── Importación ─────────────────────────────────────────────────────

    /**
     * Importa productos del archivo usando el mapeo dado.
     * Las filas cuyo número esté en filasAActualizar se sobrescriben;
     * las demás filas duplicadas se omiten.
     */
    @Transactional
    public ResultadoImportacion importarProductos(File archivo,
            Map<CampoImportacion, Integer> mapeo,
            Set<Integer> filasAActualizar) {

        List<String[]> filas = lectorArchivo.leerFilas(archivo);
        int importados = 0, actualizados = 0, omitidos = 0;

        for (int i = 0; i < filas.size(); i++) {
            String[] fila   = filas.get(i);
            int numFila     = i + 2;
            String codigo   = getValor(fila, mapeo, CampoImportacion.CODIGO_BARRAS);

            Optional<Producto> existente = codigo.isBlank()
                    ? Optional.empty()
                    : productoRepository.findByCodigoBarras(codigo);

            if (existente.isPresent()) {
                if (filasAActualizar.contains(numFila)) {
                    productoRepository.save(construirProducto(fila, mapeo, existente.get().getId()));
                    actualizados++;
                } else {
                    omitidos++;
                }
            } else {
                productoRepository.save(construirProducto(fila, mapeo, null));
                importados++;
            }
        }
        return new ResultadoImportacion(importados, actualizados, omitidos, List.of());
    }

    /**
     * Importa clientes del archivo usando el mapeo dado.
     */
    @Transactional
    public ResultadoImportacion importarClientes(File archivo,
            Map<CampoImportacion, Integer> mapeo,
            Set<Integer> filasAActualizar) {

        List<String[]> filas = lectorArchivo.leerFilas(archivo);
        int importados = 0, actualizados = 0, omitidos = 0;

        for (int i = 0; i < filas.size(); i++) {
            String[] fila   = filas.get(i);
            int numFila     = i + 2;
            String cedula   = getValor(fila, mapeo, CampoImportacion.CEDULA);

            Optional<Cliente> existente = cedula.isBlank()
                    ? Optional.empty()
                    : clienteRepository.findByCedula(cedula);

            if (existente.isPresent()) {
                if (filasAActualizar.contains(numFila)) {
                    clienteRepository.save(construirCliente(fila, mapeo, existente.get().getId()));
                    actualizados++;
                } else {
                    omitidos++;
                }
            } else {
                clienteRepository.save(construirCliente(fila, mapeo, null));
                importados++;
            }
        }
        return new ResultadoImportacion(importados, actualizados, omitidos, List.of());
    }

    // ── Constructores de dominio ────────────────────────────────────────

    private Producto construirProducto(String[] fila, Map<CampoImportacion, Integer> mapeo, Long id) {
        String nit       = getValor(fila, mapeo, CampoImportacion.NIT_PROVEEDOR);
        Proveedor proveedor = nit.isBlank()
                ? null
                : proveedorRepository.findByNit(nit).orElse(null);

        return Producto.builder()
                .id(id)
                .codigoBarras(emptyToNull(getValor(fila, mapeo, CampoImportacion.CODIGO_BARRAS)))
                .nombre(getValor(fila, mapeo, CampoImportacion.NOMBRE_PRODUCTO))
                .precioCompra(parseBD(getValor(fila, mapeo, CampoImportacion.PRECIO_COMPRA)))
                .precioVenta(parseBD(getValor(fila, mapeo, CampoImportacion.PRECIO_VENTA)))
                .stock(parseInt(getValor(fila, mapeo, CampoImportacion.STOCK)))
                .proveedorPrincipal(proveedor)
                .activo(true)
                .build();
    }

    private Cliente construirCliente(String[] fila, Map<CampoImportacion, Integer> mapeo, Long id) {
        String montoStr  = getValor(fila, mapeo, CampoImportacion.MONTO_CREDITO);
        BigDecimal monto = montoStr.isBlank() ? null : parseBD(montoStr);

        PlazoPago plazo = switch (getValor(fila, mapeo, CampoImportacion.PLAZO_PAGO).trim()) {
            case "15" -> PlazoPago.QUINCE_DIAS;
            case "30" -> PlazoPago.TREINTA_DIAS;
            default   -> null;
        };

        return Cliente.builder()
                .id(id)
                .nombre(getValor(fila, mapeo, CampoImportacion.NOMBRE_CLIENTE))
                .cedula(emptyToNull(getValor(fila, mapeo, CampoImportacion.CEDULA)))
                .celular(emptyToNull(getValor(fila, mapeo, CampoImportacion.CELULAR)))
                .direccion(emptyToNull(getValor(fila, mapeo, CampoImportacion.DIRECCION)))
                .montoCredito(monto)
                .plazoPago(plazo)
                .activo(true)
                .build();
    }

    // ── Utilidades ──────────────────────────────────────────────────────

    private String getValor(String[] fila, Map<CampoImportacion, Integer> mapeo, CampoImportacion campo) {
        Integer idx = mapeo.get(campo);
        if (idx == null || idx >= fila.length) return "";
        return fila[idx] == null ? "" : fila[idx].trim();
    }

    private BigDecimal parseBD(String s) {
        if (s.isBlank()) return BigDecimal.ZERO;
        return new BigDecimal(s.replace(",", "."));
    }

    private int parseInt(String s) {
        if (s.isBlank()) return 0;
        return Integer.parseInt(s.trim());
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .trim()
                .replaceAll("\\s+", "_");
    }

    private boolean coincide(String normalizado, CampoImportacion campo) {
        return switch (campo) {
            case CODIGO_BARRAS   -> normalizado.contains("codigo") || normalizado.contains("barras");
            case NOMBRE_PRODUCTO,
                 NOMBRE_CLIENTE  -> normalizado.equals("nombre");
            case PRECIO_COMPRA   -> normalizado.contains("compra") || normalizado.contains("costo");
            case PRECIO_VENTA    -> normalizado.contains("venta") && !normalizado.contains("compra");
            case STOCK           -> normalizado.contains("stock") || normalizado.contains("cantidad");
            case NIT_PROVEEDOR   -> normalizado.contains("nit")   || normalizado.contains("proveedor");
            case CEDULA          -> normalizado.contains("cedula") || normalizado.contains("documento");
            case CELULAR         -> normalizado.contains("celular") || normalizado.contains("telefono");
            case DIRECCION       -> normalizado.contains("direccion");
            case MONTO_CREDITO   -> normalizado.contains("credito") || normalizado.contains("monto");
            case PLAZO_PAGO      -> normalizado.contains("plazo")   || normalizado.contains("dias");
        };
    }
}
