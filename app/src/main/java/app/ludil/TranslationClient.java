package app.ludil;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Only the explicitly configured, private-LAN server receives subtitle text. */
public final class TranslationClient {
    private final Prefs prefs;
    private final android.content.Context context;
    private MenuTextTranslator menuTranslator;
    private final Map<String,String> cache = new LinkedHashMap<String,String>(128,0.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,String> e) { return size()>160; }
    };
    public TranslationClient(android.content.Context context) { this.context=context.getApplicationContext(); this.prefs=new Prefs(this.context); }
    public static URL privateUrl(String value) throws Exception {
        URL url = new URL(value);
        if (!url.getProtocol().equals("http") || url.getUserInfo()!=null || !url.getPath().matches("/?") || url.getQuery()!=null || url.getRef()!=null)
            throw new IOException("Sunucu adresi http://192.168.1.20:8766 biçiminde olmalı.");
        InetAddress[] addresses = InetAddress.getAllByName(url.getHost());
        for (InetAddress a:addresses) if (!a.isSiteLocalAddress() && !a.isLoopbackAddress() && !a.isLinkLocalAddress())
            throw new IOException("Yalnızca ev ağındaki sunuculara izin verilir.");
        return url;
    }
    public String translate(String source) throws Exception {
        source=SubtitleGate.normalize(source);
        if(source.isEmpty()||source.length()>600)throw new IOException("1–600 karakter arasında bir İngilizce altyazı gir.");
        String cached=cache.get(source); if(cached!=null)return cached;
        String endpoint=prefs.text("server", "").trim();
        if(endpoint.isEmpty())return OfflineAssets.translate(context,source);
        URL base=privateUrl(endpoint);
        HttpURLConnection conn=(HttpURLConnection)new URL(base.getProtocol(),base.getHost(),base.getPort(),"/translate").openConnection(Proxy.NO_PROXY);
        conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(2500);conn.setReadTimeout(12000);
        conn.setRequestMethod("POST");conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
        conn.setRequestProperty("Authorization","Bearer "+prefs.text("server_token",""));
        byte[] body=new JSONObject().put("q",source).put("source","en").put("target","tr").toString().getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(body.length);
        try {
            try(OutputStream out=conn.getOutputStream()){out.write(body);}
            if(conn.getResponseCode()!=200)throw new IOException("Çeviri sunucusu yanıtı: "+conn.getResponseCode());
            ByteArrayOutputStream data=new ByteArrayOutputStream();
            try(InputStream in=conn.getInputStream()) { byte[] buf=new byte[2048];int n;while((n=in.read(buf))!=-1){data.write(buf,0,n);if(data.size()>16384)throw new IOException("Sunucu yanıtı çok büyük.");}}
            String text=new JSONObject(data.toString("UTF-8")).optString("translatedText","").trim();
            if(text.isEmpty() || text.length()>1600)throw new IOException("Sunucu geçerli çeviri vermedi.");
            cache.put(source,text);return text;
        } finally { conn.disconnect(); }
    }
    public String translateMenu(String source)throws Exception {
        if(menuTranslator==null)try(InputStream in=context.getAssets().open("menu-terms.json")){menuTranslator=new MenuTextTranslator(in);}
        return menuTranslator.translate(source,this::translate);
    }
}
