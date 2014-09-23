package ca.orospakr.lookingglass;

/**
 * Seriously, java?  There's no simple callback value interface, at least in Java 7.
 */
public interface FutureValue<T> {
    void call(T value);
}
