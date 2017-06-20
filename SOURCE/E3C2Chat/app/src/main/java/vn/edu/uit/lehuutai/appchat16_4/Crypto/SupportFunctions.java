package vn.edu.uit.lehuutai.appchat16_4.Crypto;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import vn.edu.uit.lehuutai.appchat16_4.Chat;

/**
 * Created by lehuu on 3/21/2017.
 */

public class SupportFunctions {
    static class KeyPair {
        public Point PublicKey;
        public BigInteger PrivateKey;
        public KeyPair(Point Pub, BigInteger Priv) {
            this.PublicKey = Pub;
            this.PrivateKey = Priv;
        }
    }

    static class MyOwnException extends Exception {
        public MyOwnException(String msg){
            super(msg);
        }
    }

    /*checking prime*/
    public boolean returnPrime(BigInteger number) {
        if (!number.isProbablePrime(5))
            return false;

        BigInteger two = new BigInteger("2");
        if (!two.equals(number) && BigInteger.ZERO.equals(number.mod(two)))
            return false;

        for (BigInteger i = new BigInteger("3"); i.multiply(i).compareTo(number) < 1; i = i.add(two)) {
            if (BigInteger.ZERO.equals(number.mod(i)))
                return false;
        }
        return true;
    }

    /*Is prime?*/
    /*This function is written according to
    http://codereview.stackexchange.com/questions/43490/biginteger-prime-testing*/
    public boolean isprime(BigInteger n) {
        BigInteger two = new BigInteger("2");
        BigInteger three = new BigInteger("3");
        if (!n.isProbablePrime(10)) {
            return false;
        }

        if (n.compareTo(BigInteger.ONE) == 0 || n.compareTo(two) == 0) {
            return true;
        }
        BigInteger root = appxRoot(n);
//        System.out.println("Using approximate root " + root);

        int cnt = 0;
        for (BigInteger i = three; i.compareTo(root) <= 0; i = i
                .nextProbablePrime()) {
            cnt++;
            if (cnt % 1000 == 0) {
//                System.out.println(cnt + " Using next prime " + i);
            }
            if (n.mod(i).equals(BigInteger.ZERO)) {
                return false;
            }

        }
        return true;
    }

    /*support for isprime function*/
    private BigInteger appxRoot(final BigInteger n) {
        BigInteger half = n.shiftRight(1);
        while (half.multiply(half).compareTo(n) > 0) {
            half = half.shiftRight(1);
        }
        return half.shiftLeft(1);
    }

    /*generates a random BigInteger number in the range [0, n]*/
    public BigInteger randBigInt(BigInteger n) {
        Random rnd = new Random();
        int nlen = n.bitLength();
        BigInteger nm1 = n.subtract(BigInteger.ONE);
        BigInteger r, s;
        do {
            s = new BigInteger(nlen + 100, rnd);
            r = s.mod(n);
        } while (s.subtract(r).add(nm1).bitLength() >= nlen + 100);
        return r;
    }

