package simulator.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SystemProcess} validating periodic release,
 * I/O handling, queue behavior and resilience under load.
 */
public class SystemProcessTest {

    @Test
    @DisplayName("Test construction: System process should initialize with zero memory and correct period")
    /**
     * Confirms that a {@code SystemProcess} initializes with zero memory
     * requirement and the provided period.
     */
    void testInitialization_ShouldInitializeAllFieldsCorrectly() {
        SystemProcess process = new SystemProcess(0, 5);

        assertEquals(0, process.getId(), "System ID should match the provided value.");
        assertEquals(0, process.getRequiredMemory(), "System process memory requirement must always be 0.");
        assertEquals(ProcessState.NEW, process.getCurrentState(), "Initial state of the system process should be NEW.");
    }

    @Test
    @DisplayName("Test periodic release: checkReleaseTime should trigger exactly when the period is reached")
    /**
     * Verifies periodic release behavior: {@code checkReleaseTime} returns
     * true exactly when the configured period elapses.
     */
    void testCheckReleaseTime_ShouldTriggerPeriodicallyBasedOnPeriod() {
        SystemProcess process = new SystemProcess(0, 3);

        assertFalse(process.checkReleaseTime(), "Should not release on the first tick before period expires.");
        assertFalse(process.checkReleaseTime(), "Should not release on the second tick before period expires.");
        assertTrue(process.checkReleaseTime(), "Should release exactly on the third tick (matching the period).");

        // Reset check
        process.checkReleaseTime();
        process.checkReleaseTime();
        assertTrue(process.checkReleaseTime(), "Should trigger correctly again after another full period cycles.");
    }

    @Test
    @DisplayName("Test I/O execution: System process should execute user I/O bursts and return them when finished")
    /**
     * Tests that the system process performs I/O servicing for user
     * processes and reports finished I/O correctly.
     */
    void testExecuteTick_ShouldProcessIoBurstAndIdentifyFinishedProcess() {
        SystemProcess systemProcess = new SystemProcess(0, 5);
        UserProcess userProcess = new UserProcess(1, 1024, 0, new int[]{1, 2, 1});

        // Transition user process from CPU burst to IO burst
        userProcess.executeTick(1);
        systemProcess.requestSystemCall(userProcess);

        systemProcess.executeTick(2);
        assertNull(systemProcess.getFinishedIoProcess(), "Process should not be finished after only one tick of a two-tick IO burst.");

        systemProcess.executeTick(3);
        assertEquals(userProcess, systemProcess.getFinishedIoProcess(), "UserProcess should be reported as finished after completing its full IO burst duration.");
        assertFalse(userProcess.isCurrentlyDoingIo(), "Process should transition back to CPU mode for its final burst.");
        assertNotEquals(ProcessState.TERMINATED, userProcess.getCurrentState(), "Process should NOT be terminated yet.");

        userProcess.executeTick(4);
        assertEquals(ProcessState.TERMINATED, userProcess.getCurrentState(), "Process should be TERMINATED after the entire sequence ends.");
    }

    @Test
    @DisplayName("Test idle behavior: System process should transition to WAITING_IO when the queue is empty")
    /**
     * Ensures the system process transitions to {@code WAITING_IO} when
     * there are no pending I/O requests.
     */
    void testExecuteTick_ShouldTransitionToWaitingIoWhenNoTasksPending() {
        SystemProcess process = new SystemProcess(0, 5);
        process.setState(ProcessState.RUNNING);

        process.executeTick(1);

        assertEquals(ProcessState.WAITING_IO, process.getCurrentState(), "System process should move to WAITING_IO state if it has no user processes to service.");
    }

    @Test
    @DisplayName("Test resilience: Circular queue should handle requests up to capacity without crashing")
    /**
     * Exercises the internal queue to confirm it handles many enqueues
     * without throwing exceptions.
     */
    void testRequestSystemCall_ShouldHandleCapacityLimitsGracefully() {
        SystemProcess process = new SystemProcess(0, 10);

        // Fill the queue beyond its internal limit
        for (int i = 0; i < 1005; i++) {
            process.requestSystemCall(new UserProcess(i, 1024, 0, new int[]{1, 1}));
        }

        assertDoesNotThrow(() -> process.executeTick(1), "System process should handle a saturated IO queue without throwing exceptions.");
    }

    @Test
    @DisplayName("Test queue peek: getCurrentlyProcessingIo should return null when the queue is empty")
    /**
     * Validates that peeking the I/O queue returns null when empty.
     */
    void testGetCurrentlyProcessingIo_WhenQueueIsEmpty_ShouldReturnNull() {
        SystemProcess process = new SystemProcess(0, 5);

        assertNull(process.getCurrentlyProcessingIo(), "Should return null if no process is currently waiting for I/O.");
    }

    @Test
    @DisplayName("Test queue peek: getCurrentlyProcessingIo should return the process at the head of the queue")
    /**
     * Confirms the head of the I/O queue is returned while processing
     * and that completion advances the queue.
     */
    void testGetCurrentlyProcessingIo_WhenQueueHasProcesses_ShouldReturnHeadProcess() {
        SystemProcess systemProcess = new SystemProcess(0, 5);
        UserProcess process1 = new UserProcess(1, 1024, 0, new int[]{1, 2});
        UserProcess process2 = new UserProcess(2, 1024, 0, new int[]{1, 2});

        // Transition both user processes from CPU to I/O
        process1.executeTick(1);
        process2.executeTick(1);

        systemProcess.requestSystemCall(process1);
        assertEquals(process1, systemProcess.getCurrentlyProcessingIo(), "Should return the first process added to the queue.");

        systemProcess.requestSystemCall(process2);
        assertEquals(process1, systemProcess.getCurrentlyProcessingIo(), "Should still return the first process because it hasn't finished its I/O yet.");

        systemProcess.executeTick(2);
        systemProcess.executeTick(3);
        assertEquals(ProcessState.TERMINATED, process1.getCurrentState(), "Process should be TERMINATED after the entire sequence ends.");

        // process2 should be at the head of the queue
        assertEquals(process2, systemProcess.getCurrentlyProcessingIo(), "Should return the second process after the first one has finished and left the queue.");
    }
}