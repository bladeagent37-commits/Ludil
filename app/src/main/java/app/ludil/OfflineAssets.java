package app.ludil;

import android.content.Context;
import java.io.*;

public final class OfflineAssets {
    private static MarianTranslator translator;
    private OfflineAssets(){}
    public static synchronized String translate(Context context,String text)throws Exception {
        if(translator==null){File dir=new File(context.getFilesDir(),"translation");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Model klasörü oluşturulamadı.");
            for(String name:new String[]{"encoder.onnx","decoder.onnx","tokenizer.bin"}){
                File output=new File(dir,name);long expected;
                try(android.content.res.AssetFileDescriptor afd=context.getAssets().openFd("translation/"+name)){expected=afd.getLength();}
                if(output.exists()&&output.length()==expected)continue;
                File temp=new File(dir,name+".tmp");
                try(InputStream in=context.getAssets().open("translation/"+name);OutputStream out=new FileOutputStream(temp)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
                if(temp.length()!=expected||!temp.renameTo(output))throw new IOException("Çeviri modeli kurulamadı. Boş alanı kontrol et.");
            }
            translator=new MarianTranslator(dir);
        }
        return translator.translate(text);
    }
}
