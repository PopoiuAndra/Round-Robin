package simulator.model;

/**
 * Clasa abstractă care definește structura și starea de bază a unui proces din sistem.
 * Servește ca fundație atât pentru procesele de utilizator, cât și pentru procesul de sistem.
 * Gestionează informațiile independente de tipul procesului, cum ar fi identificatorul,
 * cerințele de memorie și starea curentă a acestuia în ciclul de viață.
 */
public abstract class Process {

    /** * Identificatorul unic al procesului (PID).
     */
    protected final int id;

    /** * Cantitatea de memorie RAM necesară pentru ca procesul să poată fi încărcat din memoria virtuală.
     */
    protected final int requiredMemory;

    /** * Starea curentă a procesului conform mașinii de stări a simulatorului.
     */
    protected ProcessState currentState;

    /** * ID-ul ultimului procesor pe care a rulat procesul.
     * Valoarea inițială este -1, indicând că procesul nu a fost încă programat pe niciun procesor.
     * Acest atribut este esențial pentru implementarea logicii de afinitate a procesorului.
     */
    protected int lastProcessorId = -1;

    /**
     * Construiește un proces nou și îl inițializează în starea NEW.
     *
     * @param id             Identificatorul unic al procesului.
     * @param requiredMemory Memoria necesară pentru execuție.
     */
    public Process(int id, int requiredMemory) {
        this.id = id;
        this.requiredMemory = requiredMemory;
        this.currentState = ProcessState.NEW;
    }

    /**
     * Returnează identificatorul unic al procesului.
     *
     * @return ID-ul procesului.
     */
    public int getId() {
        return id;
    }

    /**
     * Returnează necesarul de memorie al procesului.
     *
     * @return Memoria necesară în unități de memorie.
     */
    public int getRequiredMemory() {
        return requiredMemory;
    }

    /**
     * Returnează starea curentă a procesului.
     *
     * @return Starea curentă (ex. READY, RUNNING, WAITING_IO).
     */
    public ProcessState getCurrentState() {
        return currentState;
    }

    /**
     * Actualizează starea procesului în sistem.
     *
     * @param state Noua stare a procesului.
     */
    public void setState(ProcessState state) {
        this.currentState = state;
    }

    /**
     * Returnează ID-ul ultimului procesor pe care a fost executat procesul.
     *
     * @return ID-ul procesorului sau -1 dacă nu a rulat încă.
     */
    public int getLastProcessorId() {
        return lastProcessorId;
    }

    /**
     * Setează ID-ul procesorului pe care rulează procesul în prezent.
     *
     * @param lastProcessorId ID-ul procesorului alocat.
     */
    public void setLastProcessorId(int lastProcessorId) {
        this.lastProcessorId = lastProcessorId;
    }

    /**
     * Metodă abstractă apelată la fiecare unitate de timp (tick) de către procesorul
     * pe care este programat procesul. Fiecare tip de proces (User sau System)
     * trebuie să implementeze propria logică de consumare a timpului.
     *
     * @param currentTime Timpul global curent al simulării.
     */
    public abstract void executeTick(int currentTime);
}