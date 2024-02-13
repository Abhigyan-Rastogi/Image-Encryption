package iea_cleaner;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;

import javax.imageio.ImageIO;

public class ImageParser {
    public int[][] P;
    public int val;
    public BufferedImage bi;
    public Image i;
    public ImageParser(String filePath) {
        try {
            bi = ImageIO.read(new FileInputStream(filePath));
        } catch(Exception e) {
            System.out.println("Image Parser Error!");
            e.getMessage();
        }
    }
    public ImageParser(int[][] P) {
        int w = P[0].length, h = P.length;
        bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for(int i = 0; i < h; i ++) {
            for(int j = 0; j < w; j ++) {
                int val = (P[i][j]&0xFF) << 16 | (P[i][j]&0xFF) << 8 | (P[i][j]&0xFF);
                bi.setRGB(j, i, val);
            }
        }
    }
}
