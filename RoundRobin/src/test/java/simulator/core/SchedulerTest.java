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
    void testQueueCapacityOverflow() {
        Scheduler scheduler = new Scheduler();

        // Umplem coada până la limita de 1000
        for (int i = 0; i < 1000; i++) {
            scheduler.addProcess(mock(UserProcess.class));
        }

        // Testăm ramura 'else' din addProcess (când coada e plină)
        assertDoesNotThrow(() -> scheduler.addProcess(mock(UserProcess.class)),
                "Adding over capacity using addProcess should not throw exceptions.");

        // Testăm ramura 'else' din addProcessToFront
        assertDoesNotThrow(() -> scheduler.addProcessToFront(mock(UserProcess.class)),
                "Adding over capacity using addProcessToFront should not throw exceptions.");
    }

    @Test
    @DisplayName("Test addProcessToFront: Process should be placed at the head of the queue")
    void testAddProcessToFront() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };
        UserProcess process1 = new UserProcess(1, 1024, 0, new int[]{5});
        UserProcess process2 = new UserProcess(2, 1024, 0, new int[]{5});

        scheduler.addProcess(process1);
        scheduler.addProcessToFront(process2); // Va fi pus în fața lui process1

        when(memManagerMock.isProcessInRam(any())).thenReturn(true);

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        // Deoarece a fost adăugat în față, process2 trebuie extras primul
        assertEquals(process2, processors[0].getCurrentProcess(), "Process added to front should be scheduled first.");
    }

    @Test
    @DisplayName("Test schedule: CPU not idle should skip assignment")
    void testSchedule_CpuNotIdle_ShouldDoNothing() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };
        UserProcess dummyProcess = new UserProcess(1, 1024, 0, new int[]{5});

        // Ocupăm procesorul (isIdle() va fi false)
        processors[0].assignProcess(dummyProcess, 5);

        // Chemăm planificatorul
        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        // Asigurăm-ne că procesorul a rămas cu procesul inițial, fără a atinge alte logici
        assertEquals(dummyProcess, processors[0].getCurrentProcess());
        verify(engineMock, never()).logEvent(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("Test schedule: SystemProcess VIP priority (With and Without Target I/O Process)")
    void testSchedule_SystemProcessPriority() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };

        // VIP-ul are nevoie de CPU
        when(sysProcessMock.getCurrentState()).thenReturn(ProcessState.READY);

        // Cazul 1: Nu rezolvă I/O pentru nimeni (targetIo este null)
        when(sysProcessMock.getCurrentlyProcessingIo()).thenReturn(null);
        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);
        verify(engineMock, atLeastOnce()).logEvent(eq(10), eq("SCHEDULER"), contains("VIP process took CPU"));

        // Golim CPU-ul pentru a testa Cazul 2
        processors[0].evictProcess();

        // Cazul 2: Rezolvă I/O pentru un proces specific (targetIo != null)
        UserProcess targetIoMock = mock(UserProcess.class);
        when(targetIoMock.getId()).thenReturn(99);
        when(sysProcessMock.getCurrentlyProcessingIo()).thenReturn(targetIoMock);

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 11, engineMock);
        verify(engineMock, atLeastOnce()).logEvent(eq(11), eq("SCHEDULER"), contains("to resolve I/O for Process 99"));
    }

    @Test
    @DisplayName("Test schedule: Empty queue (extractNextProcess returns null)")
    void testSchedule_EmptyQueue() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };

        // Testăm ramura if (nextP != null) -> false (size == 0)
        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        assertTrue(processors[0].isIdle(), "Processor should remain idle if ready queue is empty.");
    }

    @Test
    @DisplayName("Test schedule & Affinity: Extract process from the middle of the queue (Respect Affinity)")
    void testSchedule_AffinityFoundInMiddle() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) }; // CPU disponibil are id=1

        UserProcess p0 = new UserProcess(0, 100, 0, new int[]{1}); p0.setLastProcessorId(9);
        UserProcess p1 = new UserProcess(1, 100, 0, new int[]{1}); p1.setLastProcessorId(1); // AFINITATE pentru CPU 1
        UserProcess p2 = new UserProcess(2, 100, 0, new int[]{1}); p2.setLastProcessorId(8);

        scheduler.addProcess(p0);
        scheduler.addProcess(p1);
        scheduler.addProcess(p2);

        when(memManagerMock.isProcessInRam(any())).thenReturn(true);

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        // Planificatorul trebuie să îl fi sărit pe p0 și să-l fi ales pe p1 datorită afinității
        // Asta acoperă bucla for din `removeProcessAtIndex` pentru index din mijloc
        assertEquals(p1, processors[0].getCurrentProcess(), "Process with matching affinity should be extracted.");
        verify(engineMock, atLeastOnce()).logEvent(eq(10), eq("SCHEDULER"), contains("Affinity respected"));
    }

    @Test
    @DisplayName("Test schedule: No affinity found (Classic Round Robin)")
    void testSchedule_NoAffinityFound() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };

        UserProcess p0 = new UserProcess(0, 100, 0, new int[]{1}); p0.setLastProcessorId(9); // Niciunul nu are afinitate 1
        UserProcess p1 = new UserProcess(1, 100, 0, new int[]{1}); p1.setLastProcessorId(8);

        scheduler.addProcess(p0);
        scheduler.addProcess(p1);

        when(memManagerMock.isProcessInRam(any())).thenReturn(true);

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        // Dacă nu găsește afinitate, trebuie să returneze pur și simplu primul proces din coadă (p0)
        assertEquals(p0, processors[0].getCurrentProcess(), "First process should be chosen if no affinity matches.");
        // Asigurăm-ne că NU apare "Affinity respected" în log
        verify(engineMock, never()).logEvent(anyInt(), anyString(), contains("Affinity respected"));
    }

    @Test
    @DisplayName("Test schedule & RAM: Process not in RAM, Disk is IDLE (Triggers SWAP-IN)")
    void testSchedule_NotInRam_DiskIdle() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };
        UserProcess p0 = new UserProcess(0, 100, 0, new int[]{1});
        scheduler.addProcess(p0);

        when(memManagerMock.isProcessInRam(p0)).thenReturn(false);
        when(memManagerMock.isSwapping()).thenReturn(false); // Discul este liber

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        // Procesorul rămâne liber pentru că procesul e la încărcat
        assertTrue(processors[0].isIdle());
        assertEquals(ProcessState.SWAPPING, p0.getCurrentState(), "Process state should change to SWAPPING.");
        verify(memManagerMock).startLoadingProcessToRam(p0, 10, engineMock);
    }

    @Test
    @DisplayName("Test schedule & RAM: Process not in RAM, Disk is BUSY (Puts back in queue)")
    void testSchedule_NotInRam_DiskBusy() {
        Scheduler scheduler = new Scheduler();
        Processor[] processors = { new Processor(1) };
        UserProcess p0 = new UserProcess(0, 100, 0, new int[]{1});
        scheduler.addProcess(p0);

        when(memManagerMock.isProcessInRam(p0)).thenReturn(false);
        when(memManagerMock.isSwapping()).thenReturn(true); // Discul este deja ocupat cu alt swap

        scheduler.schedule(processors, sysProcessMock, memManagerMock, 3, 10, engineMock);

        assertTrue(processors[0].isIdle());
        // Procesul a fost pus înapoi în coadă prin addProcess, ceea ce îi schimbă starea înapoi în READY
        assertEquals(ProcessState.READY, p0.getCurrentState(), "Process should be placed back into the READY queue since disk is busy.");
    }
}