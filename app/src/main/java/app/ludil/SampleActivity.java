package app.ludil;

import android.app.Activity;
import android.os.*;
import android.graphics.*;
import android.view.*;

/** Original synthetic scene, not a copyrighted game capture or a fake translator. */
public final class SampleActivity extends Activity {
    private final Handler timer=new Handler(Looper.getMainLooper());
    private int index;
    private final String[] lines={"We have to leave before it is too late.","Stay here. I will find another way.","The door is locked. Find the key."};
    private View scene;
    private final Runnable advance=new Runnable(){@Override public void run(){index++;scene.invalidate();timer.postDelayed(this,10000);}};
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(5894);
        scene=new View(this){private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();c.drawColor(0xFF0B1829);
                p.setColor(0xFF152D44);c.drawRect(w*.1f,h*.24f,w*.9f,h*.57f,p);p.setColor(0xFF24465A);c.drawCircle(w*.72f,h*.33f,h*.16f,p);
                p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setColor(0xFF60D2C8);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.045f);c.drawText("LUDIL • ÖZGÜN OCR DENEME SAHNESİ",w/2f,h*.13f,p);
                p.setColor(Color.WHITE);p.setTextSize(h*.039f);p.setShadowLayer(3,0,2,Color.BLACK);c.drawText(lines[index%lines.length],w/2f,h*.66f,p);p.clearShadowLayer();
                p.setTextSize(h*.027f);p.setColor(0xFF8CA1B5);c.drawText("Cümle 10 saniyede değişir • Sağ: sonraki • Geri: çıkış",w/2f,h*.94f,p);
            }};scene.setFocusable(true);scene.setFocusableInTouchMode(true);setContentView(scene);scene.requestFocus();timer.postDelayed(advance,10000);
    }
    @Override public boolean onKeyDown(int code,KeyEvent event){if(code==KeyEvent.KEYCODE_DPAD_RIGHT||code==KeyEvent.KEYCODE_DPAD_CENTER){index++;scene.invalidate();return true;}return super.onKeyDown(code,event);}
    @Override public void onDestroy(){timer.removeCallbacksAndMessages(null);super.onDestroy();}
}
