# CLEAN CODE GUIDE - SCHNELL-REFERENZ FÜR ENTWICKLER

## ✅ Was tun (Erlaubt)

```java
// 1. LOGGING: Alle Exceptions loggen
try {
    riskyOperation();
} catch (SpecificException e) {
    LOGGER.log(Level.FINE, "Context message", e);
}

// 2. GUARD CLAUSES: Early Returns bevorzugt
public void process(String input) {
    if (input == null || input.isEmpty()) {
        throw new IllegalArgumentException("Input required");
    }
    // ... normale Logik
}

// 3. KONSTANTEN: Statt magische Zahlen
private static final int DEFAULT_TIMEOUT = 5000;
private static final String CONFIG_DIR = "config";

// 4. RESPONSIBLY: Eine Aufgabe pro Methode
private void fetchData() { /* ... */ }
private void processData() { /* ... */ }
private void saveData() { /* ... */ }

// 5. NAMING: Sprechende Namen
class UserValidator { }      // ✅ Gut
class UserCheck { }          // ❌ Alias-Muster
class UserHelper { }         // ❌ Helper-Antipattern
class UserUtilities { }      // ❌ Utils-Antipattern
```

## ❌ Was NICHT tun (Verboten)

```java
// 1. LEERE CATCH-BLÖCKE
catch (Exception ignored) { }          // ❌ VERBOTEN
catch (InterruptedException ignored) { } // ❌ VERBOTEN

// 2. ZU LANGE METHODEN
public void doEverything() {
    // 200 Zeilen Code  ❌ VERBOTEN
}

// 3. MAGISCHE ZAHLEN
new Socket().setSoTimeout(5000);  // ❌ Wo kommt 5000 her?
buffer[256];                      // ❌ Warum genau 256?

// 4. BOOLEAN PARAMETER (versteckt Absicht)
save(user, true);  // ❌ Was bedeutet true?
saveActive(user);  // ✅ Gut

// 5. GLOBALE MUTABLE STATICS
public static User currentUser;  // ❌ Antipattern
// Statt: Dependency Injection verwenden

// 6. UTILS/HELPER/MANAGER KLASSEN
public class StringUtils { }    // ❌ Antipattern
public class DateHelper { }     // ❌ Antipattern
public class CacheManager { }   // ❌ Antipattern
```

## 📋 Refactoring-Checkliste

- [ ] Alle Catch-Blöcke haben Logging
- [ ] Keine Utils/Helper/Manager Klassen
- [ ] Keine magischen Zahlen > 10
- [ ] Klassen < 100 Zeilen
- [ ] Methoden < 60 Zeilen (30 empfohlen)
- [ ] Guard Clauses verwendet
- [ ] Single Responsibility Principle
- [ ] Keine leeren Interfaces/Methoden
- [ ] Aussagekräftige Variablennamen

## 🔧 Häufige Refactorings

### Pattern 1: Leere Catches → Logging
```java
// VORHER
try { operation(); } 
catch (Exception ignored) { }

// NACHHER
try { 
    operation(); 
} catch (SpecificException e) {
    LOGGER.log(Level.FINE, "Context", e);
}
```

### Pattern 2: Utils-Klasse → Spezialisierte Klasse
```java
// VORHER
class StringUtils {
    public static String formatDate(Date d) { ... }
    public static boolean isEmail(String s) { ... }
}

// NACHHER
class DateFormatter {
    public String format(Date date) { ... }
}

class EmailValidator {
    public boolean isValid(String email) { ... }
}
```

### Pattern 3: Zu lange Methode → Mehrere Methoden
```java
// VORHER (80 Zeilen!)
public void process(Data d) {
    // fetch...
    // transform...
    // validate...
    // save...
}

// NACHHER
public void process(Data d) {
    Data fetched = fetchData(d);
    Data transformed = transform(fetched);
    validate(transformed);
    save(transformed);
}

private Data fetchData(Data d) { /* ... */ }
private Data transform(Data d) { /* ... */ }
private void validate(Data d) { /* ... */ }
private void save(Data d) { /* ... */ }
```

---

**Status:** 8/45 Dateien behoben (18%)  
**Nächst:** Automatische Batch-Verarbeitung der Rest

