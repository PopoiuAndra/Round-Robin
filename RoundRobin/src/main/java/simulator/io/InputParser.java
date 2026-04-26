package simulator.io;

import simulator.config.SimulationConfig;
import simulator.model.UserProcess;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Clasa responsabilă cu citirea și decodificarea fișierului de intrare.
 * Respectă restricția de a nu utiliza funcții de librărie pentru procesarea șirurilor
 * (precum String.split sau Scanner complex), extrăgând numerele caracter cu caracter.
 */
public class InputParser {

    /** Obiectul care va stoca setările globale citite de pe prima linie. */
    private SimulationConfig config;

    /** Vectorul în care vor fi stocate toate procesele de utilizator citite. */
    private UserProcess[] processes;

    /** Numărul total de procese citite cu succes. */
    private int processCount = 0;

    /**
     * Citește fișierul linie cu linie, inițializând configurația și lista de procese.
     *
     * @param filePath Calea către fișierul text de intrare (ex: "input.txt").
     */
    public void parseFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            // 1. Citim prima linie pentru configurarea globală
            String configLine = reader.readLine();
            int[] configParams = parseLineToIntArray(configLine);

            // Creăm obiectul de configurare pe baza celor 5 parametri așteptați
            this.config = new SimulationConfig(
                    configParams[0], // numProcessors
                    configParams[1], // totalRAM
                    configParams[2], // timeSlice
                    configParams[3], // systemProcessPeriod
                    configParams[4]  // diskTransferRate (forțat ca int conform deciziei anterioare)
            );

            // 2. Citim restul liniilor pentru procese
            // Pre-alocăm un vector mare temporar pentru a evita ArrayList
            UserProcess[] tempProcesses = new UserProcess[1000];
            String line;
            int currentId = 1; // ID-urile proceselor de utilizator încep de la 1 (0 e rezervat pt sistem)

            while ((line = reader.readLine()) != null) {
                // Sărim peste liniile goale
                if (line.length() == 0) continue;

                int[] processData = parseLineToIntArray(line);

                int releaseTime = processData[0];
                int memory = processData[1];

                // Restul numerelor reprezintă secvența de execuție
                int sequenceLength = processData.length - 2;
                int[] sequence = new int[sequenceLength];
                for (int i = 0; i < sequenceLength; i++) {
                    sequence[i] = processData[i + 2];
                }

                // Creăm și stocăm procesul
                tempProcesses[processCount] = new UserProcess(currentId, memory, releaseTime, sequence);
                processCount++;
                currentId++;
            }

            // 3. Copiem procesele într-un vector final, dimensionat exact
            processes = new UserProcess[processCount];
            for (int i = 0; i < processCount; i++) {
                processes[i] = tempProcesses[i];
            }

        } catch (IOException e) {
            System.out.println("Eroare la citirea fișierului de intrare: " + e.getMessage());
        }
    }

    /**
     * Extrage manual toate numerele dintr-un șir de caractere.
     * Nu folosește nicio funcție utilitară Java pentru spargerea textului.
     *
     * @param line Linia de text curentă.
     * @return Un vector cu toate numerele întregi găsite pe linie.
     */
    private int[] parseLineToIntArray(String line) {
        int count = 0;
        boolean inNumber = false;

        // Pasul A: Numărăm câte numere sunt pe linie pentru a aloca vectorul
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

        // Pasul B: Construim numerele matematic (cifră cu cifră)
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c != ' ' && c != '\t') {
                // Dacă întâlnim un punct (ex. 5.0), ignorăm partea zecimală
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

        // Salvăm ultimul număr dacă rândul s-a terminat brusc
        if (inNumber && numIndex < count) {
            numbers[numIndex] = currentNumber;
        }

        return numbers;
    }

    /**
     * @return Configurația globală citită din fișier.
     */
    public SimulationConfig getConfig() {
        return config;
    }

    /**
     * @return Vectorul cu procesele de utilizator inițializate.
     */
    public UserProcess[] getProcesses() {
        return processes;
    }
}