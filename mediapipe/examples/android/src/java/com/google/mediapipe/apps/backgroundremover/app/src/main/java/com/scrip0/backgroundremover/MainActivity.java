package com.scrip0.backgroundremover;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.scrip0.backremlib.BackActivity;

import java.io.IOException;

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

        activity.setVideo(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/Testt.mp4");
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