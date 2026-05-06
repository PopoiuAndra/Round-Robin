package simulator.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessorTest {

    @Test
    @DisplayName("Test construction: All fields should be initialized correctly with valid parameters")
    void testInitialization_ShouldInitializeAllFieldsCorrectly() {
        Processor processor = new Processor(0);

        assertEquals(0, processor.getId(), "Processor ID should match the provided value.");
        assertNull(processor.getCurrentProcess(), "There should be no process assigned initially.");
        assertTrue(processor.isIdle(), "A newly created processor should be idle.");
        assertTrue(processor.isTimeSliceExpired(), "Time slice should be considered expired when no process is assigned (remaining is 0).");
    }

    @Test
    @DisplayName("Test assignment: Should update processor state, process state, and affinity correctly")
    void testAssignProcess_ShouldSetProcessAndUpdateStates() {
        Processor processor = new Processor(0);
        UserProcess process = new UserProcess(0, 1024, 0, new int[]{5});

        processor.assignProcess(process, 3);

        assertEquals(ProcessState.RUNNING, process.getCurrentState(), "Process state should be updated to RUNNING.");
        assertEquals(0, process.getLastProcessorId(), "Process affinity should be set to the processor's ID.");
        assertEquals(process, processor.getCurrentProcess(), "Processor should hold the assigned process.");
        assertFalse(processor.isIdle(), "Processor should not be idle after assignment.");
        assertFalse(processor.isTimeSliceExpired(), "Time slice should not be expired immediately after assignment.");
    }

    @Test
    @DisplayName("Test safety: assignProcess should handle null process without crashing")
    void testAssignProcess_WithNull_ShouldHandleGracefully() {
        Processor processor = new Processor(0);

        assertDoesNotThrow(() -> processor.assignProcess(null, 3), "Assigning a null process should be handled gracefully without exceptions.");
        assertTrue(processor.isIdle(), "Processor should remain idle when assigned a null process.");
    }

    @Test
    @DisplayName("Test eviction: Should clear the processor and return the evicted process")
    void testEvictProcess_ShouldClearStateAndReturnProcess() {
        Processor processor = new Processor(0);
        UserProcess process = new UserProcess(0, 1024, 0, new int[]{5});
        processor.assignProcess(process, 3);

        Process evicted = processor.evictProcess();

        assertEquals(process, evicted, "Evict should return the exact process that was previously running.");
        assertNull(processor.getCurrentProcess(), "Processor should not hold any process after eviction.");
        assertTrue(processor.isIdle(), "Processor should be idle after the process is evicted.");
        assertTrue(processor.isTimeSliceExpired(), "Time slice should be reset/expired after eviction.");
    }

    @Test
    @DisplayName("Test execution flow: Should delegate tick to the process and decrement time slice")
    void testExecuteTick_ShouldDecrementTimeSliceAndAdvanceProcess() {
        Processor processor = new Processor(0);
        UserProcess process = new UserProcess(0, 1024, 0, new int[]{2});
        processor.assignProcess(process, 3);

        processor.executeTick(1);
        assertFalse(processor.isTimeSliceExpired(), "Time slice should not be expired after 1 tick out of 3.");
        assertNotEquals(ProcessState.TERMINATED, process.getCurrentState(), "Process should NOT be TERMINATED yet.");

        processor.executeTick(2);
        assertFalse(processor.isTimeSliceExpired(), "Time slice should not be expired after 2 ticks out of 3.");
        assertEquals(ProcessState.TERMINATED, process.getCurrentState(), "Process should be TERMINATED after finishing its 2-tick burst.");

        processor.executeTick(3);
        assertTrue(processor.isTimeSliceExpired(), "Time slice should be expired after the 3rd tick.");
        assertEquals(ProcessState.TERMINATED, process.getCurrentState(), "Process should remain TERMINATED.");
    }

    @Test
    @DisplayName("Test resilience: executeTick should not throw exceptions when processor is idle")
    void testExecuteTick_WhenIdle_ShouldNotThrowExceptions() {
        Processor processor = new Processor(0);

        assertDoesNotThrow(() -> processor.executeTick(1), "Executing a tick on an idle processor should not throw any exceptions.");
    }
}