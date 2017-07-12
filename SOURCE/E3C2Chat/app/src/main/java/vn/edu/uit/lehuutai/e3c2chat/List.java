package vn.edu.uit.lehuutai.e3c2chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import vn.edu.uit.lehuutai.e3c2chat.Adapter.Chat.Message;
import vn.edu.uit.lehuutai.e3c2chat.Adapter.List.User;
import vn.edu.uit.lehuutai.e3c2chat.Adapter.List.UserAdapter;
import vn.edu.uit.lehuutai.e3c2chat.Crypto.Protocols;
import vn.edu.uit.lehuutai.e3c2chat.Crypto.SupportFunctions;

public class List extends AppCompatActivity {

    static int arrovv_right_icon;
    private Socket mSocket;
    private String mySocketID;
    private Map<String, BigInteger> privateKey;
    private BigInteger p, g;
    private String[] object = null;
    private CountDownTimer sendPublicKeyTimeout;

    Protocols protocols;
    ListView usersListView;
    ProgressBar progressBar;
    android.support.design.widget.FloatingActionButton floatingActionButtonHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        bundle();
        init();
        hanlde();
        menu();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.off("notify");
        mSocket.off("publickey exchange in listbox");
        mSocket.off("new user joined in ListActivity");
        mSocket.off("user left in ListActivity");
    }

    @Override
    public void onBackPressed() {
        mSocket.disconnect();
        Constants.message.clear();
        Constants.notify.clear();
        Constants.secretkey.clear();
        Constants.Q.clear();
        for(User u : Constants.userList){
            cancelCountDownTimer(u.getuSocketID());
        }
        Constants.countDownTimer.clear();
        Constants.isTimerRunning.clear();
        Constants.userList.clear();
        finish();
        System.exit(0);
    }

    //----------------------------------------------------------------------------------------
    private void bundle(){
        Intent intent = this.getIntent();
        mySocketID = intent.getStringExtra("username");
    }

    //----------------------------------------------------------------------------------------
    private void initSocket(){
        Connection connect = new Connection();
        mSocket = connect.getSocket();
        //Thêm user mới kết nối đến server vào danh sách
        mSocket.on("new user joined in ListActivity", onNewUserJoinedListActivity);
        //Xóa user vừa ngắt kết nối đến server ra khỏi danh sách
        mSocket.on("user left in ListActivity", onUserLeftListActivity);
        //Nhận thông báo có tin nhắn từ ai đó đến
        mSocket.on("notify", onNotify);
        //Trao đổi public key
        mSocket.on("publickey exchange in listbox", onPublicKeyExchangeListBox);
    }

    //----------------------------------------------------------------------------------------
    private void init(){
        initSocket();

        //Khai báo các thành phần giao diện
        floatingActionButtonHome = (android.support.design.widget.FloatingActionButton) findViewById(R.id.floatbutton_home);
        usersListView = (ListView)findViewById(R.id.lv_listUsers);
        progressBar = (ProgressBar)findViewById(R.id.progressBarListActivity);
        arrovv_right_icon = R.mipmap.ic_keyboard_arrow_right_cyan_24dp;

        setTitle("Members");
        privateKey = new HashMap<String, BigInteger>();
        protocols = new Protocols();
        //Danh sách các users đang trực tuyến
        Constants.userAdapter = new UserAdapter(
                getApplicationContext(),
                R.layout.custom_list,
                Constants.userList
        );
        usersListView.setAdapter(Constants.userAdapter);
        Constants.userAdapter.notifyDataSetChanged();
    }

    //----------------------------------------------------------------------------------------
    private void hanlde(){
        //Lắng nghe sự kiện khi click vào một item trong lv_ListUsers
        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Khởi tạo một số thành phần
                progressBar.setVisibility(View.VISIBLE);
                usersListView.setVisibility(View.VISIBLE);
                usersListView.setEnabled(false);
                usersListView.setBackground(getResources().getDrawable(R.color.dsb_disabled_color));
                object = new String[3];
                String socketID = Constants.userList.get(position).getuSocketID();
                String avatar = Constants.userList.get(position).getuAvatar();
                String name = Constants.userList.get(position).getuName();
                object[0] = socketID;
                object[1] = avatar;
                object[2] = name;

                //Xóa số lượng tin nhắn đi
                if (Constants.notify.get(socketID) != null) {
                    Constants.notify.remove(socketID);
                }
                //Thiệt lập lại số lượng Notification
                Constants.userList.get(position).setuNumberNotification(0);
                //Thiết lập lại background cho Notification thành dạng mũi tên >
                Constants.userList.get(position).setuBackgroundNotification(arrovv_right_icon);
                //Cập nhật lại danh sách hiển thị
                Constants.userAdapter.notifyDataSetChanged();

                if (Constants.secretkey.get(socketID) == null) {
                    //Phát sinh ra g256 và p256
                    g = new BigInteger(Constants.bits, new SecureRandom());
                    p = new BigInteger(Constants.bits, new SecureRandom());

                    //Phát sinh private Key ngẫu nhiên
                    if (privateKey.get(socketID) == null) {
                        privateKey.put(socketID, new BigInteger(Constants.bits, new SecureRandom()));
                    }

                    //Tính public Key từ g, p, privateKey
                    String publicKey = g.modPow(privateKey.get(socketID), p).toString();

                    JSONObject jsonObjectSendPublicKey = new JSONObject();
                    try {
                        jsonObjectSendPublicKey.put("reciever", socketID);
                        jsonObjectSendPublicKey.put("sender", mySocketID);
                        jsonObjectSendPublicKey.put("p", p.toString());
                        jsonObjectSendPublicKey.put("g", g.toString());
                        jsonObjectSendPublicKey.put("publickey", publicKey);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mSocket.emit("send PublicKey", jsonObjectSendPublicKey.toString());
                    sendPublicKeyTimeout = new CountDownTimer(15000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            progressBar.setVisibility(View.INVISIBLE);
                            usersListView.setVisibility(View.INVISIBLE);
                        }
                    }.start();
                } else {
                    startChatActivity();
                }
            }
        });
    }

    //----------------------------------------------------------------------------------------
    private void startChatActivity(){
        String socketID = object[0];
        String avatar = object[1];
        String name = object[2];
        computeQPoint(socketID);
        if (Constants.isTimerRunning.get(socketID) == null) {
            Constants.isTimerRunning.put(socketID, false);
        }
        if (!Constants.isTimerRunning.get(socketID)) {
            setCountDownTimer(Constants.timer, socketID);
        }
        Intent intent = new Intent(List.this, Chat.class);
        intent.putExtra("avatar", avatar);
        intent.putExtra("reciever", socketID);
        intent.putExtra("reciever-name", name);
        intent.putExtra("sender", mySocketID);
        startActivity(intent);
        finish();
    }

    //----------------------------------------------------------------------------------------
    private void menu(){
        //Thư viện có sẳn trong android
        floatingActionButtonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSocket.disconnect();
                Constants.message.clear();
                Constants.notify.clear();
                Constants.secretkey.clear();
                Constants.Q.clear();
                for(User u : Constants.userList){
                    cancelCountDownTimer(u.getuSocketID());
                }
                Constants.countDownTimer.clear();
                Constants.isTimerRunning.clear();
                Constants.userList.clear();
                Intent intent = new Intent(List.this, Login.class);
                startActivity(intent);
                finish();
            }
        });

        // in Activity Context - thư viện com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
        /*ImageView icon = new ImageView(this); // Create an icon
        icon.setImageDrawable(getDrawable(R.mipmap.ic_star_white));

        FloatingActionButton actionButton = new FloatingActionButton.Builder(this)
                .setContentView(icon)
                .build();
        actionButton.setBackground(getDrawable(R.drawable.background_floatbutton));*/

        /*SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);

        ImageView itemIcon1 = new ImageView(this);
        itemIcon1.setImageDrawable(getDrawable(R.mipmap.icon_home));
        SubActionButton button1 = itemBuilder.setContentView(itemIcon1).build();
        button1.setBackground(getDrawable(R.drawable.background_floatbutton_home));
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSocket.disconnect();
                finish();
                Intent intent = new Intent(List.this, Login.class);
                startActivity(intent);
            }
        });

        ImageView itemIcon2 = new ImageView(this);
        itemIcon2.setImageDrawable(getDrawable(R.mipmap.exit_to_app_white));
        SubActionButton button2 = itemBuilder.setContentView(itemIcon2).build();
        button2.setBackground(getDrawable(R.drawable.background_floatbutton_exit));
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSocket.disconnect();
                finishAndRemoveTask();
            }
        });

        ImageView itemIcon3 = new ImageView(this);
        itemIcon3.setImageDrawable(getDrawable(R.mipmap.ic_info_outline_white));
        SubActionButton button3 = itemBuilder.setContentView(itemIcon3).build();
        button3.setBackground(getDrawable(R.drawable.background_floatbutton_search));
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "INFO", Toast.LENGTH_SHORT).show();
            }
        });*/

