using Ludil.Core;
namespace Ludil.Windows;
public sealed class CaptionOverlay:Form {
    readonly System.Windows.Forms.Timer expiry=new(){Interval=200};
    string caption="",original="";long deadline;GameProfile profile=new();bool hiddenByUser;float menuFont=22;
    public CaptionOverlay(){FormBorderStyle=FormBorderStyle.None;StartPosition=FormStartPosition.Manual;ShowInTaskbar=false;TopMost=true;BackColor=UiTheme.Background;Opacity=.94;DoubleBuffered=true;expiry.Tick+=(_,_)=>{if(Environment.TickCount64>=deadline)ClearCaption();};expiry.Start();}
    protected override bool ShowWithoutActivation=>true;
    protected override CreateParams CreateParams{get{var p=base.CreateParams;p.ExStyle|=0x00000020|0x00000080|0x08000000;return p;}}
    protected override void OnHandleCreated(EventArgs e){base.OnHandleCreated(e);Native.SetWindowDisplayAffinity(Handle,0x11);}
    public void ShowCaption(string source,string text,GameProfile settings){profile=settings.Copy();caption=text;original=source;deadline=Environment.TickCount64+4500;LayoutCaption();if(caption.Length==0)return;if(!hiddenByUser&&!Visible)Show();Invalidate();}
    public void RefreshCaption(){if(caption.Length>0)deadline=Environment.TickCount64+4500;}
    public void ToggleHidden(){hiddenByUser=!hiddenByUser;if(hiddenByUser)Hide();else if(caption.Length>0&&Environment.TickCount64<deadline)Show();}
    public void ClearCaption(){caption="";original="";deadline=0;Hide();}
    void LayoutCaption(){var region=profile.Region;var screen=Screen.FromRectangle(region).Bounds;if(profile.MenuMode){LayoutMenu(screen,region);return;}var width=Math.Min(1300,(int)(screen.Width*.84));using var font=new Font("Segoe UI",profile.FontSize,FontStyle.Bold,GraphicsUnit.Pixel);
        var size=TextRenderer.MeasureText(caption,font,new Size(width-38,240),TextFormatFlags.WordBreak|TextFormatFlags.NoPadding);var height=Math.Clamp(size.Height+26+(profile.Original?30:0),60,Math.Min(260,screen.Height/3));
        var x=screen.Left+(screen.Width-width)/2;var below=region.Bottom+12;var above=region.Top-height-12;
        var y=profile.Above?above:below;if(y+height>screen.Bottom-12)y=above;if(y<screen.Top+12)y=screen.Top+12;
        Bounds=new Rectangle(x,y,width,height);
        // Keep our captions outside the sampled English band even if the OS cannot exclude them from capture.
        if(Bounds.IntersectsWith(region)){Hide();caption="";original="";deadline=0;}
    }
    void LayoutMenu(Rectangle screen,Rectangle region){
        if(!MenuPanelLayout.TryPlace(screen,region,out var bounds)){ClearCaption();return;}
        menuFont=Math.Min(profile.FontSize,26);int height=0;
        while(menuFont>=14){using var font=new Font("Segoe UI",menuFont,FontStyle.Bold,GraphicsUnit.Pixel);height=TextRenderer.MeasureText(caption,font,new Size(bounds.Width-36,10000),TextFormatFlags.WordBreak|TextFormatFlags.NoPadding|TextFormatFlags.NoPrefix).Height+54;if(height<=bounds.Height)break;menuFont-=1;}
        if(height>bounds.Height){ClearCaption();return;}bounds.Height=Math.Max(100,height);Bounds=bounds;
    }
    protected override void OnPaint(PaintEventArgs e){base.OnPaint(e);if(caption.Length==0)return;
        if(profile.MenuMode){using var title=new Font("Segoe UI",11,FontStyle.Bold,GraphicsUnit.Pixel);using var text=new Font("Segoe UI",menuFont,FontStyle.Bold,GraphicsUnit.Pixel);TextRenderer.DrawText(e.Graphics,"MENÜ VE AYARLAR",title,new Rectangle(18,10,Width-36,22),UiTheme.Accent,TextFormatFlags.NoPrefix);TextRenderer.DrawText(e.Graphics,caption,text,new Rectangle(18,34,Width-36,Height-44),profile.Yellow?Color.FromArgb(255,224,138):Color.White,TextFormatFlags.WordBreak|TextFormatFlags.NoPrefix);return;}
        using var font=new Font("Segoe UI",profile.FontSize,FontStyle.Bold,GraphicsUnit.Pixel);using var pen=new Pen(UiTheme.Accent,2);e.Graphics.DrawLine(pen,0,1,Width,1);
        var height=Height-22-(profile.Original?28:0);TextRenderer.DrawText(e.Graphics,caption,font,new Rectangle(18,10,Width-36,height),profile.Yellow?Color.FromArgb(255,224,138):Color.White,TextFormatFlags.WordBreak|TextFormatFlags.HorizontalCenter|TextFormatFlags.VerticalCenter|TextFormatFlags.EndEllipsis|TextFormatFlags.NoPrefix);
        if(profile.Original){using var small=new Font("Segoe UI",Math.Max(13,profile.FontSize*.58f),FontStyle.Regular,GraphicsUnit.Pixel);TextRenderer.DrawText(e.Graphics,original,small,new Rectangle(18,Height-31,Width-36,24),Color.FromArgb(169,187,204),TextFormatFlags.HorizontalCenter|TextFormatFlags.EndEllipsis|TextFormatFlags.NoPrefix);}
    }
    protected override void Dispose(bool disposing){if(disposing)expiry.Dispose();base.Dispose(disposing);}
}
