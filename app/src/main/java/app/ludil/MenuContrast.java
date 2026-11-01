package app.ludil;
import java.util.Arrays;

/** Mirrors the production C# row-contrast normalization, including selected menu buttons. */
public final class MenuContrast {
    private MenuContrast(){}
    public static byte[] normalizeRgb(byte[] rgb,int width,int height){
        if(width<1||height<1||width>4000||height>4000||rgb.length!=(long)width*height*3)throw new IllegalArgumentException("Invalid menu image dimensions");
        byte[] gray=new byte[width*height];
        for(int i=0;i<gray.length;i++)gray[i]=(byte)(((rgb[i*3]&255)*77+(rgb[i*3+1]&255)*150+(rgb[i*3+2]&255)*29)>>8);
        byte[] row=new byte[width];int[] histogram=new int[256];
        for(int y=0;y<height;y++){
            Arrays.fill(histogram,0);int offset=y*width;System.arraycopy(gray,offset,row,0,width);for(byte value:row)histogram[value&255]++;
            int sum=0,background=0;for(;background<255;background++){sum+=histogram[background];if(sum>width/2)break;}
            if(background<100)continue;
            int left=0;while(left<width&&(row[left]&255)<background-20)left++;int right=width-1;while(right>left&&(row[right]&255)<background-20)right--;
            for(int x=left;x<=right;x++)gray[offset+x]=(byte)((row[x]&255)<background-45?255:0);
        }
        return gray;
    }
}