    /*padding '0' or '1' to the binary string until equal maxlenBits*/
    /*leftAttach must be "0" or "1"*/
    /*binary must be binary string*/
    public String paddingBin(String binary, String leftAttach, BigInteger maxlenBits) {
        if (leftAttach.length() != 1 && (!"0".equals(leftAttach) || !"1".equals(leftAttach))) {
            try {
                throw new MyOwnException("2nd parameter must be " + 0 + " or " + 1);
            } catch (MyOwnException ex) {
                Logger.getLogger(SupportFunctions.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        String pad = "";
        String result = binary;
        BigInteger len = BigInteger.valueOf(new BigInteger(result, 2).bitLength());
        while(len.compareTo(maxlenBits) == -1) {
            pad = append(pad, leftAttach);
            len = len.add(BigInteger.ONE);
        }
        result = append(pad, result);
        return result;
    }

    /*convert ASCII string to binary string*/
    public String asciiToBin(String s) {
        byte[] bytes = s.getBytes();
        StringBuilder binary = new StringBuilder();
        for (byte b : bytes)
        {
            int val = b;
            for (int i = 0; i < 8; i++)
            {
                binary.append((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }
        }
        return binary.toString();
    }

    /*convert binary string to ascii string*/
    public String binToASCII(String binary) {
        String str = "";
        int oldBits = binary.length();
        int minBlocks = binary.length()/8;
        int newBits = minBlocks * 8;
        if (newBits < oldBits) {
            minBlocks++;
            newBits = minBlocks * 8;
            binary = paddingBin(binary, "0", new BigInteger(String.valueOf(newBits)));
        }
        for (int i = 0; i < minBlocks; i++) {
            String tmp = binary.substring(8*i, 8*(i+1));
            int v = Integer.parseInt(tmp, 2);
            char c = (char)v;
            str += c;
        }
        return str;
    }

    /*convert string to BigInteger*/
    /*radix = -1 if s is not {binary, decimal, octal, hexa, ...}*/
    public BigInteger stringToBigInt(String s, int radix) {
        String val = s;
        if (radix == -1) {
            val = asciiToBin(s);
            return new BigInteger(val, 2);
        }
        return new BigInteger(val, radix);
    }

    /*string concatenation*/
    public String append(String oriString, String appendRightString) {
        StringBuilder sb = new StringBuilder(oriString);
        sb.append(appendRightString);
        return sb.toString();
    }

    /*point vs binary string concatenation*/
    public String appendBinary(EllipticCurve E, Point P, String binary) {
        BigInteger maxbits = BigInteger.valueOf(E.p.toString(2).length());
        String concat = append(paddingBin(P.y.toString(2), "0", maxbits), binary);
        concat = append(paddingBin(P.x.toString(2), "0", maxbits), concat);
        return concat;
    }

    /*BigInteger converted into binary and
    taking the position of bit '1' respectively */
    public ArrayList positionPowersOfTwoPartition(BigInteger number) {
        ArrayList partition = new ArrayList();
        BigInteger tmp;
        if (number.compareTo(BigInteger.ZERO) == 0) {
            partition.add(-1);
            return partition;
        }
        for (int i = 0; i < number.bitLength(); i++) {
            BigInteger mask = BigInteger.ONE.shiftLeft(i);
            tmp = mask.and(number);
            if (tmp.compareTo(BigInteger.ZERO) != 0) {
                partition.add(i);
            }
        }
        return partition;
    }

    /*convert byte array to hex*/
    public String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }

    /*SHA1 hash function*/
    public String SHA1(String text) {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-1");
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            byte[] sha1hash = new byte[40];
            sha1hash = md.digest();
            return convertToHex(sha1hash);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            Logger.getLogger(Chat.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /*split text into blocks 'size' bytes*/
    public String[] splitEqually(String text, int size) {
        int textlength = text.length();
        List<String> ret = new ArrayList<>((textlength + size - 1) / size);
        for (int start = 0; start < textlength; start += size) {
            ret.add(text.substring(start, Math.min(textlength, start + size)));
        }
        String[] blocks = new String [ret.size()];
        blocks = ret.toArray(blocks);
        return blocks;
    }

    /*PointDoubling table created*/
    /*example: 2P, 4P, 8P, 16P, ... until the bit position '1' largest*/
    /*tableSaveFilePath is the path to save this table*/
    public void tablePointDoubling(EllipticCurve E, Point P, BigInteger times, String savePath) {
        ArrayList position = positionPowersOfTwoPartition(times);
        int size = position.size();
        int lastValue = (int) position.get(size - 1);
        String TAG = "MEDIA";
        Point point = P;
        File file = new File(savePath);

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            for(int i = 0; i < lastValue; i++) {
                point = point.Pointdoubling(E);
                pw.println("(" + point.x + "," + point.y + ")");
            }
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void makeFolder(String folderPath) {
        File folder = new File(folderPath);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {
            // Do something on success
            //Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
        } else {
            // Do something else on failure
            //Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show();
        }
    }
}
