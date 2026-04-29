package simulator.core;

import simulator.model.*;
import simulator.model.Process;

/**
 * Implementeaza algoritmul de planificare Round-Robin cu afinitate pentru procesor.
 * Gestioneaza o coada manuala a proceselor aflate in starea READY.
 */
public class Scheduler implements SchedulingStrategy {

    /** Coada manuala circulara pentru procesele pregatite de executie. */
    private final Process[] readyQueue;
    private final int capacity = 1000;
    private int head = 0;
    private int tail = 0;
    private int size = 0;

    public Scheduler() {
        this.readyQueue = new Process[capacity];
    }

    /**
     * Adauga un proces in coada de asteptare (READY).
     *
     * @param p Procesul care trebuie adaugat.
     */
    public void addProcess(Process p) {
        if (size < capacity) {
            readyQueue[tail] = p;
            tail = (tail + 1) % capacity;
            size++;
            p.setState(ProcessState.READY);
        } else {
            System.out.println("Eroare CRITICA: Scheduler Ready Queue este plina!");
        }
    }

    /**
     * Extrage din coada primul proces care a mai rulat pe procesorul specificat (Afinitate).
     * Daca nu gaseste niciunul cu afinitate, extrage pur si simplu primul proces din coada.
     *
     * @param processorId ID-ul procesorului pentru care cautam un proces.
     * @return Procesul ales sau null daca coada e goala.
     */
    private Process extractNextProcess(int processorId) {
        if (size == 0) return null;

        // 1. Cautam un proces cu afinitate pentru acest procesor
        int curr = head;
        for (int i = 0; i < size; i++) {
            if (readyQueue[curr].getLastProcessorId() == processorId) {
                return removeProcessAtIndex(curr, i);
            }
            curr = (curr + 1) % capacity;
        }

        // 2. Daca nu am gasit afinitate, dam primul proces din coada (Round-Robin clasic)
        return removeProcessAtIndex(head, 0);
    }

    /**
     * Metoda utilitara pentru a scoate un element din mijlocul cozii circulare manuale.
     */
    private Process removeProcessAtIndex(int queueIndex, int stepsFromHead) {
        Process p = readyQueue[queueIndex];

        // Shiftam elementele din spate pentru a umple "gaura"
        int curr = queueIndex;
        for (int i = stepsFromHead; i < size - 1; i++) {
            int next = (curr + 1) % capacity;
            readyQueue[curr] = readyQueue[next];
            curr = next;
        }

        tail = (tail - 1 + capacity) % capacity;
        readyQueue[tail] = null;
        size--;
        return p;
    }

    /**
     * Ruleaza logica de planificare. Asigneaza procese pe procesoarele libere.
     * Procesul de sistem are prioritate absoluta daca este in starea READY.
     *
     * @param processors  Vectorul de procesoare fizice.
     * @param sysProcess  Procesul de sistem (VIP).
     * @param memManager  Managerul de memorie (pentru a verifica daca procesul e in RAM).
     * @param timeSlice   Cuanta de timp permisa pentru executie.
     */
    public void schedule(Processor[] processors, SystemProcess sysProcess, MemoryManager memManager, int timeSlice, int globalTime, SimulationEngine engine) {
        for (int i = 0; i < processors.length; i++) {
            Processor cpu = processors[i];

            if (cpu.isIdle()) {
                if (sysProcess.getCurrentState() == ProcessState.READY) {
                    cpu.assignProcess(sysProcess, timeSlice);

                    // Verificam pentru cine face I/O ca sa punem in log
                    UserProcess targetIo = sysProcess.getCurrentlyProcessingIo();
                    String targetStr = (targetIo != null) ? " pentru a rezolva I/O la Procesul " + targetIo.getId() : "";
                    engine.logEvent(globalTime, "SCHEDULER", "VIP-ul a preluat CPU " + cpu.getId() + targetStr + ".");
                    continue;
                }

                Process nextP = extractNextProcess(cpu.getId());

                if (nextP != null) {
                    if (memManager.isProcessInRam(nextP)) {
                        cpu.assignProcess(nextP, timeSlice);
                        memManager.markAsRecentlyUsed(nextP);

                        boolean hadAffinity = (nextP.getLastProcessorId() == cpu.getId());
                        engine.logEvent(globalTime, "SCHEDULER", "Procesul " + nextP.getId() + " a preluat CPU " + cpu.getId() + (hadAffinity ? " (Afinitate respectata)" : "") + ".");
                    } else {
                        if (!memManager.isSwapping()) {
                            engine.logEvent(globalTime, "SCHEDULER", "Procesul " + nextP.getId() + " nu este in RAM. Se solicita SWAP-IN.");
                            memManager.startLoadingProcessToRam(nextP, globalTime, engine);
                            nextP.setState(ProcessState.SWAPPING);
                        } else {
                            addProcess(nextP); // Disk ocupat
                        }
                    }
                }
            }
        }
    }
    /**
     * Adauga un proces direct in fata cozii, dandu-i prioritate maxima.
     * Util pentru procesele care abia au fost aduse in RAM (pentru a evita thrashing-ul).
     */
    public void addProcessToFront(Process p) {
        if (size < capacity) {
            // Mutam head-ul cu o pozitie la stanga (circular)
            head = (head - 1 + capacity) % capacity;
            readyQueue[head] = p;
            size++;
            p.setState(ProcessState.READY);
        } else {
            System.out.println("Eroare CRITICA: Scheduler Ready Queue este plina!");
        }
    }
}