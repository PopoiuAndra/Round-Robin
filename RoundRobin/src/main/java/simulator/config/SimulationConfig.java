package simulator.config;

/**
 * Clasa SimulationConfig stochează parametrii imuabili ai simulării.
 * Aceasta servește drept "Sursă de Adevăr" pentru toate componentele sistemului.
 */
public class SimulationConfig {
    private final int numProcessors;
    private final int totalRAM;
    private final int timeSlice;
    private final int systemProcessPeriod;
    private final double diskTransferRate;

    /**
     * Construiește o configurație completă pentru simulator.
     *
     * @param numProcessors       Numărul de procesoare disponibile (n > 0).
     * @param totalRAM            Cantitatea totală de memorie RAM disponibilă.
     * @param timeSlice           Cuanta de timp pentru Round Robin.
     * @param systemProcessPeriod Perioada la care se activează procesul de sistem.
     * @param diskTransferRate    Viteza de transfer HDD -> RAM (unități/tick).
     */
    public SimulationConfig(int numProcessors, int totalRAM, int timeSlice,
                            int systemProcessPeriod, double diskTransferRate) {
        this.numProcessors = numProcessors;
        this.totalRAM = totalRAM;
        this.timeSlice = timeSlice;
        this.systemProcessPeriod = systemProcessPeriod;
        this.diskTransferRate = diskTransferRate;
    }

    // Getters
    public int getNumProcessors() { return numProcessors; }
    public int getTotalRAM() { return totalRAM; }
    public int getTimeSlice() { return timeSlice; }
    public int getSystemProcessPeriod() { return systemProcessPeriod; }
    public double getDiskTransferRate() { return diskTransferRate; }

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