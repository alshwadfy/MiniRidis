package com.miniRedis.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.miniRedis.store.Entry;

public class Store {
  private final Map<String, Entry> storage = new ConcurrentHashMap<>();

  public void set(String key, String value) {

    if (key == null || value == null) {
      throw new RuntimeException("Key and value cannot be null");
    }
    storage.put(key, new Entry(value));
  }

  public String get(String key) {

    Entry entry = storage.get(key);
    if (entry == null) {
      return null;
    }
    return entry.value();
  }

  public boolean delete(String key) {
    if (key == null) {
      throw new RuntimeException("Key cannot be null");
    }

    return storage.remove(key) != null;
  }

  public boolean exists(String key) {

    return storage.containsKey(key);

  }

 public long incr(String key) {
    Entry newEntry = storage.compute(key, (k, oldEntry) -> {
        if (oldEntry == null) {
            return new Entry("1");
        }
        long value = Long.parseLong(oldEntry.value());
        return new Entry(String.valueOf(value + 1), oldEntry.expiresAtMillis());
    });
    return Long.parseLong(newEntry.value());
}

}