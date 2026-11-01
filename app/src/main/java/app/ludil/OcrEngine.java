package app.ludil;

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
        return read(bitmap,false);
    }
    public synchronized Reading read(Bitmap bitmap,boolean menu) {
        api.setPageSegMode(menu?TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT:TessBaseAPI.PageSegMode.PSM_AUTO);
        Bitmap normalized=menu?normalizeMenu(bitmap):null;
        try {
            api.setImage(normalized==null?bitmap:normalized);
            String text=api.getUTF8Text();int confidence=api.meanConfidence();
            return new Reading(menu?MenuTextTranslator.prepare(text):SubtitleGate.normalize(text),confidence);
        } finally {api.clear();if(normalized!=null)normalized.recycle();}
    }
    private static Bitmap normalizeMenu(Bitmap bitmap){
        int width=bitmap.getWidth(),height=bitmap.getHeight();int[] pixels=new int[width*height];bitmap.getPixels(pixels,0,width,0,0,width,height);byte[] rgb=new byte[pixels.length*3];
        for(int i=0;i<pixels.length;i++){rgb[i*3]=(byte)(pixels[i]>>16);rgb[i*3+1]=(byte)(pixels[i]>>8);rgb[i*3+2]=(byte)pixels[i];}
        byte[] gray=MenuContrast.normalizeRgb(rgb,width,height);for(int i=0;i<pixels.length;i++){int value=gray[i]&255;pixels[i]=0xff000000|(value<<16)|(value<<8)|value;}
        return Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888);
    }
    @Override public synchronized void close() { api.recycle(); }
}
