package main.java.networktool.gui.security;

/** Wird geworfen wenn eine verschlüsselte Notiz nicht entschlüsselt werden kann. */
public final class NoteDecryptionException extends RuntimeException {

    public NoteDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}