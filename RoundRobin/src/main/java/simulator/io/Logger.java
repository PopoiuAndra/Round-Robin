package simulator.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Ascultă evenimentele motorului și scrie mesajele text într-un fișier fizic (output.txt).
 */
public class Logger implements SimulationEventListener {
    private PrintWriter writer;

    public Logger(String outputFilePath) {
        try {
            writer = new PrintWriter(new FileWriter(outputFilePath, false));
            writer.println("=== JURNAL EXECUTIE SIMULATOR ===");
        } catch (IOException e) {
            System.out.println("Eroare Logger: " + e.getMessage());
        }
    }

    @Override
    public void onLogMessage(String message) {
        if (writer != null) {
            writer.println(message);
            writer.flush(); // Ne asigurăm că datele sunt scrise imediat
        }
        System.out.println(message); // Printăm și în consolă
    }

    @Override
    public void onCpuExecution(int cpuId, int processId, int tick) {
        // Logger-ul text nu are nevoie de fiecare tick separat pentru CPU,
        // așa că lăsăm această metodă goală pentru el.
    }

    public void close() {
        if (writer != null) {
            writer.println("=== SIMULARE FINALIZATA ===");
            writer.close();
        }
    }

    @Override
    public void onDiskExecution(int processId, int tick) {
    }
}