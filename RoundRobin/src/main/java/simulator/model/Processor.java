package simulator.model;

/**
 * Represents a physical processing unit (CPU) within the simulator.
 * Its role is to host a process, execute it step-by-step (tick by tick),
 * and keep track of the remaining time from the quantum allocated for the Round-Robin algorithm.
 */
public class Processor {

    /** The unique identifier of the processor (e.g., 0, 1, 2...). */
    private final int id;

    /** Reference to the process currently running on this processor. Null if it is idle. */
    private Process currentProcess;

    /** The remaining time (in ticks) until the current process will be preempted. */
    private int timeSliceRemaining;

    /**
     * Constructs a new processor, initially idle.
     *
     * @param id The processor's identifier.
     */
    public Processor(int id) {
        assert id >= 0 : "Processor ID must be non-negative";
        this.id = id;
        this.currentProcess = null;
        this.timeSliceRemaining = 0;
    }

    /**
     * Returns the processor's identifier.
     *
     * @return The processor ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Checks if the processor is idle (not running any process).
     *
     * @return true if it is available, false if it is occupied.
     */
    public boolean isIdle() {
        return currentProcess == null;
    }

    /**
     * Returns the process currently running on this processor.
     *
     * @return The current process, or null if the processor is idle.
     */
    public Process getCurrentProcess() {
        return currentProcess;
    }

    /**
     * Checks if the time allocated to the current process has expired.
     * This is the preemption condition for the Round-Robin scheduler.
     *
     * @return true if the time slice has reached 0, false otherwise.
     */
    public boolean isTimeSliceExpired() {
        return timeSliceRemaining <= 0;
    }

    /**
     * Assigns a process to this processor and sets its time quantum.
     * Updates the process state and records its affinity.
     *
     * @param process   The process to be executed.
     * @param timeSlice The maximum time quantum allowed for continuous execution.
     */
    public void assignProcess(Process process, int timeSlice) {
        assert process != null : "Cannot assign a null process to CPU";
        assert timeSlice > 0 : "Time slice quantum must be greater than 0";
        assert isIdle() : "Cannot assign process; CPU is currently occupied";

        this.currentProcess = process;
        this.timeSliceRemaining = timeSlice;

        if (process != null) {
            process.setState(ProcessState.RUNNING);
            process.setLastProcessorId(this.id);
            System.out.println("    [CPU " + this.id + "] picked up Process " + process.getId());
        }

        assert this.currentProcess.getCurrentState() == ProcessState.RUNNING : "Assigned process must be in RUNNING state";
        assert this.currentProcess.getLastProcessorId() == this.id : "Process affinity ID must match this processor ID";
    }

    /**
     * Evicts the current process from the processor (e.g., when time has expired or it terminated).
     *
     * @return The process that was just removed, so it can be put back in the queue or terminated.
     */
    public Process evictProcess() {
        assert !isIdle() : "Cannot evict from an idle processor";

        Process evicted = this.currentProcess;
        this.currentProcess = null;
        this.timeSliceRemaining = 0;

        assert isIdle() : "Processor must be idle after eviction";
        assert timeSliceRemaining == 0 : "Time slice remaining must be reset to 0";

        return evicted;
    }

    /**
     * Executes one time unit of the current process, if one exists.
     * It also decreases the remaining time from the allocated quantum.
     *
     * @param currentTime The current global simulation time.
     */
    public void executeTick(int currentTime) {
        if (currentProcess != null) {
            int previousSlice = timeSliceRemaining;

            currentProcess.executeTick(currentTime);
            timeSliceRemaining--;

            assert timeSliceRemaining == previousSlice - 1 : "Time slice must decrement by exactly 1";
        }
    }
}