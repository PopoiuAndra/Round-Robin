package simulator.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import simulator.model.ProcessState;
import simulator.model.Processor;
import simulator.model.SystemProcess;
import simulator.model.UserProcess;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Scheduler} covering queue operations, affinity
 * handling, VIP priority and interactions with the memory manager.
 */
class SchedulerTest {

    private MemoryManager memManagerMock;
    private SimulationEngine engineMock;
    private SystemProcess sysProcessMock;

    @BeforeEach
    void setUp() {
        memManagerMock = mock(MemoryManager.class);
        engineMock = mock(SimulationEngine.class);
        sysProcessMock = mock(SystemProcess.class);

        // Implicitly, VIP is NOT READY ("doesn't steal" CPU)
        when(sysProcessMock.getCurrentState()).thenReturn(ProcessState.WAITING_IO);
    }

    @Test
    @DisplayName("Test addProcess & addProcessToFront: Capacity limits should trigger error messages without crashing")
    /**
     * Exercises queue overflow behavior for both addProcess and
     * addProcessToFront to ensure they handle capacity gracefully.
     */
    void testQueueCapacityOverflow() {
        Scheduler scheduler = new Scheduler();

        for (int i = 0; i < 1000; i++) {
            scheduler.addProcess(mock(UserProcess.class));
        }

        assertDoesNotThrow(() -> scheduler.addProcess(mock(UserProcess.class)),
                "Adding over capacity using addProcess should not throw exceptions.");

        assertDoesNotThrow(() -> scheduler.addProcessToFront(mock(UserProcess.class)),
                "Adding over capacity using addProcessToFront should not throw exceptions.");
    }

    @Test
    @DisplayName("Test addProcessToFront: Process should be placed at the head of the queue")
    /**
     * Verifies that adding a process to the front of the scheduler places
     * it at the head and it will be scheduled first.
     */
    void testAddProcessToFront() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };
        UserProcess process1 = new UserProcess(1, 1024, 0, new int[]{5});
        UserProcess process2 = new UserProcess(2, 1024, 0, new int[]{5});

        scheduler.addProcess(process1);
        scheduler.addProcessToFront(process2);

        when(memManagerMock.isProcessInRam(any())).thenReturn(true);

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        assertEquals(process2, processors[0].getCurrentProcess(), "Process added to front should be scheduled first.");
    }

    @Test
    @DisplayName("Test schedule: CPU not idle should skip assignment")
    /**
     * Confirms scheduler does not override a non-idle CPU's current
     * assignment.
     */
    void testSchedule_CpuNotIdle_ShouldDoNothing() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };
        UserProcess dummyProcess = new UserProcess(1, 1024, 0, new int[]{5});

        processors[0].assignProcess(dummyProcess, 5);

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        assertEquals(dummyProcess, processors[0].getCurrentProcess());
        verify(engineMock, never()).logEvent(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("Test schedule: SystemProcess VIP priority (With and Without Target I/O Process)")
    /**
     * Validates VIP priority handling both when there is no target I/O
     * and when a target I/O process exists.
     */
    void testSchedule_SystemProcessPriority() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };

        when(sysProcessMock.getCurrentState()).thenReturn(ProcessState.READY);

        when(sysProcessMock.getCurrentlyProcessingIo()).thenReturn(null);
        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);
        verify(engineMock, atLeastOnce()).logEvent(eq(10), eq("SCHEDULER"), contains("VIP process took CPU"));

        processors[0].evictProcess();

        UserProcess targetIoMock = mock(UserProcess.class);
        when(targetIoMock.getId()).thenReturn(99);
        when(sysProcessMock.getCurrentlyProcessingIo()).thenReturn(targetIoMock);

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 11, engineMock);
        verify(engineMock, atLeastOnce()).logEvent(eq(11), eq("SCHEDULER"), contains("to resolve I/O for Process 99"));
    }

    @Test
    @DisplayName("Test schedule: Empty queue (extractNextProcess returns null)")
    /**
     * Ensures a processor remains idle when the ready queue is empty.
     */
    void testSchedule_EmptyQueue() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        assertTrue(processors[0].isIdle(), "Processor should remain idle if ready queue is empty.");
    }

    @Test
    @DisplayName("Test schedule & Affinity: Extract process from the middle of the queue (Respect Affinity)")
    /**
     * Checks that scheduler respects affinity by extracting a matching
     * process from the middle of the queue.
     */
    void testSchedule_AffinityFoundInMiddle() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };

        UserProcess p0 = new UserProcess(0, 100, 0, new int[]{1}); p0.setLastProcessorId(9);
        UserProcess p1 = new UserProcess(1, 100, 0, new int[]{1}); p1.setLastProcessorId(1);
        UserProcess p2 = new UserProcess(2, 100, 0, new int[]{1}); p2.setLastProcessorId(8);

        scheduler.addProcess(p0);
        scheduler.addProcess(p1);
        scheduler.addProcess(p2);

        when(memManagerMock.isProcessInRam(any())).thenReturn(true);

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        assertEquals(p1, processors[0].getCurrentProcess(), "Process with matching affinity should be extracted.");
        verify(engineMock, atLeastOnce()).logEvent(eq(10), eq("SCHEDULER"), contains("Affinity respected"));
    }

    @Test
    @DisplayName("Test schedule: No affinity found (Classic Round Robin)")
    /**
     * Verifies round-robin selection when no affinity match is found.
     */
    void testSchedule_NoAffinityFound() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };

        UserProcess p0 = new UserProcess(0, 100, 0, new int[]{1}); p0.setLastProcessorId(9);
        UserProcess p1 = new UserProcess(1, 100, 0, new int[]{1}); p1.setLastProcessorId(8);

        scheduler.addProcess(p0);
        scheduler.addProcess(p1);

        when(memManagerMock.isProcessInRam(any())).thenReturn(true);

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        assertEquals(p0, processors[0].getCurrentProcess(), "First process should be chosen if no affinity matches.");
        verify(engineMock, never()).logEvent(anyInt(), anyString(), contains("Affinity respected"));
    }

    @Test
    @DisplayName("Test schedule & RAM: Process not in RAM, Disk is IDLE (Triggers SWAP-IN)")
    /**
     * Ensures a process not in RAM triggers swap-in when the disk is
     * idle and is not scheduled to a CPU until loaded.
     */
    void testSchedule_NotInRam_DiskIdle() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };
        UserProcess p0 = new UserProcess(0, 100, 0, new int[]{1});
        scheduler.addProcess(p0);

        when(memManagerMock.isProcessInRam(p0)).thenReturn(false);
        when(memManagerMock.isSwapping()).thenReturn(false);

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        assertTrue(processors[0].isIdle());
        assertEquals(ProcessState.SWAPPING, p0.getCurrentState(), "Process state should change to SWAPPING.");
        verify(memManagerMock).startLoadingProcessToRam(p0, 10, engineMock);
    }

    @Test
    @DisplayName("Test schedule & RAM: Process not in RAM, Disk is BUSY (Puts back in queue)")
    /**
     * Verifies that a process not in RAM is returned to the ready queue
     * when the disk is busy swapping another process.
     */
    void testSchedule_NotInRam_DiskBusy() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };
        UserProcess p0 = new UserProcess(0, 100, 0, new int[]{1});
        scheduler.addProcess(p0);

        when(memManagerMock.isProcessInRam(p0)).thenReturn(false);
        when(memManagerMock.isSwapping()).thenReturn(true);

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        assertTrue(processors[0].isIdle());
        assertEquals(ProcessState.READY, p0.getCurrentState(), "Process should be placed back into the READY queue since disk is busy.");
    }
}