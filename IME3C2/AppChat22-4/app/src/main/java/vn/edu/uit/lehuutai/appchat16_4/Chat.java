package vn.edu.uit.lehuutai.appchat16_4;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import vn.edu.uit.lehuutai.appchat16_4.Adapter.Gallery.Image;
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

    Handler handler = new Handler();
    Protocols protocols = new Protocols();
    EllipticCurve E;
    Point P, Q = null;
    myAsyncTask computeQPoint;
    boolean hide = true;

    ListView display;
    EditText inputMessage;
    ImageButton send;
    ProgressBar progressBar;
    TextView userLeftNotification;
    CircleMenu circleMenu;
    RelativeLayout relativeLayoutMenu;


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

        setTitle(reciever_name);
        messageChat = new ArrayList<Message>();
        privateKey = new HashMap<String, BigInteger>();
        E = Constants.E;
        P = Constants.P;
        display.setEnabled(false);
        inputMessage.setEnabled(false);
        send.setEnabled(false);
        computeQPoint = new myAsyncTask();
        computeQPoint.execute();
        loadDummyHistory();
        Menu();
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
                        finishAndRemoveTask();
                        break;
                    case 1:
                        Intent intent1 = new Intent(Chat.this, List.class);
                        intent1.putExtra("username", sender);
                        startActivity(intent1);
                        finish();
                        break;
                    case 2:
                        mSocket.disconnect();
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
                if (hide) {
                    circleMenu.setVisibility(View.VISIBLE);
                    hide = false;
                } else {
                    circleMenu.setVisibility(View.INVISIBLE);
                    hide = true;
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
        //Kiểm tra xem luồng xử lý điểm Q có được hủy chưa, chưa thì hủy
        if (!computeQPoint.isCancelled()) {
            computeQPoint.cancel(true);
        }
        String _message = inputMessage.getText().toString();
        if (!_message.isEmpty()) {
            //Phần mã hóa tin nhắn
            String[] CipherText = protocols.Encrypt(E, P, Q, _message);
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

    //Tính toán điểm Q ở một luồng khác, nhưng đồng bộ với khung chat, chưa tính xong thì không thể chat
    private class myAsyncTask extends AsyncTask<Void, Void, Void> {

        //Thực hiện sau khi doInBackground xong
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            display.setEnabled(true);
            inputMessage.setEnabled(true);
            send.setEnabled(true);
            progressBar.setVisibility(View.GONE);
        }

        //Khi myAsyncTask thực thi thì doInBackground chạy đầu tiên
        @Override
        protected Void doInBackground(Void... params) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    do {
                        Q = P.kPoint(E, Constants.secretkey.get(reciever));
                        if (Q.isPOSITIVE_INFINITY()) {
                            Constants.secretkey.put(reciever, Constants.secretkey.get(reciever).add(BigInteger.ONE));
                        } else {
                            break;
                        }
                    } while (true);
                }
            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
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
                        String messageDecrypted = protocols.Decrypt(E, Constants.secretkey.get(_sender), CipherText);

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
                            //Ngược lại, thì vẫn nhận tin nhắn của các _sender khác gửi tới và
                            //số lượng tin nhắn từ _sender đó gửi.
                            Constants.notify.put(_sender, Constants.notify.get(_sender) + 1);
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
                                Toast.makeText(getApplicationContext(), secretKey + "", Toast.LENGTH_LONG).show();
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
                    String userLeft, userLeftName;
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
                                Constants.message.remove(i);
                                Constants.secretkey.remove(i);
                                Constants.notify.remove(i);
                                cancelCountDownTimer(userLeft);
                                break;
                            }
                            i++;
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
