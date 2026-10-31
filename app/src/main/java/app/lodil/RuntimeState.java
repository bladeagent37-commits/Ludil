package app.lodil;

public final class RuntimeState {
    public static volatile float overlayTop=1f;
    private RuntimeState() {}
    public static volatile boolean overlay = false, capturing = false, receiver = false;
    public static volatile String status = "Hazır", pairingCode = "", address = "", lastSource = "", lastTranslation = "";
    public static volatile long translatedCount = 0, lastLatency = 0;
    public static volatile String error = "";
    public static volatile long pairingDeadline = 0;
}
