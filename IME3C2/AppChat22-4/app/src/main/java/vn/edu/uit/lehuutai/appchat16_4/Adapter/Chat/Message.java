package vn.edu.uit.lehuutai.appchat16_4.Adapter.Chat;

import android.graphics.Color;

/**
 * Created by lehuu on 5/9/2017.
 */

public class Message {
    private String mAvatar;
    private String mName;
    private String mMessage;
    private boolean isMe;
    private int mColor = Color.parseColor("#FF393939");

    public Message(String mAvatar, String mName, String mMessage, boolean isMe) {
        this.mAvatar = mAvatar;
        this.mName = mName;
        this.mMessage = mMessage;
        this.isMe = isMe;
    }

    public Message(String mAvatar, String mName, String mMessage, boolean isMe, int mColor) {
        this.mAvatar = mAvatar;
        this.mName = mName;
        this.mMessage = mMessage;
        this.isMe = isMe;
        this.mColor = mColor;
    }

    public String getmAvatar() {
        return mAvatar;
    }

    public void setmAvatar(String mAvatar) {
        this.mAvatar = mAvatar;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmMessage() {
        return mMessage;
    }

    public void setmMessage(String mMessage) {
        this.mMessage = mMessage;
    }

    public int getmColor() {
        return mColor;
    }

    public void setmColor(int mColor) {
        this.mColor = mColor;
    }

    public boolean isMe() {
        return isMe;
    }

    public void setMe(boolean me) {
        isMe = me;
    }
}
