package simulator.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import simulator.model.Process;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MemoryManager} covering loading, eviction (LRU),
 * swapping mechanics and resilience under edge conditions.
 */
class MemoryManagerTest {

    private SimulationEngine engineMock;

    @BeforeEach
    void setUp() {
        engineMock = mock(SimulationEngine.class);
    }

    private void fullyLoadProcess(MemoryManager memManager, Process p) {
        memManager.startLoadingProcessToRam(p, 1, engineMock);
        while (memManager.isSwapping()) {
            memManager.executeSwapTick();
        }
    }

    private Process createMockProcess(int id, int requiredMemory) {
        Process p = mock(Process.class);
        when(p.getId()).thenReturn(id);
        when(p.getRequiredMemory()).thenReturn(requiredMemory);
        return p;
    }

    @Test
    @DisplayName("Test initialization: Should start with empty RAM and no swapping")
    /**
     * Verifies initial MemoryManager state: empty RAM and no swapping.
     */
    void testInitialization_ShouldBeEmpty() {
        MemoryManager memManager = new MemoryManager(1000, 100);

        assertFalse(memManager.isSwapping(), "MemoryManager should not be swapping initially.");
        assertNull(memManager.getSwappingProcess(), "Swapping process should be null initially.");
        assertNull(memManager.executeSwapTick(), "Executing swap tick when idle should return null.");
    }

    @Test
    @DisplayName("Test isProcessInRam: Should correctly identify if a process is loaded")
    /**
     * Checks that {@code isProcessInRam} correctly reports loaded status
     * after loading a process into memory.
     */
    void testIsProcessInRam_ShouldReturnCorrectStatus() {
        MemoryManager memManager = new MemoryManager(1000, 100);
        Process p1 = createMockProcess(1, 200);
        Process p2 = createMockProcess(2, 200);

        assertFalse(memManager.isProcessInRam(p1), "Process should not be in RAM initially (empty RAM check).");

        fullyLoadProcess(memManager, p1);

        assertTrue(memManager.isProcessInRam(p1), "Process p1 should be found in RAM after loading.");
        assertFalse(memManager.isProcessInRam(p2), "Process p2 should not be in RAM as it was never loaded.");
    }

    @Test
    @DisplayName("Test LRU logic: markAsRecentlyUsed should shift elements correctly")
    /**
     * Exercises LRU behavior by marking processes as recently used and
     * validating eviction choices.
     */
    void testMarkAsRecentlyUsed_ShouldProtectProcessFromEviction() {
        MemoryManager memManager = new MemoryManager(1000, 100);

        Process p1 = createMockProcess(1, 300);
        Process p2 = createMockProcess(2, 300);
        Process p3 = createMockProcess(3, 300);
        Process p4 = createMockProcess(4, 300);

        fullyLoadProcess(memManager, p1);
        fullyLoadProcess(memManager, p2);
        fullyLoadProcess(memManager, p3);

        // P2(0), P3(1), P1(2)
        memManager.markAsRecentlyUsed(p1);

        // Edge case: Mark a process that is already at the end (should do nothing)
        memManager.markAsRecentlyUsed(p1);

        // Edge case: Mark a process that is not in RAM (should not crash)
        memManager.markAsRecentlyUsed(p4);

        fullyLoadProcess(memManager, p4);

        assertFalse(memManager.isProcessInRam(p2), "Process P2 should have been evicted (Least Recently Used).");
        assertTrue(memManager.isProcessInRam(p1), "Process P1 should still be in RAM because it was marked as MRU.");
        assertTrue(memManager.isProcessInRam(p3), "Process P3 should still be in RAM.");
        assertTrue(memManager.isProcessInRam(p4), "Process P4 should be in RAM.");
    }

    @Test
    @DisplayName("Test Swap-In calculation: Should calculate ticks correctly and handle 0 memory fallback")
    /**
     * Validates swap-in tick calculation and minimum tick fallback for
     * processes with zero memory requirement.
     */
    void testStartLoadingProcessToRam_TickCalculations() {
        MemoryManager memManager = new MemoryManager(1000, 100);

        // (250 / 100 = 2.5 -> ceil = 3 ticks)
        Process p1 = createMockProcess(1, 250);
        memManager.startLoadingProcessToRam(p1, 10, engineMock);

        assertTrue(memManager.isSwapping(), "Memory manager should be in swapping state.");
        assertEquals(p1, memManager.getSwappingProcess(), "Swapping process should be p1.");
        assertNull(memManager.executeSwapTick(), "Tick 1 - Should not finish yet.");
        assertNull(memManager.executeSwapTick(), "Tick 2 - Should not finish yet.");
        assertEquals(p1, memManager.executeSwapTick(), "Tick 3 - Should finish and return p1.");

        Process p2 = createMockProcess(2, 0);
        memManager.startLoadingProcessToRam(p2, 10, engineMock);

        assertEquals(p2, memManager.executeSwapTick(), "Process with 0 memory should take exactly 1 minimum tick to load.");
    }

    @Test
    @DisplayName("Test Eviction limits: evictLeastRecentlyUsed should safely handle empty RAM")
    /**
     * Ensures eviction logic handles empty RAM without throwing.
     */
    void testEvictLeastRecentlyUsed_WithEmptyRam_ShouldReturnSafely() {
        MemoryManager memManager = new MemoryManager(1000, 100);

        // Calling eviction when RAM is completely empty
        assertDoesNotThrow(() -> memManager.evictLeastRecentlyUsed(1, engineMock),
                "Evicting from an empty RAM should not throw exceptions.");
    }

    @Test
    @DisplayName("Test Eviction logic: Should continuously evict until enough space is freed")
    /**
     * Simulates multiple evictions to free space for a large process and
     * verifies eviction notifications are logged.
     */
    void testStartLoadingProcessToRam_MultipleEvictionsNeeded() {
        MemoryManager memManager = new MemoryManager(1000, 100);

        Process p1 = createMockProcess(1, 400);
        Process p2 = createMockProcess(2, 400);

        fullyLoadProcess(memManager, p1);
        fullyLoadProcess(memManager, p2);

        Process massiveProcess = createMockProcess(3, 900);

        fullyLoadProcess(memManager, massiveProcess);

        assertFalse(memManager.isProcessInRam(p1), "P1 should be evicted.");
        assertFalse(memManager.isProcessInRam(p2), "P2 should be evicted.");
        assertTrue(memManager.isProcessInRam(massiveProcess), "Massive process should be loaded.");

        verify(engineMock, times(2)).logEvent(anyInt(), eq("MEMORY"), contains("LRU Eviction"));
    }
}