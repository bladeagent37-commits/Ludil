package app.lodil;

import android.content.Context;
import android.graphics.Bitmap;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.*;

public final class OcrEngine implements AutoCloseable {
    public static final class Reading {
        public final String text; public final int confidence;
        Reading(String text, int confidence) { this.text = text; this.confidence = confidence; }
    }
    private final TessBaseAPI api;
    public OcrEngine(Context context) throws IOException {
        File base = new File(context.getFilesDir(), "ocr");
        File dir = new File(base, "tessdata");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("OCR klasörü açılamadı.");
        File model = new File(dir, "eng.traineddata");
        if (!model.exists() || model.length() != 4113088) {
            File tmp = new File(dir, "eng.tmp");
            try (InputStream in = context.getAssets().open("eng.traineddata"); OutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[32768]; int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            if (!tmp.renameTo(model)) throw new IOException("OCR modeli kurulamadı.");
        }
        api = new TessBaseAPI();
        if (!api.init(base.getAbsolutePath(), "eng")) { api.recycle(); throw new IOException("İngilizce OCR modeli açılamadı."); }
        api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        api.setVariable("preserve_interword_spaces", "1");
    }
    public synchronized Reading read(Bitmap bitmap) {
        api.setImage(bitmap);
        String text = api.getUTF8Text();
        int confidence = api.meanConfidence();
        api.clear();
        return new Reading(SubtitleGate.normalize(text), confidence);
    }
    @Override public synchronized void close() { api.recycle(); }
}
