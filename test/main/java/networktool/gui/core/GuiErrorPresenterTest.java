package main.java.networktool.gui.core;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class GuiErrorPresenterTest {

    @Test void timeout_hasFriendlyMessage() {
        assertEquals("Zeitüberschreitung – Ziel antwortet nicht.",
                GuiErrorPresenter.userMessage(new SocketTimeoutException()));
    }

    @Test void dns_hasFriendlyMessage() {
        assertEquals("Hostname konnte nicht aufgelöst werden.",
                GuiErrorPresenter.userMessage(new UnknownHostException("x")));
    }

    @Test void connectionRefused_hasFriendlyMessage() {
        assertEquals("Ziel nicht erreichbar (Verbindung abgelehnt).",
                GuiErrorPresenter.userMessage(new ConnectException()));
    }

    @Test void hostOffline_hasFriendlyMessage() {
        assertEquals("Ziel ist offline oder nicht erreichbar.",
                GuiErrorPresenter.userMessage(new NoRouteToHostException()));
    }

    @Test void connectionReset_hasFriendlyMessage() {
        assertEquals("Verbindung zum Ziel wurde unterbrochen.",
                GuiErrorPresenter.userMessage(new SocketException("Connection reset")));
    }

    @Test void permission_hasFriendlyMessage() {
        assertEquals("Keine Berechtigung für diese Aktion.",
                GuiErrorPresenter.userMessage(new AccessDeniedException("x")));
    }

    @Test void unknown_doesNotExposeExceptionDetails() {
        String message = GuiErrorPresenter.userMessage(
                new IllegalStateException("https://host.test/hook?token=secret private note"));
        assertEquals("Ein unerwarteter Fehler ist aufgetreten.", message);
        assertFalse(message.contains("secret"));
        assertFalse(message.contains("host.test"));
        assertFalse(message.contains("private note"));
    }
}
