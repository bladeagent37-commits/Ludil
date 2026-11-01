using System.Diagnostics;
using System.Text.Json;
using Ludil.Core;
namespace Ludil.Smoke;

internal static class VideoBatch {
    public static void Preprocess(string plan){
        using var items=JsonDocument.Parse(File.ReadAllText(plan));foreach(var item in items.RootElement.EnumerateArray()){
            var width=item.GetProperty("width").GetInt32();var height=item.GetProperty("height").GetInt32();var gray=MenuContrast.NormalizeRgb(File.ReadAllBytes(item.GetProperty("input").GetString()!),width,height);
            using var output=File.Create(item.GetProperty("output").GetString()!);var header=System.Text.Encoding.ASCII.GetBytes($"P5\n{width} {height}\n255\n");output.Write(header);output.Write(gray);
        }
    }
    public static void Run(string models,string input,string output){
        using var document=JsonDocument.Parse(File.ReadAllText(input));var records=new List<object>();var gates=new Dictionary<string,SubtitleGate>();var visible=new Dictionary<string,(string Source,string Text)>();
        Translator? translator=null;var menu=new MenuTextTranslator();
        try{foreach(var frame in document.RootElement.EnumerateArray()){
            var clip=frame.GetProperty("clip").GetString()!;var isMenu=frame.GetProperty("mode").GetString()=="menu";var time=frame.GetProperty("time_ms").GetInt64();var confidence=frame.GetProperty("confidence").GetSingle();var raw=frame.GetProperty("text").GetString()??"";
            var text=isMenu?MenuTextTranslator.Prepare(raw):Tokenizer.Normalize(raw);
            if(!gates.TryGetValue(clip,out var gate)){gate=new SubtitleGate(isMenu?2:3,isMenu?9000:4000);gates[clip]=gate;visible[clip]=("","");}
            var stable=gate.Observe(text,confidence,time);var current=visible[clip];
            if(!Tokenizer.Normalize(text).Equals(Tokenizer.Normalize(current.Source),StringComparison.OrdinalIgnoreCase)||confidence<.5f){current=("","");}
            var clock=Stopwatch.StartNew();var emitted=false;
            if(stable!=null){string Translate(string line)=>(translator??=new Translator(models)).Translate(line);var result=isMenu?menu.Translate(text,Translate):Translate(stable);current=(text,result);emitted=true;}
            visible[clip]=current;
            records.Add(new{clip,time_ms=time,mode=isMenu?"menu":"subtitle",input=text,confidence,translation=current.Text,emitted,translation_ms=clock.ElapsedMilliseconds});
        }}finally{translator?.Dispose();}
        File.WriteAllText(output,JsonSerializer.Serialize(records,new JsonSerializerOptions{WriteIndented=true,Encoder=System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping}));
        Console.WriteLine("VIDEO BATCH PASS: "+records.Count+" actual OCR samples through production Core translator/gates");
    }
}
