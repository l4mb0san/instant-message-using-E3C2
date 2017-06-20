package vn.edu.uit.lehuutai.appchat16_4.Adapter.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import vn.edu.uit.lehuutai.appchat16_4.R;

/**
 * Created by lehuu on 5/8/2017.
 */

public class UserAdapter extends ArrayAdapter<User> {

    Context context;

    public UserAdapter(Context context, int resource, List<User> objects) {
        super(context, resource, objects);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view =  inflater.inflate(R.layout.custom_list, null);
        }

        final User user = getItem(position);
        if (user != null) {
            /*Ánh xạ + Gán giá trị*/

            //Hiển thị avatar
            final ImageView imgAvatar = (ImageView) view.findViewById(R.id.img_avatar);
            if (imgAvatar.getDrawable() == null) {
                final Bitmap[] bmp = new Bitmap[1];
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            //Hiển thị trực tiếp từ url
                            InputStream in = new URL(user.getuAvatar()).openStream();
                            bmp[0] = BitmapFactory.decodeStream(in);
                        } catch (Exception e) {
                            // log error
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        if (bmp[0] != null) {
                            imgAvatar.setImageBitmap(bmp[0]);
                        }
                    }
                }.execute();
            }

            //Hiển thị tên người dùng
            TextView tvName = (TextView) view.findViewById(R.id.tv_name);
            tvName.setText(user.getuName());

            //Hiển thị Background của Notification, và số lượng tin nhắn
            TextView tvNotification = (TextView) view.findViewById(R.id.tv_notification);
            Integer numNotification = user.getuNumberNotification();
            if (numNotification != 0) {
                tvNotification.setText(String.valueOf(numNotification));
                tvNotification.setBackground(context.getDrawable(user.getuBackgroundNotification()));
            } else {
                int arrovv_right_icon = R.mipmap.ic_keyboard_arrow_right_cyan_24dp;
                tvNotification.setText("");
                tvNotification.setBackground(context.getDrawable(arrovv_right_icon));
            }

        }
        return view;
    }
}
