package simulator.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserProcess} covering construction, execution
 * behavior, edge cases and basic invariants such as processor affinity
 * and {@code toString()} formatting.
 */
class UserProcessTest {
    /**
     * Verifies that a {@code UserProcess} constructed with valid values
     * initializes all fields correctly (id, required memory, release time,
     * initial state and IO flag).
     */
    @Test
    @DisplayName("Test construction: All fields should be initialized correctly with valid parameters")
    void testInitialization_ShouldInitializeAllFieldsCorrectly() {
        int id = 1;
        int memory = 1024;
        int releaseTime = 10;
        int[] sequence = {5, 2, 3};

        UserProcess process = new UserProcess(id, memory, releaseTime, sequence);

        assertEquals(id, process.getId(), "Process ID should match the provided value.");
        assertEquals(memory, process.getRequiredMemory(), "Required memory should match the provided value.");
        assertEquals(releaseTime, process.getReleaseTime(), "Release time should match the provided value.");
        assertEquals(ProcessState.NEW, process.getCurrentState(), "Initial process state should be NEW.");
        assertFalse(process.isCurrentlyDoingIo(), "Process should initially start in CPU burst, not IO.");
    }

    /**
     * Verifies that {@link UserProcess#executeTick(int)} handles null or
     * empty sequences by immediately transitioning to {@code TERMINATED}.
     */
    @Test
    @DisplayName("Test safety: Should handle null or empty sequences by terminating gracefully")
    void testExecuteTick_WithInvalidSequence_ShouldTerminateImmediately() {
        UserProcess processWithNullSeq = new UserProcess(1, 1024, 10, null);
        processWithNullSeq.executeTick(1);
        assertEquals(ProcessState.TERMINATED, processWithNullSeq.getCurrentState(), "Process with null sequence should terminate on the first tick.");

        UserProcess processWithEmptySeq = new UserProcess(1, 1024, 10, new int[]{});
        processWithEmptySeq.executeTick(1);
        assertEquals(ProcessState.TERMINATED, processWithEmptySeq.getCurrentState(), "Process with empty sequence should terminate on the first tick.");
    }

    /**
     * Tests that a process alternates correctly between CPU and IO bursts
     * according to its sequence and ultimately terminates.
     */
    @Test
    @DisplayName("Test execution flow: Process should alternate between CPU and IO bursts correctly")
    void testExecuteTick_ShouldAlternateCpuAndIoCorrectly() {
        UserProcess process = new UserProcess(1, 1024, 10, new int[]{2, 1});

        // First tick on CPU
        process.executeTick(1);
        assertFalse(process.isCurrentlyDoingIo(), "Process should still be in CPU burst after 1 tick.");
        assertNotEquals(ProcessState.TERMINATED, process.getCurrentState(), "Process should not be terminated yet.");

        // Second tick on CPU (end of burst)
        process.executeTick(2);
        assertTrue(process.isCurrentlyDoingIo(), "Process should transition to IO after the first CPU burst ends.");
        assertNotEquals(ProcessState.TERMINATED, process.getCurrentState(), "Process should not be terminated yet.");

        // Final tick on IO
        process.executeTick(3);
        assertEquals(ProcessState.TERMINATED, process.getCurrentState(), "Process should be TERMINATED after the entire sequence ends.");
    }

    /**
     * Ensures negative burst values trigger immediate transitions to IO.
     */
    @Test
    @DisplayName("Test edge case: Negative burst values should trigger immediate state transition")
    void testExecuteTick_WithNegativeValuesInSequence_ShouldTransitionImmediately() {
        UserProcess process = new UserProcess(1, 1024, 10, new int[]{-1, 2});

        process.executeTick(1);
        assertTrue(process.isCurrentlyDoingIo(), "A negative CPU burst should cause an immediate switch to IO mode.");
    }

    /**
     * Verifies processor affinity storage via {@code setLastProcessorId/getLastProcessorId}.
     */
    @Test
    @DisplayName("Test affinity: Should correctly store and retrieve the last processor identifier")
    void testProcessorAffinity_ShouldStoreAndRetrieveLastProcessorId() {
        UserProcess process = new UserProcess(5, 1024, 0, new int[]{10});

        assertEquals(-1, process.getLastProcessorId(), "Initial processor ID should be -1.");

        process.setLastProcessorId(3);
        assertEquals(3, process.getLastProcessorId(), "Process should remember the last processor ID it was assigned to.");
    }

    /**
     * Confirms calling {@code executeTick} on a terminated process is a no-op
     * and does not throw exceptions.
     */
    @Test
    @DisplayName("Test resilience: Calling executeTick on a terminated process should not throw exceptions")
    void testExecuteTick_WhenAlreadyTerminated_ShouldNotThrowAndStayTerminated() {
        UserProcess process = new UserProcess(6, 100, 0, new int[]{1});

        process.executeTick(1);
        assertEquals(ProcessState.TERMINATED, process.getCurrentState(), "Process should first reach the TERMINATED state.");

        assertDoesNotThrow(() -> process.executeTick(2), "Calling executeTick on a terminated process should be handled gracefully.");
        assertEquals(ProcessState.TERMINATED, process.getCurrentState(), "Process state should remain TERMINATED even after extra ticks.");
    }

    /**
     * Checks that {@code toString()} contains the expected identifying
     * fields and handles null sequences gracefully.
     */
    @Test
    @DisplayName("Test debug: toString should return a formatted string containing key process details")
    void testToString_ShouldReturnFormattedStringWithDetails() {
        UserProcess process = new UserProcess(1, 1024, 10, new int[]{5, 2});
        process.setLastProcessorId(2);
        process.setState(ProcessState.READY);

        String result = process.toString();

        assertTrue(result.contains("id=1"), "ToString should contain the correct process ID.");
        assertTrue(result.contains("requiredMemory=1024"), "ToString should contain memory requirements.");
        assertTrue(result.contains("releaseTime=10"), "ToString should contain the release time.");
        assertTrue(result.contains("currentState=READY"), "ToString should reflect the current process state.");
        assertTrue(result.contains("lastProcessorId=2"), "ToString should show the last processor it ran on.");
        assertTrue(result.contains("sequenceLength=2"), "ToString should show the total number of bursts in the sequence.");

        // Null Sequence handled
        UserProcess processWithNullSeq = new UserProcess(1, 1024, 10, null);
        String resultNullSeq = processWithNullSeq.toString();
        assertTrue(resultNullSeq.contains("sequenceLength=0"), "ToString should handle null sequences by showing length 0.");
    }
}