package simulator.core;

import simulator.model.Processor;
import simulator.model.SystemProcess;

/**
 * Interfața care definește contractul pentru algoritmul de planificare a procesorului.
 * În sistemul nostru, această strategie este implementată de planificatorul Round-Robin cu afinitate.
 */
public interface SchedulingStrategy {
    /**
     * Planifică procesele disponibile pe procesoarele libere.
     *
     * @param processors  Vectorul de procesoare fizice.
     * @param sysProcess  Procesul de sistem (prioritate maximă).
     * @param memManager  Managerul de memorie virtuală.
     * @param timeSlice   Cuanta maximă de timp (Time Slice).
     */
    void schedule(Processor[] processors, SystemProcess sysProcess, MemoryManager memManager, int timeSlice);
}