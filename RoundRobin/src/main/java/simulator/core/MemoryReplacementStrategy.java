package simulator.core;

import simulator.model.Process;

/**
 * Interfața care definește contractul pentru algoritmul de înlocuire a paginilor în RAM.
 * În sistemul nostru, această strategie este implementată prin algoritmul LRU (Least Recently Used).
 */
public interface MemoryReplacementStrategy {
    /**
     * Marchează un proces ca fiind recent utilizat (Most Recently Used).
     * @param p Procesul accesat.
     */
    void markAsRecentlyUsed(Process p);

    /**
     * Elimină din memorie procesul cel mai puțin utilizat pentru a face loc.
     */
    void evictLeastRecentlyUsed();
}