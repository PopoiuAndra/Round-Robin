package simulator.core;

import simulator.model.*;
import simulator.model.Process;

/**
 * Implements the Round-Robin scheduling algorithm with processor affinity.
 * Manages a manual queue of processes in the READY state.
 */
public class Scheduler implements SchedulingStrategy {

    /** Manual circular queue for processes ready for execution. */
    private final Process[] readyQueue;
    private final int capacity = 1000;
    private int head = 0;
    private int tail = 0;
    private int size = 0;

    public Scheduler() {
        this.readyQueue = new Process[capacity];
    }

    /**
     * Adds a process to the waiting queue (READY).
     *
     * @param p The process to be added.
     */
    public void addProcess(Process p) {
        if (size < capacity) {
            readyQueue[tail] = p;
            tail = (tail + 1) % capacity;
            size++;
            p.setState(ProcessState.READY);
        } else {
            System.out.println("CRITICAL ERROR: Scheduler Ready Queue is full!");
        }
    }

    /**
     * Extracts from the queue the first process that previously ran on the specified processor (Affinity).
     * If none with affinity is found, simply extracts the first process in the queue.
     *
     * @param processorId The ID of the processor for which we are searching a process.
     * @return The selected process or null if the queue is empty.
     */
    private Process extractNextProcess(int processorId) {
        if (size == 0) return null;

        // 1. Search for a process with affinity for this processor
        int curr = head;
        for (int i = 0; i < size; i++) {
            if (readyQueue[curr].getLastProcessorId() == processorId) {
                return removeProcessAtIndex(curr, i);
            }
            curr = (curr + 1) % capacity;
        }

        // 2. If no affinity was found, return the first process in the queue (classic Round-Robin)
        return removeProcessAtIndex(head, 0);
    }

    /**
     * Utility method for removing an element from the middle of the manual circular queue.
     */
    private Process removeProcessAtIndex(int queueIndex, int stepsFromHead) {
        Process p = readyQueue[queueIndex];

        // Shift elements to fill the gap
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
     * Executes the scheduling logic. Assigns processes to free processors.
     * The system process has absolute priority if it is in the READY state.
     *
     * @param processors  Array of physical processors.
     * @param sysProcess  The system process (VIP).
     * @param memManager  Memory manager (used to check if the process is in RAM).
     * @param timeSlice   Allowed execution time quantum.
     */
    public void schedule(Processor[] processors, SystemProcess sysProcess,
                         MemoryManager memManager, int timeSlice,
                         int globalTime, SimulationEngine engine) {

        for (int i = 0; i < processors.length; i++) {
            Processor cpu = processors[i];

            if (cpu.isIdle()) {

                if (sysProcess.getCurrentState() == ProcessState.READY) {
                    cpu.assignProcess(sysProcess, timeSlice);

                    // Check for whom the I/O is being handled to log it
                    UserProcess targetIo = sysProcess.getCurrentlyProcessingIo();

                    String targetStr = (targetIo != null)
                            ? " to resolve I/O for Process " + targetIo.getId()
                            : "";

                    engine.logEvent(globalTime, "SCHEDULER",
                            "VIP process took CPU " + cpu.getId() + targetStr + ".");

                    continue;
                }

                Process nextP = extractNextProcess(cpu.getId());

                if (nextP != null) {

                    if (memManager.isProcessInRam(nextP)) {
                        boolean hadAffinity =
                                (nextP.getLastProcessorId() == cpu.getId());

                        cpu.assignProcess(nextP, timeSlice);
                        memManager.markAsRecentlyUsed(nextP);

                        engine.logEvent(globalTime, "SCHEDULER",
                                "Process " + nextP.getId() +
                                        " took CPU " + cpu.getId() +
                                        (hadAffinity ? " (Affinity respected)" : "") +
                                        ".");

                    } else {

                        if (!memManager.isSwapping()) {

                            engine.logEvent(globalTime, "SCHEDULER",
                                    "Process " + nextP.getId() +
                                            " is not in RAM. SWAP-IN requested.");

                            memManager.startLoadingProcessToRam(
                                    nextP, globalTime, engine);

                            nextP.setState(ProcessState.SWAPPING);

                        } else {
                            addProcess(nextP); // Disk busy
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds a process directly to the front of the queue, giving it maximum priority.
     * Useful for processes that were just loaded into RAM (to avoid thrashing).
     */
    public void addProcessToFront(Process p) {
        if (size < capacity) {

            // Move head one position to the left (circular)
            head = (head - 1 + capacity) % capacity;

            readyQueue[head] = p;
            size++;

            p.setState(ProcessState.READY);

        } else {
            System.out.println("CRITICAL ERROR: Scheduler Ready Queue is full!");
        }
    }
}