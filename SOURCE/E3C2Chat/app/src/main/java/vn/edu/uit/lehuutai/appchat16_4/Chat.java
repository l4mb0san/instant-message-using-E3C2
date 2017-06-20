package vn.edu.uit.lehuutai.appchat16_4;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.hitomi.cmlibrary.CircleMenu;
import com.hitomi.cmlibrary.OnMenuSelectedListener;
import com.hitomi.cmlibrary.OnMenuStatusChangeListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import vn.edu.uit.lehuutai.appchat16_4.Adapter.Chat.Message;
import vn.edu.uit.lehuutai.appchat16_4.Adapter.Chat.MessageAdapter;
import vn.edu.uit.lehuutai.appchat16_4.Adapter.List.User;
import vn.edu.uit.lehuutai.appchat16_4.Crypto.EllipticCurve;
import vn.edu.uit.lehuutai.appchat16_4.Crypto.Point;
import vn.edu.uit.lehuutai.appchat16_4.Crypto.Protocols;
import vn.edu.uit.lehuutai.appchat16_4.Crypto.SupportFunctions;


public class Chat extends AppCompatActivity {

    private Socket mSocket;
    private ArrayList<Message> messageChat;
    private MessageAdapter messageAdapter;
    private String reciever, reciever_name, sender, avatar;
    private Map<String, BigInteger> privateKey;
    private boolean pressTwice = false;

    Protocols protocols = new Protocols();
    EllipticCurve E;
    Point P;

