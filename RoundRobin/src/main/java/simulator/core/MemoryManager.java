package simulator.core;

import simulator.model.Process;

/**
 * Manages the system RAM memory and manually implements the
 * LRU (Least Recently Used) page replacement policy
 * without using Java libraries.
 * It also keeps track of the time required to load a process
 * from disk into RAM.
 */
public class MemoryManager implements MemoryReplacementStrategy {

    /** Total amount of RAM available in the system. */
    private final int totalRam;

    /** Amount of RAM currently in use. */
    private int usedRam;

    /** Transfer rate between Disk and RAM (memory units per tick). */
    private final double diskTransferRate;

    // --- Manual LRU Implementation ---

    /** Array storing the processes currently loaded in RAM. */
    private final Process[] ramProcesses;

    /** Number of processes currently in RAM. */
    private int ramProcessCount;

    // --- Swapping Management ---

    /** The process currently being transferred from disk into RAM. */
    private Process swappingProcess = null;

    /** Remaining time (in ticks) until the current transfer finishes. */
    private int swapTicksRemaining = 0;

    /**
     * Constructs the memory manager.
     *
     * @param totalRam         Maximum physical memory.
     * @param diskTransferRate Memory copy speed per tick.
     */
    public MemoryManager(int totalRam, double diskTransferRate) {
        this.totalRam = totalRam;
        this.diskTransferRate = diskTransferRate;
        this.usedRam = 0;
        this.ramProcessCount = 0;

        // Assume a maximum of 1000 simultaneous processes in RAM
        this.ramProcesses = new Process[1000];
    }

    /**
     * Checks whether a process is already fully loaded into RAM.
     *
     * @param p The process being searched.
     * @return true if the process is in RAM, false otherwise.
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
     * Marks a process as "Most Recently Used".
     * This moves it to the end of the array,
     * protecting it from being swapped out (LRU policy).
     * This method should be called by the Scheduler
     * whenever the process receives CPU time.
     *
     * @param p The process that was just accessed/executed.
     */
    public void markAsRecentlyUsed(Process p) {

        int index = -1;

        // 1. Find the process in the array
        for (int i = 0; i < ramProcessCount; i++) {
            if (ramProcesses[i].getId() == p.getId()) {
                index = i;
                break;
            }
        }

        // 2. If found and not already the last element, move it to the end
        if (index != -1 && index < ramProcessCount - 1) {

            Process temp = ramProcesses[index];

            // Shift all elements on the right one position to the left
            for (int i = index; i < ramProcessCount - 1; i++) {
                ramProcesses[i] = ramProcesses[i + 1];
            }

            // Place the accessed process at the last position
            ramProcesses[ramProcessCount - 1] = temp;
        }
    }

    /**
     * Starts loading a process from Disk into RAM.
     * If RAM is full, LRU eviction is performed first.
     */
    public void startLoadingProcessToRam(Process p,
                                         int globalTime,
                                         SimulationEngine engine) {

        while (usedRam + p.getRequiredMemory() > totalRam) {
            evictLeastRecentlyUsed(globalTime, engine);
        }

        this.swappingProcess = p;

        this.swapTicksRemaining =
                (int) Math.ceil(p.getRequiredMemory() / diskTransferRate);

        if (this.swapTicksRemaining <= 0) {
            this.swapTicksRemaining = 1;
        }

        engine.logEvent(
                globalTime,
                "MEMORY",
                "Disk -> RAM transfer (Swap-In) started for Process "
                        + p.getId()
                        + " (Takes "
                        + swapTicksRemaining
                        + " ticks)."
        );
    }

    @Override
    public void evictLeastRecentlyUsed(int globalTime,
                                       SimulationEngine engine) {

        if (ramProcessCount == 0) return;

        Process victim = ramProcesses[0];

        usedRam -= victim.getRequiredMemory();

        // Shift remaining processes left
        for (int i = 0; i < ramProcessCount - 1; i++) {
            ramProcesses[i] = ramProcesses[i + 1];
        }

        ramProcesses[ramProcessCount - 1] = null;
        ramProcessCount--;

        engine.logEvent(
                globalTime,
                "MEMORY",
                "RAM Full! Process "
                        + victim.getId()
                        + " was moved to Disk (LRU Eviction)."
        );
    }

    /**
     * Executes one unit of time for the disk transfer.
     * If the transfer finishes, the process is physically added to RAM.
     *
     * @return The process that has just finished loading,
     *         or null if loading is still in progress / nothing is loading.
     */
    public Process executeSwapTick() {

        if (swappingProcess != null) {

            swapTicksRemaining--;

            if (swapTicksRemaining <= 0) {

                // Transfer completed
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
     * Checks whether a Disk -> RAM transfer is currently in progress.
     */
    public boolean isSwapping() {
        return swappingProcess != null;
    }

    // --- Private Helper Methods ---

    /**
     * Adds a process to RAM, assuming enough space already exists.
     */
    private void addProcessToRam(Process p) {
        ramProcesses[ramProcessCount] = p;
        ramProcessCount++;

        usedRam += p.getRequiredMemory();
    }

    /**
     * Returns the process currently being loaded from Disk.
     */
    public simulator.model.Process getSwappingProcess() {
        return swappingProcess;
    }
}