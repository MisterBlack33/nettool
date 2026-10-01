package main.java.networktool.gui.core;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Shared state branch for GUI start/stop actions. */
public final class GuiToggleAction {

    private GuiToggleAction() {}

    public record Parameters(BooleanSupplier isActive, Runnable start, Runnable stop) {
        public Parameters {
            Objects.requireNonNull(isActive, "isActive");
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(stop, "stop");
        }
    }

    public static void toggle(Parameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        if (parameters.isActive().getAsBoolean()) {
            parameters.stop().run();
        } else {
            parameters.start().run();
        }
    }
}
