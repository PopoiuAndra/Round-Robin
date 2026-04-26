package simulator.core;

import simulator.model.Process;
import simulator.model.Processor;
import simulator.model.SystemProcess;
import simulator.model.ProcessState;

/**
 * Implementează algoritmul de planificare Round-Robin cu afinitate pentru procesor.
 * Gestionează o coadă manuală a proceselor aflate în starea READY.
 */
public class Scheduler implements SchedulingStrategy {

    /** Coada manuală circulară pentru procesele pregătite de execuție. */
    private final Process[] readyQueue;
    private final int capacity = 1000;
    private int head = 0;
    private int tail = 0;
    private int size = 0;

    public Scheduler() {
        this.readyQueue = new Process[capacity];
    }

    /**
     * Adaugă un proces în coada de așteptare (READY).
     *
     * @param p Procesul care trebuie adăugat.
     */
    public void addProcess(Process p) {
        if (size < capacity) {
            readyQueue[tail] = p;
            tail = (tail + 1) % capacity;
            size++;
            p.setState(ProcessState.READY);
        } else {
            System.out.println("Eroare CRITICĂ: Scheduler Ready Queue este plină!");
        }
    }

    /**
     * Extrage din coadă primul proces care a mai rulat pe procesorul specificat (Afinitate).
     * Dacă nu găsește niciunul cu afinitate, extrage pur și simplu primul proces din coadă.
     *
     * @param processorId ID-ul procesorului pentru care căutăm un proces.
     * @return Procesul ales sau null dacă coada e goală.
     */
    private Process extractNextProcess(int processorId) {
        if (size == 0) return null;

        // 1. Căutăm un proces cu afinitate pentru acest procesor
        int curr = head;
        for (int i = 0; i < size; i++) {
            if (readyQueue[curr].getLastProcessorId() == processorId) {
                return removeProcessAtIndex(curr, i);
            }
            curr = (curr + 1) % capacity;
        }

        // 2. Dacă nu am găsit afinitate, dăm primul proces din coadă (Round-Robin clasic)
        return removeProcessAtIndex(head, 0);
    }

    /**
     * Metodă utilitară pentru a scoate un element din mijlocul cozii circulare manuale.
     */
    private Process removeProcessAtIndex(int queueIndex, int stepsFromHead) {
        Process p = readyQueue[queueIndex];

        // Shiftăm elementele din spate pentru a umple "gaura"
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
     * Rulează logica de planificare. Asignează procese pe procesoarele libere.
     * Procesul de sistem are prioritate absolută dacă este în starea READY.
     *
     * @param processors  Vectorul de procesoare fizice.
     * @param sysProcess  Procesul de sistem (VIP).
     * @param memManager  Managerul de memorie (pentru a verifica dacă procesul e în RAM).
     * @param timeSlice   Cuanta de timp permisă pentru execuție.
     */
    public void schedule(Processor[] processors, SystemProcess sysProcess, MemoryManager memManager, int timeSlice) {
        for (int i = 0; i < processors.length; i++) {
            Processor cpu = processors[i];

            if (cpu.isIdle()) {
                // 1. Verificăm prioritatea maximă: System Process
                if (sysProcess.getCurrentState() == ProcessState.READY) {
                    cpu.assignProcess(sysProcess, timeSlice);
                    continue; // Trecem la următorul CPU
                }

                // 2. Dacă VIP-ul nu are nevoie, luăm din coada de User Processes (cu afinitate)
                Process nextP = extractNextProcess(cpu.getId());

                if (nextP != null) {
                    // 3. Verificăm Memoria Virtuală (RAM)
                    if (memManager.isProcessInRam(nextP)) {
                        cpu.assignProcess(nextP, timeSlice);
                        memManager.markAsRecentlyUsed(nextP);
                    } else {
                        // Nu e în RAM. Îl trimitem la Swap și nu îi dăm procesorul deocamdată
                        if (!memManager.isSwapping()) {
                            memManager.startLoadingProcessToRam(nextP);
                            nextP.setState(ProcessState.SWAPPING);
                            // Procesorul curent a ratat alocarea, dar va încerca din nou la tick-ul următor
                        } else {
                            // Dacă discul e ocupat cu altceva, îl punem înapoi în coadă la început
                            addProcess(nextP);
                        }
                    }
                }
            }
        }
    }
}