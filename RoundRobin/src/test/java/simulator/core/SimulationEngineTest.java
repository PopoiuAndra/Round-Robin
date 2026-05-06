package simulator.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import simulator.config.SimulationConfig;
import simulator.model.*;
import simulator.model.Process;
import simulator.io.SimulationEventListener;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SimulationEngineTest {

    private SimulationEngine engine;
    private SimulationConfig mockConfig;
    private UserProcess[] mockProcesses;
    private SimulationEventListener mockListener;

    // Dependencies that must be injected/mocked for full control
    private Scheduler mockScheduler;
    private MemoryManager mockMemManager;
    private Processor[] mockProcessors;
    private SystemProcess mockSysProcess;

    @BeforeEach
    public void setUp() throws Exception {
        mockConfig = mock(SimulationConfig.class);
        when(mockConfig.getNumProcessors()).thenReturn(1);
        when(mockConfig.getSystemProcessPeriod()).thenReturn(10);
        when(mockConfig.getTimeSlice()).thenReturn(5);

        UserProcess p1 = mock(UserProcess.class);
        when(p1.getReleaseTime()).thenReturn(0);
        when(p1.getId()).thenReturn(1);
        when(p1.getCurrentState()).thenReturn(ProcessState.NEW);

        mockProcesses = new UserProcess[]{p1};
        mockListener = mock(SimulationEventListener.class);

        engine = new SimulationEngine(mockConfig, mockProcesses);
        engine.addEventListener(mockListener);

        // Using Reflection to inject Mocks into private fields for 100% coverage
        injectMock("scheduler", mock(Scheduler.class));
        injectMock("memoryManager", mock(MemoryManager.class));
        injectMock("systemProcess", mock(SystemProcess.class));

        mockScheduler = (Scheduler) getPrivateField("scheduler");
        mockMemManager = (MemoryManager) getPrivateField("memoryManager");
        mockSysProcess = (SystemProcess) getPrivateField("systemProcess");
    }

    private void injectMock(String fieldName, Object mockObj) throws Exception {
        Field field = SimulationEngine.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(engine, mockObj);
    }

    private Object getPrivateField(String fieldName) throws Exception {
        Field field = SimulationEngine.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(engine);
    }

    @Test
    @DisplayName("Coverage: New process release branch")
    public void testNewProcessRelease() throws Exception {
        // SimulationEngine checks if releaseTime == globalTime at every tick
        engine.run();
        verify(mockScheduler, atLeastOnce()).addProcess(any(UserProcess.class));
    }

    @Test
    @DisplayName("Coverage: Swapping logic and Memory Manager")
    public void testSwappingLogic() throws Exception {
        when(mockMemManager.isSwapping()).thenReturn(true);
        when(mockMemManager.getSwappingProcess()).thenReturn(mockProcesses[0]);
        when(mockMemManager.executeSwapTick()).thenReturn(mockProcesses[0]); // Finalizare swap

        // Execute ticks via run()
        engine.run();

        verify(mockListener, atLeastOnce()).onDiskExecution(anyInt(), anyInt());
        verify(mockScheduler, atLeastOnce()).addProcess(mockProcesses[0]);
    }

    @Test
    @DisplayName("Coverage: System Process Wake Up (VIP)")
    public void testSystemProcessWakeUp() {
        when(mockSysProcess.checkReleaseTime()).thenReturn(true);
        when(mockSysProcess.getCurrentState()).thenReturn(ProcessState.READY);

        engine.run();

        //Verifying the repeated call caused by the engine's loop
        verify(mockSysProcess, atLeastOnce()).setState(ProcessState.READY);
    }

    @Test
    @DisplayName("Coverage: Processor Logic - Process Termination")
    public void testProcessTerminationOnCPU() throws Exception {
        Processor mockCpu = mock(Processor.class);
        Processor[] procs = new Processor[]{mockCpu};
        injectMock("processors", procs);

        UserProcess p = mock(UserProcess.class);
        when(mockCpu.isIdle()).thenReturn(false);
        when(mockCpu.getCurrentProcess()).thenReturn(p);
        when(p.getCurrentState()).thenReturn(ProcessState.TERMINATED);
        when(p.getId()).thenReturn(1);

        engine.run();

        verify(mockCpu).evictProcess();
    }

    @Test
    @DisplayName("Coverage: Processor Logic - I/O Blocking")
    public void testProcessIOBlocking() throws Exception {
        Processor mockCpu = mock(Processor.class);
        injectMock("processors", new Processor[]{mockCpu});

        UserProcess p = mock(UserProcess.class);
        when(mockCpu.isIdle()).thenReturn(false);
        when(mockCpu.getCurrentProcess()).thenReturn(p);
        when(p.isCurrentlyDoingIo()).thenReturn(true);
        when(mockCpu.evictProcess()).thenReturn(p);

        engine.run();

        // Verify that the state was set at least once
        verify(p, atLeastOnce()).setState(ProcessState.WAITING_IO);
        verify(mockSysProcess, atLeastOnce()).requestSystemCall(p);
    }

    @Test
    @DisplayName("Coverage: Processor Logic - Preemption (Time Slice Expired)")
    public void testTimeSlicePreemption() throws Exception {
        Processor mockCpu = mock(Processor.class);
        injectMock("processors", new Processor[]{mockCpu});

        UserProcess p = mock(UserProcess.class);
        when(mockCpu.isIdle()).thenReturn(false);
        when(mockCpu.getCurrentProcess()).thenReturn(p);
        when(mockCpu.isTimeSliceExpired()).thenReturn(true);
        when(mockCpu.evictProcess()).thenReturn(p);

        // Using atLeastOnce() because run() iterates up to 20,001 times
        engine.run();

        verify(mockScheduler, atLeastOnce()).addProcess(p);
    }

    @Test
    @DisplayName("Coverage: Safety Timeout (T=20000)")
    public void testSafetyTimeout() throws Exception {
        // Run with 0 completed processes to hit timeout
        // The engine should stop by itself at 20.000 ticks
        engine.run();
        verify(mockListener, atLeastOnce()).onLogMessage(contains("Simularea a fost oprita fortat"));
    }
}