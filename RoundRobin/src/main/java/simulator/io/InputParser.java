package simulator.io;

import simulator.config.SimulationConfig;
import simulator.model.UserProcess;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Class responsible for reading and decoding the input file.
 * Adheres to the restriction of not using library functions for string processing
 * (such as String.split or complex Scanners), extracting numbers character by character.
 */
public class InputParser {

    /** The object that will store the global settings read from the first line. */
    private SimulationConfig config;

    /** The array in which all read user processes will be stored. */
    private UserProcess[] processes;

    /** The total number of processes successfully read. */
    private int processCount = 0;

    /**
     * Reads the file line by line, initializing the configuration and the list of processes.
     *
     * @param filePath The path to the input text file (e.g., "input.txt").
     */
    public void parseFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            // 1. Read the first line for the global configuration
            String configLine = reader.readLine();
            int[] configParams = parseLineToIntArray(configLine);

            // Create the configuration object based on the 5 expected parameters
            this.config = new SimulationConfig(
                    configParams[0], // numProcessors
                    configParams[1], // totalRAM
                    configParams[2], // timeSlice
                    configParams[3], // systemProcessPeriod
                    configParams[4]  // diskTransferRate (forced as int per previous decision)
            );

            // 2. Read the remaining lines for processes
            UserProcess[] tempProcesses = new UserProcess[1000];
            String line;
            int currentId = 1; // User process IDs start from 1 (0 is reserved for system)

            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.length() == 0) continue;

                int[] processData = parseLineToIntArray(line);

                int releaseTime = processData[0];
                int memory = processData[1];

                // The remaining numbers represent the execution sequence
                int sequenceLength = processData.length - 2;
                int[] sequence = new int[sequenceLength];
                for (int i = 0; i < sequenceLength; i++) {
                    sequence[i] = processData[i + 2];
                }

                // Create and store the process
                tempProcesses[processCount] = new UserProcess(currentId, memory, releaseTime, sequence);
                processCount++;
                currentId++;
            }

            // 3. Copy processes into a final, exactly-sized array
            processes = new UserProcess[processCount];
            for (int i = 0; i < processCount; i++) {
                processes[i] = tempProcesses[i];
            }

        } catch (IOException e) {
            System.out.println("Error reading the input file: " + e.getMessage());
        }
    }

    /**
     * Manually extracts all numbers from a character string.
     * Does not use any Java utility functions for text splitting.
     *
     * @param line The current text line.
     * @return An array containing all integer numbers found on the line.
     */
    private int[] parseLineToIntArray(String line) {
        int count = 0;
        boolean inNumber = false;

        // Step A: Count how many numbers are on the line to allocate the array
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c != ' ' && c != '\t') {
                if (!inNumber) {
                    count++;
                    inNumber = true;
                }
            } else {
                inNumber = false;
            }
        }

        int[] numbers = new int[count];
        int numIndex = 0;
        int currentNumber = 0;
        inNumber = false;

        // Step B: Build the numbers mathematically (digit by digit)
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c != ' ' && c != '\t') {
                // If we encounter a dot (e.g., 5.0), ignore the decimal part
                if (c == '.') {
                    inNumber = false;
                    continue;
                }
                if (c >= '0' && c <= '9') {
                    currentNumber = currentNumber * 10 + (c - '0');
                    inNumber = true;
                }
            } else {
                if (inNumber) {
                    numbers[numIndex] = currentNumber;
                    numIndex++;
                    currentNumber = 0;
                    inNumber = false;
                }
            }
        }

        // Save the last number if the row ended abruptly
        if (inNumber && numIndex < count) {
            numbers[numIndex] = currentNumber;
        }

        return numbers;
    }

    /**
     * @return The global configuration read from the file.
     */
    public SimulationConfig getConfig() {
        return config;
    }

    /**
     * @return The array of initialized user processes.
     */
    public UserProcess[] getProcesses() {
        return processes;
    }
}