/*        FloatingActionMenu actionMenu = new FloatingActionMenu.Builder(this)
                *//*.addSubActionView(button1)
                .addSubActionView(button2)
                .addSubActionView(button3)*//*
                .attachTo(actionButton)
                .build();*/
    }

    //----------------------------------------------------------------------------------------
    private void computeQPoint(String reciever){
        if (Constants.Q.get(reciever) == null) {
            do {
                Constants.Q.put(reciever, Constants.P.kPoint(Constants.E, Constants.secretkey.get(reciever)));
                if (Constants.Q.get(reciever).isPOSITIVE_INFINITY()) {
                    Constants.secretkey.put(reciever, Constants.secretkey.get(reciever).add(BigInteger.ONE));
                } else {
                    break;
                }
            } while (true);
        }
    }

    //---Thời gian đếm ngược để xóa mảng tin nhắn---------------------------------------------
    private void setCountDownTimer(final int militimes, final String recieverID) {
        if (Constants.countDownTimer.get(recieverID) == null) {
            Constants.countDownTimer.put(recieverID,
                    new CountDownTimer(militimes, 1000) {
                        public void onTick(long millisUntilFinished) {
                            if (!Constants.isTimerRunning.get(recieverID)) {
                                Constants.isTimerRunning.put(recieverID, true);
                            }
                        }
                        public void onFinish() {
                            try {
                                //Xóa mảng tin nhắn
                                if(Constants.message.get(recieverID) != null) {
                                    Constants.message.remove(recieverID);
                                }
                                //Thiết lập lại số lượng tin nhắn = 0
                                if (Constants.notify.get(recieverID) != null) {
                                    Constants.notify.remove(recieverID);
                                }
                                //Cập nhật lại hiển thị trong List Activity
                                int i = 0;
                                for (User u : Constants.userList) {
                                    if (u.getuSocketID().equals(recieverID)) {
                                        if (Constants.userList.get(i) != null) {
                                            Constants.userList.get(i).setuNumberNotification(0);
                                        }
                                        break;
                                    }
                                    i++;
                                }
                            }catch (Exception e) {
                                Toast.makeText(getApplicationContext(), "Error CountDownTimer", Toast.LENGTH_SHORT).show();
                            }
                            if (Constants.userAdapter != null) {
                                Constants.userAdapter.notifyDataSetChanged();
                            }
                            //Thiết lập lại countdown
                            setCountDownTimer(militimes, recieverID);
                        }
                    }
            );
        }
        Constants.countDownTimer.get(recieverID).start();
    }

    //----------------------------------------------------------------------------------------
    private void cancelCountDownTimer(String socketID) {
        if(Constants.countDownTimer.get(socketID) != null) {
            Constants.countDownTimer.get(socketID).cancel();
            Constants.countDownTimer.remove(socketID);
        }
    }

    //----------------------------------------------------------------------------------------
    private Emitter.Listener onNotify = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    SupportFunctions supportFunctions = new SupportFunctions();
                    String _sender, _message, _avatar;
                    int countNewMessage;
                    int i = 0;
                    Integer numNotification;

                    try {
                        _avatar = data.getString("avatar");
                        _sender = data.getString("sender");
                        _message = data.getString("message");

                        //Tách lấy để lấy signature (trước dấu "|") & tin nhắn
                        //--**-- Kiểm tra tại sao cần _message.indexOf(": ") + 2
                        String[] CipherText = _message.substring(_message.indexOf(": ") + 2).split("[|]");
                        for (int j = 0; j < CipherText.length; j++) {
                            if (j == 0) {
                                CipherText[j] = supportFunctions.paddingBin((new BigInteger(CipherText[j], 16)).toString(2), "0", BigInteger.valueOf(Constants.E.p.toString(2).length()*2));
                            } else {
                                CipherText[j] = supportFunctions.paddingBin((new BigInteger(CipherText[j], 16)).toString(2), "0", protocols.maxblockbits);
                            }
                        }
                        String messageDecrypted = protocols.Decrypt(Constants.E, Constants.secretkey.get(_sender), CipherText);

                        //Nếu _sender là người mới trò chuyện lần đầu thì sẽ khởi tạo mảng để chứa tin nhắn.
                        if (Constants.message.get(_sender) == null) {
                            Constants.message.put(_sender, new ArrayList<Message>());
                        }
                        //Nếu có rồi thì chỉ cần thêm _message vào mảng có tên _sender
                        Constants.message.get(_sender).add(new Message(_avatar, _sender, messageDecrypted, false, R.color.colorSender));

                        //Cập nhật số lượng tin nhắn mới
                        numNotification = Constants.notify.get(_sender);
                        if (numNotification == null) {
                            Constants.notify.put(_sender, new Integer(0));
                            numNotification = 0;
                        }
                        //Nếu lớn hơn 9 tin nhắn chưa đọc thì không cần thông báo nữa
                        if (numNotification < 9) {
                            Constants.notify.put(_sender, numNotification + 1);
                            countNewMessage = Constants.notify.get(_sender).intValue();

                            //Tìm kiếm vị trí của _sender trong mảng userList để lấy đúng vị trí chỉnh sửa trong userAdapter
                            for (User u : Constants.userList) {
                                if (u.getuSocketID().equals(_sender)) {
                                    Constants.userList.get(i).setuNumberNotification(countNewMessage);
                                    Constants.userList.get(i).setuBackgroundNotification(R.drawable.notify);
                                    Constants.userAdapter.notifyDataSetChanged();
                                    break;
                                }
                                i++;
                            }
                        }

                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    //----------------------------------------------------------------------------------------
    private Emitter.Listener onPublicKeyExchangeListBox = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String _sender;
                    BigInteger _p, _g, _publicKey;
                    sendPublicKeyTimeout.cancel();

                    try {
                        _sender = data.getString("sender");
                        _publicKey = new BigInteger(data.getString("publickey"));
                        if (data.getString("p").isEmpty() || data.getString("g").isEmpty()) {
                            if (Constants.secretkey.get(_sender) == null) {
                                BigInteger secretKey = _publicKey.modPow(privateKey.get(_sender), p);
                                Constants.secretkey.put(_sender, secretKey);
                                Toast.makeText(getApplicationContext(), "Exchange Key Success", Toast.LENGTH_SHORT).show();
                                privateKey.remove(_sender);
                            }
                        } else {
                            _g = new BigInteger(data.getString("g"));
                            _p = new BigInteger(data.getString("p"));

                            if (privateKey.get(_sender) == null) {
                                privateKey.put(_sender, new BigInteger(Constants.bits, new SecureRandom()));
                            }
                            //Tính public Key từ g, p, privateKey
                            String publicKey = _g.modPow(privateKey.get(_sender), _p).toString();
                            //Tạo secret key lưu vào mảng
                            if (Constants.secretkey.get(_sender) == null) {
                                JSONObject jsonObjectSendPublicKey = new JSONObject();
                                try {
                                    jsonObjectSendPublicKey.put("reciever", _sender);
                                    jsonObjectSendPublicKey.put("sender", mySocketID);
                                    jsonObjectSendPublicKey.put("p", "");
                                    jsonObjectSendPublicKey.put("g", "");
                                    jsonObjectSendPublicKey.put("publickey", publicKey);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                mSocket.emit("send PublicKey", jsonObjectSendPublicKey.toString());

                                BigInteger secretKey = _publicKey.modPow(privateKey.get(_sender), _p);
                                Constants.secretkey.put(_sender, secretKey);
                                Toast.makeText(getApplicationContext(), "Exchange Secret-key Success", Toast.LENGTH_SHORT).show();
                                privateKey.remove(_sender);
                            }
                        }

                        //Tính điểm Q từ private key đã trao đổi
                        computeQPoint(_sender);

                        //Thiết lập bộ countdownTimer và chuyển sang Chat Activity
                        if(object != null && object.length > 0){
                            startChatActivity();
                        }
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    //---------------------------------------------------------------------------
    private Emitter.Listener onNewUserJoinedListActivity = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    JSONObject jsonData;
                    String SOCKETID, NAME, AVATAR;
                    try {
                        jsonData = data.getJSONObject("newUser");
                        SOCKETID = jsonData.getString("ID");
                        NAME = jsonData.getString("NAME");
                        AVATAR = jsonData.getString("AVATAR");
                        Constants.userList.add(new User(SOCKETID, NAME, AVATAR));
                        Constants.userAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    //---------------------------------------------------------------------------
    private Emitter.Listener onUserLeftListActivity = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String userLeft;
                    int i = 0;

                    try {
                        userLeft = data.getString("userLeft");

                        for (User u : Constants.userList) {
                            if (u.getuSocketID().equals(userLeft)) {
                                Constants.userList.remove(i);
                                Constants.userAdapter.notifyDataSetChanged();
                                break;
                            }
                            i++;
                        }
                        if(Constants.message.get(userLeft) != null){
                            Constants.message.remove(userLeft);
                        }
                        if(Constants.secretkey.get(userLeft) != null) {
                            Constants.secretkey.remove(userLeft);
                        }
                        if(Constants.notify.get(userLeft) != null) {
                            Constants.notify.remove(userLeft);
                        }
                        if(Constants.Q.get(userLeft) != null) {
                            Constants.Q.remove(userLeft);
                        }
                        cancelCountDownTimer(userLeft);
                        if(Constants.countDownTimer.get(userLeft) != null){
                            Constants.countDownTimer.remove(userLeft);
                        }
                        if(Constants.isTimerRunning.get(userLeft) != null){
                            Constants.isTimerRunning.remove(userLeft);
                        }

                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };
}
