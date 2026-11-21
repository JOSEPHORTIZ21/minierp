package com.minierp.repository;

import com.minierp.entity.Usuario;
import com.minierp.entity.enums.Rol;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * SELECT … FOR UPDATE de todos los administradores activos, en orden de id.
     * Serializa los cambios de rol o estado de administradores: así dos cambios simultáneos
     * no pueden dejar el sistema sin ningún administrador.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.rol = :rol and u.activo = true order by u.id")
    List<Usuario> bloquearActivosConRol(@Param("rol") Rol rol);
}
