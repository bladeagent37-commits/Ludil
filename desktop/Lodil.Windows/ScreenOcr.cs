using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
using Lodil.Core;
using Tesseract;
namespace Lodil.Windows;
public sealed class ScreenOcr:IDisposable {
    readonly TesseractEngine engine;
    public ScreenOcr(string directory){engine=new(directory,"eng",EngineMode.Default);engine.SetVariable("preserve_interword_spaces","1");}
    public (string Text,float Confidence) Read(Rectangle region){
        using var image=new Bitmap(region.Width,region.Height,PixelFormat.Format32bppArgb);
        using(var g=Graphics.FromImage(image))g.CopyFromScreen(region.Location,Point.Empty,region.Size,CopyPixelOperation.SourceCopy);
        return ReadImage(image);
    }
    public (string Text,float Confidence) ReadImage(Bitmap image){
        var scale=image.Height<130&&image.Width<1700?1.5:Math.Min(1.0,1800.0/image.Width);
        var width=Math.Max(1,(int)(image.Width*scale));var height=Math.Max(1,(int)(image.Height*scale));
        using var readable=new Bitmap(width,height,PixelFormat.Format24bppRgb);using(var g=Graphics.FromImage(readable)){g.InterpolationMode=InterpolationMode.HighQualityBicubic;g.DrawImage(image,0,0,width,height);}
        using var memory=new MemoryStream();readable.Save(memory,System.Drawing.Imaging.ImageFormat.Png);using var pix=Pix.LoadFromMemory(memory.ToArray());using var page=engine.Process(pix,PageSegMode.Auto);
        return(Tokenizer.Normalize(page.GetText()),page.GetMeanConfidence());
    }
    public void Dispose()=>engine.Dispose();
}
