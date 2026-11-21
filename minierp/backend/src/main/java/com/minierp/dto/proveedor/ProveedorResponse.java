package com.minierp.dto.proveedor;

import com.minierp.entity.Proveedor;

public record ProveedorResponse(Long id, String nit, String razonSocial, String telefono, String email, String direccion) {

    public static ProveedorResponse from(Proveedor p) {
        return new ProveedorResponse(p.getId(), p.getNit(), p.getRazonSocial(), p.getTelefono(), p.getEmail(), p.getDireccion());
    }
}
