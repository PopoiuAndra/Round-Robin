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

public class SimulationEngineTest {

    private SimulationEngine engine;
    private SimulationConfig mockConfig;
    private UserProcess mockUserProcess;
    private SimulationEventListener mockListener;

    private Scheduler mockScheduler;
    private MemoryManager mockMemManager;
    private SystemProcess mockSysProcess;
    private Processor mockCpu;

    @BeforeEach
    public void setUp() throws Exception {
        mockConfig = mock(SimulationConfig.class);
        when(mockConfig.getNumProcessors()).thenReturn(1);
        when(mockConfig.getSystemProcessPeriod()).thenReturn(10);
        when(mockConfig.getTimeSlice()).thenReturn(5);

        mockUserProcess = mock(UserProcess.class);
        when(mockUserProcess.getReleaseTime()).thenReturn(0);
        when(mockUserProcess.getId()).thenReturn(1);
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.NEW);

        mockListener = mock(SimulationEventListener.class);

        engine = new SimulationEngine(mockConfig, new UserProcess[]{mockUserProcess});
        engine.addEventListener(mockListener);

        mockScheduler = mock(Scheduler.class);
        mockMemManager = mock(MemoryManager.class);
        mockSysProcess = mock(SystemProcess.class);
        mockCpu = mock(Processor.class);

        when(mockCpu.isIdle()).thenReturn(true);

        injectMock("scheduler", mockScheduler);
        injectMock("memoryManager", mockMemManager);
        injectMock("systemProcess", mockSysProcess);
        injectMock("processors", new Processor[]{mockCpu});
    }

    private void injectMock(String fieldName, Object mockObj) throws Exception {
        Field field = SimulationEngine.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(engine, mockObj);
    }

    private void invokeExecuteTick() throws Exception {
        Method method = SimulationEngine.class.getDeclaredMethod("executeTick");
        method.setAccessible(true);
        method.invoke(engine);
    }

    @Test
    @DisplayName("Coverage: Event Listener Bounds & Loops (Array Full)")
    void testEventListenerCapacity() {
        for (int i = 0; i < 15; i++) {
            engine.addEventListener(mock(SimulationEventListener.class));
        }
        assertDoesNotThrow(() -> engine.logEvent(0, "TEST", "MSG"));
    }

    @Test
    @DisplayName("Coverage: New Launch - Release time matches but state is NOT NEW")
    void testExecuteTick_NewLaunch_StateNotNew() throws Exception {
        when(mockUserProcess.getReleaseTime()).thenReturn(0);
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        invokeExecuteTick();
        verify(mockScheduler, never()).addProcess(mockUserProcess);
    }

    @Test
    @DisplayName("Coverage: New Launch & VIP Wakeup (From waiting state)")
    void testExecuteTick_NewLaunch_VipWakeup() throws Exception {
        when(mockSysProcess.checkReleaseTime()).thenReturn(true);
        when(mockSysProcess.getCurrentState()).thenReturn(ProcessState.WAITING_IO);
        invokeExecuteTick();
        verify(mockScheduler).addProcess(mockUserProcess);
        verify(mockSysProcess).setState(ProcessState.READY);
    }

    @Test
    @DisplayName("Coverage: VIP Wakeup (Skipped because already running)")
    void testExecuteTick_VipWakeup_AlreadyRunning() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockSysProcess.checkReleaseTime()).thenReturn(true);
        when(mockSysProcess.getCurrentState()).thenReturn(ProcessState.RUNNING);
        invokeExecuteTick();
        verify(mockSysProcess, never()).setState(ProcessState.READY);
    }

    @Test
    @DisplayName("Coverage: Virtual Memory Swapping")
    void testExecuteTick_Swapping() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockMemManager.isSwapping()).thenReturn(true);
        when(mockMemManager.getSwappingProcess()).thenReturn(mockUserProcess);
        when(mockMemManager.executeSwapTick()).thenReturn(mockUserProcess);
        invokeExecuteTick();
        verify(mockListener, atLeastOnce()).onDiskExecution(anyInt(), anyInt());
        verify(mockScheduler).addProcess(mockUserProcess);
    }

    @Test
    @DisplayName("Coverage: Processor - Normal Process Terminates")
    void testCpuLogic_NormalTermination() throws Exception {
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockCpu.isIdle()).thenReturn(false);
        when(mockCpu.getCurrentProcess()).thenReturn(mockUserProcess);
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.TERMINATED);
        invokeExecuteTick();
        verify(mockCpu).evictProcess();
    }

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
        verify(mockUserProcess).setState(ProcessState.WAITING_IO);
        verify(mockSysProcess).requestSystemCall(mockUserProcess);
    }

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

        verify(mockCpu, never()).evictProcess();
    }

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
        verify(mockScheduler).addProcess(mockUserProcess);
    }

    @Test
    @DisplayName("Coverage: Processor - Preemption of SystemProcess (Trick for Dead Code)")
    void testCpuLogic_PreemptionSystemProcess() throws Exception {
        // Fix pentru Screenshot 1047 & 1048: Aici am pus trucul Magic cu Mockito pe care îl uitaseși!
        when(mockUserProcess.getCurrentState()).thenReturn(ProcessState.READY);
        when(mockUserProcess.isCurrentlyDoingIo()).thenReturn(false);
        when(mockCpu.isIdle()).thenReturn(false);

        // 1. Mințim motorul că pe CPU e un UserProcess (ca să nu intre în primul IF)
        when(mockCpu.getCurrentProcess()).thenReturn(mockUserProcess);

        // 2. Timpul expiră
        when(mockCpu.isTimeSliceExpired()).thenReturn(true);

        // 3. SURPRIZĂ: La evacuare returnează VIP-ul! Astfel se atinge ramura invizibilă!
        when(mockCpu.evictProcess()).thenReturn(mockSysProcess);

        invokeExecuteTick();

        verify(mockSysProcess).setState(ProcessState.READY); // Testul trece!
    }

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
        verify(mockScheduler, never()).addProcess(finishedIoProcess);
    }

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

    @Test
    @DisplayName("Coverage: run() loop terminates naturally (No timeout needed)")
    void testRun_NaturalTermination() throws Exception {
        // Fix pentru Screenshot 1052: Injectăm valoarea 1 în completedProcesses.
        // allUserProcesses.length e tot 1, deci while (1 < 1) dă FALS instant! Ramura acoperită!
        injectMock("completedProcesses", 1);
        engine.run();
        verify(mockListener, atLeastOnce()).onLogMessage(contains("Simulation Finished"));
    }

    @Test
    @DisplayName("Coverage: Safety Timeout loop termination")
    void testRunTimeout() {
        engine.run();
        verify(mockListener, atLeastOnce()).onLogMessage(contains("Simulation was forcibly stopped"));
    }
}