package app.ludil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

/** SentencePiece unigram segmentation for the bundled English source model. */
public final class MarianTokenizer {
    private static final class Piece { final int id; final float score; Piece(int i,float s){id=i;score=s;} }
    private final Map<String,Piece> pieces=new HashMap<>();
    private final String[] vocabulary;
    private int longest=1;
    private float unknownScore=-100;
    public final int pad, eos, language;
    public MarianTokenizer(File file)throws IOException {
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(file)))){
            if(in.readInt()!=0x4D414731)throw new IOException("Tokenizer biçimi geçersiz");
            pad=in.readInt();eos=in.readInt();language=in.readInt();int size=in.readInt();
            if(size<1000||size>100000)throw new IOException("Tokenizer boyutu geçersiz");vocabulary=new String[size];
            for(int i=0;i<size;i++)vocabulary[i]=string(in);
            int count=in.readInt();float minimum=0;
            for(int i=0;i<count;i++){String piece=string(in);float score=in.readFloat();int id=in.readInt();pieces.put(piece,new Piece(id,score));longest=Math.max(longest,piece.length());minimum=Math.min(minimum,score);}
            unknownScore=minimum-10;
        }
    }
    private static String string(DataInputStream in)throws IOException{int len=in.readInt();if(len<0||len>20000)throw new IOException("Tokenizer değeri geçersiz");byte[] b=new byte[len];in.readFully(b);return new String(b,StandardCharsets.UTF_8);}
    public long[] encode(String input){
        if(input==null||input.trim().isEmpty()||input.length()>600)throw new IllegalArgumentException("1–600 karakter arasında bir İngilizce altyazı gir.");
        String normalized=Normalizer.normalize(input,Normalizer.Form.NFKC).replaceAll("\\s+"," ").trim();
        String text="▁"+normalized.replace(' ','▁');int length=text.length();
        float[] best=new float[length+1];Arrays.fill(best,Float.NEGATIVE_INFINITY);best[0]=0;
        int[] previous=new int[length+1],token=new int[length+1];
        for(int i=0;i<length;i++){
            if(best[i]==Float.NEGATIVE_INFINITY)continue;
            for(int j=i+1;j<=Math.min(length,i+longest);j++){
                Piece p=pieces.get(text.substring(i,j));if(p!=null && best[i]+p.score>best[j]){best[j]=best[i]+p.score;previous[j]=i;token[j]=p.id;}
            }
            int end=i+Character.charCount(text.codePointAt(i));if(best[i]+unknownScore>best[end]){best[end]=best[i]+unknownScore;previous[end]=i;token[end]=1;}
        }
        List<Long> reversed=new ArrayList<>();for(int i=length;i>0;i=previous[i])reversed.add((long)token[i]);Collections.reverse(reversed);
        if(reversed.size()>110)throw new IllegalArgumentException("Altyazı tek seferde okunamayacak kadar uzun.");
        long[] ids=new long[reversed.size()+2];ids[0]=language;for(int i=0;i<reversed.size();i++)ids[i+1]=reversed.get(i);ids[ids.length-1]=eos;return ids;
    }
    public String decode(List<Integer> tokens){StringBuilder result=new StringBuilder();for(int id:tokens)if(id>1&&id<vocabulary.length&&id!=pad&&id!=language)result.append(vocabulary[id]);return result.toString().replace('▁',' ').trim();}
}
