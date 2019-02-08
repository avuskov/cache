package auskov;

import java.util.Properties;

public class FirstTierCacheTest extends CacheTierTest {
    @Override
    protected CacheTier createCacheTier(Properties props) {
        CacheTier cacheTier = new FirstTierCache(props);
        resourceRegistry.add(cacheTier);
        return cacheTier;
    }

    @Override
    protected CacheTier createCacheTierWithThreeObjectsCapacityAndCurrentTime100() {
        Properties props = new Properties();
        props.setProperty("cache.size.in.memory.entries", "3");
        CacheTier cacheTier = new FirstTierCache(props);
        cacheTier.setCurrentTimeSupplier(() -> 100L);
        resourceRegistry.add(cacheTier);
        return cacheTier;

    }
}