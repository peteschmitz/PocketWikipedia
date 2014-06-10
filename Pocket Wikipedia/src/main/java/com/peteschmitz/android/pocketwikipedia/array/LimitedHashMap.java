package com.peteschmitz.android.pocketwikipedia.array;

import java.util.LinkedHashMap;

/**
 * Created by Pete Schmitz on 4/7/2014.
 */
public class LimitedHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int mMaxSize;

    public LimitedHashMap(int maxSize){
        mMaxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > mMaxSize;
    }
}
