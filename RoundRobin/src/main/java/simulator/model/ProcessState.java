package simulator.model;

public enum ProcessState {
    NEW,
    SWAPPING,
    READY,
    RUNNING,
    WAITING_IO,
    TERMINATED
}