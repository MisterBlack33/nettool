package main.java.networktool.gui.core;

import main.java.networktool.gui.components.GuiProgressBar;
import main.java.networktool.gui.components.GuiSidebar;
import main.java.networktool.gui.components.GuiStatusBar;
import main.java.networktool.gui.components.table.GuiSearchBar;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

import main.java.networktool.theme.GuiTheme;

/**
 * Baut Layout (Sidebar/Center/Status) und Tastaturkürzel des Hauptfensters
 * auf. Ausgelagert aus {@link GUI}, um dessen Größe unter der Grenze zu halten.
 */
final class GuiFrameLayout {

    private GuiFrameLayout() {}

    static void assemble(JFrame frame, GuiSearchBar searchBar, GuiOutputPanel outputPanel,
                         GuiProgressBar progressBar, GuiStatusBar statusBar, GuiInputPanel inputPanel,
                         GuiMenuHandler menuHandler, Consumer<String> onMenuClick,
                         Runnable onRestart, Runnable onTheme, Runnable onSearchToggle) {
        frame.add(GuiSidebar.build(onMenuClick, menuHandler::cancel, onRestart, onTheme, menuHandler::isRunning),
                BorderLayout.WEST);
        frame.add(buildMainPanel(searchBar, outputPanel, progressBar, statusBar, inputPanel), BorderLayout.CENTER);
        frame.add(statusBar.buildPanel(), BorderLayout.SOUTH);

        installShortcuts(frame, menuHandler, onRestart, onSearchToggle);
    }

    private static void installShortcuts(JFrame frame, GuiMenuHandler menuHandler,
                                         Runnable onRestart, Runnable onSearchToggle) {
        Runnable onQuit = () -> GuiWindowActions.confirmQuit(frame, menuHandler);
        GuiWindowActions.installKeyboardShortcuts(frame, menuHandler, onQuit, onRestart, onSearchToggle);
        GuiWindowActions.installWindowClose(frame, onQuit);
    }

    private static JPanel buildMainPanel(GuiSearchBar searchBar, GuiOutputPanel outputPanel,
                                         GuiProgressBar progressBar, GuiStatusBar statusBar,
                                         GuiInputPanel inputPanel) {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(GuiTheme.PANEL_BG);
        bottom.add(progressBar.getPanel(),  BorderLayout.NORTH);
        bottom.add(inputPanel.buildPanel(), BorderLayout.SOUTH);

        JPanel centerArea = new JPanel(new BorderLayout());
        centerArea.setBackground(GuiTheme.BG);
        // searchBar ist standardmäßig unsichtbar; liegt trotzdem im Layout
        centerArea.add(searchBar,                     BorderLayout.NORTH);
        centerArea.add(outputPanel.buildScrollPane(), BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(GuiTheme.BG);
        main.add(outputPanel.buildTopBar(), BorderLayout.NORTH);
        main.add(centerArea,                BorderLayout.CENTER);
        main.add(bottom,                    BorderLayout.SOUTH);
        return main;
    }
}
