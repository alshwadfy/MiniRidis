package com.miniRedis.store;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.miniRedis.store.Entry;

public class Store {
    private final Map<String, Entry> storage = new ConcurrentHashMap<>();


 public void set(String key, String value){
   
    if(key==null || value==null){
      throw new RuntimeException("Key and value cannot be null");
    }
    storage.put(key, new Entry(value));
  }
    public String get(String key){
    
    Entry entry=storage.get(key);
    if(entry==null){
      return null;
    }
    return entry.value();
  }
  public boolean delete(String key){
    if(key==null){
      throw new RuntimeException("Key cannot be null");
    }
    
    return storage.remove(key)!=null;
  }
  public boolean exists(String key){

return storage.containsKey(key);

  }


}