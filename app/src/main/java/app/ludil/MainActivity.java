package app.ludil;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final int INK=0xFFF8F4EE, MUTED=0xFFB1AEAD, ACCENT=0xFFFF8B78, BG=0xFF16171B, SURFACE=0xFF222329;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Prefs prefs;
    private LinearLayout content;
    private final Button[] navigation=new Button[4];
    private TextView liveStatus, pairStatus, detailsStatus;
    private Button stopButton;
    private boolean wide;
    private int page=0;
    private boolean demoAfterGrant=false;
    private boolean alive=true;
    private final Runnable tick=new Runnable(){@Override public void run(){refreshStatus();handler.postDelayed(this,1000);}};
    @Override public void onCreate(Bundle state){super.onCreate(state);prefs=new Prefs(this);
        getWindow().getDecorView().setSystemUiVisibility(5894);buildShell();showPage(0);handler.post(tick);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},44);
    }
    private void buildShell(){
        wide=getResources().getConfiguration().screenWidthDp>=760;
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);shell.setBackgroundColor(BG);shell.setPadding(dp(wide?26:16),dp(wide?20:14),dp(wide?26:16),dp(12));setContentView(shell);
        LinearLayout rail=new LinearLayout(this);rail.setOrientation(LinearLayout.VERTICAL);rail.setPadding(0,0,dp(wide?20:0),0);shell.addView(rail,new LinearLayout.LayoutParams(wide?dp(182):-1,wide?-1:-2));
        TextView brand=label("Ludil",30,INK);brand.setTypeface(null,Typeface.BOLD);brand.setCompoundDrawablesWithIntrinsicBounds(getDrawable(R.drawable.ic_logo),null,null,null);brand.setCompoundDrawablePadding(dp(10));rail.addView(brand);
        if(wide){rail.addView(label("Oyunu kendi dilinde yaşa.",12,MUTED));spacer(rail,28);}
        LinearLayout menu=new LinearLayout(this);menu.setOrientation(wide?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        if(wide)rail.addView(menu);else {HorizontalScrollView strip=new HorizontalScrollView(this);strip.setHorizontalScrollBarEnabled(false);strip.addView(menu);rail.addView(strip);}
        String[] names={"Çeviri","Görünüm","Bağlantı","Yardım"};
        for(int i=0;i<names.length;i++){final int which=i;Button b=button(names[i],()->showPage(which));navigation[i]=b;b.setCompoundDrawablesWithIntrinsicBounds(new NavGlyph(i,dp(20),ACCENT),null,null,null);b.setCompoundDrawablePadding(dp(10));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(wide?-1:dp(131),dp(52));lp.bottomMargin=dp(wide?8:0);lp.rightMargin=dp(wide?0:6);menu.addView(b,lp);}
        if(wide){rail.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));rail.addView(label("EN → TR · Çevrimdışı",12,ACCENT));rail.addView(label("0.4.0 · Önizleme",12,MUTED));}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setPadding(dp(wide?16:0),dp(wide?0:16),0,0);shell.addView(scroll,new LinearLayout.LayoutParams(wide?0:-1,wide?-1:0,1));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,dp(4),dp(8),dp(24));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
    }
    private void showPage(int number){stopButton=null;int restore=(page==number)?content.indexOfChild(getCurrentFocus()):-1;page=number;content.removeAllViews();liveStatus=null;pairStatus=null;detailsStatus=null;
        String[] titles={"Oyunu kendi dilinde yaşa.","Senin ekranın, senin altyazın.","Ev ağında bir araya gelsin.","Ne çalışıyor, ne bekliyor?"};
        TextView kicker=label("LUDIL  /  "+new String[]{"ÇEVİRİ","GÖRÜNÜM","BAĞLANTI","YARDIM"}[number],12,ACCENT);content.addView(kicker);
        TextView title=label(titles[number],28,INK);title.setTypeface(null,Typeface.BOLD);content.addView(title);spacer(content,12);
        if(number==0)home();else if(number==1)appearance();else if(number==2)connection();else help();
        refreshStatus();
        for(int i=0;i<navigation.length;i++)navigation[i].setTextColor(i==number?ACCENT:INK);
        if(restore>=0&&restore<content.getChildCount())content.getChildAt(restore).requestFocus();
    }
    private void home(){
        paragraph("İngilizce oyun altyazılarını Türkçe takip et. Hesap, reklam veya ücretli API yok; çeviri motoru cihazında.");
        card("1  Oyunun İngilizce altyazısını aç.\n2  Çeviri kaynağını seç ve izin ver.\n3  Oyununa dön, Türkçeyi takip et.");
        liveStatus=card("Hazır");
        Button start=button("Çeviriyi başlat",this::chooseMode,true);content.addView(start,new LinearLayout.LayoutParams(-1,dp(60)));spacer(content,10);
        LinearLayout row=new LinearLayout(this);content.addView(row);
        Button test=button("HDMI yazı testi",()->startOverlay(OverlayService.TEST));row.addView(test,new LinearLayout.LayoutParams(0,dp(58),1));
        stopButton=button("Durdur",this::stopAll);LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,dp(58),1);right.leftMargin=dp(10);row.addView(stopButton,right);
        spacer(content,16);content.addView(label("ÖNCE DENE",12,ACCENT));
        action("İngilizce bir cümleyi Türkçeye çevir",this::translateTyped);
        action("Örnek görüntüde oku ve Türkçeye çevir",this::sampleOcr);
        action("Menü çevirisini örnek metinle dene",()->{try(java.io.InputStream in=getAssets().open("menu-terms.json")){MenuTextTranslator menus=new MenuTextTranslator(in);notice("Menü çevirisi",menus.translate("New Game\nContinue\nSettings\nGraphics\nMouse Sensitivity: 50",line->line));}catch(Exception e){notice("Açılamadı","Menü sözlüğü yüklenemedi.");}});
        action("Kendi görüntümü seç ve oku",()->{try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,51);}catch(ActivityNotFoundException e){notice("Dosya seçici bulunamadı", "TV’deki dosya yöneticisi bu işlemi desteklemiyor. Dahili OCR testini kullanabilirsin.");}});
        paragraph("HDMI kullanacaksan önce yazı testini dene. HDMI görüntüsü Android ekran paylaşımına her cihazda dahil olmaz; yardım bölümündeki bağlantı seçeneklerini incele.");
    }
    private void chooseMode(){new AlertDialog.Builder(this).setTitle("Çeviri kaynağını seç")
        .setItems(new String[]{"Bilgisayardan canlı Türkçe altyazı al","Android ekranını oku ve çevir","Örnek sahnede ekran okumayı dene","Oyun menülerini ve ayarları çevir"},(d,which)->{
            if(which==0)startOverlay(OverlayService.START);else requestCapture(which==2,which==3);
        }).setNegativeButton("Vazgeç",null).show();}
    private boolean overlayAllowed(){if(Settings.canDrawOverlays(this))return true;
        new AlertDialog.Builder(this).setTitle("Altyazıyı diğer ekranlarda göster")
            .setMessage("Ludil için ‘Diğer uygulamaların üzerinde göster’ iznini aç. Sonra uygulamaya dönüp aynı düğmeye bas. Yazı kumandanın odağını almaz.")
            .setPositiveButton("İzin ekranını aç",(d,w)->openOverlaySettings()).setNegativeButton("Şimdi değil",null).show();return false;}
    private void openOverlaySettings(){try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}
        catch(ActivityNotFoundException e){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(ActivityNotFoundException ignored){notice("İzin ekranı açılamadı","TV Ayarları → Uygulamalar → Özel uygulama erişimi → Diğer uygulamaların üzerinde göster yolundan Ludil’yı aç.");}}}
    private void startOverlay(String mode){if(!overlayAllowed())return;
        if(RuntimeState.capturing)stopService(new Intent(this,CaptureService.class));
        try{startForegroundService(OverlayService.intent(this,mode));handler.postDelayed(this::refreshStatus,400);
            if(OverlayService.START.equals(mode)){showPage(2);}else notice("HDMI yazı testi başladı","Ekranın altında test yazısı görünecek. TV kumandasından HDMI kaynağına geç. Yazı HDMI görüntüsünün üzerinde de kalıyor mu kontrol et. Çeviri değil, uyumluluk testidir. Durdurmak için Ludil’ya dön.");
        }catch(RuntimeException e){notice("Başlatılamadı","TV altyazı hizmetini başlatamadı. İzinleri kontrol edip yeniden dene.");}}
    private void requestCapture(boolean sample,boolean menu){
        if(RuntimeState.capturing){notice("Ekran okuma zaten açık","Yeni oturum için önce Durdur’a bas.");return;}
        if(!overlayAllowed())return;prefs.put("menu_mode",menu);demoAfterGrant=sample;
        try{MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(m.createScreenCaptureIntent(),50);}
        catch(RuntimeException e){notice("Ekran okuma desteklenmiyor","Bu TV ekran paylaşma izni sunmuyor. Bilgisayardan altyazı alma modunu kullanabilirsin.");}}
    private void stopAll(){stopService(new Intent(this,CaptureService.class));stopService(new Intent(this,OverlayService.class));handler.postDelayed(this::refreshStatus,400);}
    private void appearance(){
        action("Menü okuma alanı: "+(prefs.bool("menu_right",false)?"Sağ taraf":"Sol ve orta"),()->{if(RuntimeState.capturing){notice("Önce durdur","Okuma alanını değiştirmeden önce ekran okumayı durdur.");return;}prefs.put("menu_right",!prefs.bool("menu_right",false));changed();});
        paragraph("Seçimlerin kaydedilir. Menü modunda sol/orta veya sağ bölge okunur; çeviri karşı kenarda gösterilir. Yazı katmanı kumandanın ve dokunmanın odağını almaz.");
        action("Yazı boyutu: "+prefs.integer("font",26),()->select("Yazı boyutu",new String[]{"Küçük · 22","Orta · 26","Büyük · 32","Çok büyük · 38"},new int[]{22,26,32,38},"font"));
        action("Alttan uzaklık: %"+prefs.integer("bottom",6),()->select("Altyazının konumu",new String[]{"En alt · %3","Alt · %6","Biraz yukarı · %12","Orta alt · %20"},new int[]{3,6,12,20},"bottom"));
        action("Arka plan koyuluğu: %"+prefs.integer("opacity",80),()->select("Arka plan",new String[]{"Hafif · %35","Dengeli · %60","Koyu · %80","Tam siyah · %100"},new int[]{35,60,80,100},"opacity"));
        action("Yazı rengi: "+(prefs.bool("yellow",false)?"Sarı":"Beyaz"),()->{prefs.put("yellow",!prefs.bool("yellow",false));changed();});
        action("İngilizce aslını da göster: "+(prefs.bool("original",false)?"Açık":"Kapalı"),()->{prefs.put("original",!prefs.bool("original",false));changed();});
        action("Okuma bölgesi: ekranın %"+prefs.integer("region_top",58)+"–%"+prefs.integer("region_bottom",84)+" arası",()->new AlertDialog.Builder(this).setTitle("İngilizce altyazı nerede?").setItems(new String[]{"Standart alt bölge · %58–84","Daha yukarı · %42–68","Dar alt bölge · %64–84"},(d,w)->{int[] t={58,42,64},b={84,68,84};prefs.put("region_top",t[w]);prefs.put("region_bottom",b[w]);changed();}).show());
        TextView preview=card("Çok geç olmadan buradan çıkmalıyız.");preview.setTextSize(prefs.integer("font",26));preview.setTextColor(prefs.bool("yellow",false)?0xFFFFE08A:INK);
    }
    private void select(String title,String[] labels,int[] values,String key){new AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,w)->{prefs.put(key,values[w]);changed();}).show();}
    private void changed(){if(RuntimeState.overlay)startService(OverlayService.intent(this,OverlayService.REFRESH));showPage(page);}
    private void connection(){
        paragraph("TV ve bilgisayar aynı ev ağına bağlı olmalı. Çeviri ve görüntü işleme bilgisayarında yapılabilir; PSN şifresi bu uygulamaya girilmez.");
        pairStatus=card("");
        action("Bilgisayardan altyazı almaya başla",()->startOverlay(OverlayService.START));
        action("Yeni eşleştirme kodu oluştur",()->{stopAll();handler.postDelayed(()->startOverlay(OverlayService.START),500);});
        spacer(content,12);content.addView(label("İSTEĞE BAĞLI BİLGİSAYAR ÇEVİRİ SUNUCUSU",12,ACCENT));
        action("Çeviri sunucusu: "+prefs.text("server","Henüz ayarlanmadı"),()->editServer());
        action("Sunucu anahtarı: "+(prefs.text("server_token","").isEmpty()?"Girilmedi":"Kaydedildi"),()->editToken());
        paragraph("Varsayılan çeviri motoru APK’nın içindedir ve çevrimdışı çalışır. Bu alanları boş bırakabilirsin. Bilgisayarda daha hızlı çeviri istersen yardımcının server modunu kullan. Anahtar bu cihazda saklanır; yardımcı kapanınca geçersiz olur.");
    }
    private void editServer(){EditText input=entry("http://192.168.1.20:8766",prefs.text("server",""),false);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Yerel çeviri sunucusu").setView(input).setPositiveButton("Kaydet",null).setNegativeButton("Vazgeç",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String value=input.getText().toString().trim();if(value.isEmpty()){prefs.put("server","");dialog.dismiss();showPage(2);return;}
            worker.execute(()->{try{TranslationClient.privateUrl(value);handler.post(()->{if(alive){prefs.put("server",value);dialog.dismiss();showPage(2);}});}catch(Exception e){handler.post(()->input.setError("Ev ağı adresi gir: http://192.168.1.20:8766"));}});
        }));dialog.show();}
    private void editToken(){EditText input=entry("Sunucunun gösterdiği anahtar",prefs.text("server_token",""),true);
        new AlertDialog.Builder(this).setTitle("Yerel sunucu anahtarı").setView(input).setPositiveButton("Kaydet",(d,w)->{prefs.put("server_token",input.getText().toString().trim());showPage(2);}).setNegativeButton("Vazgeç",null).show();}
    private EditText entry(String hint,String value,boolean password){EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(INK);input.setHintTextColor(MUTED);input.setHint(hint);input.setText(value);input.setPadding(dp(20),dp(15),dp(20),dp(15));input.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);return input;}
    private void help(){
        detailsStatus=card("");
        paragraph("1. HDMI yazı testine izin verip başlat. Kumandadan HDMI girişine geç. Test yazısı orada da görünmeli.\n\n2. Bilgisayar yardımcısında İngilizce altyazının olduğu ekran bölgesini seç; TV adresi ve geçici kod ile eşleştir.\n\n3. Yardımcı, gördüğü İngilizce metni bilgisayarda Türkçeye çevirip TV’ye gönderir. Eski yazılar süre dolunca kaybolur.");
        paragraph("Android ekran okuma modu sadece sistemin paylaşılmasına izin verdiği görüntüyü okuyabilir. HDMI veya korumalı görüntüye erişim garantisi vermez. Remote Play bağlantısı ve TCL HDMI katmanı gerçek cihazda henüz sınanmadı.");
        action("Açık kaynak lisansları",this::licenses);action("Üzerinde gösterme izin ekranı",this::openOverlaySettings);action("Tüm işlemleri durdur",this::stopAll);
        paragraph("Gizlilik: Hesap, reklam, mikrofon, kamera veya erişilebilirlik hizmeti yok. Görüntüler kaydedilmez. Varsayılan modda İngilizce metin ve görüntüler cihazdan çıkmaz. İsteğe bağlı sunucu seçersen sadece İngilizce metin o ev ağı cihazına gider. Eşleştirme kodu beş dakika geçerlidir.\n\nAçık kaynak: Tesseract4Android 4.9.0 / Tesseract 5.5.1 (Apache-2.0), tessdata_fast İngilizce modeli (Apache-2.0), AndroidX Annotation (Apache-2.0). ONNX Runtime 1.22.0 (MIT), Helsinki-NLP/opus-mt-en-trk (Apache-2.0; hedef sadece Türkçe). Diğer lisans ayrıntıları kaynak paketindedir.");
    }
    private void licenses(){try{String[] files=getAssets().list("licenses");if(files==null)return;
        new AlertDialog.Builder(this).setTitle("Açık kaynak lisansları").setItems(files,(d,w)->{try(InputStream in=getAssets().open("licenses/"+files[w])){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);notice(files[w],out.toString("UTF-8"));}catch(IOException e){notice("Açılamadı","Lisans metni okunamadı.");}}).setNegativeButton("Kapat",null).show();
    }catch(IOException e){notice("Açılamadı","Lisans listesi okunamadı.");}}
    private void refreshStatus(){
        if(stopButton!=null)stopButton.setEnabled(RuntimeState.overlay||RuntimeState.capturing);
        if(liveStatus!=null)liveStatus.setText((RuntimeState.overlay?"● ":"○ ")+RuntimeState.status+(RuntimeState.error.isEmpty()?"":"\n"+RuntimeState.error));
        if(pairStatus!=null){long left=Math.max(0,(RuntimeState.pairingDeadline-SystemClock.elapsedRealtime())/1000);
            pairStatus.setText(RuntimeState.receiver?"TV ADRESİ\n"+RuntimeState.address+"\n\nEŞLEŞTİRME KODU  "+RuntimeState.pairingCode+(left>0&&!RuntimeState.pairingCode.equals("Eşleştirildi")?"  ·  "+left+" sn":"")+(left==0&&!RuntimeState.pairingCode.equals("Eşleştirildi")?"\nKodun süresi doldu. Yeni kod oluştur.":""):"Altyazı alıcısı kapalı.\nBaşlatınca TV adresi ve eşleştirme kodu burada görünür.");}
        if(detailsStatus!=null)detailsStatus.setText("CİHAZ  "+Build.MANUFACTURER+" "+Build.MODEL+"  ·  Android "+Build.VERSION.RELEASE+"\nÜzerinde gösterme izni  "+(Settings.canDrawOverlays(this)?"Açık":"Kapalı")+"\nEkran okuma  "+(RuntimeState.capturing?"Açık":"Kapalı")+"\nGösterilen altyazı  "+RuntimeState.translatedCount+"\nSon çeviri süresi  "+RuntimeState.lastLatency+" ms"+(RuntimeState.error.isEmpty()?"":"\n"+RuntimeState.error));
    }
    private void translateTyped(){EditText input=entry("İngilizce altyazı", "We need to get out of here.",false);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(this).setTitle("İngilizce → Türkçe").setView(input).setPositiveButton("Türkçeye çevir",(d,w)->{
            String source=input.getText().toString().trim();if(source.isEmpty())return;
            ProgressDialog progress=ProgressDialog.show(this,"Türkçeye çevriliyor","İlk işlemde yerel model hazırlanır. Biraz sürebilir…",true,false);
            worker.execute(()->{String result;try{result=source+"\n\n"+new TranslationClient(this).translate(source);}catch(Exception|LinkageError e){result="Çeviri tamamlanamadı. İlk kurulum için boş alanı kontrol et; daha kısa bir İngilizce cümle dene.";}
                final String value=result;handler.post(()->{if(alive){progress.dismiss();notice("İngilizce → Türkçe",value);}});
            });
        }).setNegativeButton("Vazgeç",null).show();}
    private void sampleOcr(){Bitmap bitmap=Bitmap.createBitmap(1280,200,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(0xFF141A23);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.WHITE);p.setTextSize(37);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));canvas.drawText("We have to leave before it is too late.",52,110,p);runOcr(bitmap);}
    private void runOcr(Bitmap bitmap){ProgressDialog progress=ProgressDialog.show(this,"Metin okunuyor","İngilizce metin okunup Türkçeye çevriliyor. İlk işlem daha uzun sürebilir…",true,false);
        worker.execute(()->{String title="OCR sonucu",result;try(OcrEngine ocr=new OcrEngine(getApplicationContext())){OcrEngine.Reading reading=ocr.read(bitmap);
            result=reading.text.isEmpty()?"Okunabilir İngilizce metin bulunamadı.":"OKUNAN İNGİLİZCE\n"+reading.text+"\n\nOCR güveni: %"+reading.confidence;
            if(!reading.text.isEmpty()&&reading.confidence>=50){try{result+="\n\nTÜRKÇE\n"+new TranslationClient(this).translate(reading.text);}catch(Exception e){result+="\n\nÇeviri tamamlanamadı. İlk kurulumda en az 1 GB boş alan önerilir.";}}
            else result+="\n\nGörüntü yeterince net olmadığı için çeviri üretilmedi.";
        }catch(Exception|LinkageError e){title="Metin okunamadı";result="OCR modeli başlatılamadı. Uygulamayı yeniden açıp tekrar dene.";}finally{bitmap.recycle();}
            final String head=title,body=result;handler.post(()->{if(alive){progress.dismiss();notice(head,body);}});
        });}
    @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==50 && result==RESULT_OK && data!=null){try{startForegroundService(new Intent(this,CaptureService.class).putExtra("grant",data).putExtra("result",result));if(demoAfterGrant)startActivity(new Intent(this,SampleActivity.class));}catch(RuntimeException e){notice("Başlatılamadı","Ekran okuma hizmeti açılamadı.");}}
        if(request==51 && result==RESULT_OK && data!=null && data.getData()!=null){Uri uri=data.getData();try{BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,options);}if(options.outWidth<=0||options.outHeight<=0)throw new IOException();options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1920)options.inSampleSize*=2;options.inJustDecodeBounds=false;Bitmap bitmap;try(InputStream in=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,options);}if(bitmap==null)throw new IOException();runOcr(bitmap);}catch(Exception e){notice("Görüntü açılamadı","PNG veya JPEG biçiminde, daha küçük bir görüntü seç.");}}
    }
    private TextView label(String text,int size,int color){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(dp(3),1.05f);return v;}
    private void paragraph(String value){TextView t=label(value,15,MUTED);t.setPadding(0,dp(8),0,dp(12));content.addView(t);}
    private TextView card(String text){TextView t=label(text,16,INK);t.setPadding(dp(18),dp(14),dp(18),dp(14));t.setBackground(background(SURFACE,0xFF3E3F48));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);content.addView(t,p);return t;}
    private Button button(String text,Runnable onClick){return button(text,onClick,false);}
    private Button button(String text,Runnable onClick,boolean primary){Button b=new Button(this);b.setId(View.generateViewId());b.setText(text);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{MUTED,primary?BG:INK}));b.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);b.setPadding(dp(16),dp(10),dp(12),dp(10));b.setMinHeight(dp(52));b.setFocusable(true);
        StateListDrawable states=new StateListDrawable();states.addState(new int[]{-android.R.attr.state_enabled},background(SURFACE,0));states.addState(new int[]{android.R.attr.state_focused},background(primary?0xFFFFAD9E:0xFF43302E,ACCENT));states.addState(new int[]{android.R.attr.state_pressed},background(primary?0xFFFFAD9E:0xFF43302E,ACCENT));states.addState(new int[]{},background(primary?ACCENT:0xFF2C2D34,0));b.setBackground(states);b.setOnClickListener(v->onClick.run());return b;}
    private void action(String text,Runnable onClick){Button b=button(text,onClick);if(wide&&navigation[page]!=null)b.setNextFocusLeftId(navigation[page].getId());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(9);content.addView(b,p);}
    private GradientDrawable background(int fill,int border){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(10));if(border!=0)d.setStroke(dp(2),border);return d;}
    private int dp(int size){return Math.round(size*getResources().getDisplayMetrics().density);}
    private void spacer(LinearLayout parent,int height){parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height)));}
    private void notice(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Tamam",null).show();}
    @Override public void onResume(){super.onResume();getWindow().getDecorView().setSystemUiVisibility(5894);refreshStatus();}
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);buildShell();showPage(page);}
    @Override public void onDestroy(){alive=false;handler.removeCallbacksAndMessages(null);worker.shutdown();super.onDestroy();}
}
