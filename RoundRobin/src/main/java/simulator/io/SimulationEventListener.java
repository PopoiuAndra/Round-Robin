package simulator.io;

/**
 * Interfață utilizată pentru a decupla motorul de simulare de sistemele de afișare.
 * Orice clasă care implementează această interfață va fi notificată despre schimbările din sistem.
 */
public interface SimulationEventListener {
    /**
     * Apelat când un mesaj text trebuie înregistrat (ex. "T=5: Procesul lansat").
     */
    void onLogMessage(String message);

    /**
     * Apelat la fiecare tick în care un procesor rulează un proces,
     * util pentru a construi diagrama Gantt.
     *
     * @param cpuId     ID-ul procesorului.
     * @param processId ID-ul procesului rulat (0 pentru sistem).
     * @param tick      Momentul de timp curent.
     */
    void onCpuExecution(int cpuId, int processId, int tick);

    void onDiskExecution(int processId, int tick);
}