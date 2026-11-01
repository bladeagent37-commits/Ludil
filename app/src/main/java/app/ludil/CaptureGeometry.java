package app.ludil;

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
    /** x,y,width,height for a menu region; the opposite 32% column remains free for translations. */
    public static int[] menuRegion(int width,int height,boolean rightSide){
        if(width<100||height<100)throw new IllegalArgumentException("Görüntü boyutu geçersiz.");
        int left=Math.round(width*(rightSide?.36f:.04f)),top=Math.round(height*.06f);
        return new int[]{left,top,Math.round(width*.60f),Math.round(height*.88f)};
    }
}
