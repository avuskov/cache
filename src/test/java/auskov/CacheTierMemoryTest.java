package auskov;

import org.junit.Test;

import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class CacheTierMemoryTest extends CacheTierTest {
    private static final Logger LOG = Logger.getLogger(CacheTierFilesystemTest.class.getName());

    @Test
    public void creatingCacheTierFilesystemWithSizeZeroShouldThrowAnException() {
        Properties props = new Properties();
        props.setProperty("cache.size.in.memory.entries", "0");
        try {
            new CacheTierMemory(props);
        } catch (InvalidPropertiesFormatException e) {
            assertEquals("Size of the cache tier must be greater than 0!", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void creatingCacheTierFilesystemWithSizeBelowZeroShouldThrowAnException() {
        Properties props = new Properties();
        props.setProperty("cache.size.in.memory.entries", "-1");
        try {
            new CacheTierMemory(props);
        } catch (InvalidPropertiesFormatException e) {
            assertEquals("Size of the cache tier must be greater than 0!", e.getMessage());
            return;
        }
        fail();
    }

    @Override
    protected CacheTier createCacheTier(Properties props) {
        CacheTier cacheTier = null;
        try {
            cacheTier = new CacheTierMemory(props);
        } catch (InvalidPropertiesFormatException e) {
            LOG.log(Level.WARNING, "Attempt to create a second cache tier with invalid properties.", e);
            fail("Attempt to create a second cache tier with invalid properties.");
        }
        resourceRegistry.add(cacheTier);
        return cacheTier;
    }

    @Override
    protected CacheTier createCacheTierWithThreeObjectsCapacityAndCurrentTime100() {
        Properties props = new Properties();
        props.setProperty("cache.size.in.memory.entries", "3");
        CacheTier cacheTier = null;
        try {
            cacheTier = new CacheTierMemory(props);
        } catch (InvalidPropertiesFormatException e) {
            LOG.log(Level.WARNING, "Attempt to create a second cache tier with invalid properties.", e);
            fail("Attempt to create a second cache tier with invalid properties.");
        }
        cacheTier.setCurrentTimeSupplier(() -> 100L);
        resourceRegistry.add(cacheTier);
        return cacheTier;

    }
}