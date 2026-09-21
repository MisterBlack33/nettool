package main.java.networktool.logic.scan.host;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TailscaleRouteParserTest {

    private static final String STATUS = """
            {"Self":{"PrimaryRoutes":null},
             "Peer":{"nodekey:a":{"HostName":"pve","PrimaryRoutes":["192.168.178.0/24","fd7a:115c:a1e0::/48"]},
                     "nodekey:b":{"PrimaryRoutes": ["10.0.0.0/23", "192.168.178.0/24"]}}}""";

    @Test void parse_collectsIpv4RoutesOfAllPeers_withoutDuplicates() {
        assertEquals(List.of("192.168.178.0/24", "10.0.0.0/23"), TailscaleRouteParser.parse(STATUS));
    }

    @Test void parse_ignoresIpv6Routes() {
        assertTrue(TailscaleRouteParser.parse("{\"PrimaryRoutes\":[\"fd7a:115c:a1e0::/48\"]}").isEmpty());
    }

    @Test void parse_nullRoutes_empty() {
        assertTrue(TailscaleRouteParser.parse("{\"PrimaryRoutes\":null}").isEmpty());
    }

    @Test void parse_emptyRouteList_empty() {
        assertTrue(TailscaleRouteParser.parse("{\"PrimaryRoutes\":[]}").isEmpty());
    }

    @Test void parse_noRouteKey_empty() {
        assertTrue(TailscaleRouteParser.parse("{\"Peer\":{}}").isEmpty());
    }

    @Test void parse_nullOrBlank_empty() {
        assertTrue(TailscaleRouteParser.parse(null).isEmpty());
        assertTrue(TailscaleRouteParser.parse("  ").isEmpty());
    }

    @Test void parse_invalidCidr_skipped() {
        assertEquals(List.of("10.1.0.0/24"),
                TailscaleRouteParser.parse("{\"PrimaryRoutes\":[\"999.1.1.1/24\",\"10.1.0.0/24\"]}"));
    }

    @Test void parse_resultIsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> TailscaleRouteParser.parse(STATUS).add("x"));
    }
}