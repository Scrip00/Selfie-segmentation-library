package com.scrip0.backgroundremover;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;

import com.scrip0.backremlib.BackActivity;

public class MainActivity extends AppCompatActivity {

    private ViewGroup viewGroup;
    private BackActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewGroup = findViewById(R.id.preview_display_layout);

        activity = new BackActivity(this, viewGroup);
        String path = "img.png";
        Log.d("PATH", path);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                activity.setImage(path);
            }
        }, 10000);

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