package main.java.networktool.logic.scan.schedule;

import main.java.networktool.model.ScanResult;
import main.java.networktool.storage.StorageLocations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Local persistence for the bounded scan-comparison history. */
final class ScanHistoryStore {

    private static final int MAGIC = 0x4E544853;
    private static final int VERSION = 1;
    private static final int MAX_RESULTS = 65_534;
    private static final Object IO_LOCK = new Object();
    private final Path file;

    ScanHistoryStore() {
        this(StorageLocations.networkData().resolve("scan-history.bin"));
    }

    ScanHistoryStore(Path file) {
        this.file = file;
    }

    List<ScanHistory.Entry> load() throws IOException {
        return withLock(() -> {
            if (!Files.exists(file)) return List.of();
            try (DataInputStream input = new DataInputStream(Files.newInputStream(file))) {
                if (input.readInt() != MAGIC || input.readInt() != VERSION)
                    throw new IOException("Unbekanntes Scan-Historienformat.");
                int entryCount = readCount(input, ScanHistory.MAX_HISTORY);
                List<ScanHistory.Entry> entries = new ArrayList<>(entryCount);
                for (int i = 0; i < entryCount; i++) {
                    String label = input.readUTF();
                    String timestamp = input.readUTF();
                    int resultCount = readCount(input, MAX_RESULTS);
                    List<ScanResult> results = new ArrayList<>(resultCount);
                    for (int j = 0; j < resultCount; j++) {
                        String ip = input.readUTF();
                        String hostname = input.readUTF();
                        String os = input.readUTF();
                        int portCount = readCount(input, 65_535);
                        Map<Integer, String> ports = new TreeMap<>();
                        for (int k = 0; k < portCount; k++) ports.put(input.readInt(), input.readUTF());
                        results.add(new ScanResult(ip, hostname, ports, os));
                    }
                    entries.add(new ScanHistory.Entry(label, results, timestamp));
                }
                if (input.read() != -1) throw new IOException("Unerwartete Daten am Ende der Scan-Historie.");
                return List.copyOf(entries);
            }
        });
    }

    void save(List<ScanHistory.Entry> entries) throws IOException {
        withLock(() -> {
            Path parent = file.toAbsolutePath().getParent();
            Path temp = Files.createTempFile(parent, "scan-history-", ".tmp");
            try {
                try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(temp))) {
                    output.writeInt(MAGIC);
                    output.writeInt(VERSION);
                    output.writeInt(entries.size());
                    for (ScanHistory.Entry entry : entries) writeEntry(output, entry);
                }
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

    private static void writeEntry(DataOutputStream output, ScanHistory.Entry entry) throws IOException {
        output.writeUTF(safe(entry.label));
        output.writeUTF(safe(entry.timestamp));
        if (entry.results.size() > MAX_RESULTS)
            throw new IOException("Scan-Ergebnis überschreitet die maximale speicherbare Hostzahl.");
        output.writeInt(entry.results.size());
        for (ScanResult result : entry.results) {
            output.writeUTF(safe(result.getIp()));
            output.writeUTF(safe(result.getHostname()));
            output.writeUTF(safe(result.getOsGuess()));
            Map<Integer, String> ports = result.getOpenPorts() != null
                    ? result.getOpenPorts() : Map.of();
            output.writeInt(ports.size());
            for (Map.Entry<Integer, String> port : ports.entrySet()) {
                output.writeInt(port.getKey());
                output.writeUTF(safe(port.getValue()));
            }
        }
    }

    private static int readCount(DataInputStream input, int max) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > max) throw new IOException("Ungültige Anzahl in Scan-Historie.");
        return count;
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
