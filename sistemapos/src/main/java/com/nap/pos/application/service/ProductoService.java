package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Producto;
import com.nap.pos.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    @Transactional
    public Producto crear(Producto producto) {
        if (productoRepository.findByCodigoBarras(producto.getCodigoBarras()).isPresent()) {
            throw new BusinessException("Ya existe un producto con código de barras '" + producto.getCodigoBarras() + "'.");
        }
        return productoRepository.save(producto);
    }

    @Transactional
    public Producto actualizar(Producto producto) {
        findById(producto.getId());
        productoRepository.findByCodigoBarras(producto.getCodigoBarras())
                .filter(p -> !p.getId().equals(producto.getId()))
                .ifPresent(p -> {
                    throw new BusinessException("Ya existe un producto con código de barras '" + producto.getCodigoBarras() + "'.");
                });
        return productoRepository.save(producto);
    }

    /** Reactiva el producto para que vuelva a aparecer en la pantalla de ventas. */
    @Transactional
    public void activar(Long id) {
        Producto producto = findById(id);
        producto.activar();
        productoRepository.save(producto);
    }

    /** Desactiva el producto sin eliminarlo — preserva el historial de ventas. */
    @Transactional
    public void desactivar(Long id) {
        Producto producto = findById(id);
        producto.desactivar();
        productoRepository.save(producto);
    }

    /** Actualiza la ruta de imagen del producto. */
    @Transactional
    public void actualizarImagen(Long id, String rutaImagen) {
        Producto producto = findById(id);
        producto.actualizarImagen(rutaImagen);
        productoRepository.save(producto);
    }

    /**
     * Ajusta el stock de un producto.
     * @param esEntrada true → incrementa stock, false → descuenta stock.
     */
    @Transactional
    public void ajustarStock(Long id, int cantidad, boolean esEntrada) {
        Producto producto = findById(id);
        if (esEntrada) {
            producto.incrementarStock(cantidad);
        } else {
            producto.descontarStock(cantidad);
        }
        productoRepository.save(producto);
    }

    // Búsqueda por scanner de código de barras — devuelve Optional para que la UI maneje "no encontrado"
    @Transactional(readOnly = true)
    public Optional<Producto> findByCodigoBarras(String codigoBarras) {
        return productoRepository.findByCodigoBarras(codigoBarras);
    }

    @Transactional(readOnly = true)
    public Producto findById(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Producto con ID " + id + " no encontrado."));
    }

    // Módulo de inventario: todos los productos (activos e inactivos)
    @Transactional(readOnly = true)
    public List<Producto> findAll() {
        return productoRepository.findAll();
    }

    // Pantalla de ventas: solo productos activos
    @Transactional(readOnly = true)
    public List<Producto> findAllActivos() {
        return productoRepository.findAllActivos();
    }
}
