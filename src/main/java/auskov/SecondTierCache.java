package auskov;

import org.ehcache.CachePersistenceException;

import java.io.Serializable;

public class SecondTierCache implements CacheTier {
    //todo логика в целом будет аналогична первому уровню, но писать будем в файлы, а не мапу, и ограничение в байтах, а не в записях

    @Override
    public void put(long key, Serializable object) {

    }

    @Override
    public Object get(long key) {
        return null;
    }

    @Override
    public void clear() {

    }

    @Override
    public void remove(long key) {

    }

    @Override
    public void close() throws CachePersistenceException {

    }

    @Override
    public boolean containsKey(long key) {
        return false;
    }

    @Override
    public void incrementWeight(long key) {

    }

    @Override
    public long getWeight(long key) {
        return 0;
    }

    @Override
    public void setDeadline(long key, long millis) {

    }

    @Override
    public long getDeadline(long key) {
        return 0;
    }
}
