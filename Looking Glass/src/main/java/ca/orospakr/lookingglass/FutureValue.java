package ca.orospakr.lookingglass;

/**
 * Seriously, java?
 */
public interface FutureValue<T> {
    void call(T value);
}
