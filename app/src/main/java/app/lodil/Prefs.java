package app.lodil;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private final SharedPreferences p;
    public Prefs(Context c) { p = c.getSharedPreferences("lodil_settings", Context.MODE_PRIVATE); }
    public int integer(String key, int fallback) { return p.getInt(key, fallback); }
    public boolean bool(String key, boolean fallback) { return p.getBoolean(key, fallback); }
    public String text(String key, String fallback) { return p.getString(key, fallback); }
    public void put(String key, int value) { p.edit().putInt(key, value).apply(); }
    public void put(String key, boolean value) { p.edit().putBoolean(key, value).apply(); }
    public void put(String key, String value) { p.edit().putString(key, value).apply(); }
}
