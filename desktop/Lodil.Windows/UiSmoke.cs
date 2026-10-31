using System.Text.Json;
using Lodil.Core;
namespace Lodil.Windows;

public sealed partial class MainForm {
    internal async Task RunUiSmoke(string output){
        Directory.CreateDirectory(output);var checks=new List<string>();var errors=new List<string>();
        void Require(bool value,string name){if(!value)throw new InvalidOperationException(name);checks.Add(name);}
        void Shot(string name){using var image=new Bitmap(ClientSize.Width,ClientSize.Height);DrawToBitmap(image,new Rectangle(Point.Empty,ClientSize));image.Save(Path.Combine(output,name+".png"),System.Drawing.Imaging.ImageFormat.Png);}
        void CheckBounds(Control root){foreach(Control child in root.Controls){if(!child.Visible)continue;if(child.Left<-1||child.Top<-1||child.Right>(root is ScrollableControl sc&&sc.AutoScroll?sc.DisplayRectangle.Width:root.ClientSize.Width)+2||child.Bottom>(root is ScrollableControl sc2&&sc2.AutoScroll?sc2.DisplayRectangle.Height:root.ClientSize.Height)+2)throw new InvalidOperationException($"Clipped control: {child.GetType().Name} {child.Text} ({child.Bounds}) in {root.GetType().Name} {root.ClientSize}");CheckBounds(child);}}
        try{
            await Task.Delay(200);Require(!start.Enabled&&choose.Enabled,"First launch guides user to select a region");
            foreach(var size in new[]{new Size(1040,750),new Size(900,650)}){Size=size;await Task.Delay(150);for(var page=0;page<4;page++){ShowPage(page);await Task.Delay(100);CheckBounds(pages[page]);Require(pages.Count(p=>p.Visible)==1,"One visible page at "+size+" / "+page);Shot($"page-{page}-{size.Width}");}}
            Size=new Size(1040,750);ShowPage(1);fontSize.Value=32;color.SelectedIndex=1;original.Checked=true;speed.SelectedIndex=1;
            Require(settings.Profiles[settings.Selected].FontSize==32&&settings.Profiles[settings.Selected].Yellow&&settings.Profiles[settings.Selected].Original&&settings.Profiles[settings.Selected].IntervalMs==450,"Appearance controls save into active profile");
            var legacy=CurrentProfile();legacy.IntervalMs=900;settings.Profiles["Önceki oyun"]=legacy;settings.Selected="Önceki oyun";ReloadProfiles();LoadCurrentProfile();fontSize.Value=34;
            Require(settings.Profiles[settings.Selected].IntervalMs==900,"Existing custom reading interval survives appearance edits");
            settings.Selected="Varsayılan";ReloadProfiles();LoadCurrentProfile();minimize.Checked=false;color.SelectedIndex=0;fontSize.Value=26;speed.SelectedIndex=0;original.Checked=false;
            var restored=JsonSerializer.Deserialize<AppSettings>(JsonSerializer.Serialize(settings));Require(restored!=null&&restored.Profiles.ContainsKey("Önceki oyun"),"Turkish profile names round-trip through JSON");
            ShowPage(3);await TestPipeline();Require(diagnostic.Text.StartsWith("Test başarılı",StringComparison.Ordinal),"Real Windows Tesseract image OCR plus EN-to-TR inference");Shot("help-tested");
            using(var scene=new Form{FormBorderStyle=FormBorderStyle.None,StartPosition=FormStartPosition.Manual,Location=new Point(120,90),ClientSize=new Size(1100,160),BackColor=Color.FromArgb(15,24,35),TopMost=true,ShowInTaskbar=false}){
                scene.Paint+=(_,e)=>{using var font=new Font("Arial",35,FontStyle.Bold,GraphicsUnit.Pixel);e.Graphics.DrawString("We need to get out of here.",font,Brushes.White,38,53);};scene.Show();await Task.Delay(300);region=scene.RectangleToScreen(scene.ClientRectangle);RefreshState();ShowPage(0);Require(start.Enabled,"Start becomes available after valid region selection");translationLabel.Text="Bekleniyor";
                await Toggle();for(var i=0;i<60&&!translationLabel.Text.Contains("Buradan çıkmalıyız.",StringComparison.Ordinal);i++)await Task.Delay(200);
                Require(translationLabel.Text.Contains("Buradan çıkmalıyız.",StringComparison.Ordinal),"Live screen capture to OCR to Turkish result");Require(overlay.Visible&&!overlay.Bounds.IntersectsWith(region),"Caption window visible outside the English capture band");
                await Stop();await Task.Delay(200);Require(!overlay.Visible&&running==null,"Stop hides captions and releases active session");
                for(var i=0;i<3;i++){await Toggle();await Stop();await Task.Delay(100);Require(!overlay.Visible&&running==null,"Rapid start-stop rejects stale result "+i);}
                scene.Close();
            }
            ShowPage(0);await Task.Delay(100);Shot("home-final");
        }catch(Exception e){errors.Add(e.ToString());}
        finally{
            try{await Stop();}catch(Exception e){errors.Add(e.ToString());}
            var report=new{version="0.3.0",passed=errors.Count==0,checks,errors};File.WriteAllText(Path.Combine(output,"ui-smoke.json"),JsonSerializer.Serialize(report,new JsonSerializerOptions{WriteIndented=true}));Environment.ExitCode=errors.Count==0?0:1;Close();
        }
    }
}
