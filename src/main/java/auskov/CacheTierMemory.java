package auskov;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class CacheTierMemory extends CacheTier implements Closeable, AutoCloseable {
    //todo add thread safety
    //todo pull common logic to the parent

    private Map<Long, Serializable> values;
    private Map<Long, Long> weights;
    private Map<Long, Long> deadlines;
    private long maxInMemoryEntries;
    private CacheTier lowerLevel;

    CacheTierMemory(Properties props) throws InvalidPropertiesFormatException {
        maxInMemoryEntries = Long.parseLong(props.getProperty("cache.size.in.memory.entries"));

        if (maxInMemoryEntries <= 0) {
            throw new InvalidPropertiesFormatException("Size of the cache tier must be greater than 0!");
        }

        values = new ConcurrentHashMap<>();
        weights = new ConcurrentHashMap<>();
        deadlines = new ConcurrentHashMap<>();
        super.open = true;
        super.timeSupplier = System::currentTimeMillis;
    }

    @Override
    public void put(long key, Serializable object) {
        checkStateIsOpen();
        while (values.size() >= maxInMemoryEntries) {
            removeAllExpiredEntries();
            if (values.size() >= maxInMemoryEntries) {
                evictTheColdestEntry();
            }
        }

        weights.put(key, 0L);
        values.put(key, object);
        deadlines.put(key, Long.MAX_VALUE);
    }

    private void removeAllExpiredEntries() {
        deadlines.entrySet().stream()
                .filter(entry -> entry.getValue() <= super.timeSupplier.getAsLong())
                .forEach(entry -> remove(entry.getKey()));
    }

    private void evictTheColdestEntry() {
        long key = weights.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue()))
                .get()
                .getKey();
        if(lowerLevel != null) {
            lowerLevel.put(key, values.get(key));
            lowerLevel.setDeadline(key, weights.get(key));
            lowerLevel.setWeight(key, deadlines.get(key));
        }
        remove(key);
    }

    @Override
    public Object get(long key) {
        checkStateIsOpen();
        if (!values.containsKey(key)) {
            return null;
        }
        if (super.timeSupplier.getAsLong() >= deadlines.get(key)) {
            remove(key);
            return null;
        }
        incrementWeight(key);
        return values.get(key);

    }

    @Override
    public void clear() {
        checkStateIsOpen();
        values.clear();
        weights.clear();
        deadlines.clear();
    }

    @Override
    public void remove(long key) {
        checkStateIsOpen();
        values.remove(key);
        weights.remove(key);
        deadlines.remove(key);
    }

    @Override
    public void close() {
        checkStateIsOpen();
        values = null;
        weights = null;
        deadlines = null;
        super.close();
    }

    @Override
    public boolean containsKey(long key) {
        checkStateIsOpen();
        return values.containsKey(key);
    }

    @Override
    public void incrementWeight(long key) {
        checkStateIsOpen();
        if (containsKey(key)) {
            long curWeight = weights.get(key);
            weights.put(key, ++curWeight);
        }
    }

    @Override
    public void setWeight(long key, long weight) {
        checkStateIsOpen();
        if (containsKey(key)) {
            weights.put(key, weight);
        }
    }

    @Override
    public long getWeight(long key) {
        checkStateIsOpen();
        if (containsKey(key)) {
            return weights.get(key);
        }
        return 0;
    }

    @Override
    public void setDeadline(long key, long millis) {
        checkStateIsOpen();
        if (containsKey(key)) {
            deadlines.put(key, millis);
        }
    }

    @Override
    public long getDeadline(long key) {
        checkStateIsOpen();
        if (containsKey(key)) {
            return deadlines.get(key);
        }
        return 0;
    }

    public void setLowerLevelCache(CacheTier cacheTier) {
        lowerLevel = cacheTier;
    }
}
