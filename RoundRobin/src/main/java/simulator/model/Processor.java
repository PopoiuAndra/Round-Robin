package simulator.model;

/**
 * Reprezinta o unitate fizica de procesare (CPU) din cadrul simulatorului.
 * Rolul sau este de a gazdui un proces, de a-l executa pas cu pas (tick cu tick)
 * si de a tine evidenta timpului ramas din cuanta alocata pentru algoritmul Round-Robin.
 */
public class Processor {

    /** Identificatorul unic al procesorului (ex: 0, 1, 2...). */
    private final int id;

    /** Referinta catre procesul care ruleaza in prezent pe acest procesor. Null daca este liber. */
    private Process currentProcess;

    /** Timpul ramas (in tick-uri) pana cand procesul curent va fi preemptionat. */
    private int timeSliceRemaining;

    /**
     * Construieste un procesor nou, initial liber.
     *
     * @param id Identificatorul procesorului.
     */
    public Processor(int id) {
        this.id = id;
        this.currentProcess = null;
        this.timeSliceRemaining = 0;
    }

    /**
     * Returneaza identificatorul procesorului.
     *
     * @return ID-ul procesorului.
     */
    public int getId() {
        return id;
    }

    /**
     * Verifica daca procesorul este liber (nu ruleaza niciun proces).
     *
     * @return true daca este disponibil, false daca este ocupat.
     */
    public boolean isIdle() {
        return currentProcess == null;
    }

    /**
     * Returneaza procesul care ruleaza in prezent pe acest procesor.
     *
     * @return Procesul curent sau null daca procesorul este liber.
     */
    public Process getCurrentProcess() {
        return currentProcess;
    }

    /**
     * Verifica daca timpul alocat procesului curent a expirat.
     * Aceasta este conditia de preemptiune pentru planificatorul Round-Robin.
     *
     * @return true daca time slice-ul a ajuns la 0, false altfel.
     */
    public boolean isTimeSliceExpired() {
        return timeSliceRemaining <= 0;
    }

    /**
     * Aloca un proces pe acest procesor si ii seteaza cuanta de timp.
     * Actualizeaza starea procesului si ii memoreaza afinitatea.
     *
     * @param process   Procesul care va fi executat.
     * @param timeSlice Cuanta maxima de timp permisa pentru o rulare continua.
     */
    public void assignProcess(Process process, int timeSlice) {
        this.currentProcess = process;
        this.timeSliceRemaining = timeSlice;

        if (process != null) {
            process.setState(ProcessState.RUNNING);
            process.setLastProcessorId(this.id);
            System.out.println("    [CPU " + this.id + "] a preluat Procesul " + process.getId());
        }
    }

    /**
     * Elimina procesul curent de pe procesor (ex: cand a expirat timpul sau a terminat).
     *
     * @return Procesul care a fost tocmai scos, pentru a putea fi repus in coada sau terminat.
     */
    public Process evictProcess() {
        Process evicted = this.currentProcess;
        this.currentProcess = null;
        this.timeSliceRemaining = 0;
        return evicted;
    }

    /**
     * Executa o unitate de timp din procesul curent, daca exista unul.
     * De asemenea, scade timpul ramas din cuanta alocata.
     *
     * @param currentTime Timpul global curent al simularii.
     */
    public void executeTick(int currentTime) {
        if (currentProcess != null) {
            currentProcess.executeTick(currentTime);
            timeSliceRemaining--;
        }
    }
}