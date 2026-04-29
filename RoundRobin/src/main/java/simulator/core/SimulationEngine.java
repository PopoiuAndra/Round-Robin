package simulator.core;

import simulator.config.SimulationConfig;
import simulator.model.*;
import simulator.model.Process;
import simulator.io.SimulationEventListener;

/**
 * Motorul principal al simularii (The Clock).
 * Controleaza scurgerea timpului (tick-urilor) si orchestreaza interactiunile
 * dintre planificator, memoria virtuala, procesoare si procesul de sistem.
 * Implementeaza un sablon de tip Observer (prin SimulationEventListener) pentru
 * a decupla logica interna de sistemele de afisare (fisier text, interfata grafica).
 */
public class SimulationEngine {

    /** Configuratia globala citita din fisierul de intrare. */
    private final SimulationConfig config;

    /** Vectorul care simuleaza unitatile fizice de procesare (CPU). */
    private final Processor[] processors;

    /** Lista completa a proceselor de utilizator care trebuie simulate. */
    private final UserProcess[] allUserProcesses;

    /** Procesul cu prioritate maxima responsabil de apelurile de sistem (I/O). */
    private final SystemProcess systemProcess;

    /** Strategia de planificare utilizata (in acest caz, Round-Robin cu afinitate). */
    private final SchedulingStrategy scheduler;

    /** Componenta responsabila de managementul memoriei RAM si a politicii LRU. */
    private final MemoryManager memoryManager;

    // --- Observer Pattern ---
    /** Vectorul intern de ascultatori pentru evenimentele motorului. */
    private final SimulationEventListener[] listeners = new SimulationEventListener[10];

    /** Numarul curent de ascultatori inregistrati. */
    private int listenerCount = 0;

    /** Timpul global al simularii, masurat in unitati abstracte (tick-uri). */
    private int globalTime = 0;

    /** Numarul de procese de utilizator care si-au incheiat complet executia. */
    private int completedProcesses = 0;

    /**
     * Construieste si initializeaza motorul de simulare.
     *
     * @param config    Configuratia parametrilor sistemului.
     * @param processes Vectorul de procese citite si pregatite de lansare.
     */
    public SimulationEngine(SimulationConfig config, UserProcess[] processes) {
        this.config = config;
        this.allUserProcesses = processes;

        // Initializare procesoare
        this.processors = new Processor[config.getNumProcessors()];
        for (int i = 0; i < config.getNumProcessors(); i++) {
            this.processors[i] = new Processor(i);
        }

        // Initializare componente cheie
        this.systemProcess = new SystemProcess(0, config.getSystemProcessPeriod());
        this.scheduler = new Scheduler();
        this.memoryManager = new MemoryManager(config.getTotalRAM(), config.getDiskTransferRate());
    }

    /**
     * Inregistreaza un nou ascultator pentru evenimentele generate de simulator.
     *
     * @param listener Obiectul care implementeaza interfata SimulationEventListener.
     */
    public void addEventListener(SimulationEventListener listener) {
        if (listenerCount < listeners.length) {
            listeners[listenerCount++] = listener;
        }
    }

    /**
     * Trimite un mesaj text catre toti ascultatorii inregistrati.
     *
     * @param message Textul evenimentului.
     */
    private void log(String message) {
        for (int i = 0; i < listenerCount; i++) {
            listeners[i].onLogMessage(message);
        }
    }

    /**
     * Notifica ascultatorii grafici despre activitatea unui procesor la tick-ul curent.
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
     * Notifica ascultatorii grafici despre activitatea hard discului
     * (transfer in memoria virtuala) la tick-ul curent.
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
     * Punctul de pornire al simularii. Ruleaza o bucla continua de timp
     * pana cand toate procesele de utilizator ajung in starea TERMINATED.
     * Include o siguranta (Time-out) la T=20000 pentru a preveni blocajele infinite.
     */
    public void run() {
        log("=== Start Simulare ===");

        while (completedProcesses < allUserProcesses.length) {
            executeTick();
            globalTime++;

            // Siguranta anti-bucla infinita
            if (globalTime > 20000) {
                log("Simularea a fost oprita fortat (Time-out) la T=20000.");
                break;
            }
        }

        log("=== Simulare Finalizata la T=" + globalTime + " ===");
    }

