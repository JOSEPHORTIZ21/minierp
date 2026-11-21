package com.minierp.seguridad;

import com.minierp.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class IntentosLoginTest {

    /** Reloj que el test puede adelantar. */
    private static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-11-21T10:00:00Z");

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Test
    void bloqueaTrasCincoFallosYSeLiberaAlTerminarLaVentana() {
        RelojManual reloj = new RelojManual();
        IntentosLogin intentos = new IntentosLogin(reloj);
        for (int i = 0; i < IntentosLogin.MAXIMO_FALLOS; i++) {
            intentos.verificar("a@x.co");
            intentos.registrarFallo("a@x.co");
        }

        DemasiadosIntentosException e = assertThrows(DemasiadosIntentosException.class, () -> intentos.verificar("a@x.co"));
        assertEquals(15 * 60, e.getSegundosEspera());
        intentos.verificar("b@x.co"); // otro correo no se afecta

        reloj.avanzar(IntentosLogin.VENTANA);
        assertDoesNotThrow(() -> intentos.verificar("a@x.co"));
    }

    @Test
    void unLoginExitosoReiniciaElConteo() {
        IntentosLogin intentos = new IntentosLogin(new RelojManual());
        for (int i = 0; i < IntentosLogin.MAXIMO_FALLOS - 1; i++) {
            intentos.registrarFallo("a@x.co");
        }
        intentos.registrarExito("a@x.co");
        intentos.registrarFallo("a@x.co");

        assertDoesNotThrow(() -> intentos.verificar("a@x.co"));
    }

    @Test
    void losFallosViejosNoCuentan() {
        RelojManual reloj = new RelojManual();
        IntentosLogin intentos = new IntentosLogin(reloj);
        for (int i = 0; i < IntentosLogin.MAXIMO_FALLOS - 1; i++) {
            intentos.registrarFallo("a@x.co");
        }
        reloj.avanzar(IntentosLogin.VENTANA.plusSeconds(1));
        intentos.registrarFallo("a@x.co"); // abre una ventana nueva con 1 fallo

        assertDoesNotThrow(() -> intentos.verificar("a@x.co"));
    }
}
