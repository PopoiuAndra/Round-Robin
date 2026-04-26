package simulator.core;

import simulator.config.SimulationConfig;
import simulator.model.*;
import simulator.model.Process;
import simulator.io.SimulationEventListener;

/**
 * Motorul principal al simulării (The Clock).
 * Controlează scurgerea timpului (tick-urilor) și orchestrează interacțiunile
 * dintre planificator, memoria virtuală, procesoare și procesul de sistem.
 * Implementează un șablon de tip Observer (prin SimulationEventListener) pentru
 * a decupla logica internă de sistemele de afișare (fișier text, interfață grafică).
 */
public class SimulationEngine {

    /** Configurația globală citită din fișierul de intrare. */
    private final SimulationConfig config;

    /** Vectorul care simulează unitățile fizice de procesare (CPU). */
    private final Processor[] processors;

    /** Lista completă a proceselor de utilizator care trebuie simulate. */
    private final UserProcess[] allUserProcesses;

    /** Procesul cu prioritate maximă responsabil de apelurile de sistem (I/O). */
    private final SystemProcess systemProcess;

    /** Strategia de planificare utilizată (în acest caz, Round-Robin cu afinitate). */
    private final SchedulingStrategy scheduler;

    /** Componenta responsabilă de managementul memoriei RAM și a politicii LRU. */
    private final MemoryManager memoryManager;

    // --- Observer Pattern ---
    /** Vectorul intern de ascultători pentru evenimentele motorului. */
    private final SimulationEventListener[] listeners = new SimulationEventListener[10];

    /** Numărul curent de ascultători înregistrați. */
    private int listenerCount = 0;

    /** Timpul global al simulării, măsurat în unități abstracte (tick-uri). */
    private int globalTime = 0;

    /** Numărul de procese de utilizator care și-au încheiat complet execuția. */
    private int completedProcesses = 0;

    /**
     * Construiește și inițializează motorul de simulare.
     *
     * @param config    Configurația parametrilor sistemului.
     * @param processes Vectorul de procese citite și pregătite de lansare.
     */
    public SimulationEngine(SimulationConfig config, UserProcess[] processes) {
        this.config = config;
        this.allUserProcesses = processes;

        // Inițializare procesoare
        this.processors = new Processor[config.getNumProcessors()];
        for (int i = 0; i < config.getNumProcessors(); i++) {
            this.processors[i] = new Processor(i);
        }

        // Inițializare componente cheie
        this.systemProcess = new SystemProcess(0, config.getSystemProcessPeriod());
        this.scheduler = new Scheduler();
        this.memoryManager = new MemoryManager(config.getTotalRAM(), config.getDiskTransferRate());
    }

    /**
     * Înregistrează un nou ascultător pentru evenimentele generate de simulator.
     *
     * @param listener Obiectul care implementează interfața SimulationEventListener.
     */
    public void addEventListener(SimulationEventListener listener) {
        if (listenerCount < listeners.length) {
            listeners[listenerCount++] = listener;
        }
    }

    /**
     * Trimite un mesaj text către toți ascultătorii înregistrați.
     *
     * @param message Textul evenimentului.
     */
    private void log(String message) {
        for (int i = 0; i < listenerCount; i++) {
            listeners[i].onLogMessage(message);
        }
    }

    /**
     * Notifică ascultătorii grafici despre activitatea unui procesor la tick-ul curent.
     *
     * @param cpuId     ID-ul procesorului activ.
     * @param processId ID-ul procesului executat (0 pentru sistem).
     * @param tick      Timpul curent.
     */
    private void notifyCpuUsage(int cpuId, int processId, int tick) {
        for (int i = 0; i < listenerCount; i++) {
            listeners[i].onCpuExecution(cpuId, processId, tick);
        }
    }

    /**
     * Notifică ascultătorii grafici despre activitatea hard discului
     * (transfer în memoria virtuală) la tick-ul curent.
     *
     * @param processId ID-ul procesului transferat de pe disc.
     * @param tick      Timpul curent.
     */
    private void notifyDiskUsage(int processId, int tick) {
        for (int i = 0; i < listenerCount; i++) {
            listeners[i].onDiskExecution(processId, tick);
        }
    }

    /**
     * Punctul de pornire al simulării. Rulează o buclă continuă de timp
     * până când toate procesele de utilizator ajung în starea TERMINATED.
     * Include o siguranță (Time-out) la T=20000 pentru a preveni blocajele infinite.
     */
    public void run() {
        log("=== Start Simulare ===");

        while (completedProcesses < allUserProcesses.length) {
            executeTick();
            globalTime++;

            // Siguranță anti-buclă infinită
            if (globalTime > 20000) {
                log("Simularea a fost oprită forțat (Time-out) la T=20000.");
                break;
            }
        }

        log("=== Simulare Finalizată la T=" + globalTime + " ===");
    }

