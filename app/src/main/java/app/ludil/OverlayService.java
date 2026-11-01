package app.ludil;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public final class OverlayService extends Service {
    public static final String START="start", TEST="test", STOP="stop", REFRESH="refresh", SHOW="show", CLEAR="clear";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private WindowManager windows;
    private LinearLayout panel;
    private TextView translated, original;
    private LanReceiver receiver;
    private String mode="",currentSource="",currentText="";
    private int sample=0;
    private boolean menuDisplay;
    private int panelWidthPixels,screenHeightPixels;
    private long captionDeadline=0;
    private final Runnable hide=()->{if(panel!=null)panel.setVisibility(View.GONE);currentText="";currentSource="";captionDeadline=0;RuntimeState.lastTranslation="";RuntimeState.lastSource="";RuntimeState.overlayTop=1f;};
    private final Runnable demo=new Runnable(){@Override public void run(){
        String[] lines={"HDMI TESTİ • Türkçe altyazı burada görünüyor.","HDMI TESTİ • Çok geç olmadan buradan çıkmalıyız.","HDMI TESTİ • Kumandayı normal şekilde kullanabilirsin."};
        show("Sabit test cümlesi • canlı çeviri değil",lines[sample++%lines.length],6000);handler.postDelayed(this,5000);
    }};
    public static Intent intent(Context c,String action){return new Intent(c,OverlayService.class).setAction(action);}
    @Override public void onCreate(){super.onCreate();windows=(WindowManager)getSystemService(WINDOW_SERVICE);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        String action=intent.getAction();
        if(STOP.equals(action)){stopService(new Intent(this,CaptureService.class));stopSelf();return START_NOT_STICKY;}
        if(CLEAR.equals(action)){handler.removeCallbacks(hide);hide.run();return START_NOT_STICKY;}
        if(!Settings.canDrawOverlays(this)){RuntimeState.error="Diğer uygulamaların üzerinde gösterme izni gerekiyor.";stopSelf();return START_NOT_STICKY;}
        if(REFRESH.equals(action)){if(panel!=null){int left=(int)(captionDeadline-SystemClock.elapsedRealtime());removePanel();buildPanel();if(!currentText.isEmpty()&&left>0)show(currentSource,currentText,left);}return START_NOT_STICKY;}
        if(SHOW.equals(action)){if(panel!=null){useMenu(intent.getBooleanExtra("menu",false));show(intent.getStringExtra("source"),intent.getStringExtra("text"),intent.getIntExtra("ttl",4500));}return START_NOT_STICKY;}
        if("capture".equals(action)||START.equals(action)||TEST.equals(action)){
            boolean requested="capture".equals(action)&&intent.getBooleanExtra("menu",false);
            if(menuDisplay!=requested){menuDisplay=requested;removePanel();}
        }
        foreground();
        if(panel==null && !buildPanel())return START_NOT_STICKY;
        if(TEST.equals(action)){
            closeReceiver();handler.removeCallbacks(demo);mode=TEST;RuntimeState.status="HDMI yazı testi açık";handler.post(demo);
        }else if(START.equals(action)){
            handler.removeCallbacks(demo);handler.removeCallbacks(hide);hide.run();mode=START;
            if(receiver==null)try{receiver=new LanReceiver(new LanReceiver.Listener(){
                @Override public void subtitle(String source,String tr,int ttl){handler.post(()->{useMenu(false);show(source,tr,ttl);});}
                @Override public void menuSubtitle(String source,String tr,int ttl){handler.post(()->{useMenu(true);show(source,tr,ttl);});}
                @Override public void clear(){handler.post(hide);}
            });RuntimeState.receiver=true;RuntimeState.status="Bilgisayardan altyazı bekleniyor";RuntimeState.error="";
            }catch(Exception e){RuntimeState.error="Altyazı bağlantısı açılamadı. Durdurup yeniden başlat.";stopSelf();}
        }else if("capture".equals(action)){
            closeReceiver();handler.removeCallbacks(demo);hide.run();mode="capture";RuntimeState.status=menuDisplay?"Menü ve ayarlar okunuyor":"Ekran okuma açık";
        }
        return START_NOT_STICKY;
    }
    private void foreground(){
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel("overlay","Altyazı katmanı",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,1,intent(this,STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,"overlay").setSmallIcon(R.drawable.ic_logo).setContentTitle("Ludil açık")
            .setContentText("Ayarlar için aç • kapatmak için Durdur").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Durdur",stop).build()).build();
        if(Build.VERSION.SDK_INT>=34)startForeground(20,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(20,n);
    }
    private boolean buildPanel(){
        Prefs prefs=new Prefs(this);panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setGravity(Gravity.CENTER);
        GradientDrawable background=new GradientDrawable();background.setColor(Color.argb(prefs.integer("opacity",80)*255/100,0,0,0));background.setCornerRadius(12);
        panel.setBackground(background);panel.setPadding(dp(18),dp(8),dp(18),dp(8));
        translated=new TextView(this);translated.setGravity(Gravity.CENTER);translated.setTextColor(prefs.bool("yellow",false)?0xFFFFE08A:Color.WHITE);
        translated.setTextSize(menuDisplay?20:prefs.integer("font",26));translated.setTypeface(null,Typeface.BOLD);translated.setMaxLines(menuDisplay?64:4);translated.setGravity(menuDisplay?Gravity.START:Gravity.CENTER);translated.setShadowLayer(3,0,1,Color.BLACK);panel.addView(translated);
        original=new TextView(this);original.setGravity(Gravity.CENTER);original.setTextColor(0xFFDAE1EB);original.setTextSize(16);original.setMaxLines(2);panel.addView(original);
        android.util.DisplayMetrics m=new android.util.DisplayMetrics();windows.getDefaultDisplay().getRealMetrics(m);
        panelWidthPixels=(int)(m.widthPixels*(menuDisplay?.32:.84));screenHeightPixels=m.heightPixels;
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(panelWidthPixels,WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        p.gravity=menuDisplay?(Gravity.TOP|(prefs.bool("menu_right",false)?Gravity.LEFT:Gravity.RIGHT)):(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);p.y=menuDisplay?m.heightPixels*6/100:m.heightPixels*prefs.integer("bottom",6)/100;p.x=menuDisplay?m.widthPixels*2/100:0;p.alpha=.8f;
        try{windows.addView(panel,p);panel.setVisibility(View.GONE);RuntimeState.overlay=true;return true;}
        catch(RuntimeException e){RuntimeState.error="TV altyazı penceresine izin vermedi.";panel=null;stopSelf();return false;}
    }
    private void show(String source,String text,int ttl){
        if(panel==null||text==null||text.isEmpty())return;
        currentSource=source==null?"":source;currentText=text;translated.setText(text);original.setText(currentSource);
        if(menuDisplay)fitMenuText(text);
        original.setVisibility(!menuDisplay&&new Prefs(this).bool("original",false)?View.VISIBLE:View.GONE);panel.setVisibility(View.VISIBLE);
        panel.post(()->{if(panel!=null&&panel.getVisibility()==View.VISIBLE){int[] location=new int[2];panel.getLocationOnScreen(location);android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();windows.getDefaultDisplay().getRealMetrics(metrics);RuntimeState.overlayTop=location[1]/(float)Math.max(1,metrics.heightPixels);}});
        RuntimeState.lastSource=currentSource;RuntimeState.lastTranslation=text;if(!TEST.equals(mode))RuntimeState.translatedCount++;
        captionDeadline=SystemClock.elapsedRealtime()+Math.max(500,Math.min(10000,ttl));handler.removeCallbacks(hide);handler.postDelayed(hide,Math.max(500,Math.min(10000,ttl)));
    }
    private void useMenu(boolean value){if(menuDisplay==value)return;menuDisplay=value;removePanel();buildPanel();}
    private void fitMenuText(String text){
        int width=Math.max(40,panelWidthPixels-dp(36)),height=Math.max(80,(int)(screenHeightPixels*.86)-dp(16));
        float scale=getResources().getDisplayMetrics().scaledDensity;float size=20*scale;
        while(size>=10*scale){translated.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,size);android.text.StaticLayout layout=android.text.StaticLayout.Builder.obtain(text,0,text.length(),translated.getPaint(),width).setIncludePad(false).build();if(layout.getHeight()<=height)break;size-=scale;}
        translated.setMaxHeight(height);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void closeReceiver(){if(receiver!=null){receiver.close();receiver=null;}RuntimeState.receiver=false;}
    private void removePanel(){if(panel!=null){try{windows.removeView(panel);}catch(RuntimeException ignored){}panel=null;}RuntimeState.overlay=false;RuntimeState.overlayTop=1f;}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);closeReceiver();removePanel();RuntimeState.status="Durduruldu";RuntimeState.lastTranslation="";RuntimeState.lastSource="";stopService(new Intent(this,CaptureService.class));super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
