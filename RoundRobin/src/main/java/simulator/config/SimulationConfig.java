package simulator.config;

/**
 * The SimulationConfig class stores the immutable parameters of the simulation.
 * This serves as the "Source of Truth" for all system components.
 */
public class SimulationConfig {

    /** Number of processors available in the system. */
    private final int numProcessors;

    /** Total amount of available RAM (in memory units). */
    private final int totalRAM;

    /** The time slice allocated to each process in Round Robin. */
    private final int timeSlice;

    /** The time period at which the system process activates periodically. */
    private final int systemProcessPeriod;

    /** Data transfer rate from Disk to RAM (units/tick). */
    private final double diskTransferRate;

    /**
     * Constructs a complete configuration for the simulator.
     *
     * @param numProcessors       Number of available processors (n > 0).
     * @param totalRAM            Total amount of available RAM.
     * @param timeSlice           Time slice for Round Robin.
     * @param systemProcessPeriod Period at which the system process activates.
     * @param diskTransferRate    Transfer rate HDD -> RAM (units/tick).
     */
    public SimulationConfig(int numProcessors, int totalRAM, int timeSlice,
                            int systemProcessPeriod, double diskTransferRate) {
        this.numProcessors = numProcessors;
        this.totalRAM = totalRAM;
        this.timeSlice = timeSlice;
        this.systemProcessPeriod = systemProcessPeriod;
        this.diskTransferRate = diskTransferRate;
    }

    /**
     * Returns the number of processors configured in the system.
     * @return Number of CPU units.
     */
    public int getNumProcessors() {
        return numProcessors;
    }

    /**
     * Returns the total amount of available RAM.
     * @return Total RAM in specific units of measurement.
     */
    public int getTotalRAM() {
        return totalRAM;
    }

    /**
     * Returns the time slice used by the Round Robin scheduler.
     * @return Time slice value in ticks.
     */
    public int getTimeSlice() {
        return timeSlice;
    }

    /**
     * Returns the activation frequency of the system process (VIP).
     * @return Number of ticks between automatic activations.
     */
    public int getSystemProcessPeriod() {
        return systemProcessPeriod;
    }

    /**
     * Returns the speed at which data is transferred from disk to RAM.
     * @return Memory units transferred per tick.
     */
    public double getDiskTransferRate() {
        return diskTransferRate;
    }

    /**
     * Returns a text representation of the current configuration.
     * @return Character string containing all configuration parameters.
     */
    @Override
    public String toString() {
        return "SimulationConfig{" +
                "numProcessors=" + numProcessors +
                ", totalRAM=" + totalRAM +
                ", timeSlice=" + timeSlice +
                ", systemProcessPeriod=" + systemProcessPeriod +
                ", diskTransferRate=" + diskTransferRate +
                '}';
    }
}