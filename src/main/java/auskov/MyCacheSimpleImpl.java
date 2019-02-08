package auskov;

import java.io.IOException;
import java.io.Serializable;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public class MyCacheSimpleImpl implements MyCache {
    private long nextId;
    private boolean open;
    private CacheTierMemory levelOne;
    private CacheTierFilesystem levelTwo;

    private boolean memoryTierEnabled;
    private boolean filesystemTierEnabled;
    private String expirationPolicy;
    private long expirationMillis;
    private boolean putToBottom;

    public static MyCacheSimpleImpl createCash() throws IOException {
        Properties props = new Properties();
        props.load(MyCacheSimpleImpl.class.getClassLoader().getResourceAsStream("application.properties"));
        return new MyCacheSimpleImpl(props);
    }

    public static MyCacheSimpleImpl createCash(Properties props) throws InvalidPropertiesFormatException {
        return new MyCacheSimpleImpl(props);
    }

    private MyCacheSimpleImpl(Properties props) throws InvalidPropertiesFormatException {
        memoryTierEnabled = "enable".equals(props.getProperty("cache.tiers.memory"));
        filesystemTierEnabled = "enable".equals(props.getProperty("cache.tiers.filesystem"));
        expirationPolicy = props.getProperty("cache.expiration.policy");
        expirationMillis = Long.parseLong(props.getProperty("cache.expiration.millis"));
        putToBottom = "bottom".equals(props.getProperty("cache.tiers.put.to"));

        if (!memoryTierEnabled && !filesystemTierEnabled) {
            throw new InvalidPropertiesFormatException("At least one caching tier should be enabled!");
        }
        if (memoryTierEnabled) {
            levelOne = new CacheTierMemory(props);
        }
        if (filesystemTierEnabled) {
            levelTwo = new CacheTierFilesystem(props);

        }
        if(memoryTierEnabled && filesystemTierEnabled) {
            levelOne.setLowerLevelCache(levelTwo);
        }

        nextId = 0;
        open = true;
    }

    @Override
    public long put(Serializable object) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        CacheTier putTier = filesystemTierEnabled && putToBottom || !memoryTierEnabled ? levelTwo : levelOne;
        putTier.put(nextId, object);
        if ("time-to-live".equals(expirationPolicy) || "time-to-idle".equals(expirationPolicy)) {
            putTier.setDeadline(nextId, System.currentTimeMillis() + expirationMillis);
        }
        return nextId++;
    }

    @Override
    public Object get(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        Object result = null;
        if (memoryTierEnabled) {
            result = levelOne.get(key);
        }
        if (result == null && filesystemTierEnabled) {
            result = levelTwo.get(key);
            if (result != null && memoryTierEnabled) {
                levelOne.put(key, (Serializable) result);
                levelOne.setDeadline(key, levelTwo.getDeadline(key));
                levelOne.setWeight(key, levelTwo.getWeight(key));
            }
        }
        if (result != null && "time-to-idle".equals(expirationPolicy)) {
            levelOne.setDeadline(key, System.currentTimeMillis() + expirationMillis);
        }
        return result;
    }

    @Override
    public void clear() {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        if (memoryTierEnabled) levelOne.clear();
        if (filesystemTierEnabled) levelTwo.clear();
        nextId = 0;
    }

    @Override
    public void remove(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        if (memoryTierEnabled) levelOne.remove(key);
        if (filesystemTierEnabled) levelTwo.remove(key);
    }

    @Override
    public void close() {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }

        if (memoryTierEnabled) levelOne.close();
        levelOne = null;
        if (filesystemTierEnabled) levelTwo.close();
        levelTwo = null;
        open = false;
    }

    @Override
    public boolean containsKey(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        return (memoryTierEnabled && levelOne.containsKey(key))
                || (filesystemTierEnabled && levelTwo.containsKey(key));
    }
}
