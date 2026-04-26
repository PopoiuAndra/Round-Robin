package simulator.model;

/**
 * Reprezintă procesul de sistem (VIP-ul simulatorului).
 * Rolul său principal este de a executa apelurile de sistem (I/O) solicitate
 * de procesele de utilizator. Spre deosebire de procesele normale, acesta
 * are prioritate absolută și este activat automat la intervale periodice.
 */
public class SystemProcess extends Process {

    /** Perioada (în tick-uri) la care procesul de sistem verifică dacă are de lucru. */
    private final int releasePeriod;

    /** Contor intern care scade la fiecare tick pentru a declanșa următoarea trezire. */
    private int ticksUntilNextRelease;

    // --- Implementare manuală de Coadă (fără librării) ---
    /** Vectorul brut care ține referințele către procesele ce așteaptă I/O. */
    private final UserProcess[] ioQueue;
    /** Capacitatea maximă a cozii de I/O. */
    private final int maxQueueCapacity = 1000;
    /** Numărul curent de elemente din coada de I/O. */
    private int queueSize = 0;
    /** Indexul de început al cozii (de unde extragem procese). */
    private int head = 0;
    /** Indexul de sfârșit al cozii (unde adăugăm procese). */
    private int tail = 0;

    /** Memorează procesul utilizator care tocmai a finalizat o operație I/O la tick-ul curent. */
    private UserProcess lastFinishedIoProcess = null;

    /**
     * Construiește procesul de sistem.
     *
     * @param id            Identificatorul unic (de obicei 0).
     * @param releasePeriod Perioada de activare a procesului.
     */
    public SystemProcess(int id, int releasePeriod) {
        super(id, 0); // Procesul de sistem este mereu în RAM, necesar de memorie 0
        this.releasePeriod = releasePeriod;
        this.ticksUntilNextRelease = releasePeriod;
        this.ioQueue = new UserProcess[maxQueueCapacity];
    }

    /**
     * Adaugă un proces de utilizator în coada de așteptare pentru apeluri de sistem.
     * Folosește o implementare de tip coadă circulară (circular buffer).
     *
     * @param process Procesul de utilizator care s-a blocat pentru I/O.
     */
    public void requestSystemCall(UserProcess process) {
        if (queueSize < maxQueueCapacity) {
            ioQueue[tail] = process;
            tail = (tail + 1) % maxQueueCapacity;
            queueSize++;
        }
    }

    /**
     * Verifică dacă timer-ul a expirat și procesul de sistem trebuie trezit.
     *
     * @return true dacă trebuie lansat pe procesor, false altfel.
     */
    public boolean checkReleaseTime() {
        ticksUntilNextRelease--;
        if (ticksUntilNextRelease <= 0) {
            ticksUntilNextRelease = releasePeriod; // Resetăm timer-ul
            return true;
        }
        return false;
    }

    /**
     * Returnează procesul care tocmai a terminat I/O-ul pentru a fi reintrodus
     * de către SimulationEngine în coada planificatorului (Ready Queue).
     *
     * @return Procesul de utilizator deblocat, sau null dacă niciunul nu a terminat.
     */
    public UserProcess getFinishedIoProcess() {
        return lastFinishedIoProcess;
    }

    /**
     * Execută o unitate de timp (tick) pentru procesul aflat în capul cozii de I/O.
     * Dacă coada este goală, procesul de sistem intră în starea de așteptare (WAITING_IO).
     *
     * @param currentTime Timpul global curent al simulării.
     */
    @Override
    public void executeTick(int currentTime) {
        lastFinishedIoProcess = null; // Resetăm la fiecare tick

        if (queueSize > 0) {
            UserProcess currentIoProcess = ioQueue[head];
            currentIoProcess.executeTick(currentTime);

            // Verificăm dacă a terminat I/O sau secvența completă
            if (!currentIoProcess.isCurrentlyDoingIo() || currentIoProcess.getCurrentState() == ProcessState.TERMINATED) {
                lastFinishedIoProcess = currentIoProcess;

                // Scoatem procesul din coada de I/O
                ioQueue[head] = null;
                head = (head + 1) % maxQueueCapacity;
                queueSize--;
            }
        } else {
            // Dacă nu are ce face, VIP-ul "se culcă" și își eliberează procesorul
            this.setState(ProcessState.WAITING_IO);
        }
    }
}