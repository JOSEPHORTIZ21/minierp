package com.minierp.dto.cliente;

import com.minierp.entity.Cliente;

public record ClienteResponse(Long id, String documento, String nombre, String telefono, String email, String direccion) {

    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(c.getId(), c.getDocumento(), c.getNombre(), c.getTelefono(), c.getEmail(), c.getDireccion());
    }
}
