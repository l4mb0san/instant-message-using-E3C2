package vn.edu.uit.lehuutai.appchat16_4;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;

import vn.edu.uit.lehuutai.appchat16_4.Adapter.Gallery.Image;
import vn.edu.uit.lehuutai.appchat16_4.Adapter.Gallery.ImageAdapter;

public class Gallery extends AppCompatActivity {

    Uri imageURI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    ArrayList<Image> imageList;
    ImageAdapter adapter;
    GridView gridViewGallery;
    String imagePath;
    String imageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        init();
        outputGallery();
        gridViewGallery.setOnItemClickListener(new GridView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = getIntent();
                intent.putExtra("imagePath", imageList.get(position).getImagePath());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void init() {
        imageList = new ArrayList<Image>();
        gridViewGallery = (GridView) findViewById(R.id.gridView_Gallery);
    }

    private void outputGallery() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(imageURI, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            imageName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
            Image image = new Image(imageName, imagePath);
            imageList.add(image);

            cursor.moveToNext();
        }
        adapter = new ImageAdapter(
                getApplicationContext(),
                R.layout.custom_gallery,
                imageList
        );
        gridViewGallery.setAdapter(adapter);
    }
}
