package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.*;
import com.nap.pos.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Datos mínimos que la UI envía por cada producto en la compra.
     * El precioCompraUnitario puede diferir del costo actual del producto.
     */
    public record ItemCompra(Long productoId, int cantidad, BigDecimal precioCompraUnitario) {}

    /**
     * Registra una compra al proveedor:
     * 1. Construye los detalles y calcula el total en el dominio.
     * 2. Por cada producto: incrementa el stock y actualiza el costo.
     *    Actualizar el costo recalcula automáticamente el precioVenta del producto.
     */
    @Transactional
    public Compra registrarCompra(Long proveedorId, Long usuarioId,
                                  String numeroFactura, List<ItemCompra> items) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new BusinessException("Proveedor con ID " + proveedorId + " no encontrado."));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("Usuario con ID " + usuarioId + " no encontrado."));

        // Construir detalles, actualizar stock y recalcular precios
        List<DetalleCompra> detalles = new ArrayList<>();
        for (ItemCompra item : items) {
            Producto producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new BusinessException("Producto con ID " + item.productoId() + " no encontrado."));
            producto.incrementarStock(item.cantidad());
            producto.actualizarCosto(item.precioCompraUnitario()); // recalcula precioVenta
            productoRepository.save(producto);
            detalles.add(new DetalleCompra(producto, item.cantidad(), item.precioCompraUnitario()));
        }

        Compra compra = Compra.builder()
                .proveedor(proveedor)
                .usuario(usuario)
                .numeroFactura(numeroFactura)
                .detalles(detalles)
                .build();
        compra.calcularTotal();
        compra.validar();

        return compraRepository.save(compra);
    }

    // Historial de compras filtrado por proveedor
    public List<Compra> findByProveedorId(Long proveedorId) {
        return compraRepository.findByProveedorId(proveedorId);
    }

    public List<Compra> findAll() {
        return compraRepository.findAll();
    }
}
