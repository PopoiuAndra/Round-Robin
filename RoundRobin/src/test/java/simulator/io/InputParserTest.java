package simulator.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import simulator.config.SimulationConfig;
import simulator.model.UserProcess;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
public class InputParserTest {

    private InputParser parser;

    @BeforeEach
    public void setUp() {
        parser = new InputParser();
    }

    @Test
    @DisplayName("Test line parsing with multiple spaces and tabs")
    public void testParseLineManual() throws Exception {
        // We test the private logic indirectly via parseFile to ensure the manual character-by-character extraction works
        // Create a temporary test file
        File tempFile = File.createTempFile("testInput", ".txt");
        try (FileWriter fw = new FileWriter(tempFile)) {
            // Config line with irregular spacing and tabs
            fw.write("2   512\t10 100 2.0\n");
            // Process line: Release=0, Mem=100, Sequence=(5, 2, 5)
            fw.write("0 100 5 2 5\n");
        }

        parser.parseFile(tempFile.getAbsolutePath());

        // Verify Configuration parsing
        SimulationConfig config = parser.getConfig();
        assertNotNull(config, "Config should not be null after parsing valid data");
        assertEquals(2, config.getNumProcessors(), "Should extract 2 processors");
        assertEquals(512, config.getTotalRAM(), "Should extract 512 units of RAM");

        // Verify Process parsing
        UserProcess[] processes = parser.getProcesses();
        assertNotNull(processes, "Process list should not be null");
        assertEquals(1, processes.length, "One process should have been successfully parsed");
        assertEquals(100, processes[0].getRequiredMemory(), "Memory required should be 100");

        // Cleanup
        tempFile.delete();
    }

    @Test
    @DisplayName("Test non-existent file - Error Handling")
    public void testFileNotFound() {
        // Verify that the parser handles missing files internally without throwing fatal exceptions
        assertDoesNotThrow(() -> parser.parseFile("non_existent_file.txt"), "The parser should handle IOExceptions internally");
        assertNull(parser.getConfig(), "Config should be null when file reading fails");
    }
}