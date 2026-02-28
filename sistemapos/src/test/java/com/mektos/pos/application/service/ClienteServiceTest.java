package com.mektos.pos.application.service;

import com.mektos.pos.domain.exception.BusinessException;
import com.mektos.pos.domain.model.Cliente;
import com.mektos.pos.domain.model.enums.PlazoPago;
import com.mektos.pos.domain.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    // --- crear ---

    @Test
    void crear_cedulaNueva_guarda() {
        Cliente nuevo = clienteBuilder(null, "123456", new BigDecimal("500000")).build();
        when(clienteRepository.findByCedula("123456")).thenReturn(Optional.empty());
        when(clienteRepository.save(nuevo)).thenReturn(nuevo);

        Cliente resultado = clienteService.crear(nuevo);

        assertThat(resultado.getCedula()).isEqualTo("123456");
        verify(clienteRepository).save(nuevo);
    }

    @Test
    void crear_cedulaDuplicada_lanzaBusinessException() {
        Cliente existente = clienteBuilder(1L, "123456", BigDecimal.ZERO).build();
        Cliente nuevo = clienteBuilder(null, "123456", BigDecimal.ZERO).build();
        when(clienteRepository.findByCedula("123456")).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> clienteService.crear(nuevo));
        verify(clienteRepository, never()).save(any());
    }

    // --- abonar ---

    @Test
    void abonar_montoValido_reduceSaldoUtilizado() {
        // Cliente con crédito de 500.000 y ya debe 200.000
        Cliente cliente = clienteBuilder(1L, "123456", new BigDecimal("500000"))
                .saldoUtilizado(new BigDecimal("200000"))
                .build();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clienteService.abonar(1L, new BigDecimal("50000"));

        // Saldo utilizado debe reducirse de 200.000 a 150.000
        verify(clienteRepository).save(argThat(c ->
                c.getSaldoUtilizado().compareTo(new BigDecimal("150000")) == 0
        ));
    }

    @Test
    void abonar_montoMayorQueDeuda_quedaEnCero() {
        // Abona más de lo que debe — el saldo se queda en 0 (no queda negativo)
        Cliente cliente = clienteBuilder(1L, "123456", new BigDecimal("500000"))
                .saldoUtilizado(new BigDecimal("100000"))
                .build();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clienteService.abonar(1L, new BigDecimal("999999"));

        verify(clienteRepository).save(argThat(c ->
                c.getSaldoUtilizado().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    // --- activar / desactivar ---

    @Test
    void activar_clienteInactivo_cambiaActivoATrue() {
        Cliente inactivo = clienteBuilder(1L, "111", BigDecimal.ZERO).activo(false).build();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(inactivo));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clienteService.activar(1L);

        verify(clienteRepository).save(argThat(c -> c.isActivo()));
    }

    @Test
    void desactivar_clienteActivo_cambiaActivoAFalse() {
        Cliente activo = clienteBuilder(1L, "111", BigDecimal.ZERO).activo(true).build();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(activo));
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clienteService.desactivar(1L);

        verify(clienteRepository).save(argThat(c -> !c.isActivo()));
    }

    // --- helper ---

    private Cliente.ClienteBuilder clienteBuilder(Long id, String cedula, BigDecimal montoCredito) {
        return Cliente.builder()
                .id(id).nombre("Cliente Test").cedula(cedula).celular("3001234567")
                .montoCredito(montoCredito).plazoPago(PlazoPago.TREINTA_DIAS).activo(true);
    }
}
