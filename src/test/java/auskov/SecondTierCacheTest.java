package auskov;

import org.ehcache.CachePersistenceException;
import org.junit.Test;

import java.io.Serializable;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class SecondTierCacheTest extends CacheTierTest {
    private SecondTierCache secondTierCache;
    private static final Logger LOG = Logger.getLogger(SecondTierCacheTest.class.getName());

    @Override
    protected CacheTier createCacheTier(Properties props) {
        try {
            CacheTier cacheTier = new SecondTierCache(props);
            resourceRegistry.add(cacheTier);
            return cacheTier;
        } catch (InvalidPropertiesFormatException e) {
            LOG.log(Level.WARNING, "Attempt to create a second cache tier with invalid properties.", e);
        }
        return null;
    }

    @Override
    protected CacheTier createCacheTierWithThreeObjectsCapacityAndCurrentTime100() {
        try {
            Properties props = new Properties();
            props.setProperty("cache.size.filesystem.bytes", "45");
            props.setProperty("cache.filesystem.storage.path", ".");
            SecondTierCache cacheTier = new SecondTierCache(props);
            cacheTier.setCurrentTimeSupplier(() -> 100L);
            cacheTier.setFileLenghtEvaluator(file -> 5L);
            resourceRegistry.add(cacheTier);
            return cacheTier;
        } catch (InvalidPropertiesFormatException e) {
            LOG.log(Level.WARNING, "Attempt to create a second cache tier with invalid properties.", e);
        }
        return null;
    }

    @Test
    public void getEntrySizeShouldThrowAnExceptionIfTheCacheIsClosed() throws CachePersistenceException {
        long key = 0;
        secondTierCache = (SecondTierCache) tierCache;
        secondTierCache.put(key, "An Object");
        secondTierCache.close();
        try {
            secondTierCache.getEntrySize(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getEntrySizeShouldReturnPositiveLongIfTheEntryExists() {
        long key = 23;
        Serializable object = "An object";
        secondTierCache = (SecondTierCache) tierCache;
        secondTierCache.put(key, object);
        assertTrue(secondTierCache.getEntrySize(key) > 0);
    }

    @Test
    public void getEntrySizeShouldReturnZeroIfTheEntryDoesNotExist() {
        long key = 23;
        secondTierCache = (SecondTierCache) tierCache;
        assertTrue(secondTierCache.getEntrySize(key) == 0);
    }

    @Test
    public void setFileLengthEvaluatorShouldAffectTheFileLengthEvaluation() {
        long key = 23;
        Serializable object = "An object";
        secondTierCache = (SecondTierCache) tierCache;
        secondTierCache.put(key, object);
        secondTierCache.setFileLenghtEvaluator(file -> {
            if (file.getName().contains("value")) {
                return 300;
            } else if (file.getName().contains("weight")) {
                return 20;
            } else if (file.getName().contains("deadline")) {
                return 1;
            }
            return 0;
        });
        assertEquals(321L, secondTierCache.getEntrySize(key));
    }
}