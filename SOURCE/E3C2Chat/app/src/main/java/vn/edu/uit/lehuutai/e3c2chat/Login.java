package vn.edu.uit.lehuutai.e3c2chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import vn.edu.uit.lehuutai.e3c2chat.Adapter.Chat.Message;
import vn.edu.uit.lehuutai.e3c2chat.Adapter.List.User;


public class Login extends AppCompatActivity {

    private Socket mSocket;
    private String Username;

    TextView tvChoosePhoto;
    TextView tvDeleteConversation;
    TextView tvLoginStatus;
    ImageView imgAvatar;
    EditText inputUsername;
    Button btnLogin;
    DiscreteSeekBar seekBar;

    public static final int CHOOSE_PHOTO_TAG = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Connection connect = new Connection();
        mSocket = connect.getSocket();
        mSocket.on(io.socket.client.Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(io.socket.client.Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.connect();

        //Login vào server
        mSocket.on("login", onLogin);
        mSocket.on("login-status", onLoginStatus);

        Constants.notify = new HashMap<String, Integer>();
        Constants.message = new HashMap<String, ArrayList<Message>>();
        Constants.secretkey = new HashMap<String, BigInteger>();
        Constants.userList = new ArrayList<User>();
        Constants.isTimerRunning = new HashMap<String, Boolean>();
        Constants.countDownTimer = new HashMap<String, CountDownTimer>();
        Constants.Q = new HashMap<>();

        tvChoosePhoto = (TextView) findViewById(R.id.tv_choose_photo);
        tvDeleteConversation = (TextView) findViewById(R.id.tv_delete_conversation);
        imgAvatar = (ImageView) findViewById(R.id.img_avatar);
        inputUsername = (EditText) findViewById(R.id.username_input);
        btnLogin = (Button) findViewById(R.id.sign_in_button);
        seekBar = (DiscreteSeekBar) findViewById(R.id.seekBar_delete_conversation);
        tvLoginStatus = (TextView) findViewById(R.id.tv_status_login);

        timeDelete(1);
        inputUsername.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    tvLoginStatus.setVisibility(View.INVISIBLE);
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mSocket.connected()){
                    tvLoginStatus.setText("Disconnected to the server!\nPlease restart this application.");
                    tvLoginStatus.setVisibility(View.VISIBLE);
                } else {
                    attemptLogin();
                }
            }
        });
        tvChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePicture();
                tvChoosePhoto.setEnabled(false);
                tvChoosePhoto.setVisibility(View.INVISIBLE);
            }
        });
        imgAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePicture();
            }
        });
        seekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                timeDelete(value);
                tvDeleteConversation.setText("\nTime to delete a conversation: " + value + " mins");
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });

    }

    //---------------------------------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.off("login", onLogin);
        mSocket.off("login-status", onLoginStatus);
    }

    //---------------------------------------------------------------------------
    private void attemptLogin() {
        // Reset errors.
        inputUsername.setError(null);
        // Store values at the time of the login attempt.
        String username = inputUsername.getText().toString().trim();

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            inputUsername.setError("This field is required");
            inputUsername.requestFocus();
            return;
        }
        Username = username;
        // perform the user login attempt.
        mSocket.emit("add user", username);
    }

    //---------------------------------------------------------------------------
    private Emitter.Listener onLogin = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    JSONObject jsonList, jsonData;
                    String SOCKETID = null, NAME, AVATAR;

                    try {
                        jsonList = data.getJSONObject("usersList");
                        JSONArray names = jsonList.names();
                        JSONArray values = jsonList.toJSONArray(names);
                        for (int i = 0; i < values.length(); i++) {
                            jsonData = values.getJSONObject(i);
                            SOCKETID = jsonData.getString("ID");
                            NAME = jsonData.getString("NAME");
                            AVATAR = jsonData.getString("AVATAR");
                            //sử dụng equal để so sánh chuỗi, == sẽ so sánh địa chỉ vùng nhớ
                            //bỏ tên mình ra khỏi mảng danh sách những người online
                            if (!NAME.equals(Username)) {
                                Constants.userList.add(new User(SOCKETID, NAME, AVATAR));
                            }
                        }
                        Intent intent = new Intent(Login.this, List.class);
                        intent.putExtra("username", SOCKETID);
                        startActivity(intent);
                        finish();
                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    //---------------------------------------------------------------------------
    private Emitter.Listener onLoginStatus = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    Boolean status;

                    try {
                        status = data.getBoolean("status");
                        if(status == true){
                            tvLoginStatus.setText("Success");
                        }else{
                            tvLoginStatus.setText("Someone used this name.\nPlease choose a cooler name");
                        }
                        tvLoginStatus.setVisibility(View.VISIBLE);

                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    //----------------------------------------------------------------------------------------
    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Login.this.getApplicationContext(),
                            "Failed to connect", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    //----------------------------------------------------------------------------------------
    //Thời gian để xóa một cuộc trò chuyện
    private void timeDelete(int time) {
        Constants.timer = time*60*1000;
    }

    //----------------------------------------------------------------------------------------
    private void choosePicture() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");
        startActivityForResult(getIntent, CHOOSE_PHOTO_TAG);
    }

    //----------------------------------------------------------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_PHOTO_TAG && resultCode == RESULT_OK) {
            InputStream is = null;
            Bitmap bm = null;
            try {
                is = getContentResolver().openInputStream(data.getData());
                bm = BitmapFactory.decodeStream(is);
                is.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            imgAvatar.setImageBitmap(bm);
            bm = resize(bm, 100, 100);
            byte[] bytes = getByteArrayFromBitmap(bm);
            mSocket.emit("client send avatar", bytes);
        }
    }

    //----------------------------------------------------------------------------------------
    private byte[] getByteArrayFromBitmap(Bitmap bm){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    //----------------------------------------------------------------------------------------
    private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }


}
