package main.java.networktool.logic;

/**
 * Einheitliches Ergebnis-Pattern für neue Pipelines (Workstream D).
 * Bestehende Optional-/null-Rückgaben werden NICHT rückwirkend umgebaut —
 * nur neuer Code darf ScanOutcome verwenden.
 */
public sealed interface ScanOutcome<T> {

    record Success<T>(T value) implements ScanOutcome<T> {}
    record Failure<T>(String reason) implements ScanOutcome<T> {}

    static <T> ScanOutcome<T> success(T value) { return new Success<>(value); }
    static <T> ScanOutcome<T> failure(String reason) { return new Failure<>(reason); }

    default boolean isSuccess() { return this instanceof Success<T>; }

    default T orElse(T fallback) {
        return this instanceof Success<T> s ? s.value() : fallback;
    }
}
