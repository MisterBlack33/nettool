package main.java.networktool.gui.components;

import main.java.networktool.security.UserAuth;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.ResourceLock;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft, dass die Sidebar-Sektion "TEST-SUITE" nur für Admins sichtbar ist.
 * Nutzt den echten UserAuth-Zustand statt eines Mocks, da GuiSidebar die
 * Zugriffsebene intern über UserAuth.getInstance().isAdmin() ermittelt.
 */
@Isolated
@ResourceLock("userAuthSingleton")
class GuiSidebarAccessTest {

    private static final String ADMIN_PW = "test1234";

    @TempDir Path tmp;
    UserAuth auth;

    @BeforeAll static void headless() { System.setProperty("java.awt.headless", "true"); }

    @BeforeEach void setup() {
        auth = UserAuth.getInstance();
        auth.init(tmp);
        auth.logout();
        auth.seedDefaultUsers();
    }

    @AfterEach void teardown() { auth.logout(); }

    @Test void testSuiteSection_hidden_forStandardUser() {
        auth.authenticateAsStandardUser();
        assertFalse(containsTestSuiteHeader(buildSidebar()));
    }

    @Test void testSuiteSection_visible_forAdmin() {
        auth.authenticate("admin", ADMIN_PW);
        assertTrue(containsTestSuiteHeader(buildSidebar()));
    }

    private JPanel buildSidebar() {
        return GuiSidebar.build(id -> {}, () -> {}, () -> {});
    }

    private boolean containsTestSuiteHeader(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel lbl && lbl.getText().contains("TEST-SUITE")) return true;
            if (c instanceof Container sub && containsTestSuiteHeader(sub)) return true;
        }
        return false;
    }
}