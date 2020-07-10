package de.mas.wiiu.jnus.utils;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import lombok.val;

public class IVCache {
    private final Map<Long, byte[]> cache = new TreeMap<>();

    public IVCache(long first, byte[] IV) {
        if (!addForOffset(first, IV)) {
            throw new IllegalArgumentException("IV was null or not 16 bytes big");
        }
    }

    public boolean addForOffset(long offset, byte[] IV) {
        if (IV == null || IV.length != 16) {
            return false;
        }

        cache.put(offset, IV);
        return true;
    }

    public Optional<Pair<Long, byte[]>> getNearestForOffset(long offset) {
        Optional<Pair<Long, byte[]>> result = Optional.empty();
        for (val e : cache.entrySet()) {
            if (e.getKey().longValue() <= offset) {
                result = Optional.of(new Pair<>(e.getKey(), e.getValue()));
            } else {
                break;
            }
        }
        return result;
    }
}