    /**
     * Metoda centrala apelata la fiecare unitate de timp.
     * Verifica lansarile noi, transferurile din memorie, executa un pas
     * din procesele aflate pe procesoare, verifica conditiile de preemptiune
     * si apeleaza planificatorul.
     */
    private void executeTick() {
        // 1. Verificam lansari noi
        for (int i = 0; i < allUserProcesses.length; i++) {
            if (allUserProcesses[i].getReleaseTime() == globalTime && allUserProcesses[i].getCurrentState() == ProcessState.NEW) {
                logEvent(globalTime, "ENGINE", "Procesul " + allUserProcesses[i].getId() + " a fost lansat in sistem.");
                ((Scheduler)scheduler).addProcess(allUserProcesses[i]);
            }
        }

        // 2. Gestionam Memoria Virtuala (Transfer Disk -> RAM)
        if (memoryManager.isSwapping()) {
            notifyDiskUsage(memoryManager.getSwappingProcess().getId(), globalTime - 1);
        }

        Process swappedInProcess = memoryManager.executeSwapTick();
        if (swappedInProcess != null) {
            logEvent(globalTime, "MEMORY", "Procesul " + swappedInProcess.getId() + " a intrat complet in RAM.");
            // REPARATIA ANTI-THRASHING: Intra direct in fata cozii!
            //((Scheduler)scheduler).addProcessToFront(swappedInProcess);
            ((Scheduler)scheduler).addProcess(swappedInProcess);
        }

        // 3. Verificam daca Procesul de Sistem (VIP) se trezeste
        if (systemProcess.checkReleaseTime()) {
            if (systemProcess.getCurrentState() != ProcessState.RUNNING) {
                logEvent(globalTime, "VIP", "System Process (0) s-a trezit. Trece in starea READY.");
                systemProcess.setState(ProcessState.READY);
            }
        }

        // 4. Avansam timpul pe procesoare
        for (int i = 0; i < processors.length; i++) {
            Processor cpu = processors[i];

            if (!cpu.isIdle()) {
                notifyCpuUsage(cpu.getId(), cpu.getCurrentProcess().getId(), globalTime - 1);
                cpu.executeTick(globalTime);
                Process p = cpu.getCurrentProcess();

                // 4.A. Logica VIP
                if (p instanceof SystemProcess) {
                    UserProcess finishedUserP = ((SystemProcess) p).getFinishedIoProcess();
                    if (finishedUserP != null) {
                        if (finishedUserP.getCurrentState() == ProcessState.TERMINATED) {
                            completedProcesses++;
                            logEvent(globalTime, "VIP", "A terminat dosarul I/O. Procesul " + finishedUserP.getId() + " a fost TERMINAT definitiv.");
                        } else {
                            ((Scheduler)scheduler).addProcess(finishedUserP);
                            logEvent(globalTime, "VIP", "A terminat I/O pentru Procesul " + finishedUserP.getId() + ". Il trimite inapoi la Scheduler (READY).");
                        }
                    }
                    if (p.getCurrentState() == ProcessState.WAITING_IO) {
                        logEvent(globalTime, "VIP", "A terminat toate cererile I/O. Adoarme si elibereaza CPU " + cpu.getId() + ".");
                        cpu.evictProcess();
                    }
                }
                // 4.B. Terminarea proceselor normale
                else if (p.getCurrentState() == ProcessState.TERMINATED) {
                    cpu.evictProcess();
                    completedProcesses++;
                    logEvent(globalTime, "CPU", "Procesul " + p.getId() + " si-a incheiat complet executia. Elibereaza CPU " + cpu.getId() + ".");
                }
                // 4.C. Blocarea pentru I/O
                else if (p instanceof UserProcess && ((UserProcess) p).isCurrentlyDoingIo()) {
                    logEvent(globalTime, "CPU", "Procesul " + p.getId() + " se blocheaza pentru System Call (I/O). Elibereaza CPU " + cpu.getId() + ".");
                    p.setState(ProcessState.WAITING_IO);
                    systemProcess.requestSystemCall((UserProcess) cpu.evictProcess());
                }
                // 4.D. Preemptiunea (Expirare Time Slice)
                else if (cpu.isTimeSliceExpired()) {
                    Process evicted = cpu.evictProcess();
                    if (evicted instanceof UserProcess) {
                        logEvent(globalTime, "CPU", "Procesul " + evicted.getId() + " a ramas fara Time Slice. Trece in READY.");
                        ((Scheduler)scheduler).addProcess(evicted);
                    } else if (evicted instanceof SystemProcess) {
                        evicted.setState(ProcessState.READY);
                        logEvent(globalTime, "VIP", "System Process a ramas fara Time Slice. Trece in READY.");
                    }
                }
            }
        }

        // 5. Apelam Planificatorul
        scheduler.schedule(processors, systemProcess, memoryManager, config.getTimeSlice(), globalTime, this);
    }
    /**
     * Metoda centralizata pentru jurnalizare.
     */
    public void logEvent(int time, String category, String message) {
        String logStr = "[T=" + time + "] [" + category + "] " + message;
        for (int i = 0; i < listenerCount; i++) {
            listeners[i].onLogMessage(logStr);
        }
    }
}