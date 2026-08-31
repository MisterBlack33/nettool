package main.java.networktool.gui.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Verwaltet die Zuordnung Menü-ID → Handler-Aktion.
 * Ersetzt den langen switch in {@link GuiMenuHandler}.
 */
final class GuiMenuRegistry {

    private final Map<String, Runnable> handlers = new LinkedHashMap<>();

    void register(String id, Runnable action) {
        handlers.put(id, action);
    }

    /** @return true wenn ein Handler für {@code id} existierte und ausgeführt wurde. */
    boolean dispatch(String id) {
        Runnable action = handlers.get(id);
        if (action == null) return false;
        action.run();
        return true;
    }

    boolean contains(String id) { return handlers.containsKey(id); }

    int size() { return handlers.size(); }
}