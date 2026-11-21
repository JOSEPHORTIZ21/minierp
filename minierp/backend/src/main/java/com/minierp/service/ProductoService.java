package com.minierp.service;

import com.minierp.dto.PageResponse;
import com.minierp.dto.inventario.AjusteInventarioRequest;
import com.minierp.dto.inventario.MovimientoResponse;
import com.minierp.dto.producto.ProductoRequest;
import com.minierp.dto.producto.ProductoResponse;
import com.minierp.entity.MovimientoInventario;
import com.minierp.entity.Producto;
import com.minierp.entity.enums.ConceptoMovimiento;
import com.minierp.entity.enums.TipoMovimiento;
import com.minierp.exception.ConflictoException;
import com.minierp.exception.RecursoNoEncontradoException;
import com.minierp.exception.ReglaNegocioException;
import com.minierp.repository.MovimientoInventarioRepository;
import com.minierp.repository.ProductoRepository;
import com.minierp.repository.ProductoSpecs;
import com.minierp.util.Textos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final CategoriaService categoriaService;
    private final InventarioService inventarioService;

    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listar(String buscar, Long categoriaId, boolean stockBajo,
                                                 boolean incluirInactivos, Pageable pageable) {
        Specification<Producto> spec = Specification.where(ProductoSpecs.soloActivos(incluirInactivos))
                .and(ProductoSpecs.busqueda(buscar))
                .and(ProductoSpecs.deCategoria(categoriaId))
                .and(ProductoSpecs.conStockBajo(stockBajo));
        return PageResponse.of(productoRepository.findAll(spec, pageable).map(ProductoResponse::from));
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.from(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        String codigo = request.codigo().trim().toUpperCase();
        if (productoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictoException("Ya existe un producto con el código " + codigo);
        }
        validarPrecios(request);

        Producto producto = Producto.builder()
                .codigo(codigo)
                .nombre(request.nombre().trim())
                .descripcion(Textos.limpiar(request.descripcion()))
                .precioCompra(request.precioCompra())
                .precioVenta(request.precioVenta())
                .porcentajeIva(request.porcentajeIva())
                .stock(0)
                .stockMinimo(request.stockMinimo())
                .activo(true)
                .categoria(categoriaService.buscar(request.categoriaId()))
                .build();
        producto = productoRepository.save(producto);

        // El stock inicial también deja huella en el kardex
        int stockInicial = request.stockInicial() == null ? 0 : request.stockInicial();
        if (stockInicial > 0) {
            inventarioService.registrarEntrada(producto, stockInicial, ConceptoMovimiento.INVENTARIO_INICIAL,
                    producto.getCodigo(), "Inventario inicial");
        }
        return ProductoResponse.from(producto);
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscar(id);
        String codigo = request.codigo().trim().toUpperCase();
        if (productoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new ConflictoException("Ya existe otro producto con el código " + codigo);
        }
        validarPrecios(request);

        producto.setCodigo(codigo);
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(Textos.limpiar(request.descripcion()));
        producto.setPrecioCompra(request.precioCompra());
        producto.setPrecioVenta(request.precioVenta());
        producto.setPorcentajeIva(request.porcentajeIva());
        producto.setStockMinimo(request.stockMinimo());
        producto.setCategoria(categoriaService.buscar(request.categoriaId()));
        // El stock NO se edita aquí: solo cambia con compras, ventas y ajustes.
        return ProductoResponse.from(producto);
    }

    /** Borrado lógico: el producto puede estar en facturas, así que solo se desactiva. */
    @Transactional
    public void desactivar(Long id) {
        buscar(id).setActivo(false);
    }

    @Transactional
    public ProductoResponse activar(Long id) {
        Producto producto = buscar(id);
        producto.setActivo(true);
        return ProductoResponse.from(producto);
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> kardex(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("el producto", id);
        }
        return movimientoRepository.findByProductoIdOrderByFechaAscIdAsc(id).stream()
                .map(MovimientoResponse::from)
                .toList();
    }

    @Transactional
    public MovimientoResponse ajustarInventario(Long id, AjusteInventarioRequest request) {
        Producto producto = productoRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el producto", id));
        String observacion = request.observacion().trim();
        MovimientoInventario movimiento = request.tipo() == TipoMovimiento.ENTRADA
                ? inventarioService.registrarEntrada(producto, request.cantidad(), ConceptoMovimiento.AJUSTE, "AJUSTE", observacion)
                : inventarioService.registrarSalida(producto, request.cantidad(), ConceptoMovimiento.AJUSTE, "AJUSTE", observacion);
        return MovimientoResponse.from(movimiento);
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el producto", id));
    }

    private void validarPrecios(ProductoRequest request) {
        if (request.precioVenta().compareTo(request.precioCompra()) < 0) {
            throw new ReglaNegocioException("El precio de venta no puede ser menor al precio de compra");
        }
    }
}
