using System.Drawing;
using System.Text.Json;
using Ludil.Core;
namespace Ludil.Smoke;

internal static class MenuRegressions {
    public static void Run(){
        var menu=new MenuTextTranslator();var checks=0;
        void Check(bool value,string name){if(!value)throw new Exception(name);checks++;}
        using var stream=typeof(MenuRegressions).Assembly.GetManifestResourceStream("Ludil.MenuCases")!;
        using var cases=JsonDocument.Parse(stream);
        foreach(var item in cases.RootElement.EnumerateArray()){var source=item.GetProperty("source").GetString()!;var expected=item.GetProperty("expected").GetString()!;Check(menu.Known(source)==expected,"Menu glossary regression: "+source);}
        var calls=0;var result=menu.Translate("New Game\nContinue\nSettings",line=>{calls++;return "wrong";});
        Check(result=="Yeni Oyun\nDevam Et\nAyarlar"&&calls==0,"Known UI bypasses model loading and preserves lines");
        result=menu.Translate("A brand new sentence.\nSettings",line=>{calls++;return "Yeni bir cümle.";});
        Check(result=="Yeni bir cümle.\nAyarlar"&&calls==1,"Unknown text uses fallback once per line");
        Check(MenuTextTranslator.Prepare("\n1234\n42%\nSettings\n")=="Settings","Numeric clutter excluded");
        Check(MenuTextTranslator.Prepare(string.Join('\n',Enumerable.Repeat("Settings",30))).Split('\n').Length==12,"Bounded menu rows");
        Check(MenuTextTranslator.Prepare(new string('x',161)).Length==0,"Oversized lines rejected");
        var gate=new SubtitleGate(2,9000);Check(gate.Observe("On",1,10000)==null&&gate.Observe("On",1,10500)=="On","Two-letter menu values become stable");
        Check(gate.IsCurrent("On",18000)&&!gate.IsCurrent("On",20000),"Menu scene timeout remains bounded");
        foreach(var screen in new[]{new Rectangle(0,0,1920,1080),new Rectangle(-1280,0,1280,720),new Rectangle(0,-1080,1920,1080)}){
            var region=new Rectangle(screen.Left+screen.Width/10,screen.Top+60,screen.Width/2,screen.Height-120);
            Check(MenuPanelLayout.TryPlace(screen,region,out var panel)&&!panel.IntersectsWith(region)&&screen.Contains(panel),"Menu panel avoids captured source on all monitors");
            Check(!MenuPanelLayout.TryPlace(screen,screen,out _),"Whole-monitor menu capture requires a free display column");
        }
        var profile=Ludil.Windows.AppSettings.Parse("{\"Profiles\":{\"Oyun\":{\"MenuMode\":true}},\"Selected\":\"Oyun\"}");Check(profile.Profiles["Oyun"].Copy().MenuMode,"Menu mode survives save and profile copy");
        Check(!Ludil.Windows.AppSettings.Parse("{\"Profiles\":{\"Eski\":{}},\"Selected\":\"Eski\"}").Profiles["Eski"].MenuMode,"Existing profiles keep subtitle mode");
        var rgb=new byte[100*4*3];for(var y=0;y<4;y++)for(var x=0;x<100;x++){var i=(y*100+x)*3;var background=y==1?new byte[]{255,139,120}:y==2?new byte[]{255,255,255}:new byte[]{10,10,10};Array.Copy(background,0,rgb,i,3);if(x>=45&&x<=50&&y<3)Array.Fill(rgb,(byte)(y==0?245:10),i,3);}
        var gray=MenuContrast.NormalizeRgb(rgb,100,4);Check(gray[45]>200&&gray[5]==10,"Dark menu rows remain intact");Check(gray[100+45]==255&&gray[100+5]==0,"Dark letters on selected menu bars become readable");Check(gray[200+45]==255&&gray[200+5]==0,"Light menu backgrounds normalize consistently");Check(gray[300+5]==10,"Blank dark rows do not become text");
        Console.WriteLine("MENU REGRESSIONS PASS: "+checks+" checks");
    }
}
