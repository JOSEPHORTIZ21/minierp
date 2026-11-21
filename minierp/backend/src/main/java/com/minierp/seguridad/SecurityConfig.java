package com.minierp.seguridad;

import com.minierp.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Autenticación con token JWT (Authorization: Bearer …) y autorización por rol en cada ruta.
 *
 * | Rutas                                                           | Quién             |
 * |-----------------------------------------------------------------|-------------------|
 * | POST /api/auth/login, GET /api/salud, Swagger (solo en dev)     | Público           |
 * | /api/auth/** (yo, cambiar-clave)                                | Cualquier usuario |
 * | GET productos, categorías, clientes y ventas; POST clientes y ventas | ADMIN y CAJERO |
 * | Todo lo demás (dashboard, compras, proveedores, escritura del catálogo, ajustes, anular ventas, usuarios) | ADMIN |
 *
 * Lo que no está listado exige ADMIN: una ruta nueva queda protegida por defecto.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, RespuestasSeguridad respuestas,
                                          UsuarioRepository usuarioRepository) throws Exception {
        http
                // Sin cookies de sesión: el token viaja en la cabecera, así que CSRF no aplica
                .csrf(AbstractHttpConfigurer::disable)
                // Usa el bean "corsConfigurationSource" de CorsConfig
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // consola H2 (solo dev) usa frames
                .authorizeHttpRequests(a -> a
                        .requestMatchers(antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/auth/login")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/salud")).permitAll()
                        .requestMatchers(antMatcher("/swagger-ui.html"), antMatcher("/swagger-ui/**"),
                                antMatcher("/v3/api-docs/**"), antMatcher("/h2-console/**"), antMatcher("/error")).permitAll()
                        .requestMatchers(antMatcher("/api/auth/**")).authenticated()
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/productos/**"),
                                antMatcher(HttpMethod.GET, "/api/categorias/**"),
                                antMatcher(HttpMethod.GET, "/api/clientes/**"),
                                antMatcher(HttpMethod.GET, "/api/ventas/**"),
                                antMatcher(HttpMethod.POST, "/api/clientes"),
                                antMatcher(HttpMethod.POST, "/api/ventas")).hasAnyRole("ADMIN", "CAJERO")
                        .anyRequest().hasRole("ADMIN"))
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(new ConvertidorTokenUsuario(usuarioRepository)))
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
