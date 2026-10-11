package com.miniRedis.store;


public record Entry(String value, Long expiresAtMillis) {
    
    public Entry(String value) {
        this(value, null);
    }
}