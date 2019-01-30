package auskov;

import org.ehcache.CachePersistenceException;

import java.io.Serializable;

interface CacheTier {
    void put(long key, Serializable object);

    Object get(long key);

    void clear();

    void remove(long key);

    void close() throws CachePersistenceException;

    boolean containsKey(long key);

    void incrementWeight(long key);

    long getWeight(long key);

    void setDeadline (long key, long millis);

    long getDeadline(long key);
}
