package com.scrip0.backgroundremover;

import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.scrip0.backremlib.BackActivity;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ViewGroup viewGroup;
    private BackActivity activity;
    private ImageView img;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewGroup = findViewById(R.id.preview_display_layout);
        img = findViewById(R.id.imgg);
        activity = new BackActivity(this, viewGroup);
//        activity.setVideo(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/Video.mp4");
        try {
            activity.setImage(BitmapFactory.decodeStream(getAssets().open("img.png")), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        activity.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        activity.pause();
    }
}