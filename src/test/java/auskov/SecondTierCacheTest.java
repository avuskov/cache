package auskov;

import org.ehcache.CachePersistenceException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import static org.junit.Assert.*;

public class SecondTierCacheTest {
    private SecondTierCache secondTierCache;

    @Before
    public void setUp() throws InvalidPropertiesFormatException {
        Properties props = new Properties();
        props.setProperty("cache.size.filesystem.bytes", "1000");
        props.setProperty("cache.filesystem.storage.path", ".");
        secondTierCache = new SecondTierCache(props);
    }

    @After
    public void tearDown() {
        try {
            secondTierCache.close();
        } catch (IllegalStateException e) {
            //already closed
        }
    }

    @Test
    public void putShouldSaveTheObject() {
        long key = 23;
        Serializable object = "An object";
        secondTierCache.put(key, object);
        assertEquals(object, secondTierCache.get(key));
    }

    @Test
    public void putShouldSetTheObjectsWeightToZero() {
        long key = 1;
        Serializable object = "An object";
        secondTierCache.put(key, object);
        assertEquals(0, secondTierCache.getWeight(key));
        secondTierCache.get(key);
        secondTierCache.put(key, object);
        assertEquals(0, secondTierCache.getWeight(key));
    }

    @Test
    public void putShouldSetTheObjectDeadlineToLongMax() {
        long key = 2;
        Serializable object = "An object";
        secondTierCache.put(key, object);
        assertEquals(Long.MAX_VALUE, secondTierCache.getDeadline(key));
    }

