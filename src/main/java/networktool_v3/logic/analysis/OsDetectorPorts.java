package main.java.networktool_v3.logic.analysis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

/** Port-basierte OS-Erkennung. Intern von OsDetector verwendet. */
final class OsDetectorPorts {

    private OsDetectorPorts() {}

    private static final int TIMEOUT_MS   = 300;
    private static final int THREAD_COUNT = 32;

    static String detectByPorts(String ip) {
        int[] ports = {
                445,139,135,3389,5985,5986,
                548,5000,7000,5353,
                22,80,443,8080,8443,
                631,9100,515,
                53,161,23,67,
                3306,5432,1433,6379,27017,
                25,110,143,993,995,
                1883,8883,
                21,9090,9200
        };
        Map<Integer, Boolean> open = new ConcurrentHashMap<>();
        ExecutorService exec = Executors.newFixedThreadPool(THREAD_COUNT,
                r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
        try {
            for (int p : ports) {
                final int fp = p;
                exec.submit(() -> open.put(fp, isOpen(ip, fp)));
            }
            exec.shutdown();
            if (!exec.awaitTermination(TIMEOUT_MS + 200L, TimeUnit.MILLISECONDS))
                exec.shutdownNow();
        } catch (InterruptedException e) {
            exec.shutdownNow();
            Thread.currentThread().interrupt();
        }
        return classify(open, ip);
    }

    private static String classify(Map<Integer, Boolean> open, String ip) {
        if (is(open,445)||is(open,3389)||is(open,5985)||is(open,5986)) return "Windows";
        if (is(open,135)&&!is(open,22))                                 return "Windows";
        if (is(open,548))                                               return "macOS";
        if (is(open,5000)&&!is(open,445)&&!is(open,22))                return "macOS";
        if (is(open,9100)) return "Drucker (JetDirect)";
        if (is(open,631))  return "Drucker (IPP/CUPS)";
        if (is(open,515))  return "Drucker (LPD)";
        if (is(open,1883)||is(open,8883)) return "IoT-Gerät (MQTT)";
        if (is(open,161)&&!is(open,22)&&!is(open,80))  return "Router / Switch (SNMP)";
        if (is(open,67) &&!is(open,22)&&!is(open,80))  return "DHCP-Server";
        if (is(open,53) &&!is(open,22)&&!is(open,80))  return "DNS-Server";
        if (is(open,23) &&!is(open,22))                 return "Router / Netzwerkgerät";
        if (is(open,22)) return classifyLinux(open, ip);
        if (is(open,80)||is(open,8080))  return detectWebServer(ip);
        if (is(open,443)||is(open,8443)) return "Web-Server (HTTPS)";
        if (is(open,53))                 return "DNS-Server";
        if (is(open,25)||is(open,110)||is(open,143)) return "Mail-Server";
        if (is(open,21))                 return "FTP-Server";
        if (is(open,3306))               return "Datenbankserver (MySQL)";
        if (is(open,5432))               return "Datenbankserver (PostgreSQL)";
        if (is(open,1433))               return "Datenbankserver (MSSQL)";
        if (is(open,6379))               return "Datenbankserver (Redis)";
        if (is(open,27017))              return "Datenbankserver (MongoDB)";
        return "Unbekannt";
    }

    private static String classifyLinux(Map<Integer, Boolean> open, String ip) {
        if (is(open,80)&&is(open,443))  return detectWebServer(ip);
        if (is(open,80))                return "Web-Server (HTTP)";
        if (is(open,443))               return "Web-Server (HTTPS)";
        if (is(open,25)&&(is(open,143)||is(open,993))) return "Mail-Server";
        if (is(open,25))                return "SMTP-Server";
        if (is(open,21))                return "FTP-Server";
        if (is(open,3306))              return "Datenbankserver (MySQL)";
        if (is(open,5432))              return "Datenbankserver (PostgreSQL)";
        if (is(open,1433))              return "Datenbankserver (MSSQL)";
        if (is(open,6379))              return "Datenbankserver (Redis)";
        if (is(open,27017))             return "Datenbankserver (MongoDB)";
        if (is(open,9090))              return "Monitoring-Server";
        if (is(open,9200))              return "Suchserver (Elasticsearch)";
        return "Linux/Unix";
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
        } catch (Exception ignored) {}
        return "Web-Server";
    }

    static boolean isOpen(String ip, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, port), TIMEOUT_MS);
            return true;
        } catch (Exception e) { return false; }
    }

    private static boolean is(Map<Integer, Boolean> map, int port) {
        return Boolean.TRUE.equals(map.get(port));
    }
}