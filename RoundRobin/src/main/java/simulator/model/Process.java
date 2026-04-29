package simulator.model;

/**
 * Clasa abstracta care defineste structura si starea de baza a unui proces din sistem.
 * Serveste ca fundatie atat pentru procesele de utilizator, cat si pentru procesul de sistem.
 * Gestioneaza informatiile independente de tipul procesului, cum ar fi identificatorul,
 * cerintele de memorie si starea curenta a acestuia in ciclul de viata.
 */
public abstract class Process {

    /** * Identificatorul unic al procesului (PID).
     */
    protected final int id;

    /** * Cantitatea de memorie RAM necesara pentru ca procesul sa poata fi incarcat din memoria virtuala.
     */
    protected final int requiredMemory;

    /** * Starea curenta a procesului conform masinii de stari a simulatorului.
     */
    protected ProcessState currentState;

    /** * ID-ul ultimului procesor pe care a rulat procesul.
     * Valoarea initiala este -1, indicand ca procesul nu a fost inca programat pe niciun procesor.
     * Acest atribut este esential pentru implementarea logicii de afinitate a procesorului.
     */
    protected int lastProcessorId = -1;

    /**
     * Construieste un proces nou si il initializeaza in starea NEW.
     *
     * @param id             Identificatorul unic al procesului.
     * @param requiredMemory Memoria necesara pentru executie.
     */
    public Process(int id, int requiredMemory) {
        this.id = id;
        this.requiredMemory = requiredMemory;
        this.currentState = ProcessState.NEW;
    }

    /**
     * Returneaza identificatorul unic al procesului.
     *
     * @return ID-ul procesului.
     */
    public int getId() {
        return id;
    }

    /**
     * Returneaza necesarul de memorie al procesului.
     *
     * @return Memoria necesara in unitati de memorie.
     */
    public int getRequiredMemory() {
        return requiredMemory;
    }

    /**
     * Returneaza starea curenta a procesului.
     *
     * @return Starea curenta (ex. READY, RUNNING, WAITING_IO).
     */
    public ProcessState getCurrentState() {
        return currentState;
    }

    /**
     * Actualizeaza starea procesului in sistem.
     *
     * @param state Noua stare a procesului.
     */
    public void setState(ProcessState state) {
        this.currentState = state;
    }

    /**
     * Returneaza ID-ul ultimului procesor pe care a fost executat procesul.
     *
     * @return ID-ul procesorului sau -1 daca nu a rulat inca.
     */
    public int getLastProcessorId() {
        return lastProcessorId;
    }

    /**
     * Seteaza ID-ul procesorului pe care ruleaza procesul in prezent.
     *
     * @param lastProcessorId ID-ul procesorului alocat.
     */
    public void setLastProcessorId(int lastProcessorId) {
        this.lastProcessorId = lastProcessorId;
    }

    /**
     * Metoda abstracta apelata la fiecare unitate de timp (tick) de catre procesorul
     * pe care este programat procesul. Fiecare tip de proces (User sau System)
     * trebuie sa implementeze propria logica de consumare a timpului.
     *
     * @param currentTime Timpul global curent al simularii.
     */
    public abstract void executeTick(int currentTime);
}