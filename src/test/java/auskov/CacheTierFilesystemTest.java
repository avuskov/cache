package auskov;

import org.ehcache.CachePersistenceException;
import org.junit.Test;

import java.io.Serializable;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class CacheTierFilesystemTest extends CacheTierTest {
    private CacheTierFilesystem cacheTierFilesystem;
    private static final Logger LOG = Logger.getLogger(CacheTierFilesystemTest.class.getName());

    @Override
    protected CacheTier createCacheTier(Properties props) {
        try {
            CacheTier cacheTier = new CacheTierFilesystem(props);
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
            CacheTierFilesystem cacheTier = new CacheTierFilesystem(props);
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
        cacheTierFilesystem = (CacheTierFilesystem) tierCache;
        cacheTierFilesystem.put(key, "An Object");
        cacheTierFilesystem.close();
        try {
            cacheTierFilesystem.getEntrySize(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getEntrySizeShouldReturnPositiveLongIfTheEntryExists() {
        long key = 23;
        Serializable object = "An object";
        cacheTierFilesystem = (CacheTierFilesystem) tierCache;
        cacheTierFilesystem.put(key, object);
        assertTrue(cacheTierFilesystem.getEntrySize(key) > 0);
    }

    @Test
    public void getEntrySizeShouldReturnZeroIfTheEntryDoesNotExist() {
        long key = 23;
        cacheTierFilesystem = (CacheTierFilesystem) tierCache;
        assertTrue(cacheTierFilesystem.getEntrySize(key) == 0);
    }

    @Test
    public void setFileLengthEvaluatorShouldAffectTheFileLengthEvaluation() {
        long key = 23;
        Serializable object = "An object";
        cacheTierFilesystem = (CacheTierFilesystem) tierCache;
        cacheTierFilesystem.put(key, object);
        cacheTierFilesystem.setFileLenghtEvaluator(file -> {
            if (file.getName().contains("value")) {
                return 300;
            } else if (file.getName().contains("weight")) {
                return 20;
            } else if (file.getName().contains("deadline")) {
                return 1;
            }
            return 0;
        });
        assertEquals(321L, cacheTierFilesystem.getEntrySize(key));
    }
}