package com.alkantemirov.openlibrary;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "com.alkantemirov.openlibrary.booksdata";
    private static final String KEY_VALUE = "com.alkantemirov.openlibrary.page-";

    private static PreferencesManager instance;
    private final SharedPreferences pref;

    private PreferencesManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    public static synchronized void initializeInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context);
        }
    }
    public static synchronized PreferencesManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(PreferencesManager.class.getSimpleName() +
                    " is not initialized, call initializeInstance(..).");
        }
        return instance;
    }
    public void setValue(String bookName, long pageNo) {
        pref.edit().putLong(KEY_VALUE + bookName, pageNo).commit();
    }
    public long getValue(String bookName) {
        return pref.getLong(KEY_VALUE + bookName, 0);
    }
    public void remove(String key) {
        pref.edit().remove(key).commit();
    }
    public boolean clear() {
        return pref.edit().clear().commit();
    }
}
