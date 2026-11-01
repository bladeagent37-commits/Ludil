import app.ludil.*;
import org.json.*;
import java.io.*;
import java.nio.file.*;
public final class MenuRegression {
    public static void main(String[] args)throws Exception{
        MenuTextTranslator menu;try(InputStream stream=new FileInputStream(args[0])){menu=new MenuTextTranslator(stream);}
        JSONArray cases=new JSONArray(Files.readString(Path.of(args[1])));int checks=0;
        for(int i=0;i<cases.length();i++){JSONObject item=cases.getJSONObject(i);String source=item.getString("source");if(!item.getString("expected").equals(menu.known(source)))throw new AssertionError("Menu glossary: "+source);checks++;}
        String value=menu.translate("New Game\nContinue\nSettings",line->{throw new AssertionError("Known menu must not load ONNX");});if(!value.equals("Yeni Oyun\nDevam Et\nAyarlar"))throw new AssertionError("Line boundaries lost");checks++;
        int[] calls={0};value=menu.translate("Unknown sentence.\nSettings",line->{calls[0]++;return "Bilinmeyen cümle.";});if(!value.equals("Bilinmeyen cümle.\nAyarlar")||calls[0]!=1)throw new AssertionError("Fallback routing");checks++;
        if(!MenuTextTranslator.prepare("1234\n42%\nSettings").equals("Settings"))throw new AssertionError("Numeric clutter");checks++;
        if(MenuTextTranslator.prepare("Settings\n".repeat(30)).split("\n").length!=12)throw new AssertionError("Menu line bound");checks++;
        SubtitleGate gate=new SubtitleGate(2,9000);if(gate.observe("On",99,10000)!=null||!"On".equals(gate.observe("On",99,10500)))throw new AssertionError("Two-letter menu values");checks++;
        for(int width:new int[]{360,1280,1920})for(boolean right:new boolean[]{false,true}){int[] r=CaptureGeometry.menuRegion(width,720,right);float panelStart=right?.02f:.66f,panelEnd=right?.34f:.98f;if(r[0]<0||r[0]+r[2]>width||r[1]+r[3]>720||!(r[0]+r[2]<=width*panelStart+1||r[0]>=width*panelEnd-1))throw new AssertionError("Menu capture overlaps translation panel");checks++;}
        byte[] rgb=new byte[100*4*3];for(int y=0;y<4;y++)for(int x=0;x<100;x++){int i=(y*100+x)*3;int[] background=y==1?new int[]{255,139,120}:y==2?new int[]{255,255,255}:new int[]{10,10,10};for(int c=0;c<3;c++)rgb[i+c]=(byte)background[c];if(x>=45&&x<=50&&y<3)for(int c=0;c<3;c++)rgb[i+c]=(byte)(y==0?245:10);}
        byte[] gray=MenuContrast.normalizeRgb(rgb,100,4);if((gray[45]&255)<200||(gray[5]&255)!=10)throw new AssertionError("Dark menu rows");checks++;
        if((gray[145]&255)!=255||(gray[105]&255)!=0)throw new AssertionError("Selected-row contrast");checks++;
        if((gray[245]&255)!=255||(gray[205]&255)!=0)throw new AssertionError("Light menu background");checks++;
        if((gray[305]&255)!=10)throw new AssertionError("Blank dark rows");checks++;
        System.out.println("JAVA MENU REGRESSIONS PASS: "+checks+" checks");
    }
}
