package simulator;

import simulator.io.InputParser;
import simulator.io.Logger;
import simulator.io.GanttChartGUI;
import simulator.core.SimulationEngine;

public class Main {
    public static void main(String[] args) {
        System.out.println("OS Simulator - Scheduling & Memory");

        // 1. Parse the file
        InputParser parser = new InputParser();
        parser.parseFile("C:\\Users\\anaun\\Desktop\\systems_quality\\Round-Robin\\RoundRobin\\input.txt");

        if (parser.getConfig() == null || parser.getProcesses() == null) {
            System.out.println("Error: Could not read data from input.txt!");
            return;
        }

        // 2. Initialize the I/O modules
        Logger textLogger = new Logger("output.txt");
        GanttChartGUI gui = new GanttChartGUI(parser.getConfig().getNumProcessors());

        // 3. Create the engine and add listeners (Observer Pattern)
        SimulationEngine engine = new SimulationEngine(parser.getConfig(), parser.getProcesses());
        engine.addEventListener(textLogger);
        engine.addEventListener(gui);

        // 4. Run the simulation
        engine.run();

        // 5. Stop the tools
        textLogger.close(); // Save the physical file to disk

        // 6. Display the graphical interface (Gantt Chart)
        System.out.println("\nLaunching graphical interface...");
        gui.showChart();
    }
}