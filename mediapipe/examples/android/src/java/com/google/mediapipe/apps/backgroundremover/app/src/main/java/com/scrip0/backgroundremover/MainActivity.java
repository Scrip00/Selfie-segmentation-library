package com.scrip0.backgroundremover;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.scrip0.backremlib.BackActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private ViewGroup viewGroup;
    private BackActivity activity;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewGroup = findViewById(R.id.preview_display_layout);
        activity = new BackActivity(this, viewGroup);
        activity.setVideo(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/Helpme.mp4", true);
        try {
            activity.setImageBackground(BitmapFactory.decodeStream(getAssets().open("img.png")), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        activity.setColor(Color.BLUE);
        activity.setImageBackground(null, false);
        activity.partymode();
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