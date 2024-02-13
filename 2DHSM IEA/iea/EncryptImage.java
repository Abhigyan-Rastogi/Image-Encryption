package iea;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import javax.swing.*;
import java.io.*;

public class EncryptImage {
    public static void main(String args[]) {
        runTest();
        // packageResults();
    }
    public static void runTest() {
        String key = "AFE16E25A23D9D178D059526D0B5C63471429DB435794F8A359004B490AB4879";

        System.out.println("Showing original and Encrypted images!");

        ShowImage test = new ShowImage("images/test.jpg", "test");
        // ShowImage test = new ShowImage("iea/images/khushi_test.jpg", "test");
        
        IEA_2DHSM hndsm = new IEA_2DHSM();
        ShowImage test_encrypted = new ShowImage(hndsm.encrypt(key, test.P), "Encrypted test");

        System.out.println("Testing!");
        // System.out.println("original apc: " + IEA_Tests.adjacent_pixel_correlation(test.P));
        // System.out.println("encrypted apc: " + IEA_Tests.adjacent_pixel_correlation(test_encrypted.P));
        // test_encrypted.showImage(IEA_Tests.histogram_analysis(test.P));
        // test_encrypted.showImage(IEA_Tests.histogram_analysis(test_encrypted.P));
        test_encrypted.showImage(IEA_Tests.secret_key_security_analysis(test.P));
    }
    static class ShowImage {
        int[][] P;
        byte[] pixels;
        int width, height;
        BufferedImage bmg;
        Image img;
        String filePath, name;
        ShowImage() {

        }
        ShowImage(int[][] P, String name) {
            this.P = P;
            this.name = name;
            arrayToImage(P);
            showImage(bmg);
        }
        ShowImage(String filePath, String name) {
            this.filePath = filePath;
            this.name = name;
            try {
                File f = new File(filePath);
                img = ImageIO.read(f);
                width = img.getWidth(null);
                height = img.getHeight(null);
                bmg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                Graphics gr = bmg.getGraphics();
                gr.drawImage(img, 0, 0, null);
                gr.dispose();
                P = new int[height][width];
                pixels = ((DataBufferByte) bmg.getRaster().getDataBuffer()).getData();
                for(int i = 0, row = 0, col = 0; i < pixels.length; i ++) {
                    P[row][col] = (int)pixels[i];
                    col++;
                    if(col == width) {
                        col = 0;
                        row ++;
                    }
                }
            } catch(Exception e) {
                System.out.println("File load error! : " + e);
            }
            showImage(bmg);
        }
        public void showImage(BufferedImage bmg) {
            JFrame f = new JFrame(name);
            JPanel image = new JPanel() {
                @Override
                public void paintComponent(Graphics gr) {
                    gr.drawImage(bmg, 0, 0, null);
                }
            };
            image.setPreferredSize(new Dimension(width, height));
            f.add(image);

            f.setSize(bmg.getWidth(null) + 50, bmg.getHeight(null) + 50);
            f.setVisible(true);
            f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        }
        public void arrayToImage(int[][] P) {
            int w = P[0].length, h = P.length;
            bmg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for(int i = 0; i < h; i ++) {
                for(int j = 0; j < w; j ++) {
                    int val = (P[i][j]&0xFF) << 16 | (P[i][j]&0xFF) << 8 | (P[i][j]&0xFF);
                    bmg.setRGB(j, i, val);
                }
            }
        }
    }
    // static void packageResults() {
    //     JFrame f = new JFrame("2DHSM");
    //     GridBagLayout gbl = new GridBagLayout();
    //     GridBagConstraints gbc = new GridBagConstraints();
    //     f.setLayout(gbl);

    //     gbc.fill = GridBagConstraints.NONE;
    //     gbc.weightx = 1.0;
    //     gbc.weighty = 1.0;
    //     gbc.gridx = 0;
    //     gbc.gridy = 0;
    //     f.add(new JLabel("Original Image"), gbc);
    //     gbc.gridx = 1;
    //     gbc.gridy = 0;
    //     f.add(new JLabel("Encrypted Image"), gbc);

    //     gbc.gridx = 0;
    //     gbc.gridy = 1;
    //     f.add(new JLabel(new Icon()))

    //     f.setSize(500, 500);
    //     f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    //     f.setVisible(true);
    // }
}
