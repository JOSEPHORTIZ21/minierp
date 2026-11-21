package com.minierp.service;

import com.minierp.entity.MovimientoInventario;
import com.minierp.entity.Producto;
import com.minierp.entity.enums.ConceptoMovimiento;
import com.minierp.entity.enums.TipoMovimiento;
import com.minierp.exception.ReglaNegocioException;
import com.minierp.exception.StockInsuficienteException;
import com.minierp.repository.MovimientoInventarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Único punto del sistema que modifica el stock.
 * Cada cambio queda registrado como un movimiento (kardex).
 * MANDATORY: debe llamarse dentro de una transacción abierta (venta, compra o ajuste),
 * para que si algo falla se revierta todo junto.
 */
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final MovimientoInventarioRepository movimientoRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public MovimientoInventario registrarEntrada(Producto producto, int cantidad, ConceptoMovimiento concepto,
                                                 String referencia, String observacion) {
        return registrar(producto, TipoMovimiento.ENTRADA, cantidad, concepto, referencia, observacion);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public MovimientoInventario registrarSalida(Producto producto, int cantidad, ConceptoMovimiento concepto,
                                                String referencia, String observacion) {
        if (producto.getStock() < cantidad) {
            throw new StockInsuficienteException(producto.getNombre(), producto.getStock(), cantidad);
        }
        return registrar(producto, TipoMovimiento.SALIDA, cantidad, concepto, referencia, observacion);
    }

    private MovimientoInventario registrar(Producto producto, TipoMovimiento tipo, int cantidad,
                                           ConceptoMovimiento concepto, String referencia, String observacion) {
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad del movimiento debe ser mayor a cero");
        }
        int anterior = producto.getStock();
        int resultante = tipo == TipoMovimiento.ENTRADA ? anterior + cantidad : anterior - cantidad;
        producto.setStock(resultante); // la entidad está gestionada: Hibernate guarda el cambio al confirmar

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .producto(producto)
                .tipo(tipo)
                .concepto(concepto)
                .cantidad(cantidad)
                .stockAnterior(anterior)
                .stockResultante(resultante)
                .referencia(referencia)
                .observacion(observacion)
                .fecha(LocalDateTime.now())
                .build();
        return movimientoRepository.save(movimiento);
    }
}