    /**
     * Metoda centrală apelată la fiecare unitate de timp.
     * Verifică lansările noi, transferurile din memorie, execută un pas
     * din procesele aflate pe procesoare, verifică condițiile de preempțiune
     * și apelează planificatorul.
     */
    private void executeTick() {
        // 1. Verificăm dacă s-au lansat procese noi la acest tick
        for (int i = 0; i < allUserProcesses.length; i++) {
            if (allUserProcesses[i].getReleaseTime() == globalTime && allUserProcesses[i].getCurrentState() == ProcessState.NEW) {
                log("T=" + globalTime + " : Procesul " + allUserProcesses[i].getId() + " a fost lansat.");
                ((Scheduler)scheduler).addProcess(allUserProcesses[i]);
            }
        }

        // 2. Gestionăm Memoria Virtuală (Transfer Disk -> RAM)
        if (memoryManager.isSwapping()) {
            notifyDiskUsage(memoryManager.getSwappingProcess().getId(), globalTime);
        }

        Process swappedInProcess = memoryManager.executeSwapTick();
        if (swappedInProcess != null) {
            log("T=" + globalTime + " : Procesul " + swappedInProcess.getId() + " a intrat în RAM.");
            ((Scheduler)scheduler).addProcess(swappedInProcess);
        }

        // 3. Verificăm dacă Procesul de Sistem (VIP) trebuie trezit periodic
        if (systemProcess.checkReleaseTime()) {
            if (systemProcess.getCurrentState() != ProcessState.RUNNING) {
                systemProcess.setState(ProcessState.READY);
            }
        }

        // 4. Avansăm timpul pe procesoare
        for (int i = 0; i < processors.length; i++) {
            Processor cpu = processors[i];

            if (!cpu.isIdle()) {
                // Notificăm graficul că procesorul funcționează
                notifyCpuUsage(cpu.getId(), cpu.getCurrentProcess().getId(), globalTime);

                cpu.executeTick(globalTime);
                Process p = cpu.getCurrentProcess();

                // 4.A. Logica specifică Procesului de Sistem (VIP)
                if (p instanceof SystemProcess) {
                    UserProcess finishedUserP = ((SystemProcess) p).getFinishedIoProcess();
                    if (finishedUserP != null) {
                        if (finishedUserP.getCurrentState() == ProcessState.TERMINATED) {
                            completedProcesses++;
                            log("T=" + globalTime + " : Procesul " + finishedUserP.getId() + " a fost TERMINAT in timpul I/O.");
                        } else {
                            ((Scheduler)scheduler).addProcess(finishedUserP);
                            log("T=" + globalTime + " : Procesul " + finishedUserP.getId() + " a terminat I/O si se întoarce (READY).");
                        }
                    }
                    if (p.getCurrentState() == ProcessState.WAITING_IO) {
                        cpu.evictProcess();
                    }
                }

                // 4.B. Tratarea terminării proceselor normale
                if (p.getCurrentState() == ProcessState.TERMINATED) {
                    cpu.evictProcess();
                    if (p instanceof UserProcess) {
                        completedProcesses++;
                        log("T=" + globalTime + " : Procesul " + p.getId() + " a fost TERMINAT de pe CPU.");
                    }
                }
                // 4.C. Blocarea proceselor utilizator pentru un Apel de Sistem (I/O)
                else if (p instanceof UserProcess && ((UserProcess) p).isCurrentlyDoingIo()) {
                    log("T=" + globalTime + " : Procesul " + p.getId() + " se blochează pentru System Call (I/O).");
                    p.setState(ProcessState.WAITING_IO);
                    systemProcess.requestSystemCall((UserProcess) cpu.evictProcess());
                }
                // 4.D. Preempțiunea: expirarea cuantei de timp (Time Slice)
                else if (cpu.isTimeSliceExpired()) {
                    Process evicted = cpu.evictProcess();
                    if (evicted instanceof UserProcess) {
                        log("T=" + globalTime + " : Procesul " + evicted.getId() + " a rămas fără Time Slice.");
                        ((Scheduler)scheduler).addProcess(evicted);
                    } else if (evicted instanceof SystemProcess) {
                        evicted.setState(ProcessState.READY);
                    }
                }
            }
        }

        // 5. Apelăm Planificatorul pentru a ocupa procesoarele devenite libere
        scheduler.schedule(processors, systemProcess, memoryManager, config.getTimeSlice());
    }
}