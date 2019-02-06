package auskov;

import org.ehcache.CachePersistenceException;

import java.io.Serializable;

interface CacheTier {
    void put(long key, Serializable object);

    Object get(long key);

    void clear();

    void remove(long key);

    void close();

    boolean containsKey(long key);

    void incrementWeight(long key);

    void setWeight(long key, long weight);

    long getWeight(long key);

    void setDeadline (long key, long millis);

    long getDeadline(long key);
}
