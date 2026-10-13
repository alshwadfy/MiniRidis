package com.miniRedis.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Store {
  private final Map<String, Entry> storage = new ConcurrentHashMap<>();

  public void set(String key, String value, Long expiresAtMillis) {

    if (key == null || value == null) {
      throw new RuntimeException("Key and value cannot be null");
    }
    storage.put(key, new Entry(value,expiresAtMillis));
  }

  public String get(String key) {
    Entry entry = storage.get(key);
    if (entry == null) {
      return null;
    }
    if (isExpired(entry)) {
      storage.remove(key);
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
    return get(key) != null;
  }

  public long incr(String key) {
    Entry newEntry = storage.compute(key, (k, oldEntry) -> {
      if (oldEntry == null || isExpired(oldEntry)) {
        return new Entry("1");
      }
      long value = Long.parseLong(oldEntry.value());
      return new Entry(String.valueOf(value + 1), oldEntry.expiresAtMillis());
    });
    return Long.parseLong(newEntry.value());
  }

  private boolean isExpired(Entry entry) {
    return entry.expiresAtMillis() != null && System.currentTimeMillis() > entry.expiresAtMillis();
  }
  public boolean expire(String key, long seconds){
    Entry entry=storage.get(key);
    if (entry == null || isExpired(entry)) {
      storage.remove(key);
      return false;
    }
    long expiresAtMillis = System.currentTimeMillis() + (seconds * 1000);
    storage.computeIfPresent(key, (k, oldEntry) -> new Entry(oldEntry.value(), expiresAtMillis));
    return true;
  }

  public long ttl(String key){
        Entry entry = storage.get(key);
        if(entry==null || isExpired(entry)){
           storage.remove(key);
          return -2;
        }
        if(entry.expiresAtMillis()==null){
          return -1;
        }
        long ttl=entry.expiresAtMillis()-System.currentTimeMillis();
        return ttl/1000;


  }

  public void sweepExpired(){
    
    storage.forEach((key,entry)->{
        if(isExpired(entry)){
            storage.remove(key,entry);
        }
    }); 
  }


}