package vn.edu.uit.lehuutai.appchat16_4;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hitomi.cmlibrary.CircleMenu;
import com.hitomi.cmlibrary.OnMenuSelectedListener;
import com.hitomi.cmlibrary.OnMenuStatusChangeListener;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

public class Test extends AppCompatActivity {

    CircleMenu circleMenu;
    boolean hide = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        circleMenu = (CircleMenu) findViewById(R.id.circle_menu);
        circleMenu.setMainMenu(Color.parseColor("#02d2a4"), R.mipmap.ic_menu_white, R.mipmap.icon_cancel);
        circleMenu.addSubMenu(Color.parseColor("#258CFF"), R.mipmap.ic_exit_to_app_white)
                .addSubMenu(Color.parseColor("#30A400"), R.mipmap.ic_go_back)
                .addSubMenu(Color.parseColor("#FF4B32"), R.mipmap.icon_notify)
                .addSubMenu(Color.parseColor("#FF6A00"), R.mipmap.icon_home);


        circleMenu.setOnMenuSelectedListener(new OnMenuSelectedListener() {
            @Override
            public void onMenuSelected(int index) {
                switch (index) {
                    case 0:
                        Toast.makeText(Test.this, "Exit Button Clicked", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        Toast.makeText(Test.this, "Back button Clicked", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(Test.this, "Notify button Clicked", Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        Toast.makeText(Test.this, "Home Button Clicked", Toast.LENGTH_SHORT).show();
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

        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.activity_test);
        relativeLayout.setOnLongClickListener(new View.OnLongClickListener() {
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

        // in Activity Context
        ImageView icon = new ImageView(this); // Create an icon
        icon.setImageDrawable(getDrawable(R.mipmap.ic_launcher));

        FloatingActionButton actionButton = new FloatingActionButton.Builder(this)
                .setContentView(icon)
                .build();

        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);
// repeat many times:
        ImageView itemIcon1 = new ImageView(this);
        itemIcon1.setImageDrawable(getDrawable(R.mipmap.ic_launcher));
        SubActionButton button1 = itemBuilder.setContentView(itemIcon1).build();
        button1.setBackground(getDrawable(R.drawable.notify));

        ImageView itemIcon2 = new ImageView(this);
        itemIcon2.setImageDrawable(getDrawable(R.mipmap.ic_launcher));
        SubActionButton button2 = itemBuilder.setContentView(itemIcon2).build();

        FloatingActionMenu actionMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(button1)
                .addSubActionView(button2)
                // ...
                .attachTo(actionButton)
                .build();


    }

    @Override
    public void onBackPressed() {
        if (circleMenu.isOpened())
            circleMenu.closeMenu();
        else
            finish();
    }
}
