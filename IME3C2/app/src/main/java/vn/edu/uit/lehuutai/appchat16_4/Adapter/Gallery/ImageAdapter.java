package vn.edu.uit.lehuutai.appchat16_4.Adapter.Gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.List;

import vn.edu.uit.lehuutai.appchat16_4.R;

/**
 * Created by lehuu on 5/12/2017.
 */

public class ImageAdapter extends ArrayAdapter<Image> {

    Context context;
    int resource;
    List<Image> objects;

    public ImageAdapter(Context context, int resource, List<Image> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = View.inflate(context, resource, null);
        ImageView imageView = (ImageView) view.findViewById(R.id.img_photo);
        Image image = objects.get(position);
        Bitmap bitmap = BitmapFactory.decodeFile(image.getImagePath());
        imageView.setImageBitmap(bitmap);

        return view;
    }
}
