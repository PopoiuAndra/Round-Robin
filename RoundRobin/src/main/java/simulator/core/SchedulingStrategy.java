package simulator.core;

import simulator.model.Processor;
import simulator.model.SystemProcess;

/**
 * Interfata care defineste contractul pentru algoritmul de planificare a procesorului.
 * In sistemul nostru, aceasta strategie este implementata de planificatorul Round-Robin cu afinitate.
 */
public interface SchedulingStrategy {
    /**
     * Planifica procesele disponibile pe procesoarele libere.
     *
     * @param processors  Vectorul de procesoare fizice.
     * @param sysProcess  Procesul de sistem (prioritate maxima).
     * @param memManager  Managerul de memorie virtuala.
     * @param timeSlice   Cuanta maxima de timp (Time Slice).
     */
    void schedule(Processor[] processors, SystemProcess sysProcess, MemoryManager memManager, int timeSlice, int globalTime, SimulationEngine engine);
}
