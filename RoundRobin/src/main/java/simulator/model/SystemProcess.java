package simulator.model;

/**
 * Reprezinta procesul de sistem (VIP-ul simulatorului).
 * Rolul sau principal este de a executa apelurile de sistem (I/O) solicitate
 * de procesele de utilizator. Spre deosebire de procesele normale, acesta
 * are prioritate absoluta si este activat automat la intervale periodice.
 */
public class SystemProcess extends Process {

    /** Perioada (in tick-uri) la care procesul de sistem verifica daca are de lucru. */
    private final int releasePeriod;

    /** Contor intern care scade la fiecare tick pentru a declansa urmatoarea trezire. */
    private int ticksUntilNextRelease;

    // --- Implementare manuala de Coada (fara librarii) ---
    /** Vectorul brut care tine referintele catre procesele ce asteapta I/O. */
    private final UserProcess[] ioQueue;
    /** Capacitatea maxima a cozii de I/O. */
    private final int maxQueueCapacity = 1000;
    /** Numarul curent de elemente din coada de I/O. */
    private int queueSize = 0;
    /** Indexul de inceput al cozii (de unde extragem procese). */
    private int head = 0;
    /** Indexul de sfarsit al cozii (unde adaugam procese). */
    private int tail = 0;

    /** Memoreaza procesul utilizator care tocmai a finalizat o operatie I/O la tick-ul curent. */
    private UserProcess lastFinishedIoProcess = null;

    /**
     * Construieste procesul de sistem.
     *
     * @param id            Identificatorul unic (de obicei 0).
     * @param releasePeriod Perioada de activare a procesului.
     */
    public SystemProcess(int id, int releasePeriod) {
        super(id, 0); // Procesul de sistem este mereu in RAM, necesar de memorie 0
        this.releasePeriod = releasePeriod;
        this.ticksUntilNextRelease = releasePeriod;
        this.ioQueue = new UserProcess[maxQueueCapacity];
    }

    /**
     * Adauga un proces de utilizator in coada de asteptare pentru apeluri de sistem.
     * Foloseste o implementare de tip coada circulara (circular buffer).
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
     * Verifica daca timer-ul a expirat si procesul de sistem trebuie trezit.
     *
     * @return true daca trebuie lansat pe procesor, false altfel.
     */
    public boolean checkReleaseTime() {
        ticksUntilNextRelease--;
        if (ticksUntilNextRelease <= 0) {
            ticksUntilNextRelease = releasePeriod; // Resetam timer-ul
            return true;
        }
        return false;
    }

    /**
     * Returneaza procesul care tocmai a terminat I/O-ul pentru a fi reintrodus
     * de catre SimulationEngine in coada planificatorului (Ready Queue).
     *
     * @return Procesul de utilizator deblocat, sau null daca niciunul nu a terminat.
     */
    public UserProcess getFinishedIoProcess() {
        return lastFinishedIoProcess;
    }

    /**
     * Executa o unitate de timp (tick) pentru procesul aflat in capul cozii de I/O.
     * Daca coada este goala, procesul de sistem intra in starea de asteptare (WAITING_IO).
     *
     * @param currentTime Timpul global curent al simularii.
     */
    @Override
    public void executeTick(int currentTime) {
        lastFinishedIoProcess = null; // Resetam la fiecare tick

        if (queueSize > 0) {
            UserProcess currentIoProcess = ioQueue[head];
            currentIoProcess.executeTick(currentTime);

            // Verificam daca a terminat I/O sau secventa completa
            if (!currentIoProcess.isCurrentlyDoingIo() || currentIoProcess.getCurrentState() == ProcessState.TERMINATED) {
                lastFinishedIoProcess = currentIoProcess;

                // Scoatem procesul din coada de I/O
                ioQueue[head] = null;
                head = (head + 1) % maxQueueCapacity;
                queueSize--;
            }
        } else {
            // Daca nu are ce face, VIP-ul "se culca" si isi elibereaza procesorul
            this.setState(ProcessState.WAITING_IO);
        }
    }

    /**
     * Returneaza procesul aflat la ghiseu in acest moment.
     */
    public UserProcess getCurrentlyProcessingIo() {
        if (queueSize > 0) {
            return ioQueue[head];
        }
        return null;
    }
}