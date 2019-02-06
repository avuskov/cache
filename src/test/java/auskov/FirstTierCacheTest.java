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
    public void setUp() {
        Properties props = new Properties();
        props.setProperty("cache.size.in.memory.entries", "10");
        firstTierCache = new FirstTierCache(props);
    }

    @After
    public void tearDown() {
        try {
            firstTierCache.close();
        } catch (IllegalStateException e) {
            //already closed
        }
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
    public void putShouldEvictTheExpiredObjectsFirstIfTheCacheIsFull() {
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
    public void putShouldEvictTheLeastFrequentlyAskedObjectsSecondIfTheCacheIsFull() {
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
    public void getShouldThrowAnExceptionIfTheCacheIsClosed() {
        long key = 0;
        firstTierCache.put(key, "Something");
        firstTierCache.close();
        try {
            firstTierCache.get(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getShouldReturnAStoredObject() {
        firstTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        Serializable theObject = "The Object";
        firstTierCache.put(key, theObject);
        firstTierCache.setDeadline(key, 101L);
        assertEquals(theObject, firstTierCache.get(key));
    }

    @Test
    public void getShouldReturnNullIfTheObjectIsMissing() {
        long missingObjectKey = 0;
        assertNull(firstTierCache.get(missingObjectKey));
    }

    @Test
    public void getShouldReturnNullIfTheObjectHasExpired() {
        firstTierCache.setCurrentTimeSupplier(() -> 100L);
        long keyExpiredOne = 0;
        long keyExpiredTwo = 1;
        firstTierCache.put(keyExpiredOne, "Expired Object");
        firstTierCache.setDeadline(keyExpiredOne, 100L);
        assertNull(firstTierCache.get(keyExpiredOne));
        firstTierCache.put(keyExpiredTwo, "Expired Object");
        firstTierCache.setDeadline(keyExpiredTwo, 99L);
        assertNull(firstTierCache.get(keyExpiredTwo));
    }

    @Test
    public void getShouldIncrementTheObjectsWeightIfTheValueIdReturned() {
        long key = 0;
        firstTierCache.put(key, "An Object");
        assertEquals(0, firstTierCache.getWeight(key));
        firstTierCache.get(key);
        assertEquals(1, firstTierCache.getWeight(key));
    }

    @Test
    public void getShouldRemoveAnExpiredObject() {
        firstTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        firstTierCache.put(key, "Expired Object");
        firstTierCache.setDeadline(key, 100L);
        firstTierCache.get(key);
        assertFalse(firstTierCache.containsKey(key));
    }

    @Test
    public void clearShouldThrowAnExceptionIfTheCacheIsClosed() {
        firstTierCache.close();
        try {
            firstTierCache.clear();
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void clearShouldWipeAllStoredObjects() {
        long keyOne = 0;
        long keyTwo = 1;
        firstTierCache.put(keyOne, "1");
        firstTierCache.put(keyTwo, "2");
        firstTierCache.get(keyOne);
        firstTierCache.get(keyTwo);
        firstTierCache.clear();
        assertFalse(firstTierCache.containsKey(keyOne));
        assertFalse(firstTierCache.containsKey(keyTwo));
        assertEquals(0, firstTierCache.getWeight(keyOne));
        assertEquals(0, firstTierCache.getWeight(keyTwo));
        assertEquals(0, firstTierCache.getDeadline(keyOne));
        assertEquals(0, firstTierCache.getDeadline(keyTwo));
    }

    @Test
    public void removeShouldRemoveTheSpecifiedObject() {
        long keyOne = 0;
        long keyTwo = 1;
        firstTierCache.put(keyOne, "1");
        firstTierCache.put(keyTwo, "2");
        firstTierCache.get(keyOne);
        firstTierCache.get(keyTwo);
        firstTierCache.remove(keyTwo);
        assertTrue(firstTierCache.containsKey(keyOne));
        assertFalse(firstTierCache.containsKey(keyTwo));
        assertNotEquals(0, firstTierCache.getWeight(keyOne));
        assertEquals(0, firstTierCache.getWeight(keyTwo));
        assertNotEquals(0, firstTierCache.getDeadline(keyOne));
        assertEquals(0, firstTierCache.getDeadline(keyTwo));
    }

    @Test
    public void closeShouldThrowAnExceptionIfUsedTwice() {
        firstTierCache.close();
        try {
            firstTierCache.close();
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void containsKeyShouldThrowAnExceptionIfTheCacheIsClosed() {
        firstTierCache.close();
        try {
            firstTierCache.close();
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void containsKeyShouldReturnTrueIfTheValueExists() {
        long key = 0;
        firstTierCache.put(key, "An Object");
        assertTrue(firstTierCache.containsKey(key));
    }

    @Test
    public void containsKeyShouldReturnFalseIfTheValueDoesNotExist() {
        long key = 0;
        firstTierCache.put(key, "An Object");
        long missingKey = 1;
        assertFalse(firstTierCache.containsKey(missingKey));
    }

    @Test
    public void containsKeyShouldReturnTrueEvenIfTheExistingValueHasExpired() {
        firstTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        firstTierCache.put(key, "An Object");
        firstTierCache.setDeadline(key, 99L);
        assertTrue(firstTierCache.containsKey(key));
    }

    @Test
    public void incrementWeightShouldThrowAnExceptionIfTheCacheIsClosed() {
        long key = 0;
        firstTierCache.put(key, "An Object");
        firstTierCache.close();
        try {
            firstTierCache.incrementWeight(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void incrementWeightShouldAddOneToTheCurrentWeightOfTheSelectedObject() {
        long key = 0;
        long zeroWeightKey = 1;
        firstTierCache.put(key, "An Object");
        firstTierCache.put(zeroWeightKey, "An Other Object");
        assertEquals(0, firstTierCache.getWeight(key));
        firstTierCache.incrementWeight(key);
        assertEquals(1, firstTierCache.getWeight(key));
        firstTierCache.incrementWeight(key);
        assertEquals(2, firstTierCache.getWeight(key));
        assertEquals(0, firstTierCache.getWeight(zeroWeightKey));
    }

    @Test
    public void incrementWeightShouldDoNothingIfTheObjectDoesNotExist() {
        long key = 0;
        firstTierCache.incrementWeight(key);
        assertEquals(0, firstTierCache.getWeight(key));
    }

    @Test
    public void incrementWeightShouldIncrementWeightEvenIfTheObjectHasExpired() {
        firstTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        firstTierCache.put(key, "An Object");
        firstTierCache.setDeadline(key, 99L);
        firstTierCache.incrementWeight(key);
        firstTierCache.incrementWeight(key);
        assertEquals(2, firstTierCache.getWeight(key));
    }

    @Test
    public void getWeightShouldThrowAnExceptionIfTheCacheIsClosed() {
        long key = 0;
        firstTierCache.put(key, "An Object");
        firstTierCache.close();
        try {
            firstTierCache.getWeight(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getWeightShouldReturnTheObjectsWeight() {
        long key = 0;
        firstTierCache.put(key, "An Object");
        assertEquals(0, firstTierCache.getWeight(key));
        firstTierCache.incrementWeight(key);
        firstTierCache.incrementWeight(key);
        assertEquals(2, firstTierCache.getWeight(key));
    }

    @Test
    public void getWeightShouldReturnZeroIfTheObjectDoesNotExist() {
        long key = 0;
        assertEquals(0, firstTierCache.getWeight(key));
    }

    @Test
    public void getWeightShouldReturnTheObjectsWeightEvenIfTheObjectHasExpired() {
        firstTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        firstTierCache.put(key, "An Object");
        firstTierCache.setDeadline(key, 1000L);
        firstTierCache.incrementWeight(key);
        firstTierCache.setDeadline(key, 99L);
        assertEquals(1, firstTierCache.getWeight(key));
    }

    @Test
    public void setDeadlineShouldThrowAnExceptionIfTheCacheIsClosed() {
        long key = 0;
        firstTierCache.put(key, "An Object");
        firstTierCache.close();
        try {
            firstTierCache.setDeadline(key, 100L);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void setDeadlineShouldSetTheObjectsDeadline() {
        long key = 0;
        firstTierCache.put(key, "An Object");
        long deadline = System.currentTimeMillis() + 1000;
        firstTierCache.setDeadline(key, deadline);
        assertEquals(deadline, firstTierCache.getDeadline(key));
        deadline += 1234;
        firstTierCache.setDeadline(key, deadline);
        assertEquals(deadline, firstTierCache.getDeadline(key));
    }

    @Test
    public void setDeadlineShouldDoNothingIfTheObjectDoesNotExist() {
        long key = 0;
        long deadline = System.currentTimeMillis() + 1000;
        firstTierCache.setDeadline(key, deadline);
        assertEquals(0, firstTierCache.getDeadline(key));
    }

    @Test
    public void setDeadlineShouldSetTheObjectsDeadlineEvenIfTheObjectHasExpired() {
        firstTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        long deadline = 50L;
        firstTierCache.put(key, "An Object");
        firstTierCache.setDeadline(key, deadline);
        long newDeadline = 150;
        firstTierCache.setDeadline(key, newDeadline);
        assertEquals(newDeadline, firstTierCache.getDeadline(key));
    }

    @Test
    public void getDeadlineShouldThrowAnExceptionIfTheCacheIsClosed() {
        long key = 0;
        firstTierCache.put(key, "An Object");
        firstTierCache.close();
        try {
            firstTierCache.getDeadline(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getDeadlineShouldReturnZeroIfTheObjectDoesNotExist() {
        long key = 12;
        assertEquals(0, firstTierCache.getDeadline(key));
    }

    @Test
    public void getDeadlineShouldReturnTheObjectsDeadline() {
        long key = 0;
        firstTierCache.put(key, "An Object");
        assertNotNull(firstTierCache.getDeadline(key));
        long deadline = System.currentTimeMillis() + 1000;
        firstTierCache.setDeadline(key, deadline);
        assertEquals(deadline, firstTierCache.getDeadline(key));
    }

    @Test
    public void getDeadlineShouldReturnTheObjectsDeadlineEvenIfTheObjectHasExpired() {
        long key = 0;
        long deadline = 50L;
        firstTierCache.put(key, "An Object");
        firstTierCache.setDeadline(key, deadline);
        firstTierCache.setCurrentTimeSupplier(() -> 100L);
        assertEquals(deadline, firstTierCache.getDeadline(key));
    }

    @Test
    public void setCurrentTimeSupplierShouldChangeCurrentTimeDefinition() {
        long key = 0;
        Serializable object = "An Object";
        firstTierCache.put(key, object);
        firstTierCache.setCurrentTimeSupplier(() -> 99L);
        firstTierCache.setDeadline(key, 100L);
        assertEquals(object, firstTierCache.get(key));
        firstTierCache.setCurrentTimeSupplier(() -> 150L);
        assertNull(firstTierCache.get(key));
    }

    @Test
    public void setWeightShouldChangeTheCurrentWeightOfTheSelectedObjectToTheSpecifiedOne() {
        long key = 0;
        long weight = 125L;
        firstTierCache.put(key, "An Object");
        assertEquals(0, firstTierCache.getWeight(key));
        firstTierCache.setWeight(key, weight);
        assertEquals(weight, firstTierCache.getWeight(key));
    }

    @Test
    public void setWeightShouldDoNothingIfTheObjectDoesNotExist() {
        long key = 0;
        firstTierCache.setWeight(key, 123);
        assertEquals(0, firstTierCache.getWeight(key));
    }

    @Test
    public void setWeightShouldSetTheWeightEvenIfTheObjectHasExpired() {
        firstTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        long weight = 123;
        firstTierCache.put(key, "An Object");
        firstTierCache.setDeadline(key, 50L);
        firstTierCache.setWeight(key, weight);
        assertEquals(weight, firstTierCache.getWeight(key));
    }
}