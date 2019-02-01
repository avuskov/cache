package auskov;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.Properties;

import static org.junit.Assert.*;

public class FirstTierCacheTest {
    private FirstTierCache firstTierCache;

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.setProperty("cache.size.in.memory.entries", "10");
        firstTierCache = new FirstTierCache(props);
    }

    @After
    public void tearDown() throws Exception {
        firstTierCache.close();
    }

    @Test
    public void putShouldSaveTheObject() {
        long key = 0;
        Serializable object = "An object";
        firstTierCache.put(key, object);
        assertEquals(object, firstTierCache.get(key));
    }

    @Test
    public void putShouldSetTheObjectsWeightToZero() {
        long key = 1;
        Serializable object = "An object";
        firstTierCache.put(key, object);
        assertEquals(0, firstTierCache.getWeight(key));
        firstTierCache.get(key);
        firstTierCache.put(key, object);
        assertEquals(0, firstTierCache.getWeight(key));
    }

    @Test
    public void putShouldSetTheObjectDeadlineToLongMax() {
        long key = 2;
        Serializable object = "An object";
        firstTierCache.put(key, object);
        assertEquals(Long.MAX_VALUE, firstTierCache.getDeadline(key));
    }

    @Test
    public void putShouldThrowAnExceptionIfTheCacheIsClosed() {
        firstTierCache.close();
        try {
            firstTierCache.put(0, "Something");
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void putShouldEvictTheExpiredObjectsFirstIfTheCacheIsFull(){
        Properties props = new Properties();
        props.setProperty("cache.size.in.memory.entries", "3");
        firstTierCache = new FirstTierCache(props);
        firstTierCache.setCurrentTimeSupplier(() -> 100L);
        long coldObjectKey = 0;
        long expiredObjectKey = 1;
        long hotObjectKey = 2;
        long newObjectKey = 4;
        Serializable coldObject = "Fresh Object #1";
        Serializable hotObject = "Fresh Object #2";
        Serializable expiredObject = "The Expired One";
        Serializable newObject = "A new Object";

        firstTierCache.put(coldObjectKey, coldObject);
        firstTierCache.put(expiredObjectKey, expiredObject);
        firstTierCache.setDeadline(expiredObjectKey, 99);
        firstTierCache.get(expiredObjectKey);
        firstTierCache.put(hotObjectKey, hotObject);
        firstTierCache.get(hotObjectKey);
        firstTierCache.get(hotObjectKey);
        firstTierCache.put(newObjectKey, newObject);

        assertFalse(firstTierCache.containsKey(expiredObjectKey));
        assertTrue(firstTierCache.containsKey(coldObjectKey));
        assertTrue(firstTierCache.containsKey(hotObjectKey));
        assertTrue(firstTierCache.containsKey(newObjectKey));
    }

    @Test
    public void putShouldEvictTheLeastFrequentlyAskedObjectsSecondIfTheCacheIsFull(){
        Properties props = new Properties();
        props.setProperty("cache.size.in.memory.entries", "3");
        firstTierCache = new FirstTierCache(props);
        firstTierCache.setCurrentTimeSupplier(() -> 100L);
        long coldObjectKey = 0;
        long warmObjectKey = 1;
        long hotObjectKey = 2;
        long newObjectKey = 4;
        Serializable coldObject = "Cold Object";
        Serializable hotObject = "Hot Object";
        Serializable warmObject = "Warm Object";
        Serializable newObject = "A new Object";

        firstTierCache.put(coldObjectKey, coldObject);
        firstTierCache.put(warmObjectKey, warmObject);
        firstTierCache.get(warmObjectKey);
        firstTierCache.put(hotObjectKey, hotObject);
        firstTierCache.get(hotObjectKey);
        firstTierCache.get(hotObjectKey);
        firstTierCache.put(newObjectKey, newObject);

        assertTrue(firstTierCache.containsKey(warmObjectKey));
        assertFalse(firstTierCache.containsKey(coldObjectKey));
        assertTrue(firstTierCache.containsKey(hotObjectKey));
        assertTrue(firstTierCache.containsKey(newObjectKey));
    }

    @Test
    public void get() {
        fail();
    }

    @Test
    public void clear() {
        fail();
    }

    @Test
    public void remove() {
        fail();
    }

    @Test
    public void close() {
        fail();
    }

    @Test
    public void containsKey() {
        fail();
    }

    @Test
    public void incrementWeight() {
        fail();
    }

    @Test
    public void getWeight() {
        fail();
    }

    @Test
    public void setDeadline() {
        fail();
    }

    @Test
    public void getDeadline() {
        fail();
    }
}