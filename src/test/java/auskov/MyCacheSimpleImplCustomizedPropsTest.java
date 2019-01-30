package auskov;

import org.ehcache.CachePersistenceException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import static org.junit.Assert.*;

public class MyCacheSimpleImplCustomizedPropsTest {
    private MyCache myCache;
    private Properties props;

    @Before
    public void setUp() throws IOException {
        props = new Properties();
        props.load(MyCacheSimpleImpl.class.getClassLoader().getResourceAsStream("application.properties"));
    }

    @After
    public void tearDown() throws CachePersistenceException {
        try {
            myCache.close();
        } catch (NullPointerException e) {
            //it's ok
        }
    }

    @Test
    public void inMemorySizeZeroShouldGenerateAnException() throws InvalidPropertiesFormatException {
        props.setProperty("cache.tiers.memory", "enable");
        props.setProperty("cache.size.in.memory.entries", "0");

        try {
            myCache = MyCacheSimpleImpl.createCash(props);
        } catch (IllegalArgumentException e) {
            assertEquals("Size must be greater than 0", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void inMemorySizeBelowZeroShouldGenerateAnException() throws InvalidPropertiesFormatException {
        props.setProperty("cache.tiers.memory", "enable");
        props.setProperty("cache.size.in.memory.entries", "-1");

        try {
            myCache = MyCacheSimpleImpl.createCash(props);
        } catch (IllegalArgumentException e) {
            assertEquals("Size must be greater than 0", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void cacheWithSizeOneShouldHoldOnlyTheLastPlacedObject() throws InvalidPropertiesFormatException {
        props.setProperty("cache.tiers.memory", "enable");
        props.setProperty("cache.size.in.memory.entries", "1");
        props.setProperty("cache.tiers.filesystem", "disable");
        myCache = MyCacheSimpleImpl.createCash(props);

        long keyOne = myCache.put("0");
        assertTrue(myCache.containsKey(keyOne));
        assertEquals("0", myCache.get(keyOne));

        long keyTwo = myCache.put("1");
        assertFalse(myCache.containsKey(keyOne));
        assertTrue(myCache.containsKey(keyTwo));
        assertEquals("1", myCache.get(keyTwo));
    }


    @Test
    public void cacheWithSizeNShouldContainNObjects() throws IOException {
        props.setProperty("cache.tiers.memory", "enable");
        props.setProperty("cache.size.in.memory.entries", "4");
        props.setProperty("cache.tiers.filesystem", "disable");
        myCache = MyCacheSimpleImpl.createCash(props);
        long[] keys = new long[8];
        for (int i = 0; i < 8; i++) {
            keys[i] = myCache.put("Object " + i);
        }
        int count = 0;
        for (int i = 0; i < 8; i++) {
            if (myCache.containsKey(keys[i])) {
                count++;
            }
        }
        assertEquals(4, count);
    }

    @Test
    public void getShouldReturnTheObjectStoredInFilesystem() throws InvalidPropertiesFormatException {
        props.setProperty("cache.tiers.memory", "disable");
        props.setProperty("cache.tiers.filesystem", "enable");
        myCache = MyCacheSimpleImpl.createCash(props);
        Serializable item = "Hello";
        long id = myCache.put(item);
        assertEquals(item, myCache.get(id));
    }

    @Test
    public void shouldThrowAnExceptionIfAllTiersWereDisabled() {
        props.setProperty("cache.tiers.memory", "disable");
        props.setProperty("cache.tiers.filesystem", "disable");

        try {
            myCache = MyCacheSimpleImpl.createCash(props);
        } catch (InvalidPropertiesFormatException e) {
            assertEquals("At least one caching tier should be enabled!", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void shouldHoldMoreInTwoTiersThanOnlyInMemory() throws IOException {
        props.setProperty("cache.tiers.memory", "enable");
        props.setProperty("cache.size.in.memory.entries", "4");
        props.setProperty("cache.tiers.filesystem", "enable");
        props.setProperty("cache.size.filesystem.bytes", "32768");
        myCache = MyCacheSimpleImpl.createCash(props);
        long[] keys = new long[8];
        Serializable[] items = new Serializable[8];
        for (int i = 0; i < 8; i++) {
            items[i] = "Object " + i;
            keys[i] = myCache.put(items[i]);
        }
        for (int i = 0; i < 8; i++) {
            assertTrue(myCache.containsKey(keys[i]));
            assertEquals(items[i], myCache.get(keys[i]));
        }
    }

    @Test
    public void getShouldReturnTheCachedValueBeforeTTLExpiration() throws InvalidPropertiesFormatException, InterruptedException {
        props.setProperty("cache.expiration.policy", "time-to-live");
        props.setProperty("cache.expiration.millis", "500");
        myCache = MyCacheSimpleImpl.createCash(props);
        Serializable item = "Hello";
        long id = myCache.put(item);
        assertEquals(item, myCache.get(id));
        Thread.sleep(250);
        assertEquals(item, myCache.get(id));
    }

    @Test
    public void getShouldReturnNullAfterTTLExpiration() throws InvalidPropertiesFormatException, InterruptedException {
        props.setProperty("cache.expiration.policy", "time-to-live");
        props.setProperty("cache.expiration.millis", "500");
        myCache = MyCacheSimpleImpl.createCash(props);
        Serializable item = "Hello";
        long id = myCache.put(item);
        Thread.sleep(250);
        myCache.get(id);
        Thread.sleep(251);
        assertNull(myCache.get(id));
    }

    @Test
    public void getShouldReturnTheCachedValueBeforeTTIExpiration() throws InvalidPropertiesFormatException, InterruptedException {
        props.setProperty("cache.expiration.policy", "time-to-idle");
        props.setProperty("cache.expiration.millis", "500");
        myCache = MyCacheSimpleImpl.createCash(props);
        Serializable item = "Hello";
        long id = myCache.put(item);
        assertEquals(item, myCache.get(id));
        Thread.sleep(250);
        assertEquals(item, myCache.get(id));
        Thread.sleep(251);
        assertEquals(item, myCache.get(id));
    }

    @Test
    public void getShouldReturnNullAfterTTIExpiration() throws InvalidPropertiesFormatException, InterruptedException {
        props.setProperty("cache.expiration.policy", "time-to-idle");
        props.setProperty("cache.expiration.millis", "500");
        myCache = MyCacheSimpleImpl.createCash(props);
        Serializable item = "Hello";
        long id = myCache.put(item);
        Thread.sleep(250);
        myCache.get(id);
        Thread.sleep(251);
        myCache.get(id);
        Thread.sleep(501);
        assertNull(myCache.get(id));
    }

    @Test
    public void getShouldReturnTheCachedValueAfterTimeoutIfNoExpirationIsSet() throws InvalidPropertiesFormatException, InterruptedException {
        props.setProperty("cache.expiration.policy", "no_expiry");
        props.setProperty("cache.expiration.millis", "50");
        myCache = MyCacheSimpleImpl.createCash(props);
        Serializable item = "Hello";
        long id = myCache.put(item);
        Thread.sleep(100);
        assertEquals(item, myCache.get(id));
    }

    //todo tests of max filesystem cache size
}