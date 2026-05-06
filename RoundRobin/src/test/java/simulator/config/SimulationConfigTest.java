package simulator.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class SimulationConfigTest {

    @Test
    @DisplayName("Test constructor and getters - Valid Data")
    public void testConfigCreation() {
        SimulationConfig config = new SimulationConfig(4, 1024, 10, 50, 5.0);

        assertEquals(4, config.getNumProcessors(), "The number of processors should be 4");
        assertEquals(1024, config.getTotalRAM(), "The RAM should be 1024");
        assertEquals(10, config.getTimeSlice(), "The time slice should be 10");
        assertEquals(50, config.getSystemProcessPeriod(), "The VIP period should be 50");
        assertEquals(5.0, config.getDiskTransferRate(), 0.001, "The disk rate should be 5.0");
    }

    @Test
    @DisplayName("Test toString - Formatting Verification")
    public void testToString() {
        SimulationConfig config = new SimulationConfig(1, 256, 5, 20, 2.0);
        String expected = "SimulationConfig{numProcessors=1, totalRAM=256, timeSlice=5, systemProcessPeriod=20, diskTransferRate=2.0}";
        assertEquals(expected, config.toString());
    }
}