package iea;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.lang.Math;

public class IEA_Tests {
    private static long start = 0, finish = 0;
    public static BufferedImage histogram_analysis(int[][] P) {
        BufferedImage bmg = new BufferedImage(255, 255, BufferedImage.TYPE_BYTE_GRAY);
        Graphics gr = bmg.getGraphics();
        int[] count = new int[256];
        int M = P.length, N = P[0].length;
        for(int i = 0; i < M; i ++) {
            for(int j = 0; j < N; j ++) {
                count[(P[i][j]+256)%256] ++;
            }
        }

        int max = count[0];
        for(int i = 0; i < 255; i ++) {
            if(count[i] > max) 
                max = count[i];
        }


        gr.setColor(Color.WHITE);
        gr.fillRect(0, 0, bmg.getWidth(), bmg.getHeight());
        
        gr.setColor(Color.BLUE);
        for(int i = 0; i < 255; i ++) {
            gr.fillRect(i, 255-255*count[i]/max, 1, 255*count[i]/max);
        }
        gr.dispose();
        return bmg;
    }
    public static double adjacent_pixel_correlation(int[][] P) {
        int M = P.length, N = P[0].length;
        int[][] X = new int[M][N], Y = new int[M][N];
        for(int i = 0; i < M; i ++) {
            System.arraycopy(P[i], 0, X[i], 0, N);
            //adjacency relationship : pixel to the top
            if(i < M-1)
            System.arraycopy(P[i+1], 1, Y[i], 0, N-1);
            else
            System.arraycopy(P[0], 0, Y[i], 0, N-1);
        }
        float apc =  0, ux = 0, uy = 0, sdx = 0, sdy = 0;
        for(int i = 0; i < M; i ++) {
            for(int j = 0; j < N; j ++) {
                ux += X[i][j];
                uy += Y[i][j];
            }
        }
        ux /= M*N;
        uy /= M*N;
        for(int i = 0; i < M; i ++) {
            for(int j = 0; j < N; j ++) {
                sdx += Math.pow(X[i][j], 2) - ux*ux;
                sdy += Math.pow(Y[i][j], 2) - uy*uy;
            }
        }
        sdx /= M*N;
        sdy /= M*N;
        sdx = (float)Math.sqrt(sdx);
        sdy = (float)Math.sqrt(sdy);
        for(int i = 0; i < M; i ++) {
            for(int j = 0; j < N; j ++) {
                apc += (X[i][j] - ux)*(Y[i][j] - uy);
            }
        }
        apc /= (sdx*sdy);
        apc /= M*N;
        // System.out.println("mul : " + sdx*sdy);
        System.out.println("ux : " + ux);
        System.out.println("uy : " + uy);
        System.out.println("sdx : " + sdx);
        System.out.println("sdy : " + sdy);
        return apc;
    }
    public static BufferedImage secret_key_security_analysis(int[][] P) {
        BufferedImage ret = new BufferedImage(500, 500, BufferedImage.TYPE_BYTE_GRAY);
        Graphics gr = ret.getGraphics();

        int checkbits = 256;
        double[] nbcr = new double[checkbits+1];
        //generate random key
        long a01 = (long)(Math.random()*Long.MAX_VALUE) | ((long)1 << 63) | (long)(Math.random()*1023), 
             a02 = (long)(Math.random()*Long.MAX_VALUE) | ((long)1 << 63) | (long)(Math.random()*1023), 
             a03 = (long)(Math.random()*Long.MAX_VALUE) | ((long)1 << 63) | (long)(Math.random()*1023), 
             a04 = (long)(Math.random()*Long.MAX_VALUE) | ((long)1 << 63) | (long)(Math.random()*1023);
        String k0 = (Long.toHexString(a01) + Long.toHexString(a02) + Long.toHexString(a03) + Long.toHexString(a04)).toUpperCase();

        //encrypt P with k2
        int[][] c0 = new IEA_2DHSM().encrypt(k0, P);
        int len = P.length*P[0].length*8;
        String[] k = new String[checkbits+1];

        //create three iterations of random key, successive keys differing by 1 one binary digit k1, k2, k3
        for(int i = 1; i <= checkbits; i ++) {
            long a1 = a01, a2 = a02, a3 = a03, a4 = a04;
            switch((i-1)/64) {
                case 0 :
                    a4 = (a4 ^ ((long)1 << (i)));
                break;
                case 1 :
                    a3 = (a3 ^ ((long)1 << (i-64)));
                break;
                case 2 :
                    a2 = (a2 ^ ((long)1 << (i-128)));
                break;
                case 3 :
                    a1 = (a1 ^ ((long)1 << (i-192)));
                break;
            }
            k[i] = (Long.toHexString(a1) + Long.toHexString(a2) + Long.toHexString(a3) + Long.toHexString(a4)).toUpperCase();
            if(k[i].length()<64)
                k[i] = "1"+k[i];
            System.out.println(i+"\t"+k[i]);
        }

        //p21, p22, p23 are decryptions by keys k1, k2, k3
        int[][][] p0 = new int[checkbits+1][][];
        IEA_2DHSM hndsm = new IEA_2DHSM();
        p0[0] = hndsm.decrypt(k0, c0);
        //-->time test
        start = System.currentTimeMillis();
        for(int i = 1; i <= checkbits; i ++) {
            p0[i] = hndsm.decrypt(k[i], c0);
        }
        finish = System.currentTimeMillis();
        System.out.println("decryption time :" + (finish-start)/256);
        //find hamming difference between c1 and c0, c2 and c0 ...
        int xor = 0;
        for(int iter = 1; iter <= checkbits; iter ++) {
            double ham0i = 0;
            for(int i = 0; i < P.length; i ++) {
                for(int j = 0; j < P[0].length; j ++) {
                    xor = p0[iter][i][j]^p0[0][i][j];
                    while(xor != 0) {
                        if((xor&1)==1)
                            ham0i++;
                        xor = xor >> 1;
                    }
                    // String xor = Integer.toBinaryString(p0[iter][i][j]^p0[0][i][j]);
                    // int se = 0;
                    // while((se=xor.indexOf("1")) != -1) {
                    //     ham0i ++;
                    //     xor = xor.substring(0, se) + xor.substring(se+1);
                    // }
                }
            }
            double nbcr0i = ham0i/len;
            nbcr[iter] += nbcr0i;
        }

        //drawing a graph
        double upper = 0.545, lower = 0.455;
        double x_val = ret.getWidth()-40, x_off = 30;
        x_val /= checkbits;
        double y_val = (int)((ret.getHeight()-10)/(upper-lower)), y_off = -(int)(lower*y_val);
        gr.setColor(Color.white);
        gr.fillRect(0, 0, ret.getWidth(), ret.getHeight());
        gr.setColor(Color.black);
        int x = 0, y = 0, px = 0, py = 0;
        for(int i = 2; i <= checkbits; i ++) {
            x = (int)(x_off+i*x_val);
            y = (int)(y_off+nbcr[i]*y_val);
            px = (int)(x_off+(i-1)*x_val);
            py = (int)(y_off+nbcr[i-1]*y_val);
            gr.drawLine(px, py, x, y);
            if(i%20==0)
                gr.drawString(i+"", x, ret.getHeight()-20);
        }
        double val = (upper-lower)/10;
        y_val = (ret.getHeight()-10)/10;
        for(int i = 0; i < 10; i ++) {
            gr.drawString("0."+(int)((lower+i*val)*100), 0, (int)(i*y_val));
        }
        gr.dispose();
        return ret;
    }
}