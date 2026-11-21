package com.minierp.service;

import com.minierp.dto.PageResponse;
import com.minierp.dto.compra.CompraRequest;
import com.minierp.dto.compra.CompraResponse;
import com.minierp.dto.compra.CompraResumenResponse;
import com.minierp.dto.compra.ItemCompraRequest;
import com.minierp.entity.Compra;
import com.minierp.entity.DetalleCompra;
import com.minierp.entity.Producto;
import com.minierp.entity.Proveedor;
import com.minierp.entity.enums.ConceptoMovimiento;
import com.minierp.exception.ConflictoException;
import com.minierp.exception.RecursoNoEncontradoException;
import com.minierp.exception.ReglaNegocioException;
import com.minierp.repository.CompraRepository;
import com.minierp.repository.ProductoRepository;
import com.minierp.util.Montos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorService proveedorService;
    private final InventarioService inventarioService;

    @Transactional(readOnly = true)
    public PageResponse<CompraResumenResponse> listar(Pageable pageable) {
        return PageResponse.of(compraRepository.findAll(pageable).map(CompraResumenResponse::from));
    }

    @Transactional(readOnly = true)
    public CompraResponse obtener(Long id) {
        return CompraResponse.from(compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("la compra", id)));
    }

    /** Registra la factura del proveedor, suma el stock y actualiza el último costo de cada producto. */
    @Transactional
    public CompraResponse registrar(CompraRequest request) {
        Proveedor proveedor = proveedorService.buscar(request.proveedorId());
        String numeroFactura = request.numeroFactura().trim().toUpperCase();
        if (compraRepository.existsByProveedorIdAndNumeroFacturaIgnoreCase(proveedor.getId(), numeroFactura)) {
            throw new ConflictoException("La factura " + numeroFactura + " de este proveedor ya fue registrada");
        }
        validarSinRepetidos(request.items());

        Compra compra = Compra.builder()
                .numeroFactura(numeroFactura)
                .fecha(LocalDateTime.now())
                .proveedor(proveedor)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;
        List<ItemCompraRequest> items = request.items().stream()
                .sorted(Comparator.comparing(ItemCompraRequest::productoId))
                .toList();

        for (ItemCompraRequest item : items) {
            Producto producto = productoRepository.findByIdParaActualizar(item.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("el producto", item.productoId()));

            inventarioService.registrarEntrada(producto, item.cantidad(), ConceptoMovimiento.COMPRA,
                    numeroFactura, "Compra a " + proveedor.getRazonSocial());
            producto.setPrecioCompra(item.costoUnitario()); // último costo

            BigDecimal sub = Montos.multiplicar(item.costoUnitario(), item.cantidad());
            BigDecimal iva = Montos.calcularIva(sub, producto.getPorcentajeIva());
            compra.agregarDetalle(DetalleCompra.builder()
                    .producto(producto)
                    .cantidad(item.cantidad())
                    .costoUnitario(item.costoUnitario())
                    .porcentajeIva(producto.getPorcentajeIva())
                    .subtotal(sub)
                    .valorIva(iva)
                    .total(sub.add(iva))
                    .build());

            subtotal = subtotal.add(sub);
            totalIva = totalIva.add(iva);
        }

        compra.setSubtotal(subtotal);
        compra.setTotalIva(totalIva);
        compra.setTotal(subtotal.add(totalIva));
        return CompraResponse.from(compraRepository.save(compra));
    }

    private void validarSinRepetidos(List<ItemCompraRequest> items) {
        Set<Long> vistos = new HashSet<>();
        for (ItemCompraRequest item : items) {
            if (!vistos.add(item.productoId())) {
                throw new ReglaNegocioException("Un producto aparece repetido en la compra (id " + item.productoId() + ")");
            }
        }
    }
}
