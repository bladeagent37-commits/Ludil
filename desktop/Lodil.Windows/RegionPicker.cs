namespace Lodil.Windows;
public sealed class RegionPicker:Form {
    Point anchor,current;bool drawing;public Rectangle Selected{get;private set;}
    public RegionPicker(){FormBorderStyle=FormBorderStyle.None;StartPosition=FormStartPosition.Manual;Bounds=SystemInformation.VirtualScreen;TopMost=true;ShowInTaskbar=false;BackColor=UiTheme.Background;Opacity=.48;Cursor=Cursors.Cross;KeyPreview=true;DoubleBuffered=true;Text="İngilizce altyazı bölgesini seç";}
    protected override void OnMouseDown(MouseEventArgs e){base.OnMouseDown(e);if(e.Button==MouseButtons.Left){anchor=current=e.Location;drawing=true;Capture=true;Invalidate();}}
    protected override void OnMouseMove(MouseEventArgs e){base.OnMouseMove(e);if(drawing){current=e.Location;Invalidate();}}
    protected override void OnMouseUp(MouseEventArgs e){base.OnMouseUp(e);if(!drawing)return;drawing=false;Capture=false;var r=Selection();if(r.Width<80||r.Height<24){Invalidate();return;}r.Offset(Location);Selected=r;DialogResult=DialogResult.OK;Close();}
    Rectangle Selection()=>Rectangle.FromLTRB(Math.Min(anchor.X,current.X),Math.Min(anchor.Y,current.Y),Math.Max(anchor.X,current.X),Math.Max(anchor.Y,current.Y));
    protected override void OnKeyDown(KeyEventArgs e){if(e.KeyCode==Keys.Escape){DialogResult=DialogResult.Cancel;Close();}base.OnKeyDown(e);}
    protected override void OnPaint(PaintEventArgs e){base.OnPaint(e);using var pen=new Pen(UiTheme.Accent,4);if(drawing)e.Graphics.DrawRectangle(pen,Selection());using var f=new Font("Segoe UI",19,FontStyle.Bold);var monitor=Screen.FromPoint(Cursor.Position).Bounds;var point=new Point(monitor.X-Left+28,monitor.Y-Top+28);e.Graphics.DrawString("Sadece İngilizce altyazının bulunduğu alanı çiz.  •  Esc: vazgeç",f,Brushes.White,point);}
}
