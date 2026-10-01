package main.java.networktool.logic.scan.host;

import main.java.networktool.logic.error.ScanContext;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;
import java.util.Locale;

/**
 * Classifies scan failures and formats descriptions without exception messages.
 */
public final class ScanErrorClassifier {

    public enum Kind {
        TIMEOUT, HOST_OFFLINE, DNS, CONNECTION_REFUSED, CONNECTION_RESET, PERMISSION, UNKNOWN,
        /** @deprecated Use {@link #CONNECTION_REFUSED}. */
        @Deprecated UNREACHABLE
    }

    private static final int MAX_CAUSE_DEPTH = 5;

    private ScanErrorClassifier() {}

    /** Inspects at most five exceptions, including the supplied throwable. */
    public static Kind classify(Throwable error) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            Kind kind = classifySingle(current);
            if (kind != Kind.UNKNOWN) {
                return kind;
            }
            current = current.getCause();
        }
        return Kind.UNKNOWN;
    }

    /**
     * Produces a log-safe description; exception messages and stack traces are omitted.
     */
    public static String describe(ScanContext context, Throwable error) {
        ScanContext safeContext = context == null ? ScanContext.unknown() : context;
        String type = error == null ? "null" : error.getClass().getSimpleName();
        return "[" + classify(error) + "] " + safeContext.host() + ":" + safeContext.port()
                + " scan=" + safeContext.scanId() + " op=" + safeContext.operation()
                + " type=" + type;
    }

    /** Retains the existing host-only logging API while enriching its output safely. */
    public static String describe(String host, Throwable error) {
        return describe(ScanContext.forHost(host), error);
    }

    private static Kind classifySingle(Throwable error) {
        if (error instanceof SocketTimeoutException || error instanceof InterruptedIOException) {
            return Kind.TIMEOUT;
        }
        if (error instanceof UnknownHostException) {
            return Kind.DNS;
        }
        if (error instanceof ConnectException) {
            return Kind.CONNECTION_REFUSED;
        }
        if (error instanceof NoRouteToHostException || error instanceof PortUnreachableException) {
            return Kind.HOST_OFFLINE;
        }
        if (error instanceof SocketException && containsReset(error.getMessage())) {
            return Kind.CONNECTION_RESET;
        }
        if (error instanceof AccessDeniedException || error instanceof SecurityException) {
            return Kind.PERMISSION;
        }
        return Kind.UNKNOWN;
    }

    private static boolean containsReset(String message) {
        return message != null && message.toLowerCase(Locale.ROOT).contains("reset");
    }
}
