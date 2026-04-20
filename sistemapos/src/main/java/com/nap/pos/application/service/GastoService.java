package com.nap.pos.application.service;

import com.nap.pos.domain.exception.BusinessException;
import com.nap.pos.domain.model.Gasto;
import com.nap.pos.domain.model.Usuario;
import com.nap.pos.domain.model.enums.FuenteGasto;
import com.nap.pos.domain.model.enums.TipoGasto;
import com.nap.pos.domain.repository.CajaRepository;
import com.nap.pos.domain.repository.GastoRepository;
import com.nap.pos.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GastoService {

    private final GastoRepository gastoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaRepository cajaRepository;

    @Transactional
    public Gasto registrar(Long usuarioId, TipoGasto tipo, FuenteGasto fuentePago,
                           String concepto, String categoria,
                           BigDecimal monto, String proveedor, String referencia, String notas) {
        if (FuenteGasto.CAJA.equals(fuentePago) && cajaRepository.findCajaAbierta().isEmpty()) {
            throw new BusinessException("No hay una caja abierta. Abre la caja antes de registrar un gasto pagado en efectivo.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BusinessException("Usuario con ID " + usuarioId + " no encontrado."));

        Gasto gasto = Gasto.builder()
                .tipo(tipo)
                .fuentePago(fuentePago)
                .concepto(concepto)
                .categoria(categoria)
                .monto(monto)
                .proveedor(proveedor)
                .referencia(referencia)
                .notas(notas)
                .usuario(usuario)
                .build();
        gasto.validar();
        return gastoRepository.save(gasto);
    }

    @Transactional(readOnly = true)
    public List<Gasto> findAll() {
        return gastoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Gasto> findByTipo(TipoGasto tipo) {
        return gastoRepository.findByTipo(tipo);
    }

    @Transactional
    public void eliminar(Long gastoId) {
        gastoRepository.findById(gastoId)
                .orElseThrow(() -> new BusinessException("Gasto con ID " + gastoId + " no encontrado."));
        gastoRepository.deleteById(gastoId);
    }
}
