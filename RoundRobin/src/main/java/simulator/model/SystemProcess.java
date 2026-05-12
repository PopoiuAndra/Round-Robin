package simulator.model;

/**
 * Represents the system process (the simulator's VIP).
 * Its main role is to execute system calls (I/O) requested by user processes.
 * Unlike normal processes, it has absolute priority and is automatically activated at periodic intervals.
 */
public class SystemProcess extends Process {

    /** The period (in ticks) at which the system process checks if it has work to do. */
    private final int releasePeriod;

    /** Internal counter that decreases at each tick to trigger the next wakeup. */
    private int ticksUntilNextRelease;

    // --- Manual Queue Implementation (no external libraries) ---
    /** The raw array holding references to processes waiting for I/O. */
    private final UserProcess[] ioQueue;
    /** The maximum capacity of the I/O queue. */
    private final int maxQueueCapacity = 1000;
    /** The current number of elements in the I/O queue. */
    private int queueSize = 0;
    /** The start index of the queue (from where processes are extracted). */
    private int head = 0;
    /** The end index of the queue (where processes are added). */
    private int tail = 0;

    /** Stores the user process that just finished an I/O operation at the current tick. */
    private UserProcess lastFinishedIoProcess = null;

    /**
     * Constructs the system process.
     *
     * @param id            The unique identifier (usually 0).
     * @param releasePeriod The activation period of the process.
     */
    public SystemProcess(int id, int releasePeriod) {
        super(id, 0); // The system process is always in RAM, memory requirement is 0
        this.releasePeriod = releasePeriod;
        this.ticksUntilNextRelease = releasePeriod;
        this.ioQueue = new UserProcess[maxQueueCapacity];
    }

    /**
     * Adds a user process to the waiting queue for system calls.
     * Uses a circular buffer queue implementation.
     *
     * @param process The user process that blocked for I/O.
     */
    public void requestSystemCall(UserProcess process) {
        if (queueSize < maxQueueCapacity) {
            ioQueue[tail] = process;
            tail = (tail + 1) % maxQueueCapacity;
            queueSize++;
        }
    }

    /**
     * Checks if the timer has expired and the system process needs to be awakened.
     *
     * @return true if it must be launched on the processor, false otherwise.
     */
    public boolean checkReleaseTime() {
        ticksUntilNextRelease--;
        if (ticksUntilNextRelease <= 0) {
            ticksUntilNextRelease = releasePeriod; // Reset the timer
            return true;
        }
        return false;
    }

    /**
     * Returns the process that just finished its I/O to be reintroduced
     * by the SimulationEngine into the scheduler's Ready Queue.
     *
     * @return The unblocked user process, or null if none finished.
     */
    public UserProcess getFinishedIoProcess() {
        return lastFinishedIoProcess;
    }

    /**
     * Executes one time unit (tick) for the process at the head of the I/O queue.
     * If the queue is empty, the system process enters the waiting state (WAITING_IO).
     *
     * @param currentTime The current global simulation time.
     */
    @Override
    public void executeTick(int currentTime) {
        lastFinishedIoProcess = null; // Reset at every tick

        if (queueSize > 0) {
            UserProcess currentIoProcess = ioQueue[head];
            currentIoProcess.executeTick(currentTime);

            // Check if it finished I/O or the entire sequence
            if (!currentIoProcess.isCurrentlyDoingIo() || currentIoProcess.getCurrentState() == ProcessState.TERMINATED) {
                lastFinishedIoProcess = currentIoProcess;

                // Remove the process from the I/O queue
                ioQueue[head] = null;
                head = (head + 1) % maxQueueCapacity;
                queueSize--;
            }
        } else {
            // If it has nothing to do, the VIP "goes to sleep" and releases its processor
            this.setState(ProcessState.WAITING_IO);
        }
    }

    /**
     * Returns the process currently at the counter (being processed for I/O) at this moment.
     */
    public UserProcess getCurrentlyProcessingIo() {
        if (queueSize > 0) {
            return ioQueue[head];
        }
        return null;
    }
}