package simulator.model;

/**
 * Abstract class defining the structure and base state of a process in the system.
 * Serves as the foundation for both user processes and the system process.
 * Manages information independent of the process type, such as the identifier,
 * memory requirements, and its current state in the lifecycle.
 */
public abstract class Process {

    /** * Unique identifier of the process (PID).
     */
    protected final int id;

    /** * The amount of RAM required for the process to be loaded from virtual memory (DISK).
     */
    protected final int requiredMemory;

    /** * The current state of the process according to the simulator's state machine.
     */
    protected ProcessState currentState;

    /** * The ID of the last processor the process ran on.
     * The initial value is -1, indicating the process hasn't been scheduled on any processor yet.
     * This attribute is essential for implementing processor affinity logic.
     */
    protected int lastProcessorId = -1;

    /**
     * Constructs a new process and initializes it in the NEW state.
     *
     * @param id             The unique identifier of the process.
     * @param requiredMemory The memory required for execution.
     */
    public Process(int id, int requiredMemory) {
        this.id = id;
        this.requiredMemory = requiredMemory;
        this.currentState = ProcessState.NEW;
    }

    /**
     * Returns the unique identifier of the process.
     *
     * @return The process ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the memory requirement of the process.
     *
     * @return Required memory in memory units.
     */
    public int getRequiredMemory() {
        return requiredMemory;
    }

    /**
     * Returns the current state of the process.
     *
     * @return Current state (e.g., READY, RUNNING, WAITING_IO).
     */
    public ProcessState getCurrentState() {
        return currentState;
    }

    /**
     * Updates the process state in the system.
     *
     * @param state The new state of the process.
     */
    public void setState(ProcessState state) {
        this.currentState = state;
    }

    /**
     * Returns the ID of the last processor the process was executed on.
     *
     * @return The processor ID or -1 if it has not yet run.
     */
    public int getLastProcessorId() {
        return lastProcessorId;
    }

    /**
     * Sets the ID of the processor the process is currently running on.
     *
     * @param lastProcessorId The allocated processor ID.
     */
    public void setLastProcessorId(int lastProcessorId) {
        this.lastProcessorId = lastProcessorId;
    }

    /**
     * Abstract method called at every time unit (tick) by the processor
     * where the process is scheduled. Each type of process (User or System)
     * must implement its own time consumption logic.
     *
     * @param currentTime The current global simulation time.
     */
    public abstract void executeTick(int currentTime);
}

