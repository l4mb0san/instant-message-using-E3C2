package vn.edu.uit.lehuutai.e3c2chat;

import android.app.Application;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

/**
 * Created by lehuu on 4/14/2017.
 */

public class Connection extends Application{
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("https://anonymous-messaging-app.herokuapp.com");
//            mSocket = IO.socket("http://192.168.237.1:1209");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }
}