    @Test
    public void putShouldThrowAnExceptionIfTheCacheIsClosed() throws CachePersistenceException {
        secondTierCache.close();
        try {
            secondTierCache.put(0, "Something");
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void putShouldEvictTheExpiredObjectsFirstIfTheCacheIsFull() throws InvalidPropertiesFormatException {
        Properties props = new Properties();
        props.setProperty("cache.size.filesystem.bytes", "45");
        secondTierCache = new SecondTierCache(props);
        secondTierCache.setCurrentTimeSupplier(() -> 100L);
        secondTierCache.setFileLenghtEvaluator(file -> 5L);
        long coldObjectKey = 0;
        long expiredObjectKey = 1;
        long hotObjectKey = 2;
        long newObjectKey = 4;
        Serializable coldObject = "Fresh Object #1";
        Serializable hotObject = "Fresh Object #2";
        Serializable expiredObject = "The Expired One";
        Serializable newObject = "A new Object";

        secondTierCache.put(coldObjectKey, coldObject);
        secondTierCache.put(expiredObjectKey, expiredObject);
        secondTierCache.setDeadline(expiredObjectKey, 99);
        secondTierCache.get(expiredObjectKey);
        secondTierCache.put(hotObjectKey, hotObject);
        secondTierCache.get(hotObjectKey);
        secondTierCache.get(hotObjectKey);
        secondTierCache.put(newObjectKey, newObject);

        assertFalse(secondTierCache.containsKey(expiredObjectKey));
        assertTrue(secondTierCache.containsKey(coldObjectKey));
        assertTrue(secondTierCache.containsKey(hotObjectKey));
        assertTrue(secondTierCache.containsKey(newObjectKey));
    }

    @Test
    public void putShouldEvictTheLeastFrequentlyAskedObjectsSecondIfTheCacheIsFull() throws InvalidPropertiesFormatException {
        Properties props = new Properties();
        props.setProperty("cache.size.filesystem.bytes", "45");
        secondTierCache = new SecondTierCache(props);
        secondTierCache.setCurrentTimeSupplier(() -> 100L);
        secondTierCache.setFileLenghtEvaluator(file -> 5L);

        long coldObjectKey = 0;
        long warmObjectKey = 1;
        long hotObjectKey = 2;
        long newObjectKey = 4;
        Serializable coldObject = "Cold Object";
        Serializable hotObject = "Hot Object";
        Serializable warmObject = "Warm Object";
        Serializable newObject = "A new Object";

        secondTierCache.put(coldObjectKey, coldObject);
        secondTierCache.put(warmObjectKey, warmObject);
        secondTierCache.get(warmObjectKey);
        secondTierCache.put(hotObjectKey, hotObject);
        secondTierCache.get(hotObjectKey);
        secondTierCache.get(hotObjectKey);
        secondTierCache.put(newObjectKey, newObject);

        assertTrue(secondTierCache.containsKey(warmObjectKey));
        assertFalse(secondTierCache.containsKey(coldObjectKey));
        assertTrue(secondTierCache.containsKey(hotObjectKey));
        assertTrue(secondTierCache.containsKey(newObjectKey));
    }

    @Test
    public void getShouldThrowAnExceptionIfTheCacheIsClosed() throws CachePersistenceException {
        long key = 0;
        secondTierCache.put(key, "Something");
        secondTierCache.close();
        try {
            secondTierCache.get(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getShouldReturnAStoredObject() {
        secondTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        Serializable theObject = "The Object";
        secondTierCache.put(key, theObject);
        secondTierCache.setDeadline(key, 101L);
        assertEquals(theObject, secondTierCache.get(key));
    }

    @Test
    public void getShouldReturnNullIfTheObjectIsMissing() {
        long missingObjectKey = 0;
        assertNull(secondTierCache.get(missingObjectKey));
    }

    @Test
    public void getShouldReturnNullIfTheObjectHasExpired() {
        secondTierCache.setCurrentTimeSupplier(() -> 100L);
        long keyExpiredOne = 0;
        long keyExpiredTwo = 1;
        secondTierCache.put(keyExpiredOne, "Expired Object");
        secondTierCache.setDeadline(keyExpiredOne, 100L);
        assertNull(secondTierCache.get(keyExpiredOne));
        secondTierCache.put(keyExpiredTwo, "Expired Object");
        secondTierCache.setDeadline(keyExpiredTwo, 99L);
        assertNull(secondTierCache.get(keyExpiredTwo));
    }

    @Test
    public void getShouldIncrementTheObjectsWeightIfTheValueIdReturned() {
        long key = 0;
        secondTierCache.put(key, "An Object");
        assertEquals(0, secondTierCache.getWeight(key));
        secondTierCache.get(key);
        assertEquals(1, secondTierCache.getWeight(key));
    }

    @Test
    public void getShouldRemoveAnExpiredObject() {
        secondTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        secondTierCache.put(key, "Expired Object");
        secondTierCache.setDeadline(key, 100L);
        secondTierCache.get(key);
        assertFalse(secondTierCache.containsKey(key));
    }

    @Test
    public void clearShouldThrowAnExceptionIfTheCacheIsClosed() throws CachePersistenceException {
        secondTierCache.close();
        try {
            secondTierCache.clear();
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void clearShouldWipeAllStoredObjects() {
        long keyOne = 0;
        long keyTwo = 1;
        secondTierCache.put(keyOne, "1");
        secondTierCache.put(keyTwo, "2");
        secondTierCache.get(keyOne);
        secondTierCache.get(keyTwo);
        secondTierCache.clear();
        assertFalse(secondTierCache.containsKey(keyOne));
        assertFalse(secondTierCache.containsKey(keyTwo));
        assertEquals(0, secondTierCache.getWeight(keyOne));
        assertEquals(0, secondTierCache.getWeight(keyTwo));
        assertEquals(0, secondTierCache.getDeadline(keyOne));
        assertEquals(0, secondTierCache.getDeadline(keyTwo));
    }

    @Test
    public void removeShouldRemoveTheSpecifiedObject() {
        long keyOne = 0;
        long keyTwo = 1;
        secondTierCache.put(keyOne, "1");
        secondTierCache.put(keyTwo, "2");
        secondTierCache.get(keyOne);
        secondTierCache.get(keyTwo);
        secondTierCache.remove(keyTwo);
        assertTrue(secondTierCache.containsKey(keyOne));
        assertFalse(secondTierCache.containsKey(keyTwo));
        assertNotEquals(0, secondTierCache.getWeight(keyOne));
        assertEquals(0, secondTierCache.getWeight(keyTwo));
        assertNotEquals(0, secondTierCache.getDeadline(keyOne));
        assertEquals(0, secondTierCache.getDeadline(keyTwo));
    }

    @Test
    public void closeShouldThrowAnExceptionIfUsedTwice() throws CachePersistenceException {
        secondTierCache.close();
        try {
            secondTierCache.close();
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void containsKeyShouldThrowAnExceptionIfTheCacheIsClosed() throws CachePersistenceException {
        secondTierCache.close();
        try {
            secondTierCache.close();
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void containsKeyShouldReturnTrueIfTheValueExists() {
        long key = 0;
        secondTierCache.put(key, "An Object");
        assertTrue(secondTierCache.containsKey(key));
    }

    @Test
    public void containsKeyShouldReturnFalseIfTheValueDoesNotExist() {
        long key = 0;
        secondTierCache.put(key, "An Object");
        long missingKey = 1;
        assertFalse(secondTierCache.containsKey(missingKey));
    }

    @Test
    public void containsKeyShouldReturnTrueEvenIfTheExistingValueHasExpired() {
        secondTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        secondTierCache.put(key, "An Object");
        secondTierCache.setDeadline(key, 99L);
        assertTrue(secondTierCache.containsKey(key));
    }

    @Test
    public void incrementWeightShouldThrowAnExceptionIfTheCacheIsClosed() throws CachePersistenceException {
        long key = 0;
        secondTierCache.put(key, "An Object");
        secondTierCache.close();
        try {
            secondTierCache.incrementWeight(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void incrementWeightShouldAddOneToTheCurrentWeightOfTheSelectedObject() {
        long key = 0;
        long zeroWeightKey = 1;
        secondTierCache.put(key, "An Object");
        secondTierCache.put(zeroWeightKey, "An Other Object");
        assertEquals(0, secondTierCache.getWeight(key));
        secondTierCache.incrementWeight(key);
        assertEquals(1, secondTierCache.getWeight(key));
        secondTierCache.incrementWeight(key);
        assertEquals(2, secondTierCache.getWeight(key));
        assertEquals(0, secondTierCache.getWeight(zeroWeightKey));
    }

    @Test
    public void incrementWeightShouldDoNothingIfTheObjectDoesNotExist() {
        long key = 0;
        secondTierCache.incrementWeight(key);
        assertEquals(0, secondTierCache.getWeight(key));
    }

    @Test
    public void incrementWeightShouldIncrementWeightEvenIfTheObjectHasExpired() {
        secondTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        secondTierCache.put(key, "An Object");
        secondTierCache.setDeadline(key, 99L);
        secondTierCache.incrementWeight(key);
        secondTierCache.incrementWeight(key);
        assertEquals(2, secondTierCache.getWeight(key));
    }

    @Test
    public void getWeightShouldThrowAnExceptionIfTheCacheIsClosed() throws CachePersistenceException {
        long key = 0;
        secondTierCache.put(key, "An Object");
        secondTierCache.close();
        try {
            secondTierCache.getWeight(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getWeightShouldReturnTheObjectsWeight() {
        long key = 0;
        secondTierCache.put(key, "An Object");
        assertEquals(0, secondTierCache.getWeight(key));
        secondTierCache.incrementWeight(key);
        secondTierCache.incrementWeight(key);
        assertEquals(2, secondTierCache.getWeight(key));
    }

    @Test
    public void getWeightShouldReturnZeroIfTheObjectDoesNotExist() {
        long key = 0;
        assertEquals(0, secondTierCache.getWeight(key));
    }

    @Test
    public void getWeightShouldReturnTheObjectsWeightEvenIfTheObjectHasExpired() {
        secondTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        secondTierCache.put(key, "An Object");
        secondTierCache.setDeadline(key, 1000L);
        secondTierCache.incrementWeight(key);
        secondTierCache.setDeadline(key, 99L);
        assertEquals(1, secondTierCache.getWeight(key));
    }

    @Test
    public void setDeadlineShouldThrowAnExceptionIfTheCacheIsClosed() throws CachePersistenceException {
        long key = 0;
        secondTierCache.put(key, "An Object");
        secondTierCache.close();
        try {
            secondTierCache.setDeadline(key, 100L);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void setDeadlineShouldSetTheObjectsDeadline() {
        long key = 0;
        secondTierCache.put(key, "An Object");
        long deadline = System.currentTimeMillis() + 1000;
        secondTierCache.setDeadline(key, deadline);
        assertEquals(deadline, secondTierCache.getDeadline(key));
        deadline += 1234;
        secondTierCache.setDeadline(key, deadline);
        assertEquals(deadline, secondTierCache.getDeadline(key));
    }

    @Test
    public void setDeadlineShouldDoNothingIfTheObjectDoesNotExist() {
        long key = 0;
        long deadline = System.currentTimeMillis() + 1000;
        secondTierCache.setDeadline(key, deadline);
        assertEquals(0, secondTierCache.getDeadline(key));
    }

    @Test
    public void setDeadlineShouldSetTheObjectsDeadlineEvenIfTheObjectHasExpired() {
        secondTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        long deadline = 50L;
        secondTierCache.put(key, "An Object");
        secondTierCache.setDeadline(key, deadline);
        long newDeadline = 150;
        secondTierCache.setDeadline(key, newDeadline);
        assertEquals(newDeadline, secondTierCache.getDeadline(key));
    }

    @Test
    public void getDeadlineShouldThrowAnExceptionIfTheCacheIsClosed() throws CachePersistenceException {
        long key = 0;
        secondTierCache.put(key, "An Object");
        secondTierCache.close();
        try {
            secondTierCache.getDeadline(key);
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getDeadlineShouldReturnZeroIfTheObjectDoesNotExist() {
        long key = 12;
        assertEquals(0, secondTierCache.getDeadline(key));
    }

    @Test
    public void getDeadlineShouldReturnTheObjectsDeadline() {
        long key = 0;
        secondTierCache.put(key, "An Object");
        assertNotNull(secondTierCache.getDeadline(key));
        long deadline = System.currentTimeMillis() + 1000;
        secondTierCache.setDeadline(key, deadline);
        assertEquals(deadline, secondTierCache.getDeadline(key));
    }

    @Test
    public void getDeadlineShouldReturnTheObjectsDeadlineEvenIfTheObjectHasExpired() {
        long key = 0;
        long deadline = 50L;
        secondTierCache.put(key, "An Object");
        secondTierCache.setDeadline(key, deadline);
        secondTierCache.setCurrentTimeSupplier(() -> 100L);
        assertEquals(deadline, secondTierCache.getDeadline(key));
    }

    @Test
    public void setCurrentTimeSupplierShouldChangeCurrentTimeDefinition() {
        long key = 0;
        Serializable object = "An Object";
        secondTierCache.put(key, object);
        secondTierCache.setCurrentTimeSupplier(() -> 99L);
        secondTierCache.setDeadline(key, 100L);
        assertEquals(object, secondTierCache.get(key));
        secondTierCache.setCurrentTimeSupplier(() -> 150L);
        assertNull(secondTierCache.get(key));
    }

    @Test
    public void getEntrySizeShouldThrowAnExceptionIfTheCacheIsClosed() throws CachePersistenceException {
        long key = 0;
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
        secondTierCache.put(key, object);
        assertTrue(secondTierCache.getEntrySize(key) > 0);
    }

    @Test
    public void getEntrySizeShouldReturnZeroIfTheEntryDoesNotExist() {
        long key = 23;
        assertTrue(secondTierCache.getEntrySize(key) == 0);
    }

    @Test
    public void setFileLengthEvaluatorShouldAffectTheFileLengthEvaluation() {
        long key = 23;
        Serializable object = "An object";
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

    @Test
    public void setWeightShouldChangeTheCurrentWeightOfTheSelectedObjectToTheSpecifiedOne() {
        long key = 0;
        long weight = 125L;
        secondTierCache.put(key, "An Object");
        assertEquals(0, secondTierCache.getWeight(key));
        secondTierCache.setWeight(key, weight);
        assertEquals(weight, secondTierCache.getWeight(key));
    }

    @Test
    public void setWeightShouldDoNothingIfTheObjectDoesNotExist() {
        long key = 0;
        secondTierCache.setWeight(key, 123);
        assertEquals(0, secondTierCache.getWeight(key));
    }

    @Test
    public void setWeightShouldSetTheWeightEvenIfTheObjectHasExpired() {
        secondTierCache.setCurrentTimeSupplier(() -> 100L);
        long key = 0;
        long weight = 123;
        secondTierCache.put(key, "An Object");
        secondTierCache.setDeadline(key, 50L);
        secondTierCache.setWeight(key, weight);
        assertEquals(weight, secondTierCache.getWeight(key));
    }
}