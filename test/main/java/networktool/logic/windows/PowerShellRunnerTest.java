package main.java.networktool.logic.windows;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class PowerShellRunnerTest {
    @Test void run_nonWindows_orInvalidScript_noThrow() {
        assertDoesNotThrow(() -> PowerShellRunner.run("echo test", 2000));
    }
    @Test void isAvailable_returnsBoolean() { assertNotNull(PowerShellRunner.isAvailable()); }
    @Test void run_windows_echoWorks() {
        assumeTrue(PowerShellRunner.isAvailable());
        assertFalse(PowerShellRunner.run("Write-Output 'hi'").isEmpty());
    }
}