package app.lodil;

/** Pixel crop calculation shared by capture and deterministic host regressions. */
public final class CaptureGeometry {
    private CaptureGeometry(){}
    public static int[] band(int height,int topPercent,int bottomPercent,float overlayTop){
        if(height<2)throw new IllegalArgumentException("Görüntü yüksekliği geçersiz.");
        int top=height*Math.max(0,Math.min(100,topPercent))/100;
        int bottom=height*Math.max(0,Math.min(100,bottomPercent))/100;
        if(Float.isFinite(overlayTop)&&overlayTop<1f)bottom=Math.min(bottom,Math.max(0,(int)(height*overlayTop)-6));
        if(bottom<=top+20)throw new IllegalArgumentException("Altyazı konumu okuma bölgesiyle çakışıyor.");
        return new int[]{top,bottom};
    }
}
