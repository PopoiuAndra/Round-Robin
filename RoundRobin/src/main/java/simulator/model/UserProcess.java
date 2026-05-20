package simulator.model;

/**
 * Represents a process launched by a user within the system.
 * Unlike a system process, a UserProcess is defined by a release time
 * and an alternating execution sequence consisting of processing intervals (CPU)
 * and system calls (I/O).
 */
public class UserProcess extends Process {

    /** The time from the start of the simulation at which the process is released into the system. */
    private final int releaseTime;

    /** * The sequence of execution intervals and system calls.
     * Even indices (0, 2, 4...) represent processor execution intervals (CPU bursts).
     * Odd indices (1, 3, 5...) represent times required for system calls (I/O bursts).
     */
    private final int[] executionSequence;

    /** Current index within the executionSequence array. */
    private int currentSequenceIndex = 0;

    /** Time (in ticks) remaining in the current processing interval or system call. */
    private int remainingTicksInCurrentBurst;

    /** Indicator showing whether the process is currently performing a system call (I/O) or normal processing. */
    private boolean isCurrentlyDoingIo = false;

    /**
     * Constructs a new user process based on data read from the file.
     *
     * @param id          Unique identifier of the process.
     * @param memory      RAM memory required (in MB).
     * @param releaseTime The time (tick) when the process appears in the system.
     * @param sequence    Array with the alternating duration sequence (CPU, IO, CPU...).
     */
    public UserProcess(int id, int memory, int releaseTime, int[] sequence) {
        super(id, memory);

        assert releaseTime >= 0 : "Release time must be non-negative";
        assert sequence != null : "Execution sequence array cannot be null";
        assert sequence.length > 0 : "Execution sequence must contain at least one burst";

        this.releaseTime = releaseTime;
        this.executionSequence = sequence;

        // Initialize remaining time with the first value in the sequence (if it exists)
        if (sequence != null && sequence.length > 0) {
            assert sequence[0] > 0 : "First burst duration must be greater than 0";

            this.remainingTicksInCurrentBurst = sequence[0];
        } else {
            this.remainingTicksInCurrentBurst = 0;
        }
    }

    /**
     * Returns the time at which the process is released (introduced) into the system.
     *
     * @return Release time.
     */
    public int getReleaseTime() {
        return releaseTime;
    }

    /**
     * Checks if the current stage of the process is a system call.
     * This information helps the Scheduler know if the process
     * needs to be removed from the processor and handled by the system process.
     *
     * @return true if the process is waiting/executing I/O, false if it requires CPU.
     */
    public boolean isCurrentlyDoingIo() {
        return isCurrentlyDoingIo;
    }

    /**
     * Executes one time unit (one tick) of the process's current interval.
     * The function manually updates the state and moves to the next interval in the array
     * when the current time reaches 0. If the array ends, the process transitions to TERMINATED.
     *
     * @param currentTime Current global simulation time.
     */
    @Override
    public void executeTick(int currentTime) {
        assert currentTime >= 0 : "Simulation time cannot be negative";

        // Save state for postcondition checking
        int previousTicks = remainingTicksInCurrentBurst;
        int previousIndex = currentSequenceIndex;

        // If we have already exceeded the sequence, the process is terminated
        if (executionSequence == null || currentSequenceIndex >= executionSequence.length) {
            if (this.currentState != ProcessState.TERMINATED) {
                this.setState(ProcessState.TERMINATED);
            }
            return;
        }

        // Subtract one tick from the current interval
        remainingTicksInCurrentBurst--;

        // Check if the current interval has reached the end
        if (remainingTicksInCurrentBurst <= 0) {
            currentSequenceIndex++;

            // If there are more elements in the sequence, move to the next interval
            if (currentSequenceIndex < executionSequence.length) {
                remainingTicksInCurrentBurst = executionSequence[currentSequenceIndex];

                // Alternate between CPU execution and system call (I/O)
                isCurrentlyDoingIo = !isCurrentlyDoingIo;

                assert (currentSequenceIndex % 2 != 0) == isCurrentlyDoingIo : "Sequence index parity must match the I/O state";
            } else {
                // No more elements, so the process has finished its execution
                this.setState(ProcessState.TERMINATED);
            }
        } else {
            assert remainingTicksInCurrentBurst == previousTicks - 1 : "Ticks should decrement by exactly 1";
            assert currentSequenceIndex == previousIndex : "Sequence index should remain unchanged during burst";
        }
    }

    /**
     * Returns a text representation of the process for debugging and logging.
     *
     * @return Formatted string with process details.
     */
    @Override
    public String toString() {
        return "UserProcess{id=" + id + ", releaseTime=" + releaseTime +
                ", requiredMemory=" + requiredMemory +
                ", currentState=" + currentState +
                ", lastProcessorId=" + lastProcessorId +
                ", sequenceLength=" + (executionSequence != null ? executionSequence.length : 0) + "}";
    }
}