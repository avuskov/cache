package auskov;

import org.ehcache.CachePersistenceException;

import java.io.Serializable;

public interface MyCache {
    long put(Serializable object);
    Object get(long key);
    void clear();
    void remove(long key);
    void close() throws CachePersistenceException;
    boolean containsKey(long key);
}
