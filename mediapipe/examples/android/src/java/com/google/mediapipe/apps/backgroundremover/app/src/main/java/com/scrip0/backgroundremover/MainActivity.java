package com.scrip0.backgroundremover;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;

import com.scrip0.backremlib.BackActivity;

public class MainActivity extends AppCompatActivity {

    ViewGroup viewGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewGroup = findViewById(R.id.preview_display_layout);

        BackActivity activity = new BackActivity(this, viewGroup);
        String path = "img.png";
        Log.d("PATH", path);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                activity.setImage(path);
            }
        }, 3000);

    }
}