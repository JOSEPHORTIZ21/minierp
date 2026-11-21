package com.minierp.seguridad;

import com.minierp.exception.DemasiadosIntentosException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frena la fuerza bruta contra el login: tras 5 intentos fallidos con el mismo correo,
 * ese correo queda bloqueado hasta 15 minutos después del primer fallo.
 *
 * Límites conocidos: vive en memoria (se reinicia con el servidor y no se comparte entre instancias)
 * y cuenta por correo, no por IP. Es suficiente para una sola instancia en Render.
 */
@Component
public class IntentosLogin {

    static final int MAXIMO_FALLOS = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);
    private static final int MAXIMO_REGISTROS = 10_000;

    private record Registro(int fallos, Instant desde) {
    }

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final Clock reloj;

    public IntentosLogin() {
        this(Clock.systemUTC());
    }

    IntentosLogin(Clock reloj) {
        this.reloj = reloj;
    }

    /** Lanza 429 si el correo está bloqueado. */
    public void verificar(String email) {
        Registro r = registros.get(email);
        if (r == null || r.fallos() < MAXIMO_FALLOS) {
            return;
        }
        long restantes = Duration.between(reloj.instant(), r.desde().plus(VENTANA)).toSeconds();
        if (restantes > 0) {
            throw new DemasiadosIntentosException(restantes);
        }
        registros.remove(email, r);
    }

    public void registrarFallo(String email) {
        if (registros.size() > MAXIMO_REGISTROS) {
            registros.values().removeIf(this::vencido); // evita que el mapa crezca sin límite
        }
        Instant ahora = reloj.instant();
        registros.compute(email, (k, r) -> r == null || vencido(r) ? new Registro(1, ahora) : new Registro(r.fallos() + 1, r.desde()));
    }

    public void registrarExito(String email) {
        registros.remove(email);
    }

    private boolean vencido(Registro r) {
        return !reloj.instant().isBefore(r.desde().plus(VENTANA));
    }
}
