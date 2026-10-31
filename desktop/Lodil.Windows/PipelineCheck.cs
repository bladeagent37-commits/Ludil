using System.Diagnostics;
using Lodil.Core;
namespace Lodil.Windows;
public sealed record PipelineCheck(string Source,string Translation,long Milliseconds){
    public static string? MissingFile(string root){foreach(var name in new[]{"models/translation/encoder.onnx","models/translation/decoder.onnx","models/translation/tokenizer.bin","models/ocr/eng.traineddata","x64/tesseract50.dll","x64/leptonica-1.82.0.dll","onnxruntime.dll"})if(!File.Exists(Path.Combine(root,name)))return name;return null;}
    public static PipelineCheck Run(Translator translator,string root,CancellationToken token){
        token.ThrowIfCancellationRequested();var missing=MissingFile(root);if(missing!=null)throw new FileNotFoundException("A required local component is missing.",missing);
        using var image=new Bitmap(1180,160);using(var graphics=Graphics.FromImage(image)){graphics.Clear(Color.FromArgb(15,24,35));using var font=new Font("Arial",35,FontStyle.Bold,GraphicsUnit.Pixel);graphics.DrawString("We need to get out of here.",font,Brushes.White,new PointF(38,53));}
        using var ocr=new ScreenOcr(Path.Combine(root,"models","ocr"));var clock=Stopwatch.StartNew();var reading=ocr.ReadImage(image);token.ThrowIfCancellationRequested();
        var comparable=reading.Text.TrimEnd(' ','.','!','?');if(reading.Confidence<.5f||!comparable.Equals("We need to get out of here",StringComparison.OrdinalIgnoreCase))throw new InvalidOperationException("Sample OCR could not be read reliably.");
        var tr=translator.Translate(reading.Text,token);token.ThrowIfCancellationRequested();if(string.IsNullOrWhiteSpace(tr))throw new InvalidOperationException("No translation was produced.");return new(reading.Text,tr,clock.ElapsedMilliseconds);
    }
}
