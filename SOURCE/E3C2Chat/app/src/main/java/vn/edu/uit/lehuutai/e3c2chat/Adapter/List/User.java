package vn.edu.uit.lehuutai.e3c2chat.Adapter.List;

/**
 * Created by lehuu on 5/8/2017.
 */

public class User {
    private String uSocketID;
    private String uName;
    private String uAvatar;
    private int uBackgroundNotification;
    private int uNumberNotification;

    public User(String uSocketID, String uName) {
        this.uSocketID = uSocketID;
        this.uName = uName;
    }

    public User(String uSocketID, String uName, String uAvatar) {
        this.uSocketID = uSocketID;
        this.uAvatar = uAvatar;
        this.uName = uName;
    }

    public User(String uSocketID, String uName, String uAvatar, int uBackgroundNotification, int uNumberNotification) {
        this.uSocketID = uSocketID;
        this.uAvatar = uAvatar;
        this.uName = uName;
        this.uBackgroundNotification = uBackgroundNotification;
        this.uNumberNotification = uNumberNotification;
    }

    public String getuSocketID() {
        return uSocketID;
    }

    public void setuSocketID(String uSocketID) {
        this.uSocketID = uSocketID;
    }

    public String getuAvatar() {
        return uAvatar;
    }

    public void setuAvatar(String uAvatar) {
        this.uAvatar = uAvatar;
    }

    public String getuName() {
        return uName;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }

    public int getuBackgroundNotification() {
        return uBackgroundNotification;
    }

    public void setuBackgroundNotification(int uBackgroundNotification) {
        this.uBackgroundNotification = uBackgroundNotification;
    }

    public int getuNumberNotification() {
        return uNumberNotification;
    }

    public void setuNumberNotification(int uNumberNotification) {
        this.uNumberNotification = uNumberNotification;
    }
}
