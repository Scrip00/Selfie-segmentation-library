package com.scrip0.backgroundremover;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;

import com.scrip0.backremlib.BackActivity;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private ViewGroup viewGroup;
    private BackActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewGroup = findViewById(R.id.preview_display_layout);

        activity = new BackActivity(this, viewGroup);
        Bitmap background = null;
        try {
            InputStream ims = getAssets().open("img.png");
            background = BitmapFactory.decodeStream(ims);
            Handler handler = new Handler();
            Bitmap finalBackground = background;
            handler.postDelayed(new Runnable() {
                public void run() {
                    activity.setImage(finalBackground);
                    Log.d("SETTT", "SET");
                }
            }, 3000);
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