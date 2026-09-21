package main.java.networktool.logic.messaging;

import main.java.networktool.logging.DebugLogger;
import main.java.networktool.storage.JsonCodec;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Sendet eine Nachricht als JSON-POST an einen Webhook (http/https). */
public final class WebhookDelivery {

    static final int TIMEOUT_MS = 3000;
    private static final int HTTP_OK_MIN = 200;
    private static final int HTTP_OK_MAX = 299;

    private WebhookDelivery() {}

    public static boolean send(String url, String message) {
        URI uri = parse(url);
        if (uri == null) {
            System.out.println("  [FEHLER] Webhook: ungültige URL (nur http/https)");
            return false;
        }
        try {
            return post(uri, buildPayload(message));
        } catch (IOException e) {
            // URL kann Tokens enthalten → nur Host und Fehlertyp loggen
            DebugLogger.getInstance().log("WARN",
                    "[WebhookDelivery] " + uri.getHost() + ": " + e.getClass().getSimpleName());
            return false;
        }
    }

    static URI parse(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            boolean http = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            return http && uri.getHost() != null ? uri : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /** "text" ist Slack/Discord/Mattermost-kompatibel. */
    static String buildPayload(String message) {
        return "{\"source\":\"NetTool\",\"text\":\"" + JsonCodec.esc(message)
                + "\",\"timestamp\":\"" + Instant.now() + "\"}";
    }

    private static boolean post(URI uri, String payload) throws IOException {
        HttpURLConnection c = (HttpURLConnection) uri.toURL().openConnection();
        try {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream out = c.getOutputStream()) {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            int code = c.getResponseCode();
            System.out.println("  Webhook: HTTP " + code);
            return code >= HTTP_OK_MIN && code <= HTTP_OK_MAX;
        } finally {
            c.disconnect();
        }
    }
}
