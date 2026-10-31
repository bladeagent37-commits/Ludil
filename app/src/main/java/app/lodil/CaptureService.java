package app.lodil;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Captures only after Android's system consent; HDMI pixels are not assumed accessible. */
public final class CaptureService extends Service {
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private HandlerThread frames;
    private Handler frameHandler;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService work=Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy=new AtomicBoolean(false);
    private final AtomicReference<Bitmap> pending=new AtomicReference<>();
    private volatile boolean stopped=false;
    private OcrEngine ocr;
    private TranslationClient translator;
    private final SubtitleGate gate=new SubtitleGate();
    private long nextFrame, lastCaption;
    private int width,height,density,blanks;
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null || projection!=null)return START_NOT_STICKY;
        Intent grant=intent.getParcelableExtra("grant");
        if(grant==null){stopSelf();return START_NOT_STICKY;}
        try{
            NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            n.createNotificationChannel(new NotificationChannel("capture","Ekran okuma",NotificationManager.IMPORTANCE_LOW));
            PendingIntent stop=PendingIntent.getService(this,2,OverlayService.intent(this,OverlayService.STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification notification=new Notification.Builder(this,"capture").setSmallIcon(R.drawable.ic_logo).setContentTitle("Lodil ekranı okuyor")
                .setContentText("Yalnızca seçtiğin görüntü bölgesi işleniyor").setOngoing(true).addAction(new Notification.Action.Builder(null,"Durdur",stop).build()).build();
            if(Build.VERSION.SDK_INT>=29)startForeground(21,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);else startForeground(21,notification);
            frames=new HandlerThread("ocr-frames");frames.start();frameHandler=new Handler(frames.getLooper());
            MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            projection=manager.getMediaProjection(intent.getIntExtra("result",Activity.RESULT_CANCELED),grant);
            projection.registerCallback(new MediaProjection.Callback(){
                @Override public void onStop(){RuntimeState.status="Ekran paylaşımı sonlandırıldı";stopSelf();}
                @Override public void onCapturedContentResize(int w,int h){if(!stopped && display!=null)frameHandler.post(()->resize(w,h));}
            },main);
            DisplayMetrics metrics=new DisplayMetrics();((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(metrics);density=metrics.densityDpi;
            setDimensions(metrics.widthPixels,metrics.heightPixels);
            reader=makeReader();
            display=projection.createVirtualDisplay("Lodil subtitle OCR",width,height,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,frameHandler);
            translator=new TranslationClient(this);
            startForegroundService(OverlayService.intent(this,"capture"));
            RuntimeState.capturing=true;RuntimeState.error="";
        }catch(Exception e){RuntimeState.error="Ekran paylaşımı açılamadı. İzin verip tekrar dene.";stopSelf();}
        return START_NOT_STICKY;
    }
    private void setDimensions(int w,int h){float ratio=Math.min(1f,1280f/Math.max(w,h));width=Math.max(2,Math.round(w*ratio));height=Math.max(2,Math.round(h*ratio));}
    private ImageReader makeReader(){ImageReader r=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2);r.setOnImageAvailableListener(this::frame,frameHandler);return r;}
    private void resize(int w,int h){
        if(w<2||h<2||display==null)return;
        int oldW=width,oldH=height;setDimensions(w,h);if(oldW==width&&oldH==height)return;
        try{ImageReader old=reader;reader=makeReader();display.resize(width,height,density);display.setSurface(reader.getSurface());if(old!=null)old.close();}
        catch(RuntimeException e){RuntimeState.error="Görüntü boyutu değişti. Ekran okumayı yeniden başlat.";main.post(this::stopSelf);}
    }
    private void frame(ImageReader r){
        Image image=null;
        try{
            image=r.acquireLatestImage();if(image==null)return;
            long now=SystemClock.elapsedRealtime();if(stopped||now<nextFrame)return;nextFrame=now+900;
            Image.Plane plane=image.getPlanes()[0];ByteBuffer bytes=plane.getBuffer();int pixel=plane.getPixelStride(),row=plane.getRowStride();
            int w=image.getWidth(),h=image.getHeight();Bitmap padded=Bitmap.createBitmap(row/pixel,h,Bitmap.Config.ARGB_8888);padded.copyPixelsFromBuffer(bytes);
            Prefs prefs=new Prefs(this);int top,bottom;
            // Keep the OCR band above our own caption panel to prevent translation feedback.
            try{int[] band=CaptureGeometry.band(h,prefs.integer("region_top",58),prefs.integer("region_bottom",84),RuntimeState.overlayTop);top=band[0];bottom=band[1];}
            catch(IllegalArgumentException e){padded.recycle();RuntimeState.error="Altyazı konumu okuma bölgesiyle çakışıyor. Yazıyı aşağı taşı veya bölgeyi yükselt.";return;}
            Bitmap crop=Bitmap.createBitmap(padded,w/20,top,w*9/10,bottom-top);padded.recycle();
            submit(crop);
        }catch(Exception e){if(!stopped)RuntimeState.error="Bir görüntü karesi okunamadı.";}
        finally{if(image!=null)image.close();}
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void submit(Bitmap bitmap){
        if(stopped){bitmap.recycle();return;}
        if(busy.compareAndSet(false,true))try{work.execute(()->process(bitmap));}catch(RejectedExecutionException e){bitmap.recycle();busy.set(false);}
        else{Bitmap old=pending.getAndSet(bitmap);if(old!=null)old.recycle();if(stopped){old=pending.getAndSet(null);if(old!=null)old.recycle();}}
    }
    private void clearCaption(){main.post(()->{if(!stopped)startService(OverlayService.intent(this,OverlayService.CLEAR));});}
    private void process(Bitmap bitmap){
        try{
            if(stopped)return;if(ocr==null)ocr=new OcrEngine(this);
            OcrEngine.Reading reading=ocr.read(bitmap);long now=SystemClock.elapsedRealtime();
            String source=gate.observe(reading.text,reading.confidence,now);if(source==null){
                if(reading.text.isEmpty()||reading.confidence<50){if(++blanks>=2){gate.retry();clearCaption();}}
                else{blanks=0;if(!SubtitleGate.normalize(reading.text).equalsIgnoreCase(RuntimeState.lastSource))clearCaption();}
                if(reading.confidence>=50 && !RuntimeState.lastTranslation.isEmpty() && SubtitleGate.normalize(reading.text).equalsIgnoreCase(RuntimeState.lastSource) && now-lastCaption>1500){
                    lastCaption=now;Intent refresh=OverlayService.intent(this,OverlayService.SHOW).putExtra("source",RuntimeState.lastSource).putExtra("text",RuntimeState.lastTranslation).putExtra("ttl",5000);main.post(()->{if(!stopped)startService(refresh);});
                }
                return;
            }
            blanks=0;if(!source.equalsIgnoreCase(RuntimeState.lastSource))clearCaption();
            long begin=SystemClock.elapsedRealtime();String tr=translator.translate(source);
            if(stopped||!gate.isCurrent(source,SystemClock.elapsedRealtime())){gate.retry();return;}
            Bitmap latest=pending.getAndSet(null);
            if(latest!=null)try{OcrEngine.Reading fresh=ocr.read(latest);if(fresh.confidence<50||!SubtitleGate.normalize(fresh.text).equalsIgnoreCase(source)){gate.observe(fresh.text,fresh.confidence,SystemClock.elapsedRealtime());gate.retry();clearCaption();return;}}finally{latest.recycle();}
            lastCaption=SystemClock.elapsedRealtime();
            RuntimeState.lastLatency=SystemClock.elapsedRealtime()-begin;RuntimeState.error="";
            Intent caption=OverlayService.intent(this,OverlayService.SHOW).putExtra("source",source).putExtra("text",tr).putExtra("ttl",5000);
            main.post(()->{if(!stopped)startService(caption);});
        }catch(Exception e){gate.retry();if(!stopped)RuntimeState.error="Çeviri tamamlanamadı; eski sonuç gösterilmedi. Boş alanı ve seçili motoru kontrol et.";}
        finally{bitmap.recycle();busy.set(false);Bitmap next=pending.getAndSet(null);if(next!=null)submit(next);}
    }
    @Override public void onDestroy(){stopped=true;RuntimeState.capturing=false;
        Bitmap queued=pending.getAndSet(null);if(queued!=null)queued.recycle();
        if(display!=null){display.release();display=null;}if(reader!=null){reader.close();reader=null;}
        if(projection!=null){projection.stop();projection=null;}if(frames!=null)frames.quitSafely();
        work.execute(()->{if(ocr!=null){ocr.close();ocr=null;}});work.shutdown();super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
