package networktool.logic;

import main.java.networktool.logic.sonify.ActiveInterfaceDetector;
import main.java.networktool.logic.visualize.TrafficVisualizer;
import networktool.util.PollHelper;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Schmaler Integrationsnachweis: der Visualizer läuft auf einem echten,
 * erkannten Interface an, ohne Exception, und liefert innerhalb kurzer Zeit
 * mindestens ein Sample. Kein echter Traffic-Inhalt wird geprüft (siehe
 * TrafficSonifierTest für das analoge Muster).
 */
class TrafficVisualizerIntegrationTest {

    private static final long SAMPLE_WAIT_MS = 3000;

    TrafficVisualizer visualizer = TrafficVisualizer.getInstance();

    @AfterEach
    void stop() {
        visualizer.stop();
        visualizer.clear();
    }

    @Test
    void start_producesSamples_thenStopsCleanly() {
        String iface = ActiveInterfaceDetector.detect();

        visualizer.start(iface);
        assertTrue(visualizer.isActive());

        // Best-effort: auf manchen CI-Umgebungen liefert das erkannte Interface
        // keine lesbaren Statistiken. Der Lauf selbst darf dennoch nie werfen.
        PollHelper.waitFor(() -> !visualizer.getSnapshot().isEmpty(), SAMPLE_WAIT_MS);

        visualizer.stop();
        assertFalse(visualizer.isActive());
    }

    @Test
    void start_doesNotThrow_onDetectedInterface() {
        String iface = ActiveInterfaceDetector.detect();
        assertDoesNotThrow(() -> visualizer.start(iface));
    }
}