package vn.edu.uit.lehuutai.e3c2chat;

import android.os.CountDownTimer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import vn.edu.uit.lehuutai.e3c2chat.Adapter.Chat.Message;
import vn.edu.uit.lehuutai.e3c2chat.Adapter.List.User;
import vn.edu.uit.lehuutai.e3c2chat.Adapter.List.UserAdapter;
import vn.edu.uit.lehuutai.e3c2chat.Crypto.EllipticCurve;
import vn.edu.uit.lehuutai.e3c2chat.Crypto.Point;

/**
 * Created by lehuu on 4/23/2017.
 */

public class Constants {

    public static final BigInteger a = new BigInteger("9", 10);
    public static final BigInteger b = new BigInteger("7", 10);
    public static final BigInteger p = new BigInteger("2011", 10);
    public static final EllipticCurve E = new EllipticCurve(a, b, p);
    public static final Point P = new Point(new BigInteger("756", 10), new BigInteger("1012", 10));

    public static HashMap<String, ArrayList<Message>> message;
    public static HashMap<String, Integer> notify;
    public static HashMap<String, BigInteger> secretkey;
    public static int bits = 256; //256 bits
    public static ArrayList<User> userList;
    public static UserAdapter userAdapter = null;
    public static int timer; //Thời gian xóa tin nhắn
    public static HashMap<String, Boolean> isTimerRunning;
    public static HashMap<String, CountDownTimer> countDownTimer = null;
    public static HashMap<String, Point> Q = null;


}
