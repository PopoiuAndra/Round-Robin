package simulator;

import simulator.io.InputParser;
import simulator.io.Logger;
import simulator.io.GanttChartGUI;
import simulator.core.SimulationEngine;

public class Main {
    public static void main(String[] args) {
        System.out.println("Simulator OS - Scheduling & Memory");

        // 1. Parsam fisierul
        InputParser parser = new InputParser();
        parser.parseFile("input.txt");

        if (parser.getConfig() == null || parser.getProcesses() == null) {
            System.out.println("Eroare: Nu s-au putut citi datele din input.txt!");
            return;
        }

        // 2. Initializam modulele de I/O
        Logger textLogger = new Logger("output.txt");
        GanttChartGUI gui = new GanttChartGUI(parser.getConfig().getNumProcessors());

        // 3. Cream motorul si adaugam ascultatorii (Observer Pattern)
        SimulationEngine engine = new SimulationEngine(parser.getConfig(), parser.getProcesses());
        engine.addEventListener(textLogger);
        engine.addEventListener(gui);

        // 4. Rulam simularea
        engine.run();

        // 5. Oprim uneltele
        textLogger.close(); // Salvam fisierul fizic pe disc

        // 6. Afisam interfata grafica (Gantt Chart)
        System.out.println("\nLansare interfata grafica...");
        gui.showChart();
    }
}