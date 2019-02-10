package auskov;

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

    @Test
    public void getEntrySizeShouldThrowAnExceptionIfTheCacheIsClosed() {
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

    @Test
    public void creatingCacheTierFilesystemWithNullStoragePathShouldThrowAnException() {
        Properties props = new Properties();
        props.setProperty("cache.size.filesystem.bytes", "45");
        try {
            new CacheTierFilesystem(props);
        } catch (InvalidPropertiesFormatException e) {
            assertEquals("Storage path can't be null!", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void creatingCacheTierFilesystemWithSizeZeroShouldThrowAnException() {
        Properties props = new Properties();
        props.setProperty("cache.size.filesystem.bytes", "0");
        props.setProperty("cache.filesystem.storage.path", ".");
        try {
            new CacheTierFilesystem(props);
        } catch (InvalidPropertiesFormatException e) {
            assertEquals("Size of the cache tier must be greater than 0!", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void creatingCacheTierFilesystemWithSizeBelowZeroShouldThrowAnException() {
        Properties props = new Properties();
        props.setProperty("cache.size.filesystem.bytes", "-1");
        props.setProperty("cache.filesystem.storage.path", ".");
        try {
            new CacheTierFilesystem(props);
        } catch (InvalidPropertiesFormatException e) {
            assertEquals("Size of the cache tier must be greater than 0!", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void creatingCacheTierFilesystemWithIncorrectPathShouldThrowAnException() {
        Properties props = new Properties();
        props.setProperty("cache.size.filesystem.bytes", "100");
        props.setProperty("cache.filesystem.storage.path", "/q.java");
        try {
            new CacheTierFilesystem(props);
        } catch (InvalidPropertiesFormatException e) {
            assertEquals("Cache storage path is not a directory!", e.getMessage());
            return;
        }
        fail();
    }

    @Override
    protected CacheTier createCacheTier(Properties props) {
        try {
            CacheTier cacheTier = new CacheTierFilesystem(props);
            resourceRegistry.add(cacheTier);
            return cacheTier;
        } catch (InvalidPropertiesFormatException e) {
            LOG.log(Level.WARNING, "Attempt to create a second cache tier with invalid properties.", e);
            fail("Attempt to create a second cache tier with invalid properties.");
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
            fail("Attempt to create a second cache tier with invalid properties.");
        }
        return null;
    }
}