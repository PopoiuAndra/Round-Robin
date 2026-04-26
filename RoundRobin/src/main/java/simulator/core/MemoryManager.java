package simulator.core;

import simulator.model.Process;

/**
 * Gestionează memoria RAM a sistemului și implementează manual politica
 * de înlocuire a paginilor LRU (Least Recently Used) fără utilizarea librăriilor Java.
 * Tot aici se ține evidența timpului necesar pentru a aduce un proces de pe disc în RAM.
 */
public class MemoryManager implements MemoryReplacementStrategy {

    /** Cantitatea totală de memorie RAM disponibilă în sistem. */
    private final int totalRam;

    /** Cantitatea de memorie RAM ocupată în prezent. */
    private int usedRam;

    /** Rata de transfer între Disk și RAM (unități de memorie per tick). */
    private final double diskTransferRate;

    // --- Implementare Manuală LRU ---
    /** Vectorul care reține procesele aflate în prezent în RAM. */
    private final Process[] ramProcesses;

    /** Numărul de procese aflate momentan în RAM. */
    private int ramProcessCount;

    // --- Gestionarea transferului (Swapping) ---
    /** Procesul care este transferat de pe disc în RAM în acest moment. */
    private Process swappingProcess = null;

    /** Timpul (în tick-uri) rămas până la finalizarea transferului procesului curent. */
    private int swapTicksRemaining = 0;

    /**
     * Construiește managerul de memorie.
     *
     * @param totalRam         Memoria maximă fizică.
     * @param diskTransferRate Viteza de copiere memorie/tick.
     */
    public MemoryManager(int totalRam, double diskTransferRate) {
        this.totalRam = totalRam;
        this.diskTransferRate = diskTransferRate;
        this.usedRam = 0;
        this.ramProcessCount = 0;
        this.ramProcesses = new Process[1000]; // Presupunem un maxim de 1000 procese simultane în RAM
    }

    /**
     * Verifică dacă un proces este deja încărcat complet în memoria RAM.
     *
     * @param p Procesul căutat.
     * @return true dacă este în RAM, false altfel.
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
     * Marchează un proces ca fiind "Recent Utilizat" (Most Recently Used).
     * Acest lucru îl mută la finalul vectorului, ferindu-l de a fi scos pe disc (LRU).
     * Această metodă trebuie apelată de Scheduler ori de câte ori procesul primește procesor.
     *
     * @param p Procesul care tocmai a fost accesat/rulat.
     */
    public void markAsRecentlyUsed(Process p) {
        int index = -1;
        // 1. Găsim procesul în vector
        for (int i = 0; i < ramProcessCount; i++) {
            if (ramProcesses[i].getId() == p.getId()) {
                index = i;
                break;
            }
        }

        // 2. Dacă l-am găsit și nu e deja ultimul, îl mutăm la final
        if (index != -1 && index < ramProcessCount - 1) {
            Process temp = ramProcesses[index];
            // Shiftăm toate elementele din dreapta lui cu o poziție la stânga
            for (int i = index; i < ramProcessCount - 1; i++) {
                ramProcesses[i] = ramProcesses[i + 1];
            }
            // Punem procesul accesat pe ultima poziție
            ramProcesses[ramProcessCount - 1] = temp;
        }
    }

    /**
     * Inițiază procesul de aducere a unui proces de pe disc în RAM.
     * Dacă nu este suficient spațiu, elimină procese folosind LRU până face loc.
     * Calculează timpul necesar transferului pe baza ratei de transfer a discului.
     *
     * @param p Procesul care trebuie adus în RAM.
     */
    public void startLoadingProcessToRam(Process p) {
        // Facem loc în RAM dacă este necesar
        while (usedRam + p.getRequiredMemory() > totalRam) {
            evictLeastRecentlyUsed();
        }

        this.swappingProcess = p;
        // Calculăm timpul de transfer (rotunjit superior pentru a asigura un număr întreg de tick-uri)
        this.swapTicksRemaining = (int) Math.ceil(p.getRequiredMemory() / diskTransferRate);

        // Dacă e atât de rapid încât ia 0 tick-uri, îl forțăm la măcar 1 tick pentru simulare
        if (this.swapTicksRemaining <= 0) {
            this.swapTicksRemaining = 1;
        }
    }

    /**
     * Execută o unitate de timp din transferul de pe disc.
     * Dacă transferul se termină, procesul este adăugat fizic în RAM.
     *
     * @return Procesul care tocmai a terminat de încărcat, sau null dacă încă se încarcă/nu se încarcă nimic.
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
     * Verifică dacă există un transfer Disk -> RAM în curs de desfășurare.
     */
    public boolean isSwapping() {
        return swappingProcess != null;
    }

    // --- Metode Private Ajutătoare ---

    /**
     * Adaugă un proces în RAM, presupunând că există deja spațiu (MRU position).
     */
    private void addProcessToRam(Process p) {
        ramProcesses[ramProcessCount] = p;
        ramProcessCount++;
        usedRam += p.getRequiredMemory();
    }

    /**
     * Elimină procesul cel mai puțin utilizat recent (aflat pe poziția 0 în vector).
     * Aceasta simulează trimiterea lui înapoi pe Disk.
     */
    @Override
    public void evictLeastRecentlyUsed() {
        if (ramProcessCount == 0) return;

        Process victim = ramProcesses[0];
        usedRam -= victim.getRequiredMemory();

        // Shiftăm tot vectorul la stânga pentru a acoperi "gaura" lăsată de victimă
        for (int i = 0; i < ramProcessCount - 1; i++) {
            ramProcesses[i] = ramProcesses[i + 1];
        }

        // Curățăm ultima poziție
        ramProcesses[ramProcessCount - 1] = null;
        ramProcessCount--;

        System.out.println("[Memory] LRU Eviction: Procesul " + victim.getId() + " a fost scos din RAM.");
    }

    /** Returnează procesul care este momentan citit de pe Disk. */
    public simulator.model.Process getSwappingProcess() {
        return swappingProcess;
    }
}