package main.java.networktool.logic.analysis.security;

import main.java.networktool.storage.JsonCodec;
import main.java.networktool.storage.StorageLocations;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Persistent local history of security findings. */
final class SecurityFindingsStore {

    private static final Object IO_LOCK = new Object();
    private final Path file;

    SecurityFindingsStore() {
        this(StorageLocations.networkData().resolve("security-findings.json"));
    }

    SecurityFindingsStore(Path file) {
        this.file = file;
    }

    List<SecurityFinding> load() throws IOException {
        return withLock(() -> {
            if (!Files.exists(file)) return List.of();
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (!Integer.valueOf(1).equals(JsonCodec.extractInt(json, "version")))
                throw new IOException("Nicht unterstützte Security-Findings-Version.");
            int start = JsonCodec.findArrayStart(json, "findings");
            if (start < 0) throw new IOException("Security-Findings-Datei enthält kein findings-Array.");
            List<SecurityFinding> result = new ArrayList<>();
            for (String object : JsonCodec.extractObjects(json, start)) {
                try {
                    result.add(new SecurityFinding(
                            required(object, "ip"),
                            SecurityFinding.Category.valueOf(required(object, "category")),
                            SecurityFinding.Severity.valueOf(required(object, "severity")),
                            required(object, "detail"),
                            SecurityFinding.Source.valueOf(required(object, "source")),
                            SecurityFinding.Confidence.valueOf(required(object, "confidence")),
                            Instant.parse(required(object, "observedAt")),
                            SecurityFinding.Status.valueOf(required(object, "status"))));
                } catch (IllegalArgumentException e) {
                    throw new IOException("Ungültiger Eintrag in Security-Findings-Datei.", e);
                }
            }
            return List.copyOf(result);
        });
    }

    void save(List<SecurityFinding> findings) throws IOException {
        withLock(() -> {
            Path parent = file.toAbsolutePath().getParent();
            Path temp = Files.createTempFile(parent, "security-findings-", ".tmp");
            try {
                Files.writeString(temp, toJson(findings), StandardCharsets.UTF_8);
                try {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
            return null;
        });
    }

    private <T> T withLock(IoOperation<T> operation) throws IOException {
        synchronized (IO_LOCK) {
            Path absoluteFile = file.toAbsolutePath().normalize();
            Path parent = absoluteFile.getParent();
            Files.createDirectories(parent);
            Path lockFile = absoluteFile.resolveSibling(absoluteFile.getFileName() + ".lock");
            try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                return operation.run();
            }
        }
    }

    @FunctionalInterface
    private interface IoOperation<T> {
        T run() throws IOException;
    }

    private static String required(String object, String field) throws IOException {
        String value = JsonCodec.extractStr(object, field);
        if (value == null) throw new IOException("Pflichtfeld fehlt: " + field);
        return value;
    }

    private static String toJson(List<SecurityFinding> findings) {
        StringBuilder json = new StringBuilder("{\n  \"version\": 1,\n  \"findings\": [\n");
        for (int i = 0; i < findings.size(); i++) {
            SecurityFinding finding = findings.get(i);
            json.append("    {\"ip\":\"").append(JsonCodec.esc(finding.ip()))
                    .append("\",\"category\":\"").append(finding.category())
                    .append("\",\"severity\":\"").append(finding.severity())
                    .append("\",\"detail\":\"").append(JsonCodec.esc(finding.detail()))
                    .append("\",\"source\":\"").append(finding.source())
                    .append("\",\"confidence\":\"").append(finding.confidence())
                    .append("\",\"observedAt\":\"").append(finding.observedAt())
                    .append("\",\"status\":\"").append(finding.status()).append("\"}")
                    .append(i + 1 < findings.size() ? "," : "").append('\n');
        }
        return json.append("  ]\n}\n").toString();
    }
}
