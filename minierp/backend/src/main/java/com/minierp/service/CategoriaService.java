package com.minierp.service;

import com.minierp.dto.categoria.CategoriaRequest;
import com.minierp.dto.categoria.CategoriaResponse;
import com.minierp.entity.Categoria;
import com.minierp.exception.ConflictoException;
import com.minierp.exception.RecursoNoEncontradoException;
import com.minierp.repository.CategoriaRepository;
import com.minierp.repository.ProductoRepository;
import com.minierp.util.Textos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll(Sort.by("nombre")).stream().map(CategoriaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.from(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe una categoría llamada '" + nombre + "'");
        }
        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .descripcion(Textos.limpiar(request.descripcion()))
                .build();
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictoException("Ya existe una categoría llamada '" + nombre + "'");
        }
        categoria.setNombre(nombre);
        categoria.setDescripcion(Textos.limpiar(request.descripcion()));
        return CategoriaResponse.from(categoria);
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new ConflictoException("No se puede eliminar la categoría porque tiene productos asociados");
        }
        categoriaRepository.delete(categoria);
    }

    Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("la categoría", id));
    }
}
