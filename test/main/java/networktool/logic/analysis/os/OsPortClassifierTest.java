package main.java.networktool.logic.analysis.os;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OsPortClassifierTest {

    private static Map<Integer, Boolean> open(int... ports) {
        Map<Integer, Boolean> m = new java.util.HashMap<>();
        for (int p : ports) m.put(p, true);
        return m;
    }

    @Test void windows_smbAndRdp_highScore() {
        OsSignature s = OsPortClassifier.classify(open(445, 3389), "192.0.2.1");
        assertEquals("Windows", s.os);
        assertEquals(95, s.score);
    }

    @Test void windows_smbAndWinrm() {
        assertEquals("Windows", OsPortClassifier.classify(open(445, 5985), "192.0.2.1").os);
    }

    @Test void windows_rdpAndRpc() {
        assertEquals("Windows", OsPortClassifier.classify(open(3389, 135), "192.0.2.1").os);
    }

    @Test void windows_smbOnly() {
        assertEquals("Windows", OsPortClassifier.classify(open(445), "192.0.2.1").os);
    }

    @Test void windows_rpcOnly() {
        assertEquals("Windows", OsPortClassifier.classify(open(135), "192.0.2.1").os);
    }

    @Test void windows_netbiosOnly() {
        assertEquals("Windows", OsPortClassifier.classify(open(139), "192.0.2.1").os);
    }

    @Test void macos_afpAndMdns() {
        assertEquals("macOS", OsPortClassifier.classify(open(548, 5353), "192.0.2.1").os);
    }

    @Test void macos_afpOnly() {
        assertEquals("macOS", OsPortClassifier.classify(open(548), "192.0.2.1").os);
    }

    @Test void macos_airplay() {
        assertEquals("macOS", OsPortClassifier.classify(open(5000), "192.0.2.1").os);
    }

    @Test void printer_jetdirect() {
        assertEquals("Drucker (JetDirect)", OsPortClassifier.classify(open(9100), "192.0.2.1").os);
    }

    @Test void printer_ippAndLpd() {
        assertEquals("Drucker (IPP/LPD)", OsPortClassifier.classify(open(631, 515), "192.0.2.1").os);
    }

    @Test void printer_ippOnly() {
        assertEquals("Drucker (IPP/CUPS)", OsPortClassifier.classify(open(631), "192.0.2.1").os);
    }

    @Test void printer_lpdOnly() {
        assertEquals("Drucker (LPD)", OsPortClassifier.classify(open(515), "192.0.2.1").os);
    }

    @Test void iot_mqtt() {
        assertEquals("IoT-Gerät (MQTT)", OsPortClassifier.classify(open(1883), "192.0.2.1").os);
        assertEquals("IoT-Gerät (MQTT)", OsPortClassifier.classify(open(8883), "192.0.2.1").os);
    }

    @Test void network_snmp() {
        assertEquals("Router / Switch (SNMP)", OsPortClassifier.classify(open(161), "192.0.2.1").os);
    }

    @Test void network_dhcp() {
        assertEquals("DHCP-Server", OsPortClassifier.classify(open(67), "192.0.2.1").os);
    }

    @Test void network_dns() {
        assertEquals("DNS-Server", OsPortClassifier.classify(open(53), "192.0.2.1").os);
    }

    @Test void network_telnet() {
        assertEquals("Router / Netzwerkgerät", OsPortClassifier.classify(open(23), "192.0.2.1").os);
    }

    @Test void linux_sshOnly() {
        assertEquals("Linux/Unix", OsPortClassifier.classify(open(22), "192.0.2.1").os);
    }

    @Test void linux_mysqlWithSsh() {
        assertEquals("Datenbankserver (MySQL/Linux)", OsPortClassifier.classify(open(22, 3306), "192.0.2.1").os);
    }

    @Test void linux_postgresWithSsh() {
        assertEquals("Datenbankserver (PostgreSQL)", OsPortClassifier.classify(open(22, 5432), "192.0.2.1").os);
    }

    @Test void linux_redisWithSsh() {
        assertEquals("Datenbankserver (Redis)", OsPortClassifier.classify(open(22, 6379), "192.0.2.1").os);
    }

    @Test void linux_prometheusWithSsh() {
        assertEquals("Monitoring-Server", OsPortClassifier.classify(open(22, 9090), "192.0.2.1").os);
    }

    @Test void linux_elasticsearchWithSsh() {
        assertEquals("Suchserver (Elasticsearch)", OsPortClassifier.classify(open(22, 9200), "192.0.2.1").os);
    }

    @Test void linux_mailServerWithSsh() {
        assertEquals("Mail-Server (Linux)", OsPortClassifier.classify(open(22, 25, 143), "192.0.2.1").os);
    }

    @Test void linux_httpsWithSsh() {
        assertEquals("Web-Server (HTTPS)", OsPortClassifier.classify(open(22, 443), "192.0.2.1").os);
    }

    @Test void mssql_windows() {
        assertEquals("Windows (MSSQL)", OsPortClassifier.classify(open(1433), "192.0.2.1").os);
    }

    @Test void mysqlOnly_linuxUnix() {
        assertEquals("Linux/Unix (MySQL)", OsPortClassifier.classify(open(3306), "192.0.2.1").os);
    }

    @Test void postgresOnly_linuxUnix() {
        assertEquals("Linux/Unix (PostgreSQL)", OsPortClassifier.classify(open(5432), "192.0.2.1").os);
    }

    @Test void httpsOnly_webServer() {
        assertEquals("Web-Server (HTTPS)", OsPortClassifier.classify(open(8443), "192.0.2.1").os);
    }

    @Test void noPorts_returnsNull() {
        assertNull(OsPortClassifier.classify(open(), "192.0.2.1"));
    }

    @Test void unrelatedPort_returnsNull() {
        assertNull(OsPortClassifier.classify(open(9999), "192.0.2.1"));
    }
}