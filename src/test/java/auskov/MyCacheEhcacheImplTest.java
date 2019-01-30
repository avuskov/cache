package auskov;

import org.ehcache.CachePersistenceException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;

import static org.junit.Assert.*;

public class MyCacheEhcacheImplTest {
    private MyCache myCache;

    @Before
    public void setUp() throws IOException {
        myCache = MyCacheEhcacheImpl.createCash();
    }

    @After
    public void tearDown() throws CachePersistenceException {
        myCache.close();
    }

    @Test
    public void putShouldReturnID() {
        assertEquals(0, myCache.put("1"));
    }

    @Test
    public void putShouldIncrementID() {
        myCache.put("1");
        assertEquals(1, myCache.put("123"));
    }

    @Test
    public void putShouldThrowAnExceptionIfTheCacheIsClosed() throws CachePersistenceException {
        myCache.close();
        try {
            myCache.put("1");
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void getShouldReturnTheObjectStoredInMemory() {
        Serializable item = "Hello";
        long id = myCache.put(item);
        assertSame(item, myCache.get(id));
    }

    @Test
    public void getShouldReturnNullIfTheIdDoesNotExist() {
        assertNull(myCache.get(0));
    }


    @Test
    public void clearShouldWipeAllStoredObjects() {
        myCache.put("1");
        myCache.put("2");
        myCache.clear();
        assertNull(myCache.get(0));
        assertNull(myCache.get(1));
    }

    @Test
    public void clearShouldResetIdCounter() {
        myCache.put("1");
        myCache.put("2");
        myCache.clear();
        assertEquals(0, myCache.put("3"));
    }

    @Test
    public void removeShouldRemoveTheObjectFromTheCache() {
        myCache.put("123");
        long idToRemove = myCache.put("234");
        myCache.put("345");
        myCache.remove(idToRemove);
        assertFalse(myCache.containsKey(idToRemove));
    }

    @Test
    public void removeShouldNotReResetTheIdCounter() {
        myCache.put("123");
        long idToRemove = myCache.put("234");
        long lastId = myCache.put("345");
        myCache.remove(idToRemove);
        long nextId = myCache.put("456");
        assertEquals(lastId + 1, nextId);
    }

    @Test
    public void closeShouldNotThrowAnExceptionIfUsedTwice() {
        try {
            myCache.close();
            myCache.close();
        } catch (Throwable throwable) {
            fail();
        }
    }

    @Test
    public void containsKeyShouldReturnTrueIfTheKeyExists() {
        long id = myCache.put("1");
        assertTrue(myCache.containsKey(id));
    }

    @Test
    public void containsKeyShouldReturnFalseIfTheKeyDoesNotExist() {
        long id = 0;
        assertFalse(myCache.containsKey(id));
    }
}