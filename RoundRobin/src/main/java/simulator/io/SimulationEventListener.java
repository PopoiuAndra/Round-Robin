package simulator.io;

/**
 * Interface used to decouple the simulation engine from the display systems.
 * Any class that implements this interface will be notified of changes within the system.
 */
public interface SimulationEventListener {
    /**
     * Called when a text message needs to be recorded (e.g., "T=5: Process launched").
     *
     * @param message The text of the event.
     */
    void onLogMessage(String message);

    /**
     * Called at each tick where a processor runs a process,
     * useful for building the Gantt chart.
     *
     * @param cpuId     The ID of the processor.
     * @param processId The ID of the running process (0 for system).
     * @param tick      The current moment in time.
     */
    void onCpuExecution(int cpuId, int processId, int tick);

    /**
     * Called at each tick where the disk is performing a transfer.
     *
     * @param processId The ID of the process being transferred.
     * @param tick      The current moment in time.
     */
    void onDiskExecution(int processId, int tick);
}