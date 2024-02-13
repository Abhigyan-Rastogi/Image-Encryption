package iea_cleaner;
import java.lang.Math;
public class IEA_2DHSM {
    final static double two_32 = (long) Math.pow(2, 32);
    final static int[] primes = new int[256];
    final static int[] prime_data;
    static {
        prime_data = new int[]{2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 
            53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 
            137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 
            223, 227, 229, 233, 239, 241, 251};
        for(int i = 0; i < prime_data.length; i ++)
            primes[prime_data[i]] = prime_data[i];
    }
    static class S_Matrix {
        double x, y, b2;
        int val[][];
        static final double b1 = 1.57;
        static final double w = 10;
        S_Matrix(double x, double y, double b2, int width, int height) {
            this.x = x;
            this.y = y;
            this.b2 = b2;
            this.val = new int[height][width];
            //DECRYPTION SHOULD BE SAME
            //arbitrarily deciding to store generated values of x ^ y in S matrix
            for(int i = 0; i < height; i ++) {
                for(int j = 0; j < width; j ++) {
                    x = 0.5 * (1 - Math.sin(1 - w*b1*x*x - w*b2*y));
                    y = b2*x;
                    this.val[i][j] = ((int)(x*255) ^ (int)(y*255)) % 255;
                }
            }
        }
        public static S_Matrix[] createMatrix(String key, int width, int height) {
            S_Matrix[] s = new S_Matrix[2];
            key.toUpperCase();
            if(key.length() != 64 || key.matches("[^A-Fa-f0-9]*")) {
                System.out.println("Illegal key :" + key);
            }
            long[] subkeys = new long[9];
            for(int i = 1; i < 9; i ++) {
                try {
                    subkeys[i] = fromHexString(key.substring(0, 8));
                } catch(StringIndexOutOfBoundsException siobe) {
                    System.out.println(siobe.getMessage() + "\n" + key);
                }
                // System.out.println("-> " + subkeys[i]);
                key = key.substring(8, key.length());
            }
            long[][] kst = new long[9][9];
            for(int i = 1; i <= 4; i ++) {
                for(int j = 5; j <= 8; j ++) {
                    kst[i][j] = subkeys[i] ^ subkeys[j];
                }
            }
            // System.out.println(((kst[3][5]*kst[4][6]) + (kst[3][7]*kst[4][8]))/two_32);
            double x1 =  (((kst[1][5]*kst[2][6]) + (kst[1][7]*kst[2][8]))/two_32) % 1,
                    y1 = (((kst[2][5]*kst[3][6]) + (kst[2][7]*kst[3][8]))/two_32) % 1,
                    x2 = (((kst[2][5]*kst[3][6]) + (kst[2][7]*kst[2][8]))/two_32) % 1,
                    y2 = (((kst[3][5]*kst[4][6]) + (kst[3][7]*kst[4][8]))/two_32) % 1,
                    b1 = 4.9 + (((kst[3][5]*kst[4][6]) + (kst[3][7]*kst[4][8]))/two_32) % 0.1,
                    b2 = 4.9 + (((kst[4][5]*kst[1][6]) + (kst[4][7]*kst[1][8]))/two_32) % 0.1;
            
            s = new S_Matrix[]{
                new S_Matrix(x1, y1, b1, width, height),
                new S_Matrix(x2, y2, b2, width, height)
            };
            // System.out.println(kst[0][4]);
            return s;
        }
        public static long fromHexString(String hex) {
            //assuming hex to be 8 digits long
            long ret = 0;
            long count = 1;
            for(int i = 7; i >= 0; i --) {
                char ch = hex.charAt(i);
                int val = 0;

                if((val = "ABCDEF".indexOf(ch)) != -1)
                    val += 10;
                else
                    val = (int)ch - 48;

                ret += count*val;
                count *= 16;
            }
            return ret;
        }
    }
    public int[][] encrypt(String key, int[][] P_in) {
        //1.Image P as plain Image (Stored in P)
        int[][] P = new int[P_in.length][P_in[0].length];
        for(int i = 0; i < P_in.length; i ++) {
            System.arraycopy(P_in[i], 0, P[i], 0, P[i].length);
        }
        //2.Adding a border in the image with the randomized pixels        
        P = addRandomBorder(P);
        
        //3.Initialization of 2D-HSM chatic maps and generation of s1 and s2 matrix
        S_Matrix[] s = S_Matrix.createMatrix(key, P[0].length, P.length);

        //4.Encrypting using prescribed algorithm
        P = encryptImage(s, P);

        return P;
    }
    public int[][] encryptImage(S_Matrix[] s, int[][] P) {
        int l = 0, m = 0;
        int M = P.length, N = P[0].length;
        for(int r = 1; r <= 2; r ++) {
            if(r == 1) {
                l = 0;//1
                m = 1;//2
            } else {
                l = 1;//2
                m = 0;//1
            }
            for(int i = 0; i < M; i ++) {
                for(int j = 0; j < N; j ++) {
                    if(isPrime(s[l].val[i][j])) {
                        //Interchange P'[i][j] (image with border) with P'[(i+s[l].val[i][j] mod M)][(j+s[l].val[i][j] mod N)]
                        P = interchange(P, i, j, s[l].val[i][j], M, N);
                    } else 
                    if(s[l].val[i][j] % 2 == 0) {
                        //cyclic shift ith row from left to right by s[l].val[i][j] steps
                        P = cyclicShiftLR(P, i, s[l].val[i][j]);
                    } else {
                        //cyclic shift jth column from top to bottom by s[l].val[i][j] steps
                        P = cyclicShiftTB(P, j, s[l].val[i][j]);
                    }
                }
            }
            int q = l;
            for(int i = 0; i < M; i ++) {
                for(int j = 0; j < N; j ++) {
                    P[i][j] = s[q].val[i][j] ^ P[i][j];
                }
            }
            q = m;
            for(int i = 0; i < M; i ++) {
                for(int j = 0; j < N; j ++) {
                    P[i][j] = s[q].val[i][j] ^ P[i][j];
                }
            }
            //diffusion 1 on P' using s[m]
            P = diffusion1Enc(s[m].val, P);

            if(r == 1) {
                //diffusion 2 on P' using s[l]
                P = diffusion2Enc(s[0].val, P);
            }
        }
        return P;
    }
    public int[][] decrypt(String key, int[][] P_in) {
        int[][] P = new int[P_in.length][P_in[0].length];
        for(int i = 0; i < P_in.length; i ++) {
            System.arraycopy(P_in[i], 0, P[i], 0, P[i].length);
        }
        //3.Initialization of 2D-HSM chatic maps and generation of s1 and s2 matrix
        S_Matrix[] s = S_Matrix.createMatrix(key, P[0].length, P.length);

        //4.Encrypting using prescribed algorithm
        P = decryptImage(s, P);
        
        //2.Adding a border in the image with the randomized pixels        
        P = removeRandomBorder(P);
        
        //1.Image P as plain Image (Stored in P)

        return P;
    }
    public int[][] decryptImage(S_Matrix[] s, int[][] P) {
        int l = 0, m = 0;
        int M = P.length, N = P[0].length;
        int li = M - 1, lj = N - 1;
        for(int r = 1; r <= 2; r ++) {
            if(r == 1) {
                l = 1;//1
                m = 0;//2
            } else {
                l = 0;//2
                m = 1;//1
            }
            //diffusion 1 on P' using s[m]
            P = diffusion1Dec(s[m].val, P);
            int q = l;
            for(int i = 0; i < M; i ++) {
                for(int j = 0; j < N; j ++) {
                    P[i][j] = s[q].val[i][j] ^ P[i][j];
                }
            }
            q = m;
            for(int i = 0; i < M; i ++) {
                for(int j = 0; j < N; j ++) {
                    P[i][j] = s[q].val[i][j] ^ P[i][j];
                }
            }
            for(int i = li; i >= 0; i --) {
                for(int j = lj; j >= 0; j --) {
                    if(isPrime(s[l].val[i][j])) {
                        //Interchange P'[i][j] (image with border) with P'[(i+s[l].val[i][j] mod M)][(j+s[l].val[i][j] mod N)]
                        P = interchange(P, i, j, s[l].val[i][j], M, N);
                    } else 
                    if(s[l].val[i][j] % 2 == 0) {
                        //cyclic shift ith row from left to right by s[l].val[i][j] steps
                        P = cyclicShiftRL(P, i, s[l].val[i][j]);
                    } else {
                        //cyclic shift jth column from top to bottom by s[l].val[i][j] steps
                        P = cyclicShiftBT(P, j, s[l].val[i][j]);
                    }
                }
            }
            if(r == 1) {
                //diffusion 2 on P' using s[l]
                P = diffusion2Dec(s[0].val, P);
            }
        }
        return P;
    }
    public static int[][] diffusion1Enc(int[][] Sm, int[][] P) {
        int M = P.length, N = P[0].length;
        int li = M-1, lj = N-1;
        for(int i = 0; i < M; i ++) {
            for(int j = 0; j < N; j ++) {
                if(i == 0 && j == 0) {
                    P[i][j] = P[i][j] ^ P[li][lj] ^ Sm[i][j];
                } else if(i == 0) {
                    P[i][j] = P[i][j] ^ P[i][j-1] ^ Sm[i][j];
                } else {
                    P[i][j] = P[i][j] ^ P[i-1][j] ^ Sm[i][j];
                }
            }
        }
        return P;
    }
    public static int[][] diffusion1Dec(int[][] Sm, int[][] P) {
        int M = P.length, N = P[0].length;
        int li = M-1, lj = N-1;
        for(int i = li; i >= 0; i --) {
            for(int j = lj; j >= 0; j --) {
                if(i == 0 && j == 0) {
                    P[i][j] = P[i][j] ^ P[li][lj] ^ Sm[i][j];
                } else if(i == 0) {
                    P[i][j] = P[i][j] ^ P[i][j-1] ^ Sm[i][j];
                } else {
                    P[i][j] = P[i][j] ^ P[i-1][j] ^ Sm[i][j];
                }
            }
        }
        return P;
    }
    public static int[][] diffusion2Enc(int[][] Sl, int[][] P) {
        int M = P.length, N = P[0].length;
        int li = M-1, lj = N-1;
        int[][] ret = P;//new int[M][N];
        for(int i = 0; i < M; i ++) {
            for(int j = 0; j < N; j ++) {
                if(i == 0 && j == 0) {
                    ret[i][j] += Sl[i][j] + P[li][j] + P[i][lj];
                } else if(i == 0) {
                    ret[i][j] += Sl[i][j] + P[i][j-1];
                } else if(j == 0) {
                    ret[i][j] += Sl[i][j] + P[i-1][j];
                } else {
                    ret[i][j] += Sl[i][j] + P[i][0] + P[i-1][j];
                }
                ret[i][j] = ((ret[i][j] % 256) + 256) % 256;
            }
        }
        return ret;
        /*if(i == 0 && j == 0) {
                    ret[i][j] = (P[li][j] + P[i][lj] + Sl[i][j])%256;
                } else if(i == 0) {
                    ret[i][j] = (P[i][j-1] + Sl[i][j])%256;
                } else if(j == 0) {
                    ret[i][j] = (P[i-1][j] + Sl[i][j])%256;
                } else {
                    ret[i][j] = (P[i][j-1] + P[i-1][j] + Sl[i][j])%256;
                } */
    }
    public static int[][] diffusion2Dec(int[][] Sl, int[][] P) {
        int M = P.length, N = P[0].length;
        int li = M-1, lj = N-1;
        int[][] ret = P;//new int[M][N];
        for(int i = li; i >= 0; i --) {
            for(int j = lj; j >= 0; j --) {
                if(i != 0 && j != 0) {
                    ret[i][j] += -Sl[i][j] - P[i-1][j] - P[i][0];
                } else if(i > 0 && j == 0) {
                    ret[i][j] += -Sl[i][j] - P[i-1][j];
                } else if(i == 0 && j > 0) {
                    ret[i][j] += -Sl[i][j] - P[i][j-1];
                } else {
                    ret[i][j] += -Sl[i][j] - P[i][lj] - P[li][j];
                }
                ret[i][j] = ((ret[i][j] % 256) + 256) % 256;
            }
        }
        return ret;
        /*
                if(i != 0 && j != 0) {
                    ret[i][j] = (-P[i-1][j] - P[i][j-1] + Sl[i][j] + 256)%256;
                } else if(i > 0 && j == 0) {
                    ret[i][j] = (-P[i-1][j] + Sl[i][j] + 256)%256;
                } else if(i == 0 && j > 0) {
                    ret[i][j] = (-P[i][j-1] + Sl[i][j] + 256)%256;
                } else {
                    ret[i][j] = (-P[i][lj] - P[li][j] + Sl[i][j] + 256)%256;
                } */
    }
    public static int[][] cyclicShiftLR(int[][] P, int i, int steps) {
        int t[] = new int[P[0].length];

        for(int k = 0; k < P[0].length-steps; k ++) {
            t[k+steps] = P[i][k];
        }
        int offset = P[0].length - steps;
        for(int k = 0; k < steps; k ++) {
            t[k] = P[i][offset + k];
        }
        P[i] = t;
        return P;
    }
    public static int[][] cyclicShiftRL(int[][] P, int i, int steps) {
        int t[] = new int[P[0].length];

        for(int k = 0; k < P[0].length-steps; k ++) {
            t[k] = P[i][k+steps];
        }
        int offset = P[0].length - steps;
        for(int k = 0; k < steps; k ++) {
            t[k + offset] = P[i][k];
        }
        P[i] = t;
        return P;
    }
    public static int[][] cyclicShiftTB(int[][] P, int j, int steps) {
        int t[] = new int[P.length];

        for(int k = 0; k < P.length-steps; k ++) {
            t[k+steps] = P[k][j];
        }
        int offset = P.length - steps;
        for(int k = 0; k < steps; k ++) {
            t[k] = P[offset + k][j];
        }
        for(int i = 0; i < P.length; i ++) 
            P[i][j] = t[i];
        return P;
    }
    public static int[][] cyclicShiftBT(int[][] P, int j, int steps) {
        int t[] = new int[P.length];

        for(int k = 0; k < P.length-steps; k ++) {
            t[k] = P[k+steps][j];
        }
        int offset = P.length - steps;
        for(int k = 0; k < steps; k ++) {
            t[offset+k] = P[k][j];
        }
        for(int i = 0; i < P.length; i ++) 
            P[i][j] = t[i];
        return P;
    }
    public static int[][] interchange(int[][] P, int i, int j, int sl, int M, int N) {
        int idash = (i + sl) % M,
            jdash = (j + sl) % N;
        int t = P[i][j];
        P[i][j] = P[idash][jdash];
        P[idash][jdash] = t;
        return P;
    }
    public static boolean isPrime(int n) {
        return primes[n] == n;
    }
    public int[][] addRandomBorder(int[][] P) {
        int[][] ret = new int[P.length+2][P[0].length+2];
        int newPixel;
        for(int i = 0; i < ret[0].length; i ++) {
            newPixel = (int)(Math.random()*255);
            ret[0][i] = newPixel;
            newPixel = (int)(Math.random()*255);
            ret[ret.length-1][i] = newPixel;
        }
        for(int i = 0; i < ret.length; i ++) {
            newPixel = (int)(Math.random()*255);
            ret[i][0] = newPixel;
            newPixel = (int)(Math.random()*255);
            ret[i][ret[0].length-1] = newPixel;
        }
        for(int i = 1; i < ret.length-1; i ++) {
            for(int j = 1; j < ret[0].length-1; j ++) {
                ret[i][j] = P[i-1][j-1];
            }
        }
        return ret;
    }
    public int[][] removeRandomBorder(int[][] P) {
        int[][] ret = new int[P.length-2][P[0].length-2];
        for(int i = 0; i < ret.length; i ++) {
            for(int j = 0; j < ret[0].length; j ++) {
                ret[i][j] = P[i+1][j+1];
            }
        }
        return ret;
    }
    public static void printArray(int[][] P) {
        int M = P.length, N = P[0].length;
        for(int i = 0; i < M; i ++) {
            for(int j = 0; j < N; j ++) {
                System.out.print(P[i][j] + "\t");
            }
            System.out.println();
        }
    }
}
