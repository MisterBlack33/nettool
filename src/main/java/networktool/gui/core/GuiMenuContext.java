package main.java.networktool.gui.core;

import main.java.networktool.gui.components.table.GuiTableRenderer;
import main.java.networktool.gui.panels.GuiInputPanel;
import main.java.networktool.gui.panels.GuiOutputPanel;

/** Gemeinsame Abhängigkeiten der Menü-Registrierungen (hält Methodenparameter unter dem Limit). */
record GuiMenuContext(GuiInputPanel input, GuiOutputPanel output,
                      GuiTableRenderer tables, GuiMenuHandler handler) {}
