package simulator.model;

/**
 * Reprezinta un proces lansat de un utilizator in cadrul sistemului.
 * Spre deosebire de un proces de sistem, un UserProcess este definit de un timp
 * de lansare (release time) si o secventa de executie alternanta formata din
 * intervale de procesare (CPU) si apeluri de sistem (I/O).
 */
public class UserProcess extends Process {

    /** Timpul de la inceputul simularii la care procesul este eliberat in sistem. */
    private final int releaseTime;

    /** * Secventa de intervale de executie si apeluri de sistem.
     * Indicii pari (0, 2, 4...) reprezinta intervale de executie pe procesor (CPU bursts).
     * Indicii impari (1, 3, 5...) reprezinta timpii necesari pentru apelurile de sistem (I/O bursts).
     */
    private final int[] executionSequence;

    /** Indexul curent din vectorul executionSequence. */
    private int currentSequenceIndex = 0;

    /** Timpul (in tick-uri) ramas din intervalul curent de procesare sau apel de sistem. */
    private int remainingTicksInCurrentBurst;

    /** Indicator care arata daca procesul executa in prezent un apel de sistem (I/O) sau procesare normala. */
    private boolean isCurrentlyDoingIo = false;

    /**
     * Construieste un nou proces de utilizator pe baza datelor citite din fisier.
     *
     * @param id          Identificatorul unic al procesului.
     * @param memory      Memoria RAM (in MB) necesara.
     * @param releaseTime Timpul (tick-ul) la care procesul apare in sistem.
     * @param sequence    Vectorul cu secventa alternanta de durate (CPU, IO, CPU...).
     */
    public UserProcess(int id, int memory, int releaseTime, int[] sequence) {
        super(id, memory);
        this.releaseTime = releaseTime;
        this.executionSequence = sequence;

        // Initializam timpul ramas cu prima valoare din secventa (daca exista)
        if (sequence != null && sequence.length > 0) {
            this.remainingTicksInCurrentBurst = sequence[0];
        } else {
            this.remainingTicksInCurrentBurst = 0;
        }
    }

    /**
     * Returneaza timpul la care procesul este eliberat (introdus) in sistem.
     *
     * @return Momentul lansarii (release time).
     */
    public int getReleaseTime() {
        return releaseTime;
    }

    /**
     * Verifica daca stadiul curent al procesului este un apel de sistem.
     * Aceasta informatie ajuta planificatorul (Scheduler) sa stie daca procesul
     * trebuie scos de pe procesor si preluat de procesul de sistem.
     *
     * @return true daca procesul asteapta/executa I/O, false daca necesita CPU.
     */
    public boolean isCurrentlyDoingIo() {
        return isCurrentlyDoingIo;
    }

    /**
     * Executa o unitate de timp (un tick) din intervalul curent al procesului.
     * Functia actualizeaza manual starea si trece la urmatorul interval din vector
     * atunci cand timpul curent ajunge la 0. Daca vectorul se termina, procesul trece in starea TERMINATED.
     *
     * @param currentTime Timpul global curent al simularii.
     */
    @Override
    public void executeTick(int currentTime) {
        // Daca am depasit deja secventa, procesul este terminat
        if (currentSequenceIndex >= executionSequence.length) {
            if (this.currentState != ProcessState.TERMINATED) {
                this.setState(ProcessState.TERMINATED);
            }
            return;
        }

        // Scadem un tick din intervalul curent
        remainingTicksInCurrentBurst--;

        // Verificam daca intervalul curent a ajuns la final
        if (remainingTicksInCurrentBurst <= 0) {
            currentSequenceIndex++;

            // Daca mai avem elemente in secventa, trecem la urmatorul interval
            if (currentSequenceIndex < executionSequence.length) {
                remainingTicksInCurrentBurst = executionSequence[currentSequenceIndex];

                // Alternam intre executie CPU si apel de sistem (I/O)
                isCurrentlyDoingIo = !isCurrentlyDoingIo;
            } else {
                // Nu mai sunt elemente, deci procesul si-a incheiat executia
                this.setState(ProcessState.TERMINATED);
            }
        }
    }

    /**
     * Returneaza o reprezentare text a procesului pentru debugging si jurnalizare.
     *
     * @return Detaliile procesului formatate ca sir de caractere.
     */
    @Override
    public String toString() {
        return "UserProcess{id=" + id + ", releaseTime=" + releaseTime +
                ", requiredMemory=" + requiredMemory +
                ", sequenceLength=" + (executionSequence != null ? executionSequence.length : 0) + "}";
    }
}