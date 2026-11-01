// Linux-only visual QA harness. Compiles the production layout and theme sources unchanged.
// It does not emulate Windows capture or claim to test the translation backend.
global using System;
global using System.Collections.Generic;
global using System.Drawing;
global using System.IO;
global using System.Threading.Tasks;
global using System.Windows.Forms;
namespace Ludil.Windows;
public sealed partial class MainForm:Form {
    static string output="";readonly List<string> issues=new();
    [STAThread]static void Main(string[] args){output=args[0];Directory.CreateDirectory(output);Application.EnableVisualStyles();Application.SetCompatibleTextRenderingDefault(false);Application.Run(new MainForm());}
    MainForm(){BuildLayout();profiles.Items.Add("Varsayılan");profiles.SelectedIndex=0;textMode.SelectedIndex=0;color.SelectedIndex=0;position.SelectedIndex=0;speed.SelectedIndex=0;start.Enabled=false;Shown+=async(_,_)=>await Run();}
    async Task Run(){try{foreach(var size in new[]{new Size(1040,750),new Size(900,650)}){Size=size;await Task.Delay(150);for(var i=0;i<4;i++){ShowPage(i);await Task.Delay(100);Validate(pages[i],size.Width+"/"+i);Snapshot($"page-{i}-{size.Width}");}}Size=new Size(1040,750);ShowPage(0);regionTitle.Text="Altyazı alanın hazır";regionDetail.Text="Varsayılan • Alanı değiştirmedikçe hatırlanır.";choose.Text="Alanı değiştir";start.Enabled=true;sourceLabel.Text="We need to get out of here.";translationLabel.Text="Buradan çıkmalıyız.";translationLabel.ForeColor=UiTheme.Ink;status.Text="Alan seçildi • Başlatabilirsin";await Task.Delay(150);Snapshot("home-preview");textMode.SelectedIndex=1;regionTitle.Text="Menü alanın hazır";sourceLabel.Text="New Game • Continue • Settings";translationLabel.Text="Yeni Oyun • Devam Et • Ayarlar";await Task.Delay(100);Validate(pages[0],"menu-preview");Snapshot("menu-preview");}catch(Exception e){issues.Add(e.ToString());}finally{File.WriteAllText(Path.Combine(output,"visual-layout.txt"),"Environment: Linux Mono visual harness; production layout/theme sources.\nSizes: 1040x750, 900x650; four pages.\nPassed: "+(issues.Count==0)+"\n"+string.Join("\n",issues));Environment.ExitCode=issues.Count==0?0:1;Close();}}
    void Validate(Control parent,string where){foreach(Control c in parent.Controls){if(!c.Visible)continue;if(c.Left<-1||c.Top<-1||c.Right>(parent is ScrollableControl sc&&sc.AutoScroll?sc.DisplayRectangle.Width:parent.ClientSize.Width)+2||c.Bottom>(parent is ScrollableControl sc2&&sc2.AutoScroll?sc2.DisplayRectangle.Height:parent.ClientSize.Height)+2)issues.Add(where+": clipped "+c.GetType().Name+" "+c.Text+" "+c.Bounds+" in "+parent.GetType().Name+" "+parent.ClientSize);if(c is Label label&&!label.AutoSize&&label.Text.Length>0){var needed=label.GetPreferredSize(new Size(label.Width,0));if(needed.Height>label.Height+3)issues.Add(where+": clipped label text "+label.Text+" needs "+needed.Height+" but has "+label.Height);}Validate(c,where);}}
    void Snapshot(string name){using var image=new Bitmap(ClientSize.Width,ClientSize.Height);Refresh();Application.DoEvents();using(var g=Graphics.FromImage(image))g.CopyFromScreen(PointToScreen(Point.Empty),Point.Empty,ClientSize);image.Save(Path.Combine(output,name+".png"),System.Drawing.Imaging.ImageFormat.Png);}
    Task PickRegion()=>Task.CompletedTask;Task Toggle()=>Task.CompletedTask;Task TestPipeline()=>Task.CompletedTask;
    Task ConnectTv()=>Task.CompletedTask;
    void ModeChanged(){}void AddProfile(){}void ProfileSelected(){}void OpenLicenses(){}void SpeedChanged(){}
    void AppearanceChanged(){fontValue.Text=fontSize.Value.ToString();appearancePreview.Font=new Font("Segoe UI",fontSize.Value,FontStyle.Bold,GraphicsUnit.Pixel);appearancePreview.ForeColor=color.SelectedIndex==1?Color.FromArgb(255,224,138):UiTheme.Ink;}
}
