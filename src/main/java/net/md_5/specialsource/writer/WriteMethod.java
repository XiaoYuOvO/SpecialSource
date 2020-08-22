package net.md_5.specialsource.writer;

public enum WriteMethod {
    APPEND(true),
    REPLACE(false);
    private final boolean append;
    WriteMethod(boolean append){
        this.append = append;
    }

    public boolean isAppend() {
        return append;
    }
}
