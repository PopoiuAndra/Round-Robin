package simulator.model;

/**
 * Reprezintă un proces lansat de un utilizator în cadrul sistemului.
 * Spre deosebire de un proces de sistem, un UserProcess este definit de un timp
 * de lansare (release time) și o secvență de execuție alternantă formată din
 * intervale de procesare (CPU) și apeluri de sistem (I/O).
 */
public class UserProcess extends Process {

    /** Timpul de la începutul simulării la care procesul este eliberat în sistem. */
    private final int releaseTime;

    /** * Secvența de intervale de execuție și apeluri de sistem.
     * Indicii pari (0, 2, 4...) reprezintă intervale de execuție pe procesor (CPU bursts).
     * Indicii impari (1, 3, 5...) reprezintă timpii necesari pentru apelurile de sistem (I/O bursts).
     */
    private final int[] executionSequence;

    /** Indexul curent din vectorul executionSequence. */
    private int currentSequenceIndex = 0;

    /** Timpul (în tick-uri) rămas din intervalul curent de procesare sau apel de sistem. */
    private int remainingTicksInCurrentBurst;

    /** Indicator care arată dacă procesul execută în prezent un apel de sistem (I/O) sau procesare normală. */
    private boolean isCurrentlyDoingIo = false;

    /**
     * Construiește un nou proces de utilizator pe baza datelor citite din fișier.
     *
     * @param id          Identificatorul unic al procesului.
     * @param memory      Memoria RAM (în MB) necesară.
     * @param releaseTime Timpul (tick-ul) la care procesul apare în sistem.
     * @param sequence    Vectorul cu secvența alternantă de durate (CPU, IO, CPU...).
     */
    public UserProcess(int id, int memory, int releaseTime, int[] sequence) {
        super(id, memory);
        this.releaseTime = releaseTime;
        this.executionSequence = sequence;

        // Inițializăm timpul rămas cu prima valoare din secvență (dacă există)
        if (sequence != null && sequence.length > 0) {
            this.remainingTicksInCurrentBurst = sequence[0];
        } else {
            this.remainingTicksInCurrentBurst = 0;
        }
    }

    /**
     * Returnează timpul la care procesul este eliberat (introdus) în sistem.
     *
     * @return Momentul lansării (release time).
     */
    public int getReleaseTime() {
        return releaseTime;
    }

    /**
     * Verifică dacă stadiul curent al procesului este un apel de sistem.
     * Această informație ajută planificatorul (Scheduler) să știe dacă procesul
     * trebuie scos de pe procesor și preluat de procesul de sistem.
     *
     * @return true dacă procesul așteaptă/execută I/O, false dacă necesită CPU.
     */
    public boolean isCurrentlyDoingIo() {
        return isCurrentlyDoingIo;
    }

    /**
     * Execută o unitate de timp (un tick) din intervalul curent al procesului.
     * Funcția actualizează manual starea și trece la următorul interval din vector
     * atunci când timpul curent ajunge la 0. Dacă vectorul se termină, procesul trece în starea TERMINATED.
     *
     * @param currentTime Timpul global curent al simulării.
     */
    @Override
    public void executeTick(int currentTime) {
        // Dacă am depășit deja secvența, procesul este terminat
        if (currentSequenceIndex >= executionSequence.length) {
            if (this.currentState != ProcessState.TERMINATED) {
                this.setState(ProcessState.TERMINATED);
            }
            return;
        }

        // Scădem un tick din intervalul curent
        remainingTicksInCurrentBurst--;

        // Verificăm dacă intervalul curent a ajuns la final
        if (remainingTicksInCurrentBurst <= 0) {
            currentSequenceIndex++;

            // Dacă mai avem elemente în secvență, trecem la următorul interval
            if (currentSequenceIndex < executionSequence.length) {
                remainingTicksInCurrentBurst = executionSequence[currentSequenceIndex];

                // Alternăm între execuție CPU și apel de sistem (I/O)
                isCurrentlyDoingIo = !isCurrentlyDoingIo;
            } else {
                // Nu mai sunt elemente, deci procesul și-a încheiat execuția
                this.setState(ProcessState.TERMINATED);
            }
        }
    }

    /**
     * Returnează o reprezentare text a procesului pentru debugging și jurnalizare.
     *
     * @return Detaliile procesului formatate ca șir de caractere.
     */
    @Override
    public String toString() {
        return "UserProcess{id=" + id + ", releaseTime=" + releaseTime +
                ", requiredMemory=" + requiredMemory +
                ", sequenceLength=" + (executionSequence != null ? executionSequence.length : 0) + "}";
    }
}