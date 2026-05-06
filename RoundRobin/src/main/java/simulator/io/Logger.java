package simulator.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Listens to engine events and writes text messages to a physical file (output.txt).
 */
public class Logger implements SimulationEventListener {
    private PrintWriter writer;

    public Logger(String outputFilePath) {
        try {
            writer = new PrintWriter(new FileWriter(outputFilePath, false));
            writer.println("=== SIMULATOR EXECUTION LOG ===");
        } catch (IOException e) {
            System.out.println("Logger Error: " + e.getMessage());
        }
    }

    @Override
    public void onLogMessage(String message) {
        if (writer != null) {
            writer.println(message);
            writer.flush(); // Ensure data is written immediately
        }
        System.out.println(message); // Also print to console
    }

    @Override
    public void onCpuExecution(int cpuId, int processId, int tick) {
        // The text logger does not need every separate tick for the CPU,
        // so we leave this method empty.
    }

    public void close() {
        if (writer != null) {
            writer.println("=== SIMULATION FINALIZED ===");
            writer.close();
        }
    }

    @Override
    public void onDiskExecution(int processId, int tick) {
    }
}