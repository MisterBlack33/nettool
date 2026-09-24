package main.java.networktool.gui.core;

import main.java.networktool.gui.components.GuiStatusBar;
import main.java.networktool.gui.components.table.GuiSearchBar;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;
import main.java.networktool.gui.components.table.GuiTableRenderer;
import main.java.networktool.security.UserAuth;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;
import javax.swing.*;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

@Isolated
@ResourceLock("userAuthSingleton")
class GuiMenuDispatchTest {
    private static final String ADMIN_PW = "test1234";
    @TempDir Path tmp;
    UserAuth auth;
    GuiOutputPanel output;
    GuiSearchBar searchBar;
    GuiMenuHandler handler;
    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }
    @BeforeEach void setup() {
        auth = UserAuth.getInstance(); auth.init(tmp); auth.logout(); auth.seedDefaultUsers();
        output = new GuiOutputPanel(); searchBar = new GuiSearchBar();
        GuiInputPanel input = new GuiInputPanel(new JLabel(), output);
        handler = new GuiMenuHandler(input, output, new GuiTableRenderer(output), new GuiStatusBar());
    }
    @AfterEach void teardown() { auth.logout(); }
    @Test void handle_11_nonAdmin_appendsWarning() throws Exception {
        auth.authenticateAsStandardUser(); int before = output.doc.getLength();
        SwingUtilities.invokeAndWait(() -> GuiMenuDispatch.handle("11", output, searchBar, handler));
        assertTrue(output.doc.getLength() > before);
    }
    @Test void handle_23_nonAdmin_appendsWarning() throws Exception {
        auth.authenticateAsStandardUser(); int before = output.doc.getLength();
        SwingUtilities.invokeAndWait(() -> GuiMenuDispatch.handle("23", output, searchBar, handler));
        assertTrue(output.doc.getLength() > before);
    }
    @Test void handle_30_hidesSearchBar() throws Exception {
        auth.authenticateAsStandardUser(); searchBar.show();
        SwingUtilities.invokeAndWait(() -> GuiMenuDispatch.handle("30", output, searchBar, handler));
        assertFalse(searchBar.isSearchVisible());
    }
    @Test void handle_09_doesNotHideSearchBar() throws Exception {
        searchBar.show();
        SwingUtilities.invokeAndWait(() -> GuiMenuDispatch.handle("09", output, searchBar, handler));
        assertTrue(searchBar.isSearchVisible());
    }
    @Test void handle_unknownId_hidesSearchBar() throws Exception {
        searchBar.show();
        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() ->
                GuiMenuDispatch.handle("99", output, searchBar, handler)));
        assertFalse(searchBar.isSearchVisible());
    }
    @Test void handle_23_admin_doesNotAppendWarning() throws Exception {
        auth.authenticate("admin", ADMIN_PW);
        SwingUtilities.invokeAndWait(() -> GuiMenuDispatch.handle("23", output, searchBar, handler));
        SwingUtilities.invokeAndWait(() -> {});
        assertFalse(output.doc.getText(0, output.doc.getLength()).contains("nur für Admins"));
    }
}
