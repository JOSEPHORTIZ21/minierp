package com.minierp.service;

import com.minierp.dto.PageResponse;
import com.minierp.dto.cliente.ClienteRequest;
import com.minierp.dto.cliente.ClienteResponse;
import com.minierp.entity.Cliente;
import com.minierp.exception.ConflictoException;
import com.minierp.exception.RecursoNoEncontradoException;
import com.minierp.repository.ClienteRepository;
import com.minierp.repository.VentaRepository;
import com.minierp.util.Textos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;

    @Transactional(readOnly = true)
    public PageResponse<ClienteResponse> listar(String buscar, Pageable pageable) {
        Page<Cliente> pagina = (buscar == null || buscar.isBlank())
                ? clienteRepository.findAll(pageable)
                : clienteRepository.findByNombreContainingIgnoreCaseOrDocumentoContaining(buscar.trim(), buscar.trim(), pageable);
        return PageResponse.of(pagina.map(ClienteResponse::from));
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return ClienteResponse.from(buscar(id));
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        String documento = request.documento().trim();
        if (clienteRepository.existsByDocumento(documento)) {
            throw new ConflictoException("Ya existe un cliente con el documento " + documento);
        }
        Cliente cliente = new Cliente();
        aplicar(cliente, request);
        return ClienteResponse.from(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscar(id);
        String documento = request.documento().trim();
        if (clienteRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new ConflictoException("Ya existe otro cliente con el documento " + documento);
        }
        aplicar(cliente, request);
        return ClienteResponse.from(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscar(id);
        if (ventaRepository.existsByClienteId(id)) {
            throw new ConflictoException("No se puede eliminar el cliente porque tiene ventas registradas");
        }
        clienteRepository.delete(cliente);
    }

    Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el cliente", id));
    }

    private void aplicar(Cliente cliente, ClienteRequest request) {
        cliente.setDocumento(request.documento().trim());
        cliente.setNombre(request.nombre().trim());
        cliente.setTelefono(Textos.limpiar(request.telefono()));
        cliente.setEmail(Textos.limpiar(request.email()));
        cliente.setDireccion(Textos.limpiar(request.direccion()));
    }
}
