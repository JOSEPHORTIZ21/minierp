package com.minierp.service;

import com.minierp.dto.PageResponse;
import com.minierp.dto.proveedor.ProveedorRequest;
import com.minierp.dto.proveedor.ProveedorResponse;
import com.minierp.entity.Proveedor;
import com.minierp.exception.ConflictoException;
import com.minierp.exception.RecursoNoEncontradoException;
import com.minierp.repository.CompraRepository;
import com.minierp.repository.ProveedorRepository;
import com.minierp.util.Textos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final CompraRepository compraRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProveedorResponse> listar(String buscar, Pageable pageable) {
        Page<Proveedor> pagina = (buscar == null || buscar.isBlank())
                ? proveedorRepository.findAll(pageable)
                : proveedorRepository.findByRazonSocialContainingIgnoreCaseOrNitContaining(buscar.trim(), buscar.trim(), pageable);
        return PageResponse.of(pagina.map(ProveedorResponse::from));
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return ProveedorResponse.from(buscar(id));
    }

    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        String nit = request.nit().trim();
        if (proveedorRepository.existsByNit(nit)) {
            throw new ConflictoException("Ya existe un proveedor con el NIT " + nit);
        }
        Proveedor proveedor = new Proveedor();
        aplicar(proveedor, request);
        return ProveedorResponse.from(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest request) {
        Proveedor proveedor = buscar(id);
        String nit = request.nit().trim();
        if (proveedorRepository.existsByNitAndIdNot(nit, id)) {
            throw new ConflictoException("Ya existe otro proveedor con el NIT " + nit);
        }
        aplicar(proveedor, request);
        return ProveedorResponse.from(proveedor);
    }

    @Transactional
    public void eliminar(Long id) {
        Proveedor proveedor = buscar(id);
        if (compraRepository.existsByProveedorId(id)) {
            throw new ConflictoException("No se puede eliminar el proveedor porque tiene compras registradas");
        }
        proveedorRepository.delete(proveedor);
    }

    Proveedor buscar(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el proveedor", id));
    }

    private void aplicar(Proveedor proveedor, ProveedorRequest request) {
        proveedor.setNit(request.nit().trim());
        proveedor.setRazonSocial(request.razonSocial().trim());
        proveedor.setTelefono(Textos.limpiar(request.telefono()));
        proveedor.setEmail(Textos.limpiar(request.email()));
        proveedor.setDireccion(Textos.limpiar(request.direccion()));
    }
}
