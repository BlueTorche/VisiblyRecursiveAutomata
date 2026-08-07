package learner.ObservationTable.Row;

public interface Content<I> {
    boolean get(I idx);

    void set(I idx);

    int getSeparator(Content<I> content);

    boolean isAccepting();
}
