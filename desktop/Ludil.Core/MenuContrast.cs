namespace Ludil.Core;

/// <summary>Normalizes light menu selection bars without altering dark-background rows.</summary>
public static class MenuContrast {
    public static byte[] NormalizeRgb(byte[] rgb,int width,int height){
        if(width<1||height<1||width>4000||height>4000||rgb.Length!=(long)width*height*3)throw new ArgumentException("Invalid menu image dimensions");
        var gray=new byte[width*height];
        for(var i=0;i<gray.Length;i++)gray[i]=(byte)((rgb[i*3]*77+rgb[i*3+1]*150+rgb[i*3+2]*29)>>8);
        var row=new byte[width];var histogram=new int[256];
        for(var y=0;y<height;y++){
            Array.Clear(histogram);var offset=y*width;Array.Copy(gray,offset,row,0,width);foreach(var value in row)histogram[value]++;
            var sum=0;var background=0;for(;background<255;background++){sum+=histogram[background];if(sum>width/2)break;}
            if(background<100)continue;
            var left=0;while(left<width&&row[left]<background-20)left++;var right=width-1;while(right>left&&row[right]<background-20)right--;
            for(var x=left;x<=right;x++)gray[offset+x]=(byte)(row[x]<background-45?255:0);
        }
        return gray;
    }
}
