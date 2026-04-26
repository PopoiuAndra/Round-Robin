package simulator.model;

/**
 * Reprezintă o unitate fizică de procesare (CPU) din cadrul simulatorului.
 * Rolul său este de a găzdui un proces, de a-l executa pas cu pas (tick cu tick)
 * și de a ține evidența timpului rămas din cuanta alocată pentru algoritmul Round-Robin.
 */
public class Processor {

    /** Identificatorul unic al procesorului (ex: 0, 1, 2...). */
    private final int id;

    /** Referință către procesul care rulează în prezent pe acest procesor. Null dacă este liber. */
    private Process currentProcess;

    /** Timpul rămas (în tick-uri) până când procesul curent va fi preempționat. */
    private int timeSliceRemaining;

    /**
     * Construiește un procesor nou, inițial liber.
     *
     * @param id Identificatorul procesorului.
     */
    public Processor(int id) {
        this.id = id;
        this.currentProcess = null;
        this.timeSliceRemaining = 0;
    }

    /**
     * Returnează identificatorul procesorului.
     *
     * @return ID-ul procesorului.
     */
    public int getId() {
        return id;
    }

    /**
     * Verifică dacă procesorul este liber (nu rulează niciun proces).
     *
     * @return true dacă este disponibil, false dacă este ocupat.
     */
    public boolean isIdle() {
        return currentProcess == null;
    }

    /**
     * Returnează procesul care rulează în prezent pe acest procesor.
     *
     * @return Procesul curent sau null dacă procesorul este liber.
     */
    public Process getCurrentProcess() {
        return currentProcess;
    }

    /**
     * Verifică dacă timpul alocat procesului curent a expirat.
     * Aceasta este condiția de preempțiune pentru planificatorul Round-Robin.
     *
     * @return true dacă time slice-ul a ajuns la 0, false altfel.
     */
    public boolean isTimeSliceExpired() {
        return timeSliceRemaining <= 0;
    }

    /**
     * Alocă un proces pe acest procesor și îi setează cuanta de timp.
     * Actualizează starea procesului și îi memorează afinitatea.
     *
     * @param process   Procesul care va fi executat.
     * @param timeSlice Cuanta maximă de timp permisă pentru o rulare continuă.
     */
    public void assignProcess(Process process, int timeSlice) {
        this.currentProcess = process;
        this.timeSliceRemaining = timeSlice;

        if (process != null) {
            process.setState(ProcessState.RUNNING);
            process.setLastProcessorId(this.id);
            // ADĂUGĂM ACEASTĂ LINIE PENTRU LOGGING:
            System.out.println("    [CPU " + this.id + "] a preluat Procesul " + process.getId());
        }
    }

    /**
     * Elimină procesul curent de pe procesor (ex: când a expirat timpul sau a terminat).
     *
     * @return Procesul care a fost tocmai scos, pentru a putea fi repus în coadă sau terminat.
     */
    public Process evictProcess() {
        Process evicted = this.currentProcess;
        this.currentProcess = null;
        this.timeSliceRemaining = 0;
        return evicted;
    }

    /**
     * Execută o unitate de timp din procesul curent, dacă există unul.
     * De asemenea, scade timpul rămas din cuanta alocată.
     *
     * @param currentTime Timpul global curent al simulării.
     */
    public void executeTick(int currentTime) {
        if (currentProcess != null) {
            currentProcess.executeTick(currentTime);
            timeSliceRemaining--;
        }
    }
}