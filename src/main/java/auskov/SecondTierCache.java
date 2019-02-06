package auskov;

import java.io.*;
import java.util.Arrays;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.function.LongSupplier;
import java.util.function.ToLongFunction;

public class SecondTierCache implements CacheTier {
    //todo add logging, make 'put' atomic

    private static final String VALUE_FILE_SUFFIX = ".value";
    private static final String WEIGHT_FILE_SUFFIX = ".weight";
    private static final String DEADLINE_FILE_SUFFIX = ".deadline";

    private boolean open;
    private long maxInMemoryBytes;
    private long currentCacheSizeBytes;
    private LongSupplier timeSupplier;
    private ToLongFunction<File> fileLengthEvaluator;
    private String storagePath;
    private File storageDir;

    SecondTierCache(Properties props) throws InvalidPropertiesFormatException {
        maxInMemoryBytes = Long.parseLong(props.getProperty("cache.size.filesystem.bytes"));
        if (maxInMemoryBytes <= 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }

        open = true;
        timeSupplier = System::currentTimeMillis;
        fileLengthEvaluator = (file -> file.length());
        storagePath = props.getProperty("cache.filesystem.storage.path") + "/second_tier_cache/" + Thread.currentThread().getId() + "_" + timeSupplier.getAsLong(); //todo handle crashes
        storageDir = new File(storagePath);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        } else if (!storageDir.isDirectory()) {
            throw new InvalidPropertiesFormatException("Cache path " + storagePath + " is not a directory!");
        }
        currentCacheSizeBytes = 0;
    }

    @Override
    public void put(long key, Serializable object) {
        checkStateIsOpen();
        writeObjectToFile(object, key + VALUE_FILE_SUFFIX);
        writeLongToFile(0L, key + WEIGHT_FILE_SUFFIX);
        writeLongToFile(Long.MAX_VALUE, key + DEADLINE_FILE_SUFFIX);
        long currentEntrySize = getEntrySize(key);
        if (currentEntrySize > maxInMemoryBytes) {
            remove(key);
        }
        currentCacheSizeBytes += currentEntrySize;

        while (currentCacheSizeBytes > maxInMemoryBytes) {
            removeAllExpiredEntries();
            if (currentCacheSizeBytes > maxInMemoryBytes) {
                evictTheColdestEntry();
            }
        }
    }

    private void removeAllExpiredEntries() {
        Arrays.stream(storageDir.list())
                .filter(fileName -> fileName.contains(VALUE_FILE_SUFFIX))
                .map(fileName -> fileName.substring(0, fileName.indexOf(VALUE_FILE_SUFFIX)))
                .mapToLong(foundStringKey -> Long.parseLong(foundStringKey))
                .forEach(foundKey -> {
                    if (getDeadline(foundKey) <= timeSupplier.getAsLong()) {
                        remove(foundKey);
                    }
                });
    }

    private void evictTheColdestEntry() {
        remove(Arrays.stream(storageDir.list())
                .filter(fileName -> fileName.contains(VALUE_FILE_SUFFIX))
                .map(fileName -> fileName.substring(0, fileName.indexOf(VALUE_FILE_SUFFIX)))
                .mapToLong(foundStringKey -> Long.parseLong(foundStringKey))
                .min()
                .getAsLong());
    }

    @Override
    public Object get(long key) {
        checkStateIsOpen();
        if (!containsKey(key)) {
            return null;
        }
        if (timeSupplier.getAsLong() >= getDeadline(key)) {
            remove(key);
            return null;
        }
        incrementWeight(key);
        return readObjectFromFile(key + VALUE_FILE_SUFFIX);
    }

    @Override
    public void clear() {
        checkStateIsOpen();
        if (storageDir.exists() && storageDir.isDirectory()) {
            Arrays.stream(storageDir.list())
                    .filter(fileName -> !fileName.startsWith("."))
                    .forEach(fileName -> {
                        File f = new File(storageDir, fileName);
                        f.delete();
                    });
            currentCacheSizeBytes = 0;
        }
    }

    @Override
    public void remove(long key) {
        checkStateIsOpen();
        long entrySize = getEntrySize(key);
        if (storageDir.exists() && storageDir.isDirectory()) {
            Arrays.stream(storageDir.list())
                    .filter(fileName -> fileName.startsWith("" + key))
                    .forEach(fileName -> {
                        File f = new File(storageDir, fileName);
                        f.delete();
                    });
            currentCacheSizeBytes -= entrySize;
        }
    }

    @Override
    public void close() {
        checkStateIsOpen();
        clear();
        storageDir.delete();
        open = false;
    }

    @Override
    public boolean containsKey(long key) {
        checkStateIsOpen();
        File valueFile = new File(storageDir, key + VALUE_FILE_SUFFIX);
        return valueFile.exists();
    }

    @Override
    public void incrementWeight(long key) {
        checkStateIsOpen();
        if (containsKey(key)) {
            long currentWeight = getWeight(key);
            writeLongToFile(++currentWeight, key + WEIGHT_FILE_SUFFIX);
        }
    }

    @Override
    public void setWeight(long key, long weight) {
        checkStateIsOpen();
        if (containsKey(key)) {
            writeLongToFile(weight, key + WEIGHT_FILE_SUFFIX);
        }
    }

    @Override
    public long getWeight(long key) {
        checkStateIsOpen();
        return readLongFromFile(key + WEIGHT_FILE_SUFFIX);
    }

    @Override
    public void setDeadline(long key, long millis) {
        checkStateIsOpen();
        if (containsKey(key)) {
            writeLongToFile(millis, key + DEADLINE_FILE_SUFFIX);
        }
    }

    @Override
    public long getDeadline(long key) {
        checkStateIsOpen();
        return readLongFromFile(key + DEADLINE_FILE_SUFFIX);
    }

    public long getEntrySize(long key) {
        checkStateIsOpen();
        if (containsKey(key)) {
            File value = new File(storageDir, key + VALUE_FILE_SUFFIX);
            File weight = new File(storageDir, key + WEIGHT_FILE_SUFFIX);
            File deadline = new File(storageDir, key + DEADLINE_FILE_SUFFIX);
            return fileLengthEvaluator.applyAsLong(value) + fileLengthEvaluator.applyAsLong(weight) + fileLengthEvaluator.applyAsLong(deadline);
        }
        return 0;
    }

    void setCurrentTimeSupplier(LongSupplier timeSupplier) {
        checkStateIsOpen();
        this.timeSupplier = timeSupplier;
    }

    void setFileLenghtEvaluator(ToLongFunction<File> fileLenghtEvaluator) {
        checkStateIsOpen();
        this.fileLengthEvaluator = fileLenghtEvaluator;
    }

    private void checkStateIsOpen() {
        if (!open) {
            throw new IllegalStateException("The cache is closed!");
        }
    }

    private void writeObjectToFile(Serializable object, String fileName) {
        File file = new File(storageDir, fileName);
        try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file))) {
            stream.writeObject(object);
            stream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeLongToFile(long value, String fileName) {
        File file = new File(storageDir, fileName);
        try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file))) {
            stream.writeLong(value);
            stream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Object readObjectFromFile(String fileName) {
        File valueFile = new File(storageDir, fileName);
        Object object = null;
        try (ObjectInputStream valueInputStream = new ObjectInputStream(new FileInputStream(valueFile))) {
            object = valueInputStream.readObject();
        } catch (FileNotFoundException e) {
            //it's ok, we'll just return the default value
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return object;
    }

    private long readLongFromFile(String fileName) {
        File file = new File(storageDir, fileName);
        long value = 0;
        try (ObjectInputStream valueInputStream = new ObjectInputStream(new FileInputStream(file))) {
            value = valueInputStream.readLong();
        } catch (FileNotFoundException e) {
            //it's ok, we'll just return the default value
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }

}