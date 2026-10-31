package app.lodil;

import java.util.Locale;

/** Pure-Java temporal filter. It never invents or translates text. */
public final class SubtitleGate {
    private String candidate = "", emitted = "";
    private int count;
    private long seenAt;
    public static String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
    }
    public String observe(String raw, int confidence, long now) {
        String text = normalize(raw);
        if (now - seenAt > 2200) { candidate = ""; count = 0; emitted = ""; }
        if (text.length() < 3 || text.length() > 600 || confidence < 50 || confidence > 100 || !text.matches(".*[A-Za-z]{2}.*")) {
            candidate = ""; emitted = ""; count = 0; return null;
        }
        String key = text.toLowerCase(Locale.ROOT);
        if (key.equals(candidate)) count++; else { candidate = key; count = 1; }
        seenAt = now;
        if (count >= 2 && !key.equals(emitted)) { emitted = key; return text; }
        return null;
    }
    public void retry() { emitted = ""; }
    public boolean isCurrent(String text, long now) {
        return normalize(text).toLowerCase(Locale.ROOT).equals(candidate) && now - seenAt < 4000;
    }
}
