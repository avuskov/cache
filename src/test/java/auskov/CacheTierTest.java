package auskov;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public abstract class CacheTierTest {
    private static final Logger LOG = Logger.getLogger(CacheTierTest.class.getName());

    protected CacheTier tierCache;
    protected List<Closeable> resourceRegistry;

    protected abstract CacheTier createCacheTier(Properties props);

    protected abstract CacheTier createCacheTierWithThreeObjectsCapacityAndCurrentTime100();

    @Before
    public void setUp() throws IOException {
        Properties props = new Properties();
        props.load(MyCacheSimpleImpl.class.getClassLoader().getResourceAsStream("application.properties"));
        resourceRegistry = new ArrayList<>();
        tierCache = createCacheTier(props);
    }

    @After
    public void tearDown() {
        resourceRegistry.stream().forEach(resource -> {
            try {
                resource.close();
            } catch (IllegalStateException e) {
                LOG.fine("Closing an already closed resource.");
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Can't close the resource", e);
            }
        });

    }

    @Test
    public void putShouldSaveTheObject() {
        long key = 0;
        Serializable object = "An object";
        tierCache.put(key, object);
        assertEquals(object, tierCache.get(key));
    }

    @Test
    public void putShouldSetTheObjectsWeightToZero() {
        long key = 1;
        Serializable object = "An object";
        tierCache.put(key, object);
        assertEquals(0, tierCache.getWeight(key));
        tierCache.get(key);
        tierCache.put(key, object);
        assertEquals(0, tierCache.getWeight(key));
    }

    @Test
    public void putShouldSetTheObjectDeadlineToLongMax() {
        long key = 2;
        Serializable object = "An object";
        tierCache.put(key, object);
        assertEquals(Long.MAX_VALUE, tierCache.getDeadline(key));
    }

    @Test
    public void putShouldThrowAnExceptionIfTheCacheIsClosed() {
        tierCache.close();
        try {
            tierCache.put(0, "Something");
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void putShouldEvictTheExpiredObjectsFirstIfTheCacheIsFull() {
        CacheTier cacheTier = createCacheTierWithThreeObjectsCapacityAndCurrentTime100();

        long coldObjectKey = 0;
        long expiredObjectKey = 1;
        long hotObjectKey = 2;
        long newObjectKey = 4;
        Serializable coldObject = "Fresh Object #1";
        Serializable hotObject = "Fresh Object #2";
        Serializable expiredObject = "The Expired One";
        Serializable newObject = "A new Object";

        cacheTier.put(coldObjectKey, coldObject);
        cacheTier.put(expiredObjectKey, expiredObject);
        cacheTier.setDeadline(expiredObjectKey, 99);
        cacheTier.get(expiredObjectKey);
        cacheTier.put(hotObjectKey, hotObject);
        cacheTier.get(hotObjectKey);
        cacheTier.get(hotObjectKey);
        cacheTier.put(newObjectKey, newObject);

        assertFalse(cacheTier.containsKey(expiredObjectKey));
        assertTrue(cacheTier.containsKey(coldObjectKey));
        assertTrue(cacheTier.containsKey(hotObjectKey));
        assertTrue(cacheTier.containsKey(newObjectKey));
    }

    @Test
    public void putShouldEvictTheLeastFrequentlyAskedObjectsSecondIfTheCacheIsFull() {
        CacheTier cacheTier = createCacheTierWithThreeObjectsCapacityAndCurrentTime100();

        long coldObjectKey = 0;
        long warmObjectKey = 1;
        long hotObjectKey = 2;
        long newObjectKey = 4;
        Serializable coldObject = "Cold Object";
        Serializable hotObject = "Hot Object";
        Serializable warmObject = "Warm Object";
        Serializable newObject = "A new Object";

        cacheTier.put(coldObjectKey, coldObject);
        cacheTier.put(warmObjectKey, warmObject);
        cacheTier.get(warmObjectKey);
        cacheTier.put(hotObjectKey, hotObject);
        cacheTier.get(hotObjectKey);
        cacheTier.get(hotObjectKey);
        cacheTier.put(newObjectKey, newObject);

        assertTrue(cacheTier.containsKey(warmObjectKey));
        assertFalse(cacheTier.containsKey(coldObjectKey));
        assertTrue(cacheTier.containsKey(hotObjectKey));
        assertTrue(cacheTier.containsKey(newObjectKey));
    }

    @Test
    public void getShouldThrowAnExceptionIfTheCacheIsClosed() {
        long key = 0;
        tierCache.put(key, "Something");
        tierCache.close();
        try {
            tierCache.get(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getShouldReturnAStoredObject() {
        tierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        Serializable theObject = "The Object";
        tierCache.put(key, theObject);
        tierCache.setDeadline(key, 101L);
        assertEquals(theObject, tierCache.get(key));
    }

    @Test
    public void getShouldReturnNullIfTheObjectIsMissing() {
        long missingObjectKey = 0;
        assertNull(tierCache.get(missingObjectKey));
    }

    @Test
    public void getShouldReturnNullIfTheObjectHasExpired() {
        tierCache.setCurrentTimeSupplier(() -> 100L);
        long keyExpiredOne = 0;
        long keyExpiredTwo = 1;
        tierCache.put(keyExpiredOne, "Expired Object");
        tierCache.setDeadline(keyExpiredOne, 100L);
        assertNull(tierCache.get(keyExpiredOne));
        tierCache.put(keyExpiredTwo, "Expired Object");
        tierCache.setDeadline(keyExpiredTwo, 99L);
        assertNull(tierCache.get(keyExpiredTwo));
    }

    @Test
    public void getShouldIncrementTheObjectsWeightIfTheValueIdReturned() {
        long key = 0;
        tierCache.put(key, "An Object");
        assertEquals(0, tierCache.getWeight(key));
        tierCache.get(key);
        assertEquals(1, tierCache.getWeight(key));
    }

    @Test
    public void getShouldRemoveAnExpiredObject() {
        tierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        tierCache.put(key, "Expired Object");
        tierCache.setDeadline(key, 100L);
        tierCache.get(key);
        assertFalse(tierCache.containsKey(key));
    }

    @Test
    public void clearShouldThrowAnExceptionIfTheCacheIsClosed() {
        tierCache.close();
        try {
            tierCache.clear();
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void clearShouldWipeAllStoredObjects() {
        long keyOne = 0;
        long keyTwo = 1;
        tierCache.put(keyOne, "1");
        tierCache.put(keyTwo, "2");
        tierCache.get(keyOne);
        tierCache.get(keyTwo);
        tierCache.clear();
        assertFalse(tierCache.containsKey(keyOne));
        assertFalse(tierCache.containsKey(keyTwo));
        assertEquals(0, tierCache.getWeight(keyOne));
        assertEquals(0, tierCache.getWeight(keyTwo));
        assertEquals(0, tierCache.getDeadline(keyOne));
        assertEquals(0, tierCache.getDeadline(keyTwo));
    }

    @Test
    public void removeShouldRemoveTheSpecifiedObject() {
        long keyOne = 0;
        long keyTwo = 1;
        tierCache.put(keyOne, "1");
        tierCache.put(keyTwo, "2");
        tierCache.get(keyOne);
        tierCache.get(keyTwo);
        tierCache.remove(keyTwo);
        assertTrue(tierCache.containsKey(keyOne));
        assertFalse(tierCache.containsKey(keyTwo));
        assertNotEquals(0, tierCache.getWeight(keyOne));
        assertEquals(0, tierCache.getWeight(keyTwo));
        assertNotEquals(0, tierCache.getDeadline(keyOne));
        assertEquals(0, tierCache.getDeadline(keyTwo));
    }

    @Test
    public void closeShouldThrowAnExceptionIfUsedTwice() {
        tierCache.close();
        try {
            tierCache.close();
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void containsKeyShouldThrowAnExceptionIfTheCacheIsClosed() {
        tierCache.close();
        try {
            tierCache.containsKey(0L);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void containsKeyShouldReturnTrueIfTheValueExists() {
        long key = 0;
        tierCache.put(key, "An Object");
        assertTrue(tierCache.containsKey(key));
    }

    @Test
    public void containsKeyShouldReturnFalseIfTheValueDoesNotExist() {
        long key = 0;
        tierCache.put(key, "An Object");
        long missingKey = 1;
        assertFalse(tierCache.containsKey(missingKey));
    }

    @Test
    public void containsKeyShouldReturnTrueEvenIfTheExistingValueHasExpired() {
        tierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        tierCache.put(key, "An Object");
        tierCache.setDeadline(key, 99L);
        assertTrue(tierCache.containsKey(key));
    }

    @Test
    public void incrementWeightShouldThrowAnExceptionIfTheCacheIsClosed() {
        long key = 0;
        tierCache.put(key, "An Object");
        tierCache.close();
        try {
            tierCache.incrementWeight(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void incrementWeightShouldAddOneToTheCurrentWeightOfTheSelectedObject() {
        long key = 0;
        long zeroWeightKey = 1;
        tierCache.put(key, "An Object");
        tierCache.put(zeroWeightKey, "An Other Object");
        assertEquals(0, tierCache.getWeight(key));
        tierCache.incrementWeight(key);
        assertEquals(1, tierCache.getWeight(key));
        tierCache.incrementWeight(key);
        assertEquals(2, tierCache.getWeight(key));
        assertEquals(0, tierCache.getWeight(zeroWeightKey));
    }

    @Test
    public void incrementWeightShouldDoNothingIfTheObjectDoesNotExist() {
        long key = 0;
        tierCache.incrementWeight(key);
        assertEquals(0, tierCache.getWeight(key));
    }

    @Test
    public void incrementWeightShouldIncrementWeightEvenIfTheObjectHasExpired() {
        tierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        tierCache.put(key, "An Object");
        tierCache.setDeadline(key, 99L);
        tierCache.incrementWeight(key);
        tierCache.incrementWeight(key);
        assertEquals(2, tierCache.getWeight(key));
    }

    @Test
    public void getWeightShouldThrowAnExceptionIfTheCacheIsClosed() {
        long key = 0;
        tierCache.put(key, "An Object");
        tierCache.close();
        try {
            tierCache.getWeight(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getWeightShouldReturnTheObjectsWeight() {
        long key = 0;
        tierCache.put(key, "An Object");
        assertEquals(0, tierCache.getWeight(key));
        tierCache.incrementWeight(key);
        tierCache.incrementWeight(key);
        assertEquals(2, tierCache.getWeight(key));
    }

    @Test
    public void getWeightShouldReturnZeroIfTheObjectDoesNotExist() {
        long key = 0;
        assertEquals(0, tierCache.getWeight(key));
    }

    @Test
    public void getWeightShouldReturnTheObjectsWeightEvenIfTheObjectHasExpired() {
        tierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        tierCache.put(key, "An Object");
        tierCache.setDeadline(key, 1000L);
        tierCache.incrementWeight(key);
        tierCache.setDeadline(key, 99L);
        assertEquals(1, tierCache.getWeight(key));
    }

    @Test
    public void setDeadlineShouldThrowAnExceptionIfTheCacheIsClosed() {
        long key = 0;
        tierCache.put(key, "An Object");
        tierCache.close();
        try {
            tierCache.setDeadline(key, 100L);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void setDeadlineShouldSetTheObjectsDeadline() {
        long key = 0;
        tierCache.put(key, "An Object");
        long deadline = System.currentTimeMillis() + 1000;
        tierCache.setDeadline(key, deadline);
        assertEquals(deadline, tierCache.getDeadline(key));
        deadline += 1234;
        tierCache.setDeadline(key, deadline);
        assertEquals(deadline, tierCache.getDeadline(key));
    }

    @Test
    public void setDeadlineShouldDoNothingIfTheObjectDoesNotExist() {
        long key = 0;
        long deadline = System.currentTimeMillis() + 1000;
        tierCache.setDeadline(key, deadline);
        assertEquals(0, tierCache.getDeadline(key));
    }

    @Test
    public void setDeadlineShouldSetTheObjectsDeadlineEvenIfTheObjectHasExpired() {
        tierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        long deadline = 50L;
        tierCache.put(key, "An Object");
        tierCache.setDeadline(key, deadline);
        long newDeadline = 150;
        tierCache.setDeadline(key, newDeadline);
        assertEquals(newDeadline, tierCache.getDeadline(key));
    }

    @Test
    public void getDeadlineShouldThrowAnExceptionIfTheCacheIsClosed() {
        long key = 0;
        tierCache.put(key, "An Object");
        tierCache.close();
        try {
            tierCache.getDeadline(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getDeadlineShouldReturnZeroIfTheObjectDoesNotExist() {
        long key = 12;
        assertEquals(0, tierCache.getDeadline(key));
    }

    @Test
    public void getDeadlineShouldReturnTheObjectsDeadline() {
        long key = 0;
        tierCache.put(key, "An Object");
        assertNotNull(tierCache.getDeadline(key));
        long deadline = System.currentTimeMillis() + 1000;
        tierCache.setDeadline(key, deadline);
        assertEquals(deadline, tierCache.getDeadline(key));
    }

    @Test
    public void getDeadlineShouldReturnTheObjectsDeadlineEvenIfTheObjectHasExpired() {
        long key = 0;
        long deadline = 50L;
        tierCache.put(key, "An Object");
        tierCache.setDeadline(key, deadline);
        tierCache.setCurrentTimeSupplier(() -> 100L);
        assertEquals(deadline, tierCache.getDeadline(key));
    }

    @Test
    public void setCurrentTimeSupplierShouldChangeCurrentTimeDefinition() {
        long key = 0;
        Serializable object = "An Object";
        tierCache.put(key, object);
        tierCache.setCurrentTimeSupplier(() -> 99L);
        tierCache.setDeadline(key, 100L);
        assertEquals(object, tierCache.get(key));
        tierCache.setCurrentTimeSupplier(() -> 150L);
        assertNull(tierCache.get(key));
    }

    @Test
    public void setWeightShouldChangeTheCurrentWeightOfTheSelectedObjectToTheSpecifiedOne() {
        long key = 0;
        long weight = 125L;
        tierCache.put(key, "An Object");
        assertEquals(0, tierCache.getWeight(key));
        tierCache.setWeight(key, weight);
        assertEquals(weight, tierCache.getWeight(key));
    }

    @Test
    public void setWeightShouldDoNothingIfTheObjectDoesNotExist() {
        long key = 0;
        tierCache.setWeight(key, 123);
        assertEquals(0, tierCache.getWeight(key));
    }

    @Test
    public void setWeightShouldSetTheWeightEvenIfTheObjectHasExpired() {
        tierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        long weight = 123;
        tierCache.put(key, "An Object");
        tierCache.setDeadline(key, 50L);
        tierCache.setWeight(key, weight);
        assertEquals(weight, tierCache.getWeight(key));
    }
}
