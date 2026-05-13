package simulator.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import simulator.config.SimulationConfig;
import simulator.model.*;
import simulator.model.Process;
import simulator.io.SimulationEventListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test suite for the SimulationEngine.
 * It uses Mockito for dependency isolation and Java Reflection to access private members.
 */
public class SimulationEngineTest {

    private SimulationEngine engine;
    private SimulationConfig mockConfig;
    private UserProcess mockUserProcess;
    private SimulationEventListener mockListener;

    private Scheduler mockScheduler;
    private MemoryManager mockMemManager;
    private SystemProcess mockSysProcess;
    private Processor mockCpu;

    /**
     * Set up the testing environment before each test case.
     * Initializes mocks and uses Reflection to inject them into the SimulationEngine.
     */
    @BeforeEach
    public void setUp() throws Exception {
        // 1. Setup global simulation parameters via a mock config
        mockConfig = mock(SimulationConfig.class);
        when(mockConfig.getNumProcessors()).thenReturn(1);
        when(mockConfig.getSystemProcessPeriod()).thenReturn(10);
        when(mockConfig.getTimeSlice()).thenReturn(5);

        // 2. Setup a dummy user process starting at T=0
        mockUserProcess = mock(UserProcess.class);
        when(mockUserProcess.getReleaseTime()).thenReturn(0);
        when(mockUserProcess.getId()).thenReturn(1);
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.NEW);

        mockListener = mock(SimulationEventListener.class);

        // 3. Create the real engine instance with the mocked process list
        engine = new SimulationEngine(mockConfig, new UserProcess[]{mockUserProcess});
        engine.addEventListener(mockListener);

        // 4. Initialize mocks for the internal subsystems
        mockScheduler = mock(Scheduler.class);
        mockMemManager = mock(MemoryManager.class);
        mockSysProcess = mock(SystemProcess.class);
        mockCpu = mock(Processor.class);

        // Default state: CPU is free
        when(mockCpu.isIdle()).thenReturn(true);

