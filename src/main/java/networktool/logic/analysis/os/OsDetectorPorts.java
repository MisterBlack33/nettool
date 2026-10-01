package main.java.networktool.logic.analysis.os;

import main.java.networktool.logic.TimeoutConfig;
import main.java.networktool.logic.error.ScanContext;
import main.java.networktool.logic.error.ScanFailure;
import main.java.networktool.logic.scan.host.ScanErrorClassifier;
import main.java.networktool.logging.DebugLogger;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;

/**
 * OS-Erkennung per Port-Kombination.
 * Probing + Web-Banner hier; die eigentliche Klassifizierungs-Logik
 * (Port-Kombination → OS-Signatur) liegt in {@link OsPortClassifier}.
 */
public final class OsDetectorPorts {

    private OsDetectorPorts() {}

    public static void setTestTimeout(int ms) { TimeoutConfig.OS_DETECT_PORT_SCAN_MS = ms; } // package-private Hook für Tests
    private static final int THREAD_COUNT =
            Math.min(16, Runtime.getRuntime().availableProcessors() * 2);

    // Sortiert nach Wahrscheinlichkeit: häufigste Ports zuerst
    private static final int[] PROBE_PORTS = {
            22, 80, 443, 8080,
            445, 3389, 135, 139, 5985, 5986,
            548, 5000, 7000, 5353,
            9100, 631, 515,
            23, 53, 67, 161,
            25, 110, 143,
            1883, 8883,
            3306, 5432, 1433, 6379, 27017,
            2222, 8000, 8888, 9000, 443
    };

    static String detectByPorts(String ip) {
        Map<Integer, Boolean> open = probeAllPorts(ip);
        OsSignature signature = OsPortClassifier.classify(open, ip);
        return signature != null ? signature.os : "Unbekannt";
    }

    static OsSignature detectWithSignature(String ip) {
        Map<Integer, Boolean> open = probeAllPorts(ip);
        return OsPortClassifier.classify(open, ip);
    }

    private static Map<Integer, Boolean> probeAllPorts(String ip) {
        Map<Integer, Boolean> open = new ConcurrentHashMap<>();
        ExecutorService exec = Executors.newFixedThreadPool(THREAD_COUNT,
                r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
        try {
            for (int port : PROBE_PORTS) {
                final int p = port;
                exec.submit(() -> open.put(p, isOpen(ip, p)));
            }
            exec.shutdown();
            exec.awaitTermination(TimeoutConfig.OS_DETECT_PORT_SCAN_MS + 200L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            exec.shutdownNow();
            Thread.currentThread().interrupt();
        }
        return open;
    }

    static String detectWebServer(String ip) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, 80), 400);
            s.setSoTimeout(400);
            s.getOutputStream().write(
                    ("HEAD / HTTP/1.1\r\nHost: " + ip + "\r\nConnection: close\r\n\r\n").getBytes());
            s.getOutputStream().flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.toLowerCase().startsWith("server:")) continue;
                String srv = line.substring(7).trim().toLowerCase();
                if (srv.contains("nginx"))    return "Web-Server (nginx)";
                if (srv.contains("apache"))   return "Web-Server (Apache)";
                if (srv.contains("iis"))      return "Web-Server (IIS/Windows)";
                if (srv.contains("caddy"))    return "Web-Server (Caddy)";
                if (srv.contains("lighttpd")) return "Web-Server (lighttpd)";
                if (!srv.isBlank())           return "Web-Server (" + srv.split("/")[0] + ")";
            }
        } catch (SocketTimeoutException e) {
            logExpected("web-server-banner", ip, 80, e);
        } catch (IOException e) {
            logExpected("web-server-banner", ip, 80, e);
        } catch (RuntimeException e) {
            logFailure("web-server-banner", ip, 80, e);
        }
        return "Web-Server";
    }

    static boolean isOpen(String ip, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, port), TimeoutConfig.OS_DETECT_PORT_SCAN_MS);
            return true;
        } catch (SocketTimeoutException e) {
            logExpected("port-probe", ip, port, e);
            return false;
        } catch (IOException e) {
            logExpected("port-probe", ip, port, e);
            return false;
        } catch (RuntimeException e) {
            logFailure("port-probe", ip, port, e);
            return false;
        }
    }

    private static void logExpected(String operation, String host, int port, IOException error) {
        ScanFailure failure = ScanFailure.from(new ScanContext("unknown", operation, host, port), error);
        DebugLogger.getInstance().log("FINE", "[OsDetectorPorts] " + failure.describe());
    }

    private static void logFailure(String operation, String host, int port, Throwable error) {
        ScanFailure failure = ScanFailure.from(new ScanContext("unknown", operation, host, port), error);
        DebugLogger.getInstance().log(failure.kind() == ScanErrorClassifier.Kind.TIMEOUT
                ? "FINE" : "WARN", "[OsDetectorPorts] " + failure.describe());
    }
}