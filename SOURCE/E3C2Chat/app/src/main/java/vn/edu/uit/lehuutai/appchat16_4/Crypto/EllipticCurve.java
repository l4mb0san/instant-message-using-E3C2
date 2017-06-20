package vn.edu.uit.lehuutai.appchat16_4.Crypto;

import java.math.BigInteger;

/**
 * Created by lehuu on 3/21/2017.
 */

public class EllipticCurve {
    public BigInteger a; //factor a
    public BigInteger b; //factor b
    public BigInteger p; //infinity field p
    public BigInteger n = BigInteger.ZERO; //number of points

    public EllipticCurve(BigInteger a, BigInteger b, BigInteger p) {
        this.a = a;
        this.b = b;
        this.p = p;
    }

    public BigInteger numberofpoints() {
        return n;
    }

    /*check two numbers a and b factor belong to the finite field p*/
    public boolean belongsField() {
        return !(a.compareTo(p) == 1 || b.compareTo(p) == 1);
    }

    public boolean PointbelongsField(Point P) {
        if (P.x.compareTo(p) == 1 || P.y.compareTo(p) == 1) {
            return false;
        }
        BigInteger left_side = P.y.pow(2).mod(p);
        BigInteger right_side = P.x.pow(3).add(a.multiply(P.x)).add(b).mod(p);
        return left_side.compareTo(right_side) == 0;
    }
}