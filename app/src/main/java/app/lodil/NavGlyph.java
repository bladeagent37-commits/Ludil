package app.lodil;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Four distinct scalable navigation marks, with no font or network dependency. */
public final class NavGlyph extends Drawable {
    private final int kind,size;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    public NavGlyph(int kind,int size,int color){this.kind=kind;this.size=size;p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);}
    @Override public int getIntrinsicWidth(){return size;}
    @Override public int getIntrinsicHeight(){return size;}
    @Override public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
        if(kind==0){c.drawRoundRect(2,4,22,19,3,3,p);c.drawLine(6,10,18,10,p);c.drawLine(6,14,14,14,p);}
        else if(kind==1){c.drawLine(2,7,22,7,p);c.drawLine(2,17,22,17,p);c.drawCircle(8,7,3,p);c.drawCircle(16,17,3,p);}
        else if(kind==2){c.drawRect(2,3,9,10,p);c.drawRect(15,14,22,21,p);c.drawLine(9,6,18,6,p);c.drawLine(18,6,18,14,p);c.drawLine(5,10,5,18,p);c.drawLine(5,18,15,18,p);}
        else{c.drawCircle(12,12,10,p);c.drawLine(12,11,12,17,p);c.drawPoint(12,7,p);}c.restore();}
    @Override public void setAlpha(int alpha){p.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
