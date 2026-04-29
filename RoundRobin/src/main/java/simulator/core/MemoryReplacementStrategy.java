package simulator.core;

import simulator.model.Process;

/**
 * Interfata care defineste contractul pentru algoritmul de inlocuire a paginilor in RAM.
 * In sistemul nostru, aceasta strategie este implementata prin algoritmul LRU (Least Recently Used).
 */
public interface MemoryReplacementStrategy {
    /**
     * Marcheaza un proces ca fiind recent utilizat (Most Recently Used).
     * @param p Procesul accesat.
     */
    void markAsRecentlyUsed(Process p);

    /**
     * Elimina din memorie procesul cel mai putin utilizat pentru a face loc.
     */
    void evictLeastRecentlyUsed(int globalTime, SimulationEngine engine);
}
