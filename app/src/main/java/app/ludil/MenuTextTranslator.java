package app.ludil;

import org.json.JSONObject;
import org.json.JSONException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Uses the exact glossary embedded by the Windows core, with bounded per-line fallback. */
public final class MenuTextTranslator {
    public interface Fallback { String translate(String text)throws Exception; }
    public static final int MAXIMUM_LINES=12;
    private final Map<String,String> terms=new HashMap<>();
    private final List<String> longest=new ArrayList<>();
    public MenuTextTranslator(InputStream stream)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=stream.read(buffer))!=-1)bytes.write(buffer,0,n);
        try {
            JSONObject json=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            Iterator<String> keys=json.keys();while(keys.hasNext()){String key=keys.next();terms.put(SubtitleGate.normalize(key).toLowerCase(Locale.ROOT),json.getString(key));}
        } catch(JSONException error) { throw new IOException("Invalid menu glossary",error); }
        longest.addAll(terms.keySet());longest.sort((a,b)->Integer.compare(b.length(),a.length()));
    }
    public static String prepare(String raw){
        List<String> accepted=new ArrayList<>();int size=0;
        for(String line:(raw==null?"":raw).replace("\r","").split("\n")){
            line=SubtitleGate.normalize(line);if(line.length()<2||line.length()>160||!Pattern.compile("[A-Za-z]{2}").matcher(line).find())continue;
            if(accepted.size()>=MAXIMUM_LINES||size+line.length()+1>600)break;accepted.add(line);size+=line.length()+1;
        }
        return String.join("\n",accepted);
    }
    public String known(String line){
        line=SubtitleGate.normalize(line);String key=line.replaceAll("\\.+$","").toLowerCase(Locale.ROOT);String hit=terms.get(key);if(hit!=null)return hit;
        Matcher hint=Pattern.compile("^(\\[[A-Za-z0-9+ ]{1,12}\\]\\s*|(?:Esc|Enter|Space|F\\d{1,2})\\s*[-:]\\s*)(.+)$",Pattern.CASE_INSENSITIVE).matcher(line);
        if(hint.matches()){String value=known(hint.group(2));return value==null?null:hint.group(1)+value;}
        String lower=line.toLowerCase(Locale.ROOT);
        for(String label:longest){
            if(!lower.startsWith(label)||line.length()<=label.length())continue;
            String tail=line.substring(label.length());if(!Character.isWhitespace(tail.charAt(0))&&tail.charAt(0)!=':')continue;
            String value=tail.trim().replaceFirst("^:","").trim();if(value.isEmpty())continue;
            hit=terms.get(value.toLowerCase(Locale.ROOT));if(hit!=null)return terms.get(label)+": "+hit;
            if(Pattern.compile("^[\\d\\s.,%xX×:+/\\-]+(?:\\s*(?:Hz|FPS|ms))?$",Pattern.CASE_INSENSITIVE).matcher(value).matches())return terms.get(label)+": "+value;
        }
        return null;
    }
    public String translate(String raw,Fallback fallback)throws Exception {
        String input=prepare(raw);if(input.isEmpty())return "";List<String> result=new ArrayList<>();
        for(String line:input.split("\n")){String hit=known(line);result.add(hit==null?fallback.translate(line):hit);}
        return String.join("\n",result);
    }
}
