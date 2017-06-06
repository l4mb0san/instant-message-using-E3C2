package vn.edu.uit.lehuutai.appchat16_4.Crypto;

/**
 *
 * @author lehuu
 */

import java.math.BigInteger;
import java.util.Random;

/**
 * Encryption and Decryption Protocol is based on article
 * "Elgamal Encryption using Elliptic Curve Cryptography"
 *          Rosy Sunuwar, Suraj Ketan Samal
 *      CSCE 877 - Cryptography and Computer Security
 *          University of Nebraska- Lincoln
 *              December 9, 2015
 */

public class Protocols {
    SupportFunctions obj = new SupportFunctions();
    public BigInteger sizeblock = new BigInteger("20");
    public BigInteger maxblockbits = sizeblock.multiply(new BigInteger("8"));


    /*Elgamal Encryption using Elliptic Curve Cryptography*/
    /*The calculation is stored in binary form, but hexadecimal format screen output*/
    public String[] Encrypt (EllipticCurve E, Point alphaPoint, Point betaPoint, String plaintext) {
        //  String[] m = plaintext.split("(?<=\\G.{" + sizeblock + "})"); //split into blocks of 20 bytes but not usable if plaintext have "\n"
        String[] m = obj.splitEqually(plaintext, sizeblock.intValue()); //split into blocks of 20 bytes
        int maxblocks = m.length; //the number of blocks
        BigInteger ki;
        Point ri, kiBeta;
        do {
            ki = new BigInteger(256, new Random()); //choosing a random ki
            ri = alphaPoint.kPoint(E, ki); //ri = (ki o α)
            kiBeta = betaPoint.kPoint(E, ki); //(ki o β)
        } while (ri.isPOSITIVE_INFINITY() || kiBeta.isPOSITIVE_INFINITY());
        BigInteger ht = new BigInteger(obj.SHA1(obj.append(kiBeta.x.toString(), kiBeta.y.toString())), 16); //ht = H (ki o β)

        String[] htmi = new String[maxblocks + 1];
        htmi[0] = obj.appendBinary(E, ri, ""); //store a point by binary format (not 20 bytes)
        for (int i = 1; i < maxblocks + 1; i++) {
            BigInteger mi = obj.stringToBigInt(m[i - 1], -1);
            System.out.println("Plaintext (Hex): " + mi.toString(16));
            htmi[i] = ht.xor(mi).toString(2); //calculate htmi = ht ⊕ mi (⊕ is bitwise XOR)
            htmi[i] = obj.paddingBin(htmi[i], "0", maxblockbits); //padded "0" on the left until maxblockbits
        }
        return htmi; //array htmi is array binary strings (cipher text)
    }

    /*Elgamal Decryption using Elliptic Curve Cryptography*/
    /*The calculation is stored in binary form*/
    public String Decrypt (EllipticCurve E, BigInteger secretNumber, String[] ciphertext) {
        int maxbits = E.p.toString(2).length(); //the maximum number of bits to represent a point in the finite field p
        int cipherblocks = ciphertext.length; //the number of blocks
        BigInteger x = obj.stringToBigInt(ciphertext[0].substring(0, maxbits), 2); //get coordinates X from the first block
        BigInteger y = obj.stringToBigInt(ciphertext[0].substring(maxbits, ciphertext[0].length()), 2); //get coordinates Y from the first block
        Point ri = new Point(x, y); //point α
        ri = ri.kPoint(E, secretNumber); //point β
        BigInteger ari = new BigInteger(obj.SHA1(obj.append(ri.x.toString(), ri.y.toString())),16); //H(a o ri ) = H(ki o β)
        String m = "";
        for (int i = 1; i < cipherblocks; i++) {
            BigInteger htmi = obj.stringToBigInt(ciphertext[i], 2);
            String mi = ari.xor(htmi).toString(2); //H(a o ri ) ⊕ htmi
            m += obj.binToASCII(mi); //convert binary string into ASCII string
        }
        return m; // m is ASCII string (clear text)
    }
}
