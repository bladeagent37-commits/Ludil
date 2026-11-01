package app.ludil;

import android.os.SystemClock;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

/** Small bounded HTTP receiver. Session credentials never leave the LAN or persist to disk. */
public final class LanReceiver implements AutoCloseable {
    public interface Listener { void subtitle(String original, String translated, int ttl); default void menuSubtitle(String original,String translated,int ttl){subtitle(original,translated,ttl);} void clear(); }
    private final Listener listener;
    private final String code, token;
    private final long deadline = SystemClock.elapsedRealtime()+300000;
    private final ServerSocket server;
    private final Set<Socket> clients=ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(2,2,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(4));
    private volatile boolean running=true;
    private boolean paired=false;
    private long lastSequence=-1, attemptWindow=0;
    private int attempts=0;
    public LanReceiver(Listener listener) throws IOException {
        this.listener=listener;
        SecureRandom random=new SecureRandom();code=String.format(Locale.ROOT,"%06d",random.nextInt(1000000));
        byte[] bytes=new byte[32];random.nextBytes(bytes);StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));token=s.toString();
        server=new ServerSocket();server.setReuseAddress(true);server.bind(new InetSocketAddress("0.0.0.0",8765),6);
        RuntimeState.pairingCode=code;RuntimeState.pairingDeadline=deadline;RuntimeState.address=localAddresses();
        Thread thread=new Thread(this::accept,"subtitle-listener");thread.setDaemon(true);thread.start();
    }
    public static String localAddresses() {
        List<String> addresses=new ArrayList<>();
        try { Enumeration<NetworkInterface> nets=NetworkInterface.getNetworkInterfaces();
            while(nets!=null && nets.hasMoreElements()) { NetworkInterface net=nets.nextElement();if(!net.isUp()||net.isLoopback())continue;
                Enumeration<InetAddress> ips=net.getInetAddresses();while(ips.hasMoreElements()){InetAddress ip=ips.nextElement();if(ip instanceof Inet4Address && ip.isSiteLocalAddress())addresses.add("http://"+ip.getHostAddress()+":8765");}
            }
        } catch(SocketException ignored) {}
        return addresses.isEmpty()?"Yerel ağ bağlantısı bulunamadı":String.join("\n",addresses);
    }
    private void accept() {
        while(running)try {Socket socket=server.accept();
            InetAddress ip=socket.getInetAddress();
            if(!ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && !ip.isLinkLocalAddress()){socket.close();continue;}
            clients.add(socket);if(!running){clients.remove(socket);socket.close();continue;}
            try{pool.execute(()->handle(socket));}catch(RejectedExecutionException e){clients.remove(socket);socket.close();}
        }catch(IOException e){if(running)RuntimeState.error="Yerel ağ dinleyicisi durdu.";}
    }
    private static String line(InputStream in,int limit)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();int c;
        while((c=in.read())!=-1){if(c=='\n')break;if(c!='\r')bytes.write(c);if(bytes.size()>limit)throw new IOException("Header too large");}
        if(c==-1 && bytes.size()==0)throw new EOFException();return bytes.toString("US-ASCII");
    }
    private void handle(Socket socket) {
        try(Socket s=socket){s.setSoTimeout(3000);InputStream in=new BufferedInputStream(s.getInputStream());
            String[] request=line(in,1024).split(" ");if(request.length!=3){reply(s,400,"{}");return;}
            Map<String,String> headers=new HashMap<>();int total=0;
            while(true){String h=line(in,2048);if(h.isEmpty())break;total+=h.length();if(total>8192)throw new IOException("Headers too large");int colon=h.indexOf(':');if(colon<1)throw new IOException("Bad header");String key=h.substring(0,colon).toLowerCase(Locale.ROOT);if(headers.put(key,h.substring(colon+1).trim())!=null)throw new IOException("Duplicate header");}
            String path=request[1];String method=request[0];
            if(headers.containsKey("origin")){reply(s,403,"{}");return;}
            if(method.equals("GET") && path.equals("/health")){reply(s,200,new JSONObject().put("app","Ludil").put("version","0.4.0").put("paired",paired).toString());return;}
            if(!method.equals("POST")){reply(s,405,"{}");return;}
            if(headers.containsKey("transfer-encoding") || !headers.getOrDefault("content-type","").toLowerCase(Locale.ROOT).startsWith("application/json")){reply(s,415,"{}");return;}
            int size=Integer.parseInt(headers.getOrDefault("content-length","0"));if(size<=0||size>8192){reply(s,413,"{}");return;}
            byte[] bytes=new byte[size];int offset=0,n;while(offset<size && (n=in.read(bytes,offset,size-offset))>0)offset+=n;if(offset!=size)throw new EOFException();
            JSONObject body=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
            synchronized(this){
                if(!running){reply(s,503,"{}");return;}
                if(path.equals("/pair")){
                    long now=SystemClock.elapsedRealtime();if(now-attemptWindow>60000){attemptWindow=now;attempts=0;}
                    if(++attempts>8){reply(s,429,"{}");return;}
                    if(now>deadline || paired || !same(code,body.optString("code",""))){reply(s,403,"{}");return;}
                    paired=true;RuntimeState.pairingCode="Eşleştirildi";
                    reply(s,200,new JSONObject().put("token",token).put("expires","service_stop").toString());return;
                }
                if(!paired || !same("Bearer "+token,headers.getOrDefault("authorization",""))){reply(s,401,"{}");return;}
                long sequence=body.optLong("id",-1);if(sequence<=lastSequence){reply(s,409,"{}");return;}
                if(path.equals("/subtitle")){
                    String tr=body.optString("translatedText","").trim(),source=body.optString("sourceText","").trim();
                    if(tr.isEmpty()||tr.length()>1600||source.length()>800){reply(s,422,"{}");return;}
                    int ttl=Math.max(500,Math.min(10000,body.optInt("ttlMs",4500)));
                    lastSequence=sequence;if(body.optBoolean("menu",false))listener.menuSubtitle(source,tr,ttl);else listener.subtitle(source,tr,ttl);reply(s,200,"{\"ok\":true}");return;
                }
                if(path.equals("/clear")){lastSequence=sequence;listener.clear();reply(s,200,"{\"ok\":true}");return;}
                reply(s,404,"{}");
            }
        }catch(Exception ignored){/* Untrusted requests must not kill the listener or be logged. */}finally{clients.remove(socket);}
    }
    private static boolean same(String a,String b){return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    private static void reply(Socket s,int code,String body)throws IOException {
        byte[] data=body.getBytes(StandardCharsets.UTF_8);String header="HTTP/1.1 "+code+" Result\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: "+data.length+"\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n";
        OutputStream out=s.getOutputStream();out.write(header.getBytes(StandardCharsets.US_ASCII));out.write(data);out.flush();
    }
    @Override public synchronized void close(){running=false;paired=false;try{server.close();}catch(IOException ignored){}for(Socket socket:clients)try{socket.close();}catch(IOException ignored){}clients.clear();pool.shutdownNow();RuntimeState.pairingCode="";RuntimeState.pairingDeadline=0;}
}