    ListView display;
    EditText inputMessage;
    ImageButton send;
    ProgressBar progressBar;
    TextView userLeftNotification, newMessageNameSender, newMessageContent;
    CircleMenu circleMenu;
    RelativeLayout relativeLayoutMenu, relativeLayoutNewMessage;
    ImageView imageViewNewMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        bundle();
        init();
        handle();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.off("new user joined in ChatActivity");
        mSocket.off("user left in ChatActivity");
        mSocket.off("conversation private post");
        mSocket.off("publickey exchange in chatbox");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home: {
                Intent intent = new Intent(this, List.class);
                startActivity(intent);
                finish();
                return true;
            }
            case R.id.action_back: {
                Intent intent = new Intent(this, List.class);
                intent.putExtra("username", sender);
                startActivity(intent);
                finish();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if(pressTwice == true){
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
        pressTwice = true;
        Toast.makeText(this, "Press BACK button again to exit", Toast.LENGTH_LONG).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                pressTwice = false;
                Intent intent = new Intent(Chat.this, List.class);
                intent.putExtra("username", sender);
                startActivity(intent);
                finish();
            }
        }, 1000);
    }

    //----------------------------------------------------------------------------------------
    private void bundle(){
        Intent intent = this.getIntent();
        avatar = intent.getStringExtra("avatar"); //avatar của người nhận
        reciever = intent.getStringExtra("reciever"); //reciever là socketid của người nhận
        reciever_name = intent.getStringExtra("reciever-name"); //tên của reciever
        sender = intent.getStringExtra("sender"); //sender là bản thân mình
    }

    //----------------------------------------------------------------------------------------
    private void initSocket(){
        Connection connect = new Connection();
        mSocket = connect.getSocket();
        //Thêm user mới kết nối đến server vào danh sách
        mSocket.on("new user joined in ChatActivity", onNewUserJoinedChatActivity);
        //Xóa user vừa ngắt kết nối đến server ra khỏi danh sách
        mSocket.on("user left in ChatActivity", onUserLeftChatActivity);
        //Có người nhắn tin tới
        mSocket.on("conversation private post", onConversationPrivate);
        //Có người muốn trao đổi khóa bí mật
        mSocket.on("publickey exchange in chatbox", onPublicKeyExchangeChatBox);
    }

    //----------------------------------------------------------------------------------------
    private void init(){
        initSocket();

        display = (ListView)findViewById(R.id.lv_display);
        inputMessage = (EditText)findViewById(R.id.edt_inputMessage);
        send = (ImageButton) findViewById(R.id.btn_send);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        userLeftNotification = (TextView) findViewById(R.id.tv_userLeft);
        circleMenu = (CircleMenu) findViewById(R.id.circle_menu);
        relativeLayoutMenu = (RelativeLayout) findViewById(R.id.RelativeLayoutMenu);
        relativeLayoutNewMessage = (RelativeLayout) findViewById(R.id.layout_new_message);
        newMessageNameSender = (TextView) findViewById(R.id.tv_new_message_name_sender);
        newMessageContent = (TextView) findViewById(R.id.tv_new_message_content);
        imageViewNewMessage = (ImageView) findViewById(R.id.img_new_message);

        setTitle(reciever_name);
        display.setEnabled(false);
        inputMessage.setEnabled(false);
        send.setEnabled(false);
        display.setBackground(getResources().getDrawable(R.color.dsb_disabled_color));
        messageChat = new ArrayList<Message>();
        privateKey = new HashMap<String, BigInteger>();
        E = Constants.E;
        P = Constants.P;
        loadDummyHistory();
        Menu();
        display.setEnabled(true);
        inputMessage.setEnabled(true);
        send.setEnabled(true);
        display.setBackground(getResources().getDrawable(android.R.color.white));
        progressBar.setVisibility(View.GONE);
    }

    //----------------------------------------------------------------------------------------
    private void handle(){
        inputMessage.requestFocus();
        inputMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.btn_send || id == EditorInfo.IME_NULL) {
                    attempSend();
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attempSend();
                hideKeyboard();
            }
        });
        handleMenu();
    }

    //----------------------------------------------------------------------------------------
    private void Menu(){
        circleMenu.setMainMenu(Color.parseColor("#02d2a4"), R.mipmap.ic_star_white, R.mipmap.icon_cancel);
        circleMenu.addSubMenu(Color.parseColor("#258CFF"), R.mipmap.ic_exit_to_app_white)
                .addSubMenu(Color.parseColor("#30A400"), R.mipmap.ic_go_back)
 //               .addSubMenu(Color.parseColor("#FF4B32"), R.mipmap.icon_notify)
                .addSubMenu(Color.parseColor("#FF6A00"), R.mipmap.icon_home);
    }

    //----------------------------------------------------------------------------------------
    private void handleMenu(){
        circleMenu.setOnMenuSelectedListener(new OnMenuSelectedListener() {
            @Override
            public void onMenuSelected(int index) {
                switch (index) {
                    case 0:
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
                        break;
                    case 1:
                        Intent intent1 = new Intent(Chat.this, List.class);
                        intent1.putExtra("username", sender);
                        startActivity(intent1);
                        finish();
                        break;
                    case 2:
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
                        Intent intent2 = new Intent(Chat.this, Login.class);
                        startActivity(intent2);
                        finish();
                        break;
                }
            }
        });

        circleMenu.setOnMenuStatusChangeListener(new OnMenuStatusChangeListener() {
            @Override
            public void onMenuOpened() {}
            @Override
            public void onMenuClosed() {}
        });


        relativeLayoutMenu.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (circleMenu.getVisibility() == View.INVISIBLE) {
                    circleMenu.setVisibility(View.VISIBLE);
                } else {
                    circleMenu.setVisibility(View.INVISIBLE);
                }
                return true;
            }
        });
    }

    //----------------------------------------------------------------------------------------
    private void hideKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    //----------------------------------------------------------------------------------------
    private void attempSend(){
        if(Constants.Q.get(reciever) == null) {
            Toast.makeText(getApplicationContext(), "Not Exchange Secret-key", Toast.LENGTH_LONG).show();
        }
        String _message = inputMessage.getText().toString();
        if (!_message.isEmpty()) {
            //Phần mã hóa tin nhắn
            String[] CipherText = protocols.Encrypt(E, P, Constants.Q.get(reciever), _message);
            String encryptedMessage = "";
            int i = 1;
            for (String s : CipherText) {
                if (i == CipherText.length) {
                    encryptedMessage += "" + new BigInteger(s, 2).toString(16);
                } else {
                    encryptedMessage += "" + new BigInteger(s, 2).toString(16) + "|";
                    i++;
                }
            }
            //Gửi tin nhắn đã mã hóa
            JSONObject jsonObjectSendMessage = new JSONObject();
            try {
                jsonObjectSendMessage.put("reciever", reciever);
                jsonObjectSendMessage.put("sender", sender);
                jsonObjectSendMessage.put("message", encryptedMessage);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSocket.emit("send message", jsonObjectSendMessage.toString());
            inputMessage.setText("");
            //Hiển thị tin nhắn không mã hóa lên màn hình
            if (Constants.message.get(reciever) == null) {
                Constants.message.put(reciever, new ArrayList<Message>());
            }
            Constants.message.get(reciever).add(new Message(avatar, sender, _message, true, R.color.colorReciever));
            messageChat.add(new Message(avatar, sender, _message, true, R.color.colorReciever));
            messageAdapter.notifyDataSetChanged();
            scroll();
        }
    }

    //----------------------------------------------------------------------------------------
    private void scroll() {
        display.setSelection(display.getCount() - 1);
    }

    //----------------------------------------------------------------------------------------
    private void loadDummyHistory(){
        if(Constants.message.get(reciever) != null) {
            messageChat.clear();
            messageChat.addAll(Constants.message.get(reciever));
        }
        messageAdapter = new MessageAdapter(
                getApplicationContext(),
                R.layout.custom_chat,
                messageChat
        );
        display.setAdapter(messageAdapter);
        messageAdapter.notifyDataSetChanged();
        scroll();

    }

    //----------------------------------------------------------------------------------------
    private Emitter.Listener onConversationPrivate = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    SupportFunctions supportFunctions = new SupportFunctions();
                    String _sender, _message, _avatar;
                    Integer numNotification;
                    int countNewMessage;
                    int i = 0;

                    try {
                        _avatar = data.getString("avatar");
                        _sender = data.getString("sender");
                        _message = data.getString("message");

                        //Tách lấy để lấy signature (trước dấu "|") & tin nhắn
                        //--**-- Kiểm tra tại sao cần _message.indexOf(": ") + 2
                        String[] CipherText = _message.substring(_message.indexOf(": ") + 2).split("[|]");
                        for (int j = 0; j < CipherText.length; j++) {
                            if (j == 0) {
                                CipherText[j] = supportFunctions.paddingBin((new BigInteger(CipherText[j], 16)).toString(2), "0", BigInteger.valueOf(E.p.toString(2).length()*2));
                            } else {
                                CipherText[j] = supportFunctions.paddingBin((new BigInteger(CipherText[j], 16)).toString(2), "0", protocols.maxblockbits);
                            }
                        }
                        final String messageDecrypted = protocols.Decrypt(E, Constants.secretkey.get(_sender), CipherText);

                        //Notify được thiết lập để lúc trở về giao diện ListBox, có thể cho biết
                        //ai đã nhắn tin.
                        if (Constants.notify.get(_sender) == null) {
                            Constants.notify.put(_sender, new Integer(0));
                        }

                        //Nếu _sender là người mới trò chuyện lần đầu thì sẽ khởi tạo mảng để chứa
                        //chứa tin nhắn.
                        if (Constants.message.get(_sender) == null) {
                            Constants.message.put(_sender, new ArrayList<Message>());
                        }
                        //Nếu có rồi thì chỉ cần thêm _message vào mảng có tên _sender
                        Constants.message.get(_sender).add(new Message(_avatar, _sender, messageDecrypted, false, R.color.colorSender));

                        //Điều kiện này mang ý nghĩa là hiện tại đang ở đúng trong chatbox và hiển
                        //thị tin nhắn đó lên.
                        if (_sender.equals(reciever)) {
                            messageChat.add(new Message(_avatar, _sender, messageDecrypted, false, R.color.colorSender));
                            messageAdapter.notifyDataSetChanged();
                            scroll();
                        } else {
                            String _nameSender = "";
                            for(User u : Constants.userList){
                                if(u.getuSocketID().equals(_sender)){
                                    _nameSender = u.getuName();
                                }
                            }
                            //Hiển thị tin nhắn từ một người không thuộc Chat Activity này
                            final String final_nameSender = _nameSender;
                            new CountDownTimer(5000, 500) {
                                public void onTick(long millisUntilFinished) {
                                    relativeLayoutNewMessage.setVisibility(View.VISIBLE);
                                    if(relativeLayoutNewMessage.getBackground().getConstantState() == getResources().getDrawable(R.color.colorWhite).getConstantState()){
                                        relativeLayoutNewMessage.setBackground(getResources().getDrawable(R.color.dsb_track_color));
                                        newMessageNameSender.setTextColor(Color.parseColor("#FFFFFFFF"));
                                        newMessageContent.setTextColor(Color.parseColor("#FFF0F0F0"));
                                        imageViewNewMessage.setImageResource(R.drawable.puffin_bird_white);
                                    }else{
                                        relativeLayoutNewMessage.setBackground(getResources().getDrawable(R.color.colorWhite));
                                        newMessageNameSender.setTextColor(getResources().getColor(R.color.dsb_track_color));
                                        newMessageContent.setTextColor(getResources().getColor(R.color.dsb_ripple_color_pressed));
                                        imageViewNewMessage.setImageResource(R.drawable.puffin_bird_gray);
                                    }
                                    newMessageNameSender.setText(final_nameSender);
                                    newMessageContent.setText(messageDecrypted);
                                }

                                public void onFinish() {
                                    relativeLayoutNewMessage.setVisibility(View.INVISIBLE);
                                    newMessageNameSender.setText("");
                                    newMessageContent.setText("");
                                }
                            }.start();
                            //Ngược lại, thì vẫn nhận tin nhắn của các _sender khác gửi tới và
                            //số lượng tin nhắn từ _sender đó gửi.
                            numNotification = Constants.notify.get(_sender);
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
                        }

                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    //----------------------------------------------------------------------------------------
    private Emitter.Listener onPublicKeyExchangeChatBox = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String _sender;
                    BigInteger _p, _g, _publicKey;

                    try {
                        _sender = data.getString("sender");
                        _publicKey = new BigInteger(data.getString("publickey"));
                        if (data.getString("p").isEmpty() || data.getString("g").isEmpty()) {
                            return;
                        } else {
                            //_sender sẽ trở thành người nhận
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
                                    jsonObjectSendPublicKey.put("reciever", _sender); //là người vừa gửi cho mình
                                    jsonObjectSendPublicKey.put("sender", sender); //là bản thân mình
                                    jsonObjectSendPublicKey.put("p", "");
                                    jsonObjectSendPublicKey.put("g", "");
                                    jsonObjectSendPublicKey.put("publickey", publicKey);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                mSocket.emit("send PublicKey", jsonObjectSendPublicKey.toString());

                                BigInteger secretKey = _publicKey.modPow(privateKey.get(_sender), _p);
                                Constants.secretkey.put(_sender, secretKey);
                                Toast.makeText(getApplicationContext(), "Exchange Secret-key Success", Toast.LENGTH_LONG).show();
                                privateKey.remove(_sender);

                            }
                        }

                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    //---------------------------------------------------------------------------
    private Emitter.Listener onNewUserJoinedChatActivity = new Emitter.Listener() {
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
                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    //---------------------------------------------------------------------------
    private Emitter.Listener onUserLeftChatActivity = new Emitter.Listener() {
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

                        if (userLeft.equals(reciever)) {
                            inputMessage.setEnabled(false);
                            send.setEnabled(false);
                            userLeftNotification.setVisibility(View.VISIBLE);
                            userLeftNotification.setText(reciever_name + " is offline\n" +
                                    "You can not chat to his/her anymore");
                        }
                        for (User u : Constants.userList) {
                            if (u.getuSocketID().equals(userLeft)) {
                                Constants.userList.remove(i);
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

    //----------------------------------------------------------------------------------------
    private void cancelCountDownTimer(String socketID) {
        if(Constants.countDownTimer.get(socketID) != null) {
            Constants.countDownTimer.get(socketID).cancel();
            Constants.countDownTimer.remove(socketID);
        }
    }

}
