using System.ComponentModel;
using System.Drawing.Drawing2D;
namespace Ludil.Windows;

internal static class UiTheme {
    public static readonly Color Background=Color.FromArgb(22,23,27),Rail=Color.FromArgb(28,29,34),Surface=Color.FromArgb(34,35,41),SurfaceRaised=Color.FromArgb(44,45,52),Border=Color.FromArgb(62,63,72),Ink=Color.FromArgb(248,244,238),Muted=Color.FromArgb(177,174,173),Accent=Color.FromArgb(255,139,120),Warning=Color.FromArgb(255,191,112);
    public static Font Font(float size=11,FontStyle style=FontStyle.Regular)=>new("Segoe UI",size,style);
    public static Label Text(string value,float size=11,Color? color=null,bool bold=false)=>new(){Text=value,Font=Font(size,bold?FontStyle.Bold:FontStyle.Regular),ForeColor=color??Ink,BackColor=Color.Transparent,AutoSize=false,TextAlign=ContentAlignment.MiddleLeft,UseMnemonic=false};
    public static GraphicsPath Round(RectangleF box,float radius){var r=Math.Min(radius,Math.Min(box.Width,box.Height)/2);var d=r*2;var p=new GraphicsPath();p.AddArc(box.X,box.Y,d,d,180,90);p.AddArc(box.Right-d,box.Y,d,d,270,90);p.AddArc(box.Right-d,box.Bottom-d,d,d,0,90);p.AddArc(box.X,box.Bottom-d,d,d,90,90);p.CloseFigure();return p;}
}
internal sealed class SoftCard:Panel {
    [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]public Color Fill{get;set;}=UiTheme.Surface;[DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]public Color Stroke{get;set;}=UiTheme.Border;[DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]public int Radius{get;set;}=16;
    public SoftCard(){DoubleBuffered=true;BackColor=UiTheme.Surface;Padding=new Padding(18);SetStyle(ControlStyles.ResizeRedraw,true);}
    protected override void OnPaintBackground(PaintEventArgs e){e.Graphics.Clear(Parent?.BackColor??UiTheme.Background);e.Graphics.SmoothingMode=SmoothingMode.AntiAlias;using var path=UiTheme.Round(new RectangleF(.5f,.5f,Width-1,Height-1),Radius);using var fill=new SolidBrush(Fill);using var pen=new Pen(Stroke);e.Graphics.FillPath(fill,path);e.Graphics.DrawPath(pen,path);}
}
internal sealed class SoftButton:Button {
    [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]public bool Primary{get;set;}[DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]public bool Active{get;set;}[DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]public bool Danger{get;set;}[DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]public bool AlignLeft{get;set;}
    [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]public int Glyph{get;set;}=-1;
    bool hover,pressed;
    public SoftButton(){Font=UiTheme.Font(11,FontStyle.Bold);FlatStyle=FlatStyle.Flat;FlatAppearance.BorderSize=0;Cursor=Cursors.Hand;UseVisualStyleBackColor=false;Height=46;SetStyle(ControlStyles.UserPaint|ControlStyles.AllPaintingInWmPaint|ControlStyles.OptimizedDoubleBuffer|ControlStyles.ResizeRedraw,true);}
    protected override void OnMouseEnter(EventArgs e){hover=true;Invalidate();base.OnMouseEnter(e);}protected override void OnMouseLeave(EventArgs e){hover=pressed=false;Invalidate();base.OnMouseLeave(e);}protected override void OnMouseDown(MouseEventArgs e){pressed=true;Invalidate();base.OnMouseDown(e);}protected override void OnMouseUp(MouseEventArgs e){pressed=false;Invalidate();base.OnMouseUp(e);}protected override void OnEnabledChanged(EventArgs e){Invalidate();base.OnEnabledChanged(e);}
    protected override void OnPaint(PaintEventArgs e){e.Graphics.Clear(Parent?.BackColor??UiTheme.Background);e.Graphics.SmoothingMode=SmoothingMode.AntiAlias;
        var fill=Primary?UiTheme.Accent:Active?Color.FromArgb(67,45,44):UiTheme.SurfaceRaised;var ink=Primary?Color.FromArgb(35,22,22):UiTheme.Ink;
        if(Danger){fill=Color.FromArgb(104,46,58);ink=Color.FromArgb(255,227,231);}if(!Enabled){fill=Color.FromArgb(39,40,46);ink=Color.FromArgb(143,140,144);}else if(pressed)fill=ControlPaint.Dark(fill,.12f);else if(hover)fill=ControlPaint.Light(fill,.1f);
        using var path=UiTheme.Round(new RectangleF(.5f,.5f,Width-1,Height-1),12);using var brush=new SolidBrush(fill);e.Graphics.FillPath(brush,path);using var border=new Pen(Focused&&Enabled?UiTheme.Accent:Primary?fill:UiTheme.Border,Focused?2:1);e.Graphics.DrawPath(border,path);
        if(Glyph>=0){using var p=new Pen(Active?UiTheme.Accent:ink,1.8f);var y=Height/2-8;var x=15;
            if(Glyph==0){e.Graphics.DrawRectangle(p,x,y,19,14);e.Graphics.DrawLine(p,x+4,y+5,x+15,y+5);e.Graphics.DrawLine(p,x+4,y+10,x+11,y+10);}
            else if(Glyph==1){e.Graphics.DrawLine(p,x,y+4,x+20,y+4);e.Graphics.DrawLine(p,x,y+13,x+20,y+13);e.Graphics.DrawEllipse(p,x+5,y+1,6,6);e.Graphics.DrawEllipse(p,x+12,y+10,6,6);}
            else if(Glyph==2){e.Graphics.DrawRectangle(p,x,y,8,7);e.Graphics.DrawRectangle(p,x+12,y,8,7);e.Graphics.DrawRectangle(p,x,y+11,8,7);e.Graphics.DrawRectangle(p,x+12,y+11,8,7);}
            else{e.Graphics.DrawEllipse(p,x,y,19,19);TextRenderer.DrawText(e.Graphics,"?",Font,new Rectangle(x,y-1,20,21),ink,TextFormatFlags.HorizontalCenter|TextFormatFlags.VerticalCenter|TextFormatFlags.NoPadding);}}
        var left=Glyph>=0?47:AlignLeft?18:12;var rect=new Rectangle(left,2,Width-left-12,Height-4);TextRenderer.DrawText(e.Graphics,Text,Font,rect,ink,TextFormatFlags.VerticalCenter|(AlignLeft?TextFormatFlags.Left:TextFormatFlags.HorizontalCenter)|TextFormatFlags.EndEllipsis|TextFormatFlags.NoPrefix);
    }
}
internal sealed class BrandMark:Control {
    public BrandMark(){Size=new Size(44,44);SetStyle(ControlStyles.UserPaint|ControlStyles.OptimizedDoubleBuffer,true);}
    protected override void OnPaint(PaintEventArgs e){e.Graphics.SmoothingMode=SmoothingMode.AntiAlias;using var box=UiTheme.Round(new RectangleF(0,0,43,43),12);using var bg=new SolidBrush(UiTheme.Accent);using var ink=new SolidBrush(UiTheme.Background);e.Graphics.FillPath(bg,box);e.Graphics.FillRectangle(ink,12,10,6,23);e.Graphics.FillRectangle(ink,12,27,20,6);e.Graphics.FillPolygon(ink,new[]{new Point(26,29),new Point(32,29),new Point(26,38)});}
}
