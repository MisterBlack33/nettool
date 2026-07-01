#!/usr/bin/env python3
"""
Auto-Refactor Empty Catch Blocks
Ersetzt leere Catch-Blöcke mit strukturiertem Logging nach Clean-Code-Guide
"""

import re
import os
import sys
from pathlib import Path
from typing import Tuple, List

class CatchBlockRefactorer:
    def __init__(self, project_root: str = "."):
        self.project_root = Path(project_root)
        self.refactored = []
        self.failed = []

    def find_java_files(self) -> List[Path]:
        """Finde alle Java-Dateien im Projekt"""
        return list(self.project_root.rglob("*.java"))

    def has_empty_catch_block(self, content: str) -> bool:
        """Prüfe ob Datei leere Catch-Blöcke enthält"""
        return bool(re.search(r'catch\s*\([^)]+\)\s*\{\s*\}', content))

    def has_logger_import(self, content: str) -> bool:
        """Prüfe ob Logger bereits importiert ist"""
        return 'java.util.logging.Logger' in content or 'org.slf4j.Logger' in content

    def add_logger_import(self, content: str) -> str:
        """Füge Logger-Import hinzu wenn nicht vorhanden"""
        if self.has_logger_import(content):
            return content

        # Finde die letzte import-Zeile
        import_pattern = r'(package [^;]+;.*?)(\n\n|\Z)'
        match = re.search(import_pattern, content, re.DOTALL)

        if match:
            header = match.group(1)
            rest = content[len(match.group(1)):]

            # Füge Logger-Import nach anderen java-Imports ein
            logger_imports = "\nimport java.util.logging.Logger;\nimport java.util.logging.Level;"

            if 'import java.util' not in header:
                # Logger am Ende der Imports hinzufügen
                insert_pos = header.rfind('\n')
                if insert_pos > 0:
                    return header[:insert_pos] + logger_imports + header[insert_pos:] + rest
            else:
                # Nach anderen java.util Imports einfügen
                return header + logger_imports + rest

        return content

    def add_logger_field(self, content: str) -> str:
        """Füge Logger-Feld zur Klasse hinzu"""
        # Finde die Klassendeklaration
        class_pattern = r'(public\s+(?:final\s+)?(?:class|interface|enum|record)\s+\w+[^{]*\{)'

        if not re.search(class_pattern, content):
            return content

        # Prüfe ob Logger bereits vorhanden ist
        if 'private static final Logger' in content or 'private static Logger' in content:
            return content

        # Füge Logger nach der Klassendeklaration ein
        def replace_class(match):
            return match.group(1) + "\n\n    private static final Logger LOGGER = Logger.getLogger(" + \
                   self.extract_class_name(content) + ".class.getName());"

        return re.sub(class_pattern, replace_class, content, count=1)

    def extract_class_name(self, content: str) -> str:
        """Extrahiere Klassennamen"""
        match = re.search(r'(?:public\s+)?(?:final\s+)?(?:class|interface|enum|record)\s+(\w+)', content)
        return match.group(1) if match else "Logger"

    def refactor_empty_catches(self, content: str) -> str:
        """Refaktoriere leere Catch-Blöcke"""

        # Muster für verschiedene Exception-Typen
        patterns = [
            # catch (Exception ignored) {}
            (r'catch\s*\(\s*(\w+)\s+(\w+)\s*\)\s*\{\s*\}',
             r'catch (\1 \2) {\n            LOGGER.log(Level.FINE, "Unexpected error: " + \2.getMessage(), \2);\n        }'),

            # catch (InterruptedException ignored) {} - spezielle Behandlung
            (r'catch\s*\(\s*InterruptedException\s+(\w+)\s*\)\s*\{\s*\}',
             r'catch (InterruptedException \1) {\n            LOGGER.log(Level.FINE, "Thread interrupted", \1);\n            Thread.currentThread().interrupt();\n        }'),

            # catch (SocketTimeoutException ignored) {}
            (r'catch\s*\(\s*SocketTimeoutException\s+(\w+)\s*\)\s*\{\s*\}',
             r'catch (SocketTimeoutException \1) {\n            LOGGER.log(Level.FINE, "Socket timeout", \1);\n        }'),

            # catch (IOException ignored) {}
            (r'catch\s*\(\s*IOException\s+(\w+)\s*\)\s*\{\s*\}',
             r'catch (IOException \1) {\n            LOGGER.log(Level.FINE, "IO error", \1);\n        }'),
        ]

        result = content
        for pattern, replacement in patterns:
            result = re.sub(pattern, replacement, result)

        return result

    def process_file(self, filepath: Path) -> Tuple[bool, str]:
        """Verarbeite eine einzelne Datei"""
        try:
            content = filepath.read_text(encoding='utf-8')

            if not self.has_empty_catch_block(content):
                return True, "Keine leeren Catch-Blöcke"

            # Schritt 1: Logger-Import hinzufügen
            content = self.add_logger_import(content)

            # Schritt 2: Logger-Feld hinzufügen
            content = self.add_logger_field(content)

            # Schritt 3: Leere Catches refaktorieren
            content = self.refactor_empty_catches(content)

            # Speichere die Datei zurück
            filepath.write_text(content, encoding='utf-8')

            return True, "Erfolgreich refaktoriert"

        except Exception as e:
            return False, str(e)

    def run(self, dry_run: bool = True):
        """Führe Refactoring durch"""
        java_files = self.find_java_files()
        print(f"Gefunden: {len(java_files)} Java-Dateien\n")

        files_to_process = [f for f in java_files if self.has_empty_catch_block(f.read_text())]
        print(f"Mit leeren Catches: {len(files_to_process)} Dateien\n")

        if dry_run:
            print("=== DRY-RUN MODE (keine Änderungen) ===\n")
        else:
            print("=== REFACTORING (Änderungen werden gespeichert) ===\n")

        for filepath in sorted(files_to_process)[:20]:  # Begrenzen auf 20 für Demo
            success, message = self.process_file(filepath) if not dry_run else (True, "Würde refaktorieren")

            rel_path = filepath.relative_to(self.project_root)
            status = "✓" if success else "✗"
            print(f"  {status} {rel_path}: {message}")

            if success:
                self.refactored.append(str(rel_path))
            else:
                self.failed.append((str(rel_path), message))

        print(f"\n=== Zusammenfassung ===")
        print(f"Erfolgreich: {len(self.refactored)}")
        print(f"Fehler: {len(self.failed)}")

        if self.failed:
            print("\nFehlerhafte Dateien:")
            for filepath, error in self.failed:
                print(f"  - {filepath}: {error}")

if __name__ == "__main__":
    dry_run = "--dry-run" in sys.argv or "-d" in sys.argv
    refactorer = CatchBlockRefactorer(".")
    refactorer.run(dry_run=dry_run)

