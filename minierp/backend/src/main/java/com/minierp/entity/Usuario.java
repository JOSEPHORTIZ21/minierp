package com.minierp.entity;

import com.minierp.entity.enums.Rol;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Persona que inicia sesión en el sistema. No se borra: se desactiva, para conservar el historial. */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Se guarda en minúsculas: es el nombre de usuario para iniciar sesión. */
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** Hash BCrypt. Nunca se guarda ni se devuelve la contraseña. */
    @Column(nullable = false, length = 100)
    private String claveHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Rol rol;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    private LocalDateTime creadoEn;
}
