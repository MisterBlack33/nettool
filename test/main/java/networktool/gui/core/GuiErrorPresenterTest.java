package main.java.networktool.gui.core;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;
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

    @Test void unreachable_hasFriendlyMessage() {
        assertEquals("Ziel nicht erreichbar (Verbindung abgelehnt).",
                GuiErrorPresenter.userMessage(new ConnectException()));
    }

    @Test void permission_hasFriendlyMessage() {
        assertEquals("Keine Berechtigung für diese Aktion.",
                GuiErrorPresenter.userMessage(new AccessDeniedException("x")));
    }

    @Test void unknown_includesExceptionMessage() {
        assertTrue(GuiErrorPresenter.userMessage(new IllegalStateException("boom")).contains("boom"));
    }

    @Test void unknown_blankMessage_fallsBackToClassName() {
        assertTrue(GuiErrorPresenter.userMessage(new RuntimeException()).contains("RuntimeException"));
    }
}
