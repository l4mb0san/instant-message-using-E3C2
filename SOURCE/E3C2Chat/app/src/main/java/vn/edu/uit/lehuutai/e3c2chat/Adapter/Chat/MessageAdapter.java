package vn.edu.uit.lehuutai.e3c2chat.Adapter.Chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import vn.edu.uit.lehuutai.e3c2chat.R;

/**
 * Created by lehuu on 5/9/2017.
 */

public class MessageAdapter extends ArrayAdapter<Message> {

    Context context;

    public MessageAdapter(Context context, int resource, List<Message> objects) {
        super(context, resource, objects);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        Message message = getItem(position);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView =  inflater.inflate(R.layout.custom_chat, null);
            holder = createViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (message != null) {
            setAlignment(holder, message.isMe(), message.getmAvatar());
            holder.tvMessage.setText(message.getmMessage());
        }
        return convertView;
    }

    private void setAlignment(final ViewHolder holder, boolean isMe, final String avatar) {
        if (isMe) {
            holder.imgAvatar.setVisibility(View.INVISIBLE);
            //holder.contentWithBG.setBackgroundResource(R.drawable.in_message_bg);
            holder.contentWithBG.setBackgroundResource(R.drawable.sender_message_background);

            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            holder.contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            holder.content.setLayoutParams(lp);
            layoutParams = (LinearLayout.LayoutParams) holder.tvMessage.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            holder.tvMessage.setLayoutParams(layoutParams);
        } else {
            //Hiển thị ảnh từ một url
            if (holder.imgAvatar.getDrawable() == null) {
                final Bitmap[] bmp = new Bitmap[1];
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            //Hiển thị trực tiếp từ url
                            InputStream in = new URL(avatar).openStream();
                            bmp[0] = BitmapFactory.decodeStream(in);
                        } catch (Exception e) {
                            // log error
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        if (bmp[0] != null) {
                            holder.imgAvatar.setImageBitmap(bmp[0]);
                        }
                    }
                }.execute();
            }
            holder.imgAvatar.setVisibility(View.VISIBLE);
            //holder.contentWithBG.setBackgroundResource(R.drawable.out_message_bg);
            holder.contentWithBG.setBackgroundResource(R.drawable.reciever_message_background);

            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            holder.contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            holder.content.setLayoutParams(lp);
            layoutParams = (LinearLayout.LayoutParams) holder.tvMessage.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            holder.tvMessage.setLayoutParams(layoutParams);
        }
    }

    private ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.imgAvatar = (ImageView) v.findViewById(R.id.img_avatar);
        holder.tvMessage = (TextView) v.findViewById(R.id.tv_message);
        holder.content = (LinearLayout) v.findViewById(R.id.content);
        holder.contentWithBG = (LinearLayout) v.findViewById(R.id.contentWithBackground);
        return holder;
    }


    private static class ViewHolder {
        public ImageView imgAvatar;
        public TextView tvMessage;
        public LinearLayout content;
        public LinearLayout contentWithBG;
    }
}
