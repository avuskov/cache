package auskov;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.CachePersistenceException;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public class MyCacheEhcacheImpl implements MyCache {

    private CacheManager cacheManager;
    private Cache<Long, Serializable> firstCache;
    private long nextId;
    private boolean open;
    private boolean needToCleanFS;

    public static MyCacheEhcacheImpl createCash() throws IOException {
        Properties props = new Properties();
        props.load(MyCacheEhcacheImpl.class.getClassLoader().getResourceAsStream("application.properties"));
        return new MyCacheEhcacheImpl(props);
    }

    public static MyCacheEhcacheImpl createCash(Properties props) throws InvalidPropertiesFormatException {
        return new MyCacheEhcacheImpl(props);
    }

    private MyCacheEhcacheImpl(Properties props) throws InvalidPropertiesFormatException {
        String memoryTier = props.getProperty("cache.tiers.memory");
        String filesystemTier = props.getProperty("cache.tiers.filesystem");
        long maxInMemoryEntries = Long.parseLong(props.getProperty("cache.size.in.memory.entries"));
        long maxFilesystemCacheBytes = Long.parseLong(props.getProperty("cache.size.filesystem.bytes"));
        String expirationPolicy = props.getProperty("cache.expiration.policy");
        long expirationMillis = Long.parseLong(props.getProperty("cache.expiration.millis"));

        ResourcePoolsBuilder resources = ResourcePoolsBuilder.newResourcePoolsBuilder();
        CacheManagerBuilder cacheManagerBuilder = CacheManagerBuilder.newCacheManagerBuilder();

        if ("enable".equals(memoryTier)) {
            resources = resources.heap(maxInMemoryEntries, EntryUnit.ENTRIES);
        }
        if ("enable".equals(filesystemTier)) {
            cacheManagerBuilder = cacheManagerBuilder.with(CacheManagerBuilder.persistence(new File(getStoragePath(), "myData")));
            resources = resources.disk(maxFilesystemCacheBytes, MemoryUnit.B, true);
            needToCleanFS = true;
        }
        if(!"enable".equals(memoryTier) && !"enable".equals(filesystemTier)) {
            throw new InvalidPropertiesFormatException("At least one caching tier should be enabled!");
        }

        CacheConfigurationBuilder<Long, Serializable> cacheConfigurationBuilder = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Long.class, Serializable.class, resources);

        if("time-to-live".equals(expirationPolicy)) {
            cacheConfigurationBuilder = cacheConfigurationBuilder.withExpiry(
                    ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMillis(expirationMillis)));
        }
        if("time-to-idle".equals(expirationPolicy)) {
            cacheConfigurationBuilder = cacheConfigurationBuilder.withExpiry(
                    ExpiryPolicyBuilder.timeToIdleExpiration(Duration.ofMillis(expirationMillis)));
        }
        if("no_expiry".equals(expirationPolicy)) {
            cacheConfigurationBuilder = cacheConfigurationBuilder.withExpiry(
                    ExpiryPolicyBuilder.noExpiration());
        }

        cacheManager = cacheManagerBuilder
                .withCache("firstCache", cacheConfigurationBuilder)
                .build(true);
        firstCache = cacheManager.getCache("firstCache", Long.class, Serializable.class);
        nextId = 0;
        open = true;
    }

    @Override
    public long put(Serializable object) {
        firstCache.put(nextId, object);
        return nextId++;
    }

    @Override
    public Object get(long key) {
        return firstCache.get(key);
    }

    @Override
    public void clear() {
        firstCache.clear();
        nextId = 0;
    }

    @Override
    public void remove(long key) {
        firstCache.remove(key);
    }

    @Override
    public void close() throws CachePersistenceException {
        if (open) {
            cacheManager.removeCache("firstCache");
            cacheManager.close();
            open = false;
            if(needToCleanFS) {
                ((PersistentCacheManager) cacheManager).destroy();
            }
        } else {
            throw new IllegalStateException("The cache is already closed");
        }

    }

    @Override
    public boolean containsKey(long key) {
        return firstCache.containsKey(key);
    }

    private String getStoragePath() {
        return ".";
    }
}
