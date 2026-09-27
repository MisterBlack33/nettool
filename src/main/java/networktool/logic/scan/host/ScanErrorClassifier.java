package main.java.networktool.logic.scan.host;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;

/**
 * Ordnet Scan-Exceptions einer groben Fehlerart zu, damit Debug-Logs
 * Timeout/DNS/Unreachable/Permission unterscheiden statt nur "fehlgeschlagen: ...".
 * Reine Klassifizierung — kein Verhalten (Rückgabewerte, Retries) ändert sich.
 */
public final class ScanErrorClassifier {

    public enum Kind { TIMEOUT, DNS, UNREACHABLE, PERMISSION, UNKNOWN }

    private ScanErrorClassifier() {}

    public static Kind classify(Throwable e) {
        if (e == null) return Kind.UNKNOWN;
        if (e instanceof SocketTimeoutException || e instanceof InterruptedIOException) return Kind.TIMEOUT;
        if (e instanceof UnknownHostException) return Kind.DNS;
        if (e instanceof ConnectException) return Kind.UNREACHABLE;
        if (e instanceof AccessDeniedException || e instanceof SecurityException) return Kind.PERMISSION;
        return Kind.UNKNOWN;
    }

    /** Log-Zeile ohne Stacktrace/Secrets: nur Fehlerart, Host und Exception-Typ. */
    public static String describe(String host, Throwable e) {
        String type = e != null ? e.getClass().getSimpleName() : "null";
        return "[" + classify(e) + "] " + host + ": " + type;
    }
}
