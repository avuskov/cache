package auskov;

import java.io.Closeable;
import java.io.Serializable;
import java.util.function.LongSupplier;

abstract class CacheTier implements Closeable, AutoCloseable {
    protected LongSupplier timeSupplier;

    public abstract void put(long key, Serializable object);

    public abstract Object get(long key);

    public abstract void clear();

    public abstract void remove(long key);

    public abstract void close();

    public abstract boolean containsKey(long key);

    public abstract void incrementWeight(long key);

    public abstract void setWeight(long key, long weight);

    public abstract long getWeight(long key);

    public abstract void setDeadline (long key, long millis);

    public abstract long getDeadline(long key);

    void setCurrentTimeSupplier(LongSupplier timeSupplier) {
        this.timeSupplier = timeSupplier;
    }
}
