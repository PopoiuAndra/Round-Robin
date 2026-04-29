package simulator.core;

import simulator.model.Process;

/**
 * Gestioneaza memoria RAM a sistemului si implementeaza manual politica
 * de inlocuire a paginilor LRU (Least Recently Used) fara utilizarea librariilor Java.
 * Tot aici se tine evidenta timpului necesar pentru a aduce un proces de pe disc in RAM.
 */
public class MemoryManager implements MemoryReplacementStrategy {

    /** Cantitatea totala de memorie RAM disponibila in sistem. */
    private final int totalRam;

    /** Cantitatea de memorie RAM ocupata in prezent. */
    private int usedRam;

    /** Rata de transfer intre Disk si RAM (unitati de memorie per tick). */
    private final double diskTransferRate;

    // --- Implementare Manuala LRU ---
    /** Vectorul care retine procesele aflate in prezent in RAM. */
    private final Process[] ramProcesses;

    /** Numarul de procese aflate momentan in RAM. */
    private int ramProcessCount;

    // --- Gestionarea transferului (Swapping) ---
    /** Procesul care este transferat de pe disc in RAM in acest moment. */
    private Process swappingProcess = null;

    /** Timpul (in tick-uri) ramas pana la finalizarea transferului procesului curent. */
    private int swapTicksRemaining = 0;

    /**
     * Construieste managerul de memorie.
     *
     * @param totalRam         Memoria maxima fizica.
     * @param diskTransferRate Viteza de copiere memorie/tick.
     */
    public MemoryManager(int totalRam, double diskTransferRate) {
        this.totalRam = totalRam;
        this.diskTransferRate = diskTransferRate;
        this.usedRam = 0;
        this.ramProcessCount = 0;
        this.ramProcesses = new Process[1000]; // Presupunem un maxim de 1000 procese simultane in RAM
    }

    /**
     * Verifica daca un proces este deja incarcat complet in memoria RAM.
     *
     * @param p Procesul cautat.
     * @return true daca este in RAM, false altfel.
     */
    public boolean isProcessInRam(Process p) {
        for (int i = 0; i < ramProcessCount; i++) {
            if (ramProcesses[i].getId() == p.getId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Marcheaza un proces ca fiind "Recent Utilizat" (Most Recently Used).
     * Acest lucru il muta la finalul vectorului, ferindu-l de a fi scos pe disc (LRU).
     * Aceasta metoda trebuie apelata de Scheduler ori de cate ori procesul primeste procesor.
     *
     * @param p Procesul care tocmai a fost accesat/rulat.
     */
    public void markAsRecentlyUsed(Process p) {
        int index = -1;
        // 1. Gasim procesul in vector
        for (int i = 0; i < ramProcessCount; i++) {
            if (ramProcesses[i].getId() == p.getId()) {
                index = i;
                break;
            }
        }

        // 2. Daca l-am gasit si nu e deja ultimul, il mutam la final
        if (index != -1 && index < ramProcessCount - 1) {
            Process temp = ramProcesses[index];
            // Shiftam toate elementele din dreapta lui cu o pozitie la stanga
            for (int i = index; i < ramProcessCount - 1; i++) {
                ramProcesses[i] = ramProcesses[i + 1];
            }
            // Punem procesul accesat pe ultima pozitie
            ramProcesses[ramProcessCount - 1] = temp;
        }
    }

    public void startLoadingProcessToRam(Process p, int globalTime, SimulationEngine engine) {
        while (usedRam + p.getRequiredMemory() > totalRam) {
            evictLeastRecentlyUsed(globalTime, engine);
        }
        this.swappingProcess = p;
        this.swapTicksRemaining = (int) Math.ceil(p.getRequiredMemory() / diskTransferRate);
        if (this.swapTicksRemaining <= 0) {
            this.swapTicksRemaining = 1;
        }
        engine.logEvent(globalTime, "MEMORY", "Incepe transferul de pe Disk in RAM (Swap-In) pentru Procesul " + p.getId() + " (Dureaza " + swapTicksRemaining + " tick-uri).");
    }

    @Override
    public void evictLeastRecentlyUsed(int globalTime, SimulationEngine engine) {
        if (ramProcessCount == 0) return;

        Process victim = ramProcesses[0];
        usedRam -= victim.getRequiredMemory();

        for (int i = 0; i < ramProcessCount - 1; i++) {
            ramProcesses[i] = ramProcesses[i + 1];
        }

        ramProcesses[ramProcessCount - 1] = null;
        ramProcessCount--;

        engine.logEvent(globalTime, "MEMORY", "RAM Plin! Procesul " + victim.getId() + " a fost scos pe Disk (Evacuare LRU).");
    }
    /**
     * Executa o unitate de timp din transferul de pe disc.
     * Daca transferul se termina, procesul este adaugat fizic in RAM.
     *
     * @return Procesul care tocmai a terminat de incarcat, sau null daca inca se incarca/nu se incarca nimic.
     */
    public Process executeSwapTick() {
        if (swappingProcess != null) {
            swapTicksRemaining--;
            if (swapTicksRemaining <= 0) {
                // Transfer finalizat
                addProcessToRam(swappingProcess);
                Process finishedProcess = swappingProcess;

                swappingProcess = null;
                swapTicksRemaining = 0;

                return finishedProcess;
            }
        }
        return null;
    }

    /**
     * Verifica daca exista un transfer Disk -> RAM in curs de desfasurare.
     */
    public boolean isSwapping() {
        return swappingProcess != null;
    }

    // --- Metode Private Ajutatoare ---

    /**
     * Adauga un proces in RAM, presupunand ca exista deja spatiu.
     */
    private void addProcessToRam(Process p) {
        ramProcesses[ramProcessCount] = p;
        ramProcessCount++;
        usedRam += p.getRequiredMemory();
    }

    /** Returneaza procesul care este momentan citit de pe Disk. */
    public simulator.model.Process getSwappingProcess() {
        return swappingProcess;
    }
}