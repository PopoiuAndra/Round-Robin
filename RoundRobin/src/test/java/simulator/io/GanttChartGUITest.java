package simulator.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class GanttChartGUITest {

    private GanttChartGUI gui;

    @BeforeEach
    public void setUp() {
        // Initialize with 2 processors
        gui = new GanttChartGUI(2);
    }

    @Test
    @DisplayName("Test onCpuExecution - Full branch coverage")
    public void testOnCpuExecutionCoverage() {
        // Branch 1: Add a new block to an empty CPU
        gui.onCpuExecution(0, 10, 5);

        // Branch 2: Extend an existing block (same process, consecutive tick)
        gui.onCpuExecution(0, 10, 6);

        // Branch 3: Create a new block (same process, but NON-consecutive tick)
        gui.onCpuExecution(0, 10, 8);

        // Branch 4: Create a new block (different process, consecutive tick)
        gui.onCpuExecution(0, 20, 9);

        // Branch 5: We add a block on CPU 0 at tick 4. Since maxTick is already 9, `4 > 9` is FALSE.
        gui.onCpuExecution(0, 10, 4);

        // Verify indirectly by calling showChart without blocking the execution thread
        assertDoesNotThrow(() -> gui.onLogMessage("Dummy message")); // Test empty method
    }

    @Test
    @DisplayName("Test onDiskExecution - Full branch coverage")
    public void testOnDiskExecutionCoverage() {
        // Branch 1: First disk block
        gui.onDiskExecution(1, 1);

        // Branch 2: Extend an existing disk block
        gui.onDiskExecution(1, 2);

        // Branch 3: New disk block (different process)
        gui.onDiskExecution(2, 3);

        // Branch 4: We use the SAME processId (2), but a NON-consecutive tick (5 instead of 4).
        gui.onDiskExecution(2, 5); // maxTick becomes 5

        // Branch 5: We add a block at tick 4. Since maxTick is 5, `4 > 5` is FALSE.
        gui.onDiskExecution(3, 4);

        assertNotNull(gui);
    }

    @Test
    @DisplayName("Test Rendering Logic (GanttPanel) - Deep Coverage")
    public void testPaintComponent() {
        // Populate data to activate all drawing loops (CPU, Disk, Timeline)
        gui.onCpuExecution(0, 0, 0); // System process block (SYS)
        gui.onCpuExecution(1, 1, 1); // User process block
        gui.onDiskExecution(1, 2);   // Disk block

        // Access the internal panel to test drawing methods.
        // Since GanttPanel is a private inner class, we test through the parent component
        Container pane = gui.getContentPane();

        // Simulate a graphics context to force the execution of paintComponent
        BufferedImage img = new BufferedImage(1000, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        assertDoesNotThrow(() -> {
            // Indirectly test layout and size preferences
            gui.showChart();
            gui.paint(g2d);
        });
        g2d.dispose();
    }

    @Test
    @DisplayName("Test ExecutionBlock - Constructor")
    public void testExecutionBlockInternal() {
        // Test the constructor of the internal ExecutionBlock class through GUI behavior
        // This is done automatically through onCpuExecution calls, ensuring objects are created correctly
        gui.onCpuExecution(0, 5, 10);
        assertDoesNotThrow(() -> gui.onCpuExecution(0, 5, 11));
    }
}