package net.atos.zac.zoeken.model.index;

public record IndexResult(long indexed, long removed, long remaining) {
    public IndexResult() {
        this(0, 0, 0);
    }
}

