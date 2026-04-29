package simulator.config;

/**
 * Clasa SimulationConfig stocheaza parametrii imuabili ai simularii.
 * Aceasta serveste drept "Sursa de Adevar" pentru toate componentele sistemului.
 */
public class SimulationConfig {

    /** Numarul de procesoare disponibile in sistem. */
    private final int numProcessors;

    /** Cantitatea totala de memorie RAM disponibila (in unitati de memorie). */
    private final int totalRAM;

    /** Cuanta de timp (Time Slice) alocata fiecarui proces in Round Robin. */
    private final int timeSlice;

    /** Perioada de timp la care procesul de sistem se activeaza periodic. */
    private final int systemProcessPeriod;

    /** Viteza de transfer a datelor de pe Disk in RAM (unitati/tick). */
    private final double diskTransferRate;

    /**
     * Construieste o configuratie completa pentru simulator.
     *
     * @param numProcessors       Numarul de procesoare disponibile (n > 0).
     * @param totalRAM            Cantitatea totala de memorie RAM disponibila.
     * @param timeSlice           Cuanta de timp pentru Round Robin.
     * @param systemProcessPeriod Perioada la care se activeaza procesul de sistem.
     * @param diskTransferRate    Viteza de transfer HDD -> RAM (unitati/tick).
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
     * Returneaza numarul de procesoare configurate in sistem.
     * * @return Numarul de unitati CPU.
     */
    public int getNumProcessors() {
        return numProcessors;
    }

    /**
     * Returneaza cantitatea totala de memorie RAM disponibila.
     * * @return Memoria RAM totala in unitati de masura specifice.
     */
    public int getTotalRAM() {
        return totalRAM;
    }

    /**
     * Returneaza cuanta de timp (time slice) folosita de planificatorul Round Robin.
     * * @return Valoarea time slice-ului in tick-uri.
     */
    public int getTimeSlice() {
        return timeSlice;
    }

    /**
     * Returneaza frecventa de activare a procesului de sistem (VIP).
     * * @return Numarul de tick-uri dintre activarile automate.
     */
    public int getSystemProcessPeriod() {
        return systemProcessPeriod;
    }

    /**
     * Returneaza viteza cu care datele sunt transferate de pe disk in RAM.
     * * @return Unitati de memorie transferate per tick.
     */
    public double getDiskTransferRate() {
        return diskTransferRate;
    }

    /**
     * Returneaza o reprezentare sub forma de text a configuratiei curente.
     * * @return Sir de caractere continand toti parametrii de configurare.
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