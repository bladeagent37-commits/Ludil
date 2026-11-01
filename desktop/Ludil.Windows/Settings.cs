using System.Drawing;
using System.Text.Json;
namespace Ludil.Windows;
public sealed class GameProfile {
    public int X{get;set;}public int Y{get;set;}public int Width{get;set;}public int Height{get;set;}
    public int FontSize{get;set;}=26;public bool Yellow{get;set;}public bool Original{get;set;}
    public bool Above{get;set;}public int IntervalMs{get;set;}=700;
    public bool MenuMode{get;set;}
    [System.Text.Json.Serialization.JsonIgnore]public Rectangle Region=>new(X,Y,Width,Height);
    public GameProfile Copy()=>new(){X=X,Y=Y,Width=Width,Height=Height,FontSize=FontSize,Yellow=Yellow,Original=Original,Above=Above,IntervalMs=IntervalMs,MenuMode=MenuMode};
    public void SetRegion(Rectangle r){X=r.X;Y=r.Y;Width=r.Width;Height=r.Height;}
    public void Validate(){FontSize=Math.Clamp(FontSize,18,48);IntervalMs=Math.Clamp(IntervalMs,450,1500);}
}
public sealed class AppSettings {
    public string Selected{get;set;}="Varsayılan";
    public bool MinimizeOnStart{get;set;}=true;
    public Dictionary<string,GameProfile> Profiles{get;set;}=new(){["Varsayılan"]=new()};
    static string DirectoryPath=>Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"Ludil");
    public static AppSettings Parse(string json){
        try{var result=JsonSerializer.Deserialize<AppSettings>(json);if(result?.Profiles is not {Count:>0 and <=1000})return new();foreach(var p in result.Profiles.Values){if(p is null)return new();p.Validate();}if(string.IsNullOrWhiteSpace(result.Selected)||!result.Profiles.ContainsKey(result.Selected))result.Selected=result.Profiles.Keys.First();return result;}
        catch(Exception e)when(e is JsonException or ArgumentException){return new();}
    }
    public static AppSettings Load(){try{var path=Path.Combine(DirectoryPath,"settings.json");if(!File.Exists(path))path=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"Lodil","settings.json");return Parse(File.ReadAllText(path));}catch(Exception e)when(e is IOException or UnauthorizedAccessException){return new();}}
    public void Save(){Directory.CreateDirectory(DirectoryPath);var target=Path.Combine(DirectoryPath,"settings.json");var temporary=target+".tmp";File.WriteAllText(temporary,JsonSerializer.Serialize(this,new JsonSerializerOptions{WriteIndented=true}));File.Move(temporary,target,true);}
}
