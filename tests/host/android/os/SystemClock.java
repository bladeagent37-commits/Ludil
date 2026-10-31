package android.os;
/** Host-test substitute only; excluded from Android app sources. */
public final class SystemClock { public static long elapsedRealtime(){return System.nanoTime()/1000000;} }
