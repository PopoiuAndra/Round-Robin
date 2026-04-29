package simulator.io;

import simulator.config.SimulationConfig;
import simulator.model.UserProcess;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Clasa responsabila cu citirea si decodificarea fisierului de intrare.
 * Respecta restrictia de a nu utiliza functii de librarie pentru procesarea sirurilor
 * (precum String.split sau Scanner complex), extragand numerele caracter cu caracter.
 */
public class InputParser {

    /** Obiectul care va stoca setarile globale citite de pe prima linie. */
    private SimulationConfig config;

    /** Vectorul in care vor fi stocate toate procesele de utilizator citite. */
    private UserProcess[] processes;

    /** Numarul total de procese citite cu succes. */
    private int processCount = 0;

    /**
     * Citeste fisierul linie cu linie, initializand configuratia si lista de procese.
     *
     * @param filePath Calea catre fisierul text de intrare (ex: "input.txt").
     */
    public void parseFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            // 1. Citim prima linie pentru configurarea globala
            String configLine = reader.readLine();
            int[] configParams = parseLineToIntArray(configLine);

            // Cream obiectul de configurare pe baza celor 5 parametri asteptati
            this.config = new SimulationConfig(
                    configParams[0], // numProcessors
                    configParams[1], // totalRAM
                    configParams[2], // timeSlice
                    configParams[3], // systemProcessPeriod
                    configParams[4]  // diskTransferRate (fortat ca int conform deciziei anterioare)
            );

            // 2. Citim restul liniilor pentru procese
            UserProcess[] tempProcesses = new UserProcess[1000];
            String line;
            int currentId = 1; // ID-urile proceselor de utilizator incep de la 1 (0 e rezervat pt sistem)

            while ((line = reader.readLine()) != null) {
                // Sarim peste liniile goale
                if (line.length() == 0) continue;

                int[] processData = parseLineToIntArray(line);

                int releaseTime = processData[0];
                int memory = processData[1];

                // Restul numerelor reprezinta secventa de executie
                int sequenceLength = processData.length - 2;
                int[] sequence = new int[sequenceLength];
                for (int i = 0; i < sequenceLength; i++) {
                    sequence[i] = processData[i + 2];
                }

                // Cream si stocam procesul
                tempProcesses[processCount] = new UserProcess(currentId, memory, releaseTime, sequence);
                processCount++;
                currentId++;
            }

            // 3. Copiem procesele intr-un vector final, dimensionat exact
            processes = new UserProcess[processCount];
            for (int i = 0; i < processCount; i++) {
                processes[i] = tempProcesses[i];
            }

        } catch (IOException e) {
            System.out.println("Eroare la citirea fisierului de intrare: " + e.getMessage());
        }
    }

    /**
     * Extrage manual toate numerele dintr-un sir de caractere.
     * Nu foloseste nicio functie utilitara Java pentru spargerea textului.
     *
     * @param line Linia de text curenta.
     * @return Un vector cu toate numerele intregi gasite pe linie.
     */
    private int[] parseLineToIntArray(String line) {
        int count = 0;
        boolean inNumber = false;

        // Pasul A: Numaram cate numere sunt pe linie pentru a aloca vectorul
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

        // Pasul B: Construim numerele matematic (cifra cu cifra)
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c != ' ' && c != '\t') {
                // Daca intalnim un punct (ex. 5.0), ignoram partea zecimala
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

        // Salvam ultimul numar daca randul s-a terminat brusc
        if (inNumber && numIndex < count) {
            numbers[numIndex] = currentNumber;
        }

        return numbers;
    }

    /**
     * @return Configuratia globala citita din fisier.
     */
    public SimulationConfig getConfig() {
        return config;
    }

    /**
     * @return Vectorul cu procesele de utilizator initializate.
     */
    public UserProcess[] getProcesses() {
        return processes;
    }
}