using System.Text.Json;
using System.Text.RegularExpressions;
namespace Ludil.Core;

/// <summary>Shared, bounded menu glossary with per-line offline translation fallback.</summary>
public sealed class MenuTextTranslator {
    public const int MaximumLines=12;
    readonly Dictionary<string,string> terms;
    readonly string[] longest;
    public MenuTextTranslator(){using var stream=typeof(MenuTextTranslator).Assembly.GetManifestResourceStream("Ludil.MenuTerms")??throw new InvalidDataException("Menü sözlüğü eksik.");terms=JsonSerializer.Deserialize<Dictionary<string,string>>(stream)!.ToDictionary(p=>Tokenizer.Normalize(p.Key),p=>p.Value,StringComparer.OrdinalIgnoreCase);longest=terms.Keys.OrderByDescending(x=>x.Length).ToArray();}
    public static string Prepare(string raw){
        var lines=(raw??"").Replace("\r","").Split('\n').Select(Tokenizer.Normalize).Where(x=>x.Length is >=2 and <=160&&Regex.IsMatch(x,"[A-Za-z]{2}"));
        var accepted=new List<string>();var size=0;
        foreach(var line in lines){if(accepted.Count>=MaximumLines||size+line.Length+1>600)break;accepted.Add(line);size+=line.Length+1;}
        return string.Join("\n",accepted);
    }
    public string? Known(string line){
        line=Tokenizer.Normalize(line);
        if(terms.TryGetValue(line.TrimEnd('.'),out var hit))return hit;
        var hint=Regex.Match(line,@"^(\[[A-Za-z0-9+ ]{1,12}\]\s*|(?:Esc|Enter|Space|F\d{1,2})\s*[-:]\s*)(.+)$",RegexOptions.IgnoreCase|RegexOptions.CultureInvariant);
        if(hint.Success){var translated=Known(hint.Groups[2].Value);return translated==null?null:hint.Groups[1].Value+translated;}
        foreach(var key in longest){
            if(!line.StartsWith(key,StringComparison.OrdinalIgnoreCase)||line.Length<=key.Length)continue;
            var tail=line[key.Length..];if(!char.IsWhiteSpace(tail[0])&&tail[0]!=':')continue;
            var value=tail.Trim().TrimStart(':').Trim();if(value.Length==0)continue;
            if(terms.TryGetValue(value,out var translated))return terms[key]+": "+translated;
            if(Regex.IsMatch(value,@"^[\d\s.,%xX×:+/\-]+(?:\s*(?:Hz|FPS|ms))?$",RegexOptions.IgnoreCase|RegexOptions.CultureInvariant))return terms[key]+": "+value;
        }
        return null;
    }
    public string Translate(string raw,Func<string,string> fallback){
        var input=Prepare(raw);if(input.Length==0)return "";
        return string.Join("\n",input.Split('\n').Select(line=>Known(line)??fallback(line)));
    }
}
