package auskov;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public class FirstTierCache implements CacheTier {
    //todo add logging, make 'put' atomic
    private boolean open;
    private Map<Long, Serializable> values;
    private Map<Long, Long> weights;
    private Map<Long, Long> deadlines;
    private long maxInMemoryEntries;
    private LongSupplier timeSupplier;

    FirstTierCache(Properties props) {
        maxInMemoryEntries = Long.parseLong(props.getProperty("cache.size.in.memory.entries"));

        if (maxInMemoryEntries <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }

        values = new ConcurrentHashMap<>();
        weights = new ConcurrentHashMap<>();
        deadlines = new ConcurrentHashMap<>();
        open = true;
        timeSupplier = System::currentTimeMillis;
    }

    @Override
    public void put(long key, Serializable object) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        while (values.size() >= maxInMemoryEntries) {
            deadlines.entrySet().stream()
                    .filter(entry -> entry.getValue() <= timeSupplier.getAsLong())
                    .forEach(entry -> remove(entry.getKey()));
            if (values.size() >= maxInMemoryEntries) {
                remove(weights.entrySet().stream()
                        .min(Comparator.comparing(entry -> entry.getValue()))
                        .get()
                        .getKey());
            }
        }

        weights.put(key, 0L);
        values.put(key, object);
        deadlines.put(key, Long.MAX_VALUE);
    }

    @Override
    public Object get(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        if (!values.containsKey(key)) {
            return null;
        }
        if (timeSupplier.getAsLong() >= deadlines.get(key)) {
            remove(key);
            return null;
        }
        incrementWeight(key);
        return values.get(key);

    }

    @Override
    public void clear() {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        values.clear();
        weights.clear();
        deadlines.clear();
    }

    @Override
    public void remove(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        values.remove(key);
        weights.remove(key);
        deadlines.remove(key);
    }

    @Override
    public void close() {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        values = null;
        weights = null;
        deadlines = null;
        open = false;
    }

    @Override
    public boolean containsKey(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        return values.containsKey(key);
    }

    @Override
    public void incrementWeight(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        if (containsKey(key)) {
            long curWeight = weights.get(key);
            weights.put(key, ++curWeight);
        }
    }

    @Override
    public long getWeight(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        if (containsKey(key)) {
            return weights.get(key);
        }
        return 0;
    }

    @Override
    public void setDeadline(long key, long millis) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        if (containsKey(key)) {
            deadlines.put(key, millis);
        }
    }

    @Override
    public long getDeadline(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        if (containsKey(key)) {
            return deadlines.get(key);
        }
        return 0;
    }

    void setCurrentTimeSupplier(LongSupplier timeSupplier) {
        this.timeSupplier = timeSupplier;
    }
}
