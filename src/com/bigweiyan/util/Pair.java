package com.bigweiyan.util;

public class Pair<K,V> {
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }
    private K key;
    public K getKey() {
        return key;
    }
    private V value;
    public V getValue() {
        return value;
    }
}