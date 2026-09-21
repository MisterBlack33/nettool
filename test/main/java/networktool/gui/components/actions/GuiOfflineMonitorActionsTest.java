package main.java.networktool.gui.components.actions;

import com.sun.net.httpserver.HttpServer;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.logic.scan.schedule.OfflineThresholdMonitor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Isolated;

import javax.swing.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
class GuiOfflineMonitorActionsTest {

    GuiOutputPanel output;
    GuiInputPanel input;

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @BeforeEach void setup() {
        output = new GuiOutputPanel();
        input = new GuiInputPanel(new JLabel(), output);
        OfflineThresholdMonitor.getInstance().stop();
    }

    @AfterEach void teardown() { OfflineThresholdMonitor.getInstance().stop(); }

    @Test void parsePositive_cases() {
        assertEquals(3, GuiOfflineMonitorActions.parsePositive(" 3 "));
        assertEquals(-1, GuiOfflineMonitorActions.parsePositive("abc"));
        assertEquals(-1, GuiOfflineMonitorActions.parsePositive(null));
    }

    @Test void start_invalidHours_notActive() {
        GuiOfflineMonitorActions.start("0", "", output);
        assertFalse(OfflineThresholdMonitor.getInstance().isActive());
    }

    @Test void start_valid_active_thenHandleStops() {
        GuiOfflineMonitorActions.start("2", null, output);
        assertTrue(OfflineThresholdMonitor.getInstance().isActive());
        GuiOfflineMonitorActions.handle(input, output);
        assertFalse(OfflineThresholdMonitor.getInstance().isActive());
    }

    @Test void handle_inactive_registersPrompt() {
        assertDoesNotThrow(() -> GuiOfflineMonitorActions.handle(input, output));
        assertFalse(OfflineThresholdMonitor.getInstance().isActive());
    }

    @Test void notifyOffline_withWebhook_postsMessage() throws IOException {
        AtomicInteger hits = new AtomicInteger();
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/", ex -> { hits.incrementAndGet(); ex.sendResponseHeaders(200, -1); ex.close(); });
        s.start();
        try {
            GuiOfflineMonitorActions.notifyOffline("x",
                    "http://127.0.0.1:" + s.getAddress().getPort() + "/", output);
            assertEquals(1, hits.get());
        } finally {
            s.stop(0);
        }
    }

    @Test void notifyOffline_withoutWebhook_doesNotThrow() {
        assertDoesNotThrow(() -> GuiOfflineMonitorActions.notifyOffline("x", "", output));
    }
}