        // 5. Inject mocked subsystems into private fields of the engine using Reflection
        injectMock("scheduler", mockScheduler);
        injectMock("memoryManager", mockMemManager);
        injectMock("systemProcess", mockSysProcess);
        injectMock("processors", new Processor[]{mockCpu});
    }

    /**
     * Utility method using Reflection to set private fields in SimulationEngine.
     */
    private void injectMock(String fieldName, Object mockObj) throws Exception {
        Field field = SimulationEngine.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(engine, mockObj);
    }

    /**
     * Utility method using Reflection to call the private 'executeTick' method.
     */
    private void invokeExecuteTick() throws Exception {
        Method method = SimulationEngine.class.getDeclaredMethod("executeTick");
        method.setAccessible(true);
        method.invoke(engine);
    }

    /**
     * Checks if the engine can handle more listeners than the initial array capacity.
     */
    @Test
    @DisplayName("Coverage: Event Listener Bounds & Loops (Array Full)")
    void testEventListenerCapacity() {
        for (int i = 0; i < 15; i++) {
            engine.addEventListener(mock(SimulationEventListener.class));
        }
        // Verify that logging an event doesn't crash even if the listener array is "full"
        assertDoesNotThrow(() -> engine.logEvent(0, "TEST", "MSG"));
    }

    /**
     * Verifies that a process is not re-launched if it's already active (not NEW).
     */
    @Test
    @DisplayName("Coverage: New Launch - Release time matches but state is NOT NEW")
    void testExecuteTick_NewLaunch_StateNotNew() throws Exception {
        when(mockUserProcess.getReleaseTime()).thenReturn(0);
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        invokeExecuteTick();
        verify(mockScheduler, never()).addProcess(mockUserProcess);
    }

    /**
     * Verifies that the engine launches a process and wakes up the VIP simultaneously.
     */
    @Test
    @DisplayName("Coverage: New Launch & VIP Wakeup (From waiting state)")
    void testExecuteTick_NewLaunch_VipWakeup() throws Exception {
        when(mockSysProcess.checkReleaseTime()).thenReturn(true);
        when(mockSysProcess.getCurrentState()).thenReturn(ProcessState.WAITING_IO);
        invokeExecuteTick();
        // Verify both the user process is added and the VIP is set to READY
        verify(mockScheduler).addProcess(mockUserProcess);
        verify(mockSysProcess).setState(ProcessState.READY);
    }

    /**
     * Verifies that the VIP wakeup is skipped if the VIP is already running.
     */
    @Test
    @DisplayName("Coverage: VIP Wakeup (Skipped because already running)")
    void testExecuteTick_VipWakeup_AlreadyRunning() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockSysProcess.checkReleaseTime()).thenReturn(true);
        when(mockSysProcess.getCurrentState()).thenReturn(ProcessState.RUNNING);
        invokeExecuteTick();
        // The VIP is already on a CPU, so its state shouldn't be reset to READY
        verify(mockSysProcess, never()).setState(ProcessState.READY);
    }

    /**
     * Tests the flow when Virtual Memory finishes a disk transfer.
     */
    @Test
    @DisplayName("Coverage: Virtual Memory Swapping")
    void testExecuteTick_Swapping() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockMemManager.isSwapping()).thenReturn(true);
        when(mockMemManager.getSwappingProcess()).thenReturn(mockUserProcess);
        when(mockMemManager.executeSwapTick()).thenReturn(mockUserProcess);
        invokeExecuteTick();
        // Verify disk activity notification and process handover to scheduler
        verify(mockListener, atLeastOnce()).onDiskExecution(anyInt(), anyInt());
        verify(mockScheduler).addProcess(mockUserProcess);
    }

    /**
     * Tests that a process reaching TERMINATED state is removed from the CPU.
     */
    @Test
    @DisplayName("Coverage: Processor - Normal Process Terminates")
    void testCpuLogic_NormalTermination() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockCpu.isIdle()).thenReturn(false);
        when(mockCpu.getCurrentProcess()).thenReturn(mockUserProcess);
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.TERMINATED);
        invokeExecuteTick();
        // CPU must evict the process if it has finished
        verify(mockCpu).evictProcess();
    }

    /**
     * Tests the transition when a process requests I/O and blocks.
     */
    @Test
    @DisplayName("Coverage: Processor - Normal Process Blocks for I/O")
    void testCpuLogic_IoBlocking() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockCpu.isIdle()).thenReturn(false);
        when(mockCpu.getCurrentProcess()).thenReturn(mockUserProcess);
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.RUNNING);
        when(mockUserProcess.isCurrentlyDoingIo()).thenReturn(true);
        when(mockCpu.evictProcess()).thenReturn(mockUserProcess);
        invokeExecuteTick();
        // Process moves to WAITING_IO and the VIP receives the request
        verify(mockUserProcess).setState(ProcessState.WAITING_IO);
        verify(mockSysProcess).requestSystemCall(mockUserProcess);
    }

    /**
     * Verifies that a process continues execution if no preemption or blocking occurs.
     */
    @Test
    @DisplayName("Coverage: Processor - Normal Process running (No Preemption, No IO)")
    void testCpuLogic_NormalExecution_NoPreemption() throws Exception {
        // Fix pentru Screenshot 1049: Testăm un tick curat (false la time slice expired)
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockCpu.isIdle()).thenReturn(false);
        when(mockCpu.getCurrentProcess()).thenReturn(mockUserProcess);
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.RUNNING);
        when(mockUserProcess.isCurrentlyDoingIo()).thenReturn(false);
        when(mockCpu.isTimeSliceExpired()).thenReturn(false); // Ramura FALSE

        invokeExecuteTick();
        // CPU should NOT evict the process if it's still healthy and has time
        verify(mockCpu, never()).evictProcess();
    }

    /**
     * Verifies Round Robin preemption when the time slice expires.
     */
    @Test
    @DisplayName("Coverage: Processor - Preemption of UserProcess (Time Slice Expired)")
    void testCpuLogic_PreemptionUserProcess() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockCpu.isIdle()).thenReturn(false);
        when(mockCpu.getCurrentProcess()).thenReturn(mockUserProcess);
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.RUNNING);
        when(mockUserProcess.isCurrentlyDoingIo()).thenReturn(false);
        when(mockCpu.isTimeSliceExpired()).thenReturn(true);
        when(mockCpu.evictProcess()).thenReturn(mockUserProcess);
        invokeExecuteTick();
        // Verify process is evicted and sent back to Scheduler
        verify(mockScheduler).addProcess(mockUserProcess);
    }

    /**
     * Advanced trick to force preemption logic on the VIP process for code coverage.
     */
    @Test
    @DisplayName("Coverage: Processor - Preemption of SystemProcess (Trick for Dead Code)")
    void testCpuLogic_PreemptionSystemProcess() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockUserProcess.isCurrentlyDoingIo()).thenReturn(false);
        when(mockCpu.isIdle()).thenReturn(false);

        // 1. Lie to the engine that a user process is on CPU
        when(mockCpu.getCurrentProcess()).thenReturn(mockUserProcess);
        // 2. Trigger time slice expiration
        when(mockCpu.isTimeSliceExpired()).thenReturn(true);
        // 3. SURPRISE: Return the VIP process during eviction to trigger the specific VIP branch
        when(mockCpu.evictProcess()).thenReturn(mockSysProcess);

        invokeExecuteTick();
        // Verify VIP is correctly moved to READY state

        verify(mockSysProcess).setState(ProcessState.READY);
    }

    /**
     * Tests the case where the VIP finishes I/O for a process that then terminates.
     */
    @Test
    @DisplayName("Coverage: Processor - VIP Finished I/O (Target Terminates directly)")
    void testCpuLogic_Vip_FinishedIo_TargetTerminated() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockCpu.isIdle()).thenReturn(false);
        when(mockCpu.getCurrentProcess()).thenReturn(mockSysProcess);
        when(mockSysProcess.getCurrentState()).thenReturn(ProcessState.RUNNING);

        UserProcess finishedIoProcess = mock(UserProcess.class);
        when(finishedIoProcess.getCurrentState()).thenReturn(ProcessState.TERMINATED);
        when(mockSysProcess.getFinishedIoProcess()).thenReturn(finishedIoProcess);

        invokeExecuteTick();
        // The process finished its last burst, so it shouldn't go back to the scheduler
        verify(mockScheduler, never()).addProcess(finishedIoProcess);
    }

    /**
     * Tests the case where the VIP finishes I/O and the process returns to READY queue.
     */
    @Test
    @DisplayName("Coverage: Processor - VIP Finished I/O (Target Resumes Execution)")
    void testCpuLogic_Vip_FinishedIo_TargetResumes() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockCpu.isIdle()).thenReturn(false);
        when(mockCpu.getCurrentProcess()).thenReturn(mockSysProcess);
        when(mockSysProcess.getCurrentState()).thenReturn(ProcessState.RUNNING);

        UserProcess finishedIoProcess = mock(UserProcess.class);
        when(finishedIoProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockSysProcess.getFinishedIoProcess()).thenReturn(finishedIoProcess);

        invokeExecuteTick();
        verify(mockScheduler).addProcess(finishedIoProcess);
    }

    /**
     * Tests that the VIP releases the CPU and goes to sleep when idle.
     */
    @Test
    @DisplayName("Coverage: Processor - VIP Goes to sleep when queue is empty")
    void testCpuLogic_Vip_GoesToSleep() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockCpu.isIdle()).thenReturn(false);
        when(mockCpu.getCurrentProcess()).thenReturn(mockSysProcess);
        when(mockSysProcess.getFinishedIoProcess()).thenReturn(null);
        when(mockSysProcess.getCurrentState()).thenReturn(ProcessState.WAITING_IO);

        invokeExecuteTick();
        verify(mockCpu).evictProcess();
    }

    /**
     * Verifies that the 'run' loop stops correctly when all processes are done.
     */
    @Test
    @DisplayName("Coverage: run() loop terminates naturally (No timeout needed)")
    void testRun_NaturalTermination() throws Exception {
        // Forcefully set completed processes count to match total length
        injectMock("completedProcesses", 1);
        engine.run();
        verify(mockListener, atLeastOnce()).onLogMessage(contains("Simulation Finished"));
    }

    /**
     * Verifies the infinite loop safety mechanism (20,000 ticks limit).
     */
    @Test
    @DisplayName("Coverage: Safety Timeout loop termination")
    void testRunTimeout() {
        engine.run();
        verify(mockListener, atLeastOnce()).onLogMessage(contains("Simulation was forcibly stopped"));
    }
}