package vn.edu.uit.lehuutai.e3c2chat.Crypto;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by lehuu on 3/21/2017.
 */

public class Point {
    public BigInteger x;
    public BigInteger y;

    public Point() {
        this.x = BigInteger.ONE.negate();
        this.y = BigInteger.ONE.negate();
    }

    public Point(BigInteger x, BigInteger y){
        this.x = x;
        this.y = y;
    }

    public void setValue(BigInteger x, BigInteger y) {
        this.x = x;
        this.y = y;
    }

    /*Is infinity point?*/
    public boolean isPOSITIVE_INFINITY() {
        BigInteger val = BigInteger.ONE.negate();
        return (y).compareTo(val) == 0
                || (x).compareTo(val) == 0;
    }

    /*Is negative point of other point?*/
    public boolean isNEGATIVE_P(Point P) {
        return ((P.x).compareTo(x) == 0)
                && ((P.y).compareTo((y).negate()) == 0);
    }

    /*point add point method*/
    public Point Pointaddition(Point Q, EllipticCurve E) {
        Point P = new Point(x,y);
        BigInteger negONE = BigInteger.ONE.negate();
        Point R = new Point(negONE, negONE);
        BigInteger numerator, denominator;
        if (Q.isPOSITIVE_INFINITY()) {
            return P;
        } else if (P.isPOSITIVE_INFINITY()) {
            return Q;
        }
        if (Q.isNEGATIVE_P(P)) {
            return R;
        }
        numerator = (Q.y).subtract(P.y).mod(E.p);
        denominator = (Q.x).subtract(P.x);
        if (denominator.compareTo(BigInteger.ZERO) == 0) {
            return R;
        }
        denominator = denominator.modInverse(E.p);
        numerator = numerator.multiply(denominator).mod(E.p);
        R.x = numerator.pow(2).mod(E.p);
        R.x = (R.x).subtract(P.x).subtract(Q.x).mod(E.p);
        R.y = (P.x).subtract(R.x).mod(E.p).multiply(numerator).mod(E.p);
        R.y = (R.y).subtract(P.y).mod(E.p);
        return R;
    }

    /*point doubling method*/
    public Point Pointdoubling(EllipticCurve E) {
        Point R = new Point();
        BigInteger numerator, denominator;

        numerator = BigInteger.valueOf(3).multiply(x.pow(2)).add(E.a).mod(E.p);
        denominator = BigInteger.valueOf(2).multiply(y);
        if (denominator.compareTo(BigInteger.ZERO) == 0) {
            return R;
        }
        denominator = denominator.modInverse(E.p);
        numerator = numerator.multiply(denominator).mod(E.p);
        R.x = numerator.pow(2).mod(E.p);
        R.x = (R.x).subtract(BigInteger.valueOf(2).multiply(x).mod(E.p)).mod(E.p);
        R.y = numerator.multiply((x).subtract(R.x).mod(E.p)).mod(E.p);
        R.y = (R.y).subtract(y).mod(E.p);
        return R;
    }

    /*compare 2 points*/
    public boolean compareTo(Point Q) {
        return x.compareTo(Q.x) == 0 && y.compareTo(Q.y) == 0;
    }

    /*this is a self-written hash function*/
    public BigInteger hashCoordinate() {
        BigInteger hash = new BigInteger(256, new Random());
        hash = ((hash.add(x)).shiftLeft(5)).subtract((hash.add(x)));
        hash = ((hash.add(y)).shiftLeft(5)).subtract((hash.add(y)));
        return hash;
    }

    /*showing a point on the screen*/
    public String println(String header) {
        return (header + ": (" + x + "," + y + ")");
    }

    /*compute fast multiplication of points using array*/
    public Point kPoint(EllipticCurve E, BigInteger k) {
        SupportFunctions supportFunctions = new SupportFunctions();
        ArrayList position = supportFunctions.positionPowersOfTwoPartition(k);
        int size = position.size();

        Point P = new Point(this.x, this.y);
        Point[] Q = new Point[size];
        BigInteger TWO = new BigInteger("2");

        //tính toán các điểm theo vị trí bit 1 của số k
        //VD: 11 = 1011 --> lưu trong mảng Q = {P, 2P, 8P}
        for(int i = 0; i < size; i++) {
            int currentElementValue = (int) position.get(i);
            BigInteger value = TWO.pow(currentElementValue);

            if (i == 0) {
                if(value.equals(BigInteger.ONE)) {
                    Q[i] = P;
                } else {
                    for(int j = 0; j < currentElementValue; j++) {
                        if(j == 0) {
                            Q[i] = P.Pointdoubling(E);
                        } else {
                            Q[i] = Q[i].Pointdoubling(E);
                        }
                    }
                }
            } else {
                int previousElementValue = (int) position.get(i - 1);
                int loop = currentElementValue - previousElementValue;
                for(int j = 0; j < loop; j++) {
                    if(j == 0) {
                        Q[i] = Q[i - 1].Pointdoubling(E);
                    } else {
                        Q[i] = Q[i].Pointdoubling(E);
                    }
                }
            }
        }

        //trả về kết quả k lần P bằng cách cộng tất cả các điểm trong mảng Q
        P = Q[0];
        for(int i = 1; i < size; i++) {
            P = P.Pointaddition(Q[i], E);
        }

        return P;
    }

    /*compute fast multiplication of points using file*/
    public Point kPoint(EllipticCurve E, BigInteger k, String savePath) {
        SupportFunctions obj = new SupportFunctions();
        ArrayList position = obj.positionPowersOfTwoPartition(k);
        int size = position.size();
        Point P = new Point(this.x, this.y);
        obj.tablePointDoubling(E, P, k, savePath);

        File file = new File(savePath);
        String sdcard = file.getPath();
        int preindex = 0;
        int currindex = (int) position.get(0);
        String data = "";
        String[] xy;
        try {
            Scanner scan = new Scanner(new File(sdcard));
            if (currindex != 0) {
                currindex = currindex - preindex;
                while (scan.hasNext() && currindex > 0) {
                    currindex--;
                    data = scan.nextLine();
                }
                data = data.replaceAll("[^0-9\\-]+", " ");
                xy = data.trim().split(" ");
                P.x = new BigInteger(xy[0]);
                P.y = new BigInteger(xy[1]);
                preindex = (int) position.get(0);
            }
            int pos = 1;
            while(scan.hasNext() && (pos < size))
            {
                currindex = (int) position.get(pos);
                currindex = currindex - preindex;
                do {
                    currindex--;
                    data = scan.nextLine();
                } while (currindex > 0);
                data = data.replaceAll("[^0-9\\-]+", " ");
                xy = data.trim().split(" ");
                Point Q = new Point(new BigInteger(xy[0]), new BigInteger(xy[1]));
                P = P.Pointaddition(Q, E);
                preindex = (int) position.get(pos);
                pos++;
            }
            scan.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return P;
    }
}
