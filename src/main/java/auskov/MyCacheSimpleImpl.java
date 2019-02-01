package auskov;

import java.io.IOException;
import java.io.Serializable;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public class MyCacheSimpleImpl implements MyCache {

    private long nextId;
    private boolean open;
    private boolean needToCleanFS;
    private FirstTierCache levelOne;

    private String memoryTier;
    private String filesystemTier;
    private String expirationPolicy;
    private long expirationMillis;

    public static MyCacheSimpleImpl createCash() throws IOException {
        Properties props = new Properties();
        props.load(MyCacheSimpleImpl.class.getClassLoader().getResourceAsStream("application.properties"));
        return new MyCacheSimpleImpl(props);
    }

    public static MyCacheSimpleImpl createCash(Properties props) throws InvalidPropertiesFormatException {
        return new MyCacheSimpleImpl(props);
    }

    private MyCacheSimpleImpl(Properties props) throws InvalidPropertiesFormatException {
        memoryTier = props.getProperty("cache.tiers.memory");
        filesystemTier = props.getProperty("cache.tiers.filesystem");
        expirationPolicy = props.getProperty("cache.expiration.policy");
        expirationMillis = Long.parseLong(props.getProperty("cache.expiration.millis"));

        if ("enable".equals(memoryTier)) {
            levelOne = new FirstTierCache(props);
        }
        if ("enable".equals(filesystemTier)) {

            needToCleanFS = true;
        }
        if (!"enable".equals(memoryTier) && !"enable".equals(filesystemTier)) {
            throw new InvalidPropertiesFormatException("At least one caching tier should be enabled!");
        }

        nextId = 0;
        open = true;
    }

    @Override
    public long put(Serializable object) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        levelOne.put(nextId, object);
        if ("time-to-live".equals(expirationPolicy) || "time-to-idle".equals(expirationPolicy)) {
            levelOne.setDeadline(nextId, System.currentTimeMillis() + expirationMillis);
        }
        return nextId++;
    }

    @Override
    public Object get(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        Object result = levelOne.get(key);
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
        levelOne.clear();
        nextId = 0;
    }

    @Override
    public void remove(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        levelOne.remove(key);
    }

    @Override
    public void close() {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }

        levelOne = null;
        open = false;
        if (needToCleanFS) {
// todo               ((PersistentCacheManager) cacheManager).destroy();
        }

    }

    @Override
    public boolean containsKey(long key) {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
        return levelOne.containsKey(key);
    }

    private String getStoragePath() {
        return ".";
    }
}
