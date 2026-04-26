package simulator.model;

public enum ProcessState {
    NEW,
    READY,
    RUNNING,
    WAITING_IO,
    SWAPPING,
    TERMINATED
}