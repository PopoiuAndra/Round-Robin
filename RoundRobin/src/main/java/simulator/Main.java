package simulator;

import simulator.io.InputParser;
import simulator.io.Logger;
import simulator.io.GanttChartGUI;
import simulator.core.SimulationEngine;

public class Main {
    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Simulator OS - Scheduling & Memory    ");
        System.out.println("=========================================\n");

        // 1. Parsăm fișierul
        InputParser parser = new InputParser();
        parser.parseFile("input.txt");

        if (parser.getConfig() == null || parser.getProcesses() == null) {
            System.out.println("Eroare: Nu s-au putut citi datele din input.txt!");
            return;
        }

        // 2. Inițializăm modulele de I/O
        Logger textLogger = new Logger("output.txt");
        GanttChartGUI gui = new GanttChartGUI(parser.getConfig().getNumProcessors());

        // 3. Creăm motorul și adăugăm ascultătorii (Observer Pattern)
        SimulationEngine engine = new SimulationEngine(parser.getConfig(), parser.getProcesses());
        engine.addEventListener(textLogger);
        engine.addEventListener(gui);

        // 4. Rulăm simularea
        engine.run();

        // 5. Oprim uneltele
        textLogger.close(); // Salvăm fișierul fizic pe disc

        // 6. Afișăm interfața grafică (Gantt Chart)
        System.out.println("\nLansare interfata grafica...");
        gui.showChart();
    }
}