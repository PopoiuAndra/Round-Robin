package simulator.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DisplayName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LoggerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Test Logger - Full success coverage (Constructor, onLogMessage, close)")
    public void testLoggerFullSuccess() throws IOException {
        Path logPath = tempDir.resolve("output_full.txt");
        Logger logger = new Logger(logPath.toString());

        // Test onLogMessage
        logger.onLogMessage("Test Message 1");

        // Test close (which also writes the footer)
        logger.close();

        List<String> lines = Files.readAllLines(logPath);

        assertTrue(lines.contains("=== SIMULATOR EXECUTION LOG ==="), "Header should be present");
        assertTrue(lines.contains("Test Message 1"), "Log message should be written to file");
        assertTrue(lines.contains("=== SIMULATION FINALIZED ==="), "Footer should be written upon closin");
    }

    @Test
    @DisplayName("Test Logger - Empty interface methods coverage (onCpuExecution, onDiskExecution)")
    public void testEmptyInterfaceMethods() {
        Path logPath = tempDir.resolve("empty_methods.txt");
        Logger logger = new Logger(logPath.toString());

        // Call empty methods to ensure 100% method coverage
        assertDoesNotThrow(() -> {
            logger.onCpuExecution(0, 1, 10);
            logger.onDiskExecution(2, 11);
        }, "Empty listener methods should not throw exceptions");

        logger.close();
    }

    @Test
    @DisplayName("Test Logger - Error branch coverage (Catch IOException in Constructor)")
    public void testLoggerConstructorError() {
        // Use an invalid path (empty string) to force the catch block in the constructor
        String invalidPath = "";

        // The constructor catches the error internally and prints to console
        // We verify the object is created without crashing the test (fail-safe)
        Logger logger = new Logger(invalidPath);

        // Verify methods handle cases where the writer is null
        assertDoesNotThrow(() -> {
            logger.onLogMessage("This message will not be written anywhere");
            logger.close();
        }, "Logger methods should handle null writers gracefully");
    }

    @Test
    @DisplayName("Test Logger - Null Writer Protection (onLogMessage method)")
    public void testOnLogMessageNullWriter() {
        // Create a logger and close it immediately to simulate an inactive writer
        Path logPath = tempDir.resolve("null_test.txt");
        Logger logger = new Logger(logPath.toString());
        logger.close();

        // Call onLogMessage after close() - the 'if (writer != null)' check protects execution
        // For 100% coverage on the null check, the previous constructor error test is the decisive one.
        assertDoesNotThrow(() -> logger.onLogMessage("Post-close test message"),
                "Logging after close should not throw exceptions");
    }
}