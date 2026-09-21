package main.java.networktool.gui.components.actions;

import com.sun.net.httpserver.HttpServer;
import main.java.networktool.gui.core.GuiMenuHandler;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.IOException;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class GuiWebhookActionsTest {

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    private final GuiOutputPanel output = new GuiOutputPanel();

    @Test void send_invalidUrl_false() {
        assertFalse(GuiWebhookActions.send("nope", "x", output));
    }

    @Test void send_okServer_true() throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/", ex -> { ex.sendResponseHeaders(200, -1); ex.close(); });
        s.start();
        try {
            assertTrue(GuiWebhookActions.send("http://127.0.0.1:" + s.getAddress().getPort() + "/", "hi", output));
        } finally {
            s.stop(0);
        }
    }

    @Test void handle_registersPrompt_doesNotThrow() {
        GuiInputPanel input = new GuiInputPanel(new JLabel(), output);
        GuiMenuHandler handler = new GuiMenuHandler(input, output, null, null);
        assertDoesNotThrow(() -> GuiWebhookActions.handle(input, output, handler));
    }
}
