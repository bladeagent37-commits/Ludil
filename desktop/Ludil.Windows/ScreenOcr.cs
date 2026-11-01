using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
using Ludil.Core;
using Tesseract;
namespace Ludil.Windows;
public sealed class ScreenOcr:IDisposable {
    readonly TesseractEngine engine;
    public ScreenOcr(string directory){engine=new(directory,"eng",EngineMode.Default);engine.SetVariable("preserve_interword_spaces","1");}
    public (string Text,float Confidence) Read(Rectangle region,bool menu=false){
        using var image=new Bitmap(region.Width,region.Height,PixelFormat.Format32bppArgb);
        using(var g=Graphics.FromImage(image))g.CopyFromScreen(region.Location,Point.Empty,region.Size,CopyPixelOperation.SourceCopy);
        return ReadImage(image,menu);
    }
    public (string Text,float Confidence) ReadImage(Bitmap image,bool menu=false){
        var scale=image.Height<130&&image.Width<1700?1.5:Math.Min(1.0,1800.0/image.Width);
        var width=Math.Max(1,(int)(image.Width*scale));var height=Math.Max(1,(int)(image.Height*scale));
        using var readable=new Bitmap(width,height,PixelFormat.Format24bppRgb);using(var g=Graphics.FromImage(readable)){g.InterpolationMode=InterpolationMode.HighQualityBicubic;g.DrawImage(image,0,0,width,height);}
        using var normalized=menu?NormalizeMenu(readable):null;
        using var memory=new MemoryStream();(normalized??readable).Save(memory,System.Drawing.Imaging.ImageFormat.Png);using var pix=Pix.LoadFromMemory(memory.ToArray());using var page=engine.Process(pix,menu?PageSegMode.SparseText:PageSegMode.Auto);
        return(menu?MenuTextTranslator.Prepare(page.GetText()):Tokenizer.Normalize(page.GetText()),page.GetMeanConfidence());
    }
    static Bitmap NormalizeMenu(Bitmap image){
        var rgb=new byte[image.Width*image.Height*3];var area=new Rectangle(0,0,image.Width,image.Height);var data=image.LockBits(area,ImageLockMode.ReadOnly,PixelFormat.Format24bppRgb);
        try{var row=new byte[Math.Abs(data.Stride)];for(var y=0;y<image.Height;y++){System.Runtime.InteropServices.Marshal.Copy(IntPtr.Add(data.Scan0,y*data.Stride),row,0,row.Length);for(var x=0;x<image.Width;x++){var i=(y*image.Width+x)*3;rgb[i]=row[x*3+2];rgb[i+1]=row[x*3+1];rgb[i+2]=row[x*3];}}}finally{image.UnlockBits(data);}
        var gray=MenuContrast.NormalizeRgb(rgb,image.Width,image.Height);var result=new Bitmap(image.Width,image.Height,PixelFormat.Format24bppRgb);data=result.LockBits(area,ImageLockMode.WriteOnly,PixelFormat.Format24bppRgb);
        try{var row=new byte[Math.Abs(data.Stride)];for(var y=0;y<image.Height;y++){for(var x=0;x<image.Width;x++)row[x*3]=row[x*3+1]=row[x*3+2]=gray[y*image.Width+x];System.Runtime.InteropServices.Marshal.Copy(row,0,IntPtr.Add(data.Scan0,y*data.Stride),row.Length);}}finally{result.UnlockBits(data);}return result;
    }
    public void Dispose()=>engine.Dispose();
}